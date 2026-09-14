param(
    [string]$Base = "http://127.0.0.1:8080",
    [string]$OutFile = "build/http-e2e.log"
)

# =============================================================================
#  校园报修系统 · 端到端接口验证脚本
#  验证内容：登录鉴权、未登录/越权拦截、报修闭环（提交→审核→派单→接单→维修→
#            耗材扣减与回滚→完成→确认→评价）、统计分析、账户审核与基础数据、消息通知
#  说明：请求体采用 application/x-www-form-urlencoded 提交（服务端 ParamMap 同时
#        支持表单与 JSON 两种格式，前端页面使用 JSON）。
# =============================================================================

[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$log = New-Object System.Collections.Generic.List[string]
$script:passed = 0
$script:failed = 0

function Log([string]$text) {
    $log.Add($text)
    Write-Host $text
}

function Check([string]$name, [bool]$condition, [string]$detail) {
    if ($condition) {
        $script:passed++
        $suffix = if ($detail) { "（" + $detail + "）" } else { "" }
        Log ("  [OK] " + $name + $suffix)
    } else {
        $script:failed++
        $suffix = if ($detail) { "（" + $detail + "）" } else { "" }
        Log ("  [FAIL] " + $name + $suffix)
    }
}

function Form([hashtable]$data) {
    $parts = @()
    foreach ($key in $data.Keys) {
        $value = [string]$data[$key]
        if ($value -eq "") { continue }
        $parts += ([uri]::EscapeDataString($key) + "=" + [uri]::EscapeDataString($value))
    }
    return ($parts -join "&")
}

function NewSession([string]$token) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    if ($token) {
        $hostName = ([uri]$Base).Host
        $cookie = New-Object System.Net.Cookie("CRS_TOKEN", $token, "/", $hostName)
        $session.Cookies.Add($cookie)
    }
    return $session
}

function JsonBody($data) {
    if (-not $data) { return "{}" }
    # 全部值按字符串输出：服务端 ParamMap 会把字符串再解析为所需类型，
    # 这样可避免数字型口令/编号被误当作 JSON 数字（如 123456 → 123456 数字）。
    $parts = @()
    $quote = [string][char]34
    foreach ($key in $data.Keys) {
        $value = [string]$data[$key]
        $escaped = $value.Replace('\', '\\')
        $escaped = $escaped.Replace("`r", '\r').Replace("`n", '\n').Replace("`t", '\t')
        $parts += ($quote + $key + $quote + ':' + $quote + $escaped + $quote)
    }
    return ('{' + ($parts -join ',') + '}')
}

function Api([string]$method, [string]$path, $data, $session) {
    $uri = $Base + $path
    if ($method -eq "GET") {
        $query = Form $data
        if ($query) { $uri = $uri + "?" + $query }
        $options = @{ Uri = $uri; Method = "Get"; TimeoutSec = 20; UseBasicParsing = $true }
        if ($session) { $options["WebSession"] = $session }
        return Invoke-RestMethod @options
    }
    $json = JsonBody $data
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $options = @{ Uri = $uri; Method = "Post"; TimeoutSec = 20; UseBasicParsing = $true
                  ContentType = "application/json; charset=UTF-8"; Body = $bytes }
    if ($session) { $options["WebSession"] = $session }
    return Invoke-RestMethod @options
}

function Login([string]$username, [string]$password) {
    $res = Api "POST" "/api/account/login" @{ username = $username; password = $password } $null
    if ($res.code -ne 0) { throw "登录失败：$username -> $($res.message)" }
    return (NewSession $res.data.token)
}

function Section([string]$title) { Log ""; Log ("---- " + $title + " ----") }

Log "==================== 校园报修系统 · 端到端接口验证 ===================="
Log ("目标服务：" + $Base)

Section "1. 系统信息与静态资源"
$info = Api "GET" "/api/system/info" $null $null
Check "系统信息接口可用" ($info.code -eq 0) ("存储模式 " + $info.data.storageMode)
$page = Invoke-WebRequest -Uri ($Base + "/index.html") -UseBasicParsing -TimeoutSec 20
Check "登录页静态资源可访问" ($page.StatusCode -eq 200)
$page2 = Invoke-WebRequest -Uri ($Base + "/js/common.js") -UseBasicParsing -TimeoutSec 20
Check "前端脚本可访问" ($page2.StatusCode -eq 200 -and $page2.Content.Length -gt 1000)
$page3 = Invoke-WebRequest -Uri ($Base + "/css/style.css") -UseBasicParsing -TimeoutSec 20
Check "前端样式可访问" ($page3.StatusCode -eq 200)
$pageOk = $true
foreach ($view in @("/home.html", "/report.html", "/report-submit.html", "/report-detail.html", "/task.html",
        "/dispatch.html", "/audit.html", "/monitor.html", "/statistics.html", "/evaluation.html",
        "/material.html", "/admin.html", "/base-data.html", "/messages.html", "/register.html")) {
    $resp = Invoke-WebRequest -Uri ($Base + $view) -UseBasicParsing -TimeoutSec 20
    if ($resp.StatusCode -ne 200) { $pageOk = $false }
}
Check "全部 15 个功能页面可访问" $pageOk

Section "2. 角色登录"
$reporter = Login "student" "123456"
Check "报修人（student）登录成功" ($null -ne $reporter)
$teacher = Login "teacher" "123456"
Check "报修人（teacher）登录成功" ($null -ne $teacher)
$manager = Login "manager" "manager123"
Check "维修管理员（manager）登录成功" ($null -ne $manager)
$admin = Login "admin" "admin123"
Check "系统管理员（admin）登录成功" ($null -ne $admin)
$workers = @{}
foreach ($w in @("worker01", "worker02", "worker03")) { $workers[$w] = Login $w "worker123" }
Check "3 名维修人员登录成功" ($workers.Count -eq 3)
$bad = Api "POST" "/api/account/login" @{ username = "student"; password = "wrong" } $null
Check "错误密码被拒绝" ($bad.code -ne 0) $bad.message
$noUser = Api "POST" "/api/account/login" @{ username = "not_exist"; password = "whatever" } $null
Check "不存在的用户被拒绝" ($noUser.code -ne 0) $noUser.message

Section "3. 未登录与越权访问拦截"
$anon = Api "GET" "/api/report/list" $null $null
Check "未登录访问报修列表被拦截" ($anon.code -eq 1003) $anon.message
$forbidden = Api "POST" "/api/dispatch/manual" @{ orderId = "1"; workerId = "1" } $reporter
Check "报修人调用派单接口被拒绝" ($forbidden.code -eq 1004) $forbidden.message
$forbidden2 = Api "GET" "/api/stat/report" $null $workers["worker01"]
Check "维修人员访问统计接口被拒绝" ($forbidden2.code -eq 1004) $forbidden2.message
$forbidden3 = Api "POST" "/api/account/list" @{} $manager
Check "维修管理员访问账户管理被拒绝" ($forbidden3.code -eq 1004) $forbidden3.message

Section "4. 提交报修（报修人）"
$options = Api "GET" "/api/report/options" $null $reporter
Check "报修表单可选项加载成功" ($options.code -eq 0 -and $options.data.buildings.Count -gt 0) ("楼栋 " + $options.data.buildings.Count + " 个")
$submit = Api "POST" "/api/report/submit" @{
    building = "实验楼"; floor = "2"; room = "215"; category = "水电"
    description = "E2E 验证：走廊灯不亮，需更换灯管"; priority = "1"
} $reporter
Check "提交报修成功" ($submit.code -eq 0) $submit.message
$orderId = $submit.data.orderId
Check "报修单状态为待审核" ($submit.data.status -eq 0) ("报修单号 #" + $orderId)
$badBuilding = Api "POST" "/api/report/submit" @{
    building = "不存在的楼栋"; floor = "1"; room = "101"; category = "水电"; description = "非法楼栋测试"
} $reporter
Check "非系统维护楼栋被拒绝" ($badBuilding.code -ne 0) $badBuilding.message
$emptyDesc = Api "POST" "/api/report/submit" @{
    building = "实验楼"; floor = "1"; room = "101"; category = "水电"; description = ""
} $reporter
Check "必填项为空被拒绝" ($emptyDesc.code -ne 0) $emptyDesc.message

Section "5. 报修审核（维修管理员）"
$selfAudit = Api "POST" "/api/report/audit" @{ orderId = $orderId; accept = "true" } $reporter
Check "报修人无权审核报修单" ($selfAudit.code -eq 1004) $selfAudit.message
$audit = Api "POST" "/api/report/audit" @{ orderId = $orderId; accept = "true"; priority = "1" } $manager
Check "审核通过并进入待派单" ($audit.code -eq 0 -and $audit.data.status -eq 1) $audit.message

Section "6. 智能派单匹配度计算（calcMatchScore）"
$candidates = Api "GET" ("/api/dispatch/candidates?orderId=" + $orderId) $null $manager
$top = $candidates.data.candidates[0]
Check "候选维修人员匹配度已计算" ($candidates.code -eq 0 -and $candidates.data.candidates.Count -ge 3) ("首位：" + $top.worker.name + " " + $top.totalScore + " 分")
Check "技能匹配得分大于 0" ($top.skillScore -gt 0) ("技能得分 " + $top.skillScore + "/40")
Check "总分不超过 100" ($top.totalScore -le 100) ("总分 " + $top.totalScore)
Check "推荐理由可展示" ($top.reasons.Count -gt 0) ($top.reasons -join "；")

Section "7. 智能派单与接单"
$auto = Api "POST" "/api/dispatch/auto" @{ orderId = $orderId } $manager
Check "智能派单成功" ($auto.code -eq 0) $auto.message
$taskId = $auto.data.task.taskId
$workerName = $auto.data.task.workerName
$workerUser = switch ($workerName) { "赵强" { "worker01" } "孙勇" { "worker02" } "周涛" { "worker03" } default { "worker01" } }
$worker = $workers[$workerUser]
Check "维修任务已创建且状态为待接单" ($auto.data.task.status -eq 0) ("任务号 #" + $taskId + "，派给 " + $workerName)
$orderAfterDispatch = Api "GET" ("/api/report/detail?orderId=" + $orderId) $null $manager
Check "报修单状态变为已派单" ($orderAfterDispatch.data.order.status -eq 2) $orderAfterDispatch.data.order.statusText
$dupDispatch = Api "POST" "/api/dispatch/auto" @{ orderId = $orderId } $manager
Check "已派单的报修单不能重复派单" ($dupDispatch.code -ne 0) $dupDispatch.message
$accept = Api "POST" "/api/dispatch/accept" @{ taskId = $taskId } $worker
Check "接单成功，任务状态为维修中" ($accept.code -eq 0 -and $accept.data.status -eq 1) $accept.message
$otherWorker = if ($workerUser -eq "worker03") { "worker02" } else { "worker03" }
$wrongTask = Api "POST" "/api/repair/start" @{ taskId = $taskId } $workers[$otherWorker]
Check "非本人任务不能操作" ($wrongTask.code -eq 1004) $wrongTask.message

Section "8. 维修处理、进度反馈与耗材扣减"
$start = Api "POST" "/api/repair/start" @{ taskId = $taskId; remark = "已到现场" } $worker
Check "开始维修记录开始时间" ($start.code -eq 0 -and $start.data.startTime) $start.message
$feedback = Api "POST" "/api/repair/feedback" @{ taskId = $taskId; content = "正在检查线路" } $worker
Check "进度反馈成功" ($feedback.code -eq 0) $feedback.message
$delay = Api "POST" "/api/repair/feedback" @{ taskId = $taskId; delayReason = "需更换整段线路" } $worker
Check "延期原因反馈成功" ($delay.code -eq 0) $delay.message
$matList = Api "GET" "/api/material/list" $null $worker
$led = $matList.data.materials | Where-Object { $_.matName -eq "LED灯管" }
$stockBefore = $led.stock
$over = Api "POST" "/api/repair/material" @{ taskId = $taskId; matId = $led.matId; useCount = ($stockBefore + 999) } $worker
Check "超库存登记被拒绝" ($over.code -ne 0) $over.message
$matAfterFail = Api "GET" "/api/material/list" $null $worker
$ledAfterFail = $matAfterFail.data.materials | Where-Object { $_.matName -eq "LED灯管" }
Check "扣减失败后库存保持不变" ($ledAfterFail.stock -eq $stockBefore) ("库存 " + $ledAfterFail.stock)
$use = Api "POST" "/api/repair/material" @{ taskId = $taskId; matId = $led.matId; useCount = "2" } $worker
Check "耗材登记成功" ($use.code -eq 0) $use.message
$matList2 = Api "GET" "/api/material/list" $null $worker
$led2 = $matList2.data.materials | Where-Object { $_.matName -eq "LED灯管" }
Check "库存按登记数量扣减" ($led2.stock -eq ($stockBefore - 2)) ("库存 " + $stockBefore + " → " + $led2.stock)
$noResult = Api "POST" "/api/repair/finish" @{ taskId = $taskId; result = "" } $worker
Check "维修结果为空不能提交完成" ($noResult.code -ne 0) $noResult.message
$finish = Api "POST" "/api/repair/finish" @{ taskId = $taskId; result = "更换 LED 灯管 2 支，线路复测正常。" } $worker
Check "维修完成登记成功，状态为待确认" ($finish.code -eq 0 -and $finish.data.status -eq 2) $finish.message

Section "9. 结果确认与耗材回滚"
$reject = Api "POST" "/api/repair/confirm" @{ orderId = $orderId; pass = "false"; reason = "灯管亮度不足，请重新处理" } $reporter
Check "确认不通过退回重新处理" ($reject.code -eq 0 -and $reject.data.status -eq 3) $reject.message
$matList3 = Api "GET" "/api/material/list" $null $worker
$led3 = $matList3.data.materials | Where-Object { $_.matName -eq "LED灯管" }
Check "不通过后耗材库存已回滚" ($led3.stock -eq $stockBefore) ("库存恢复到 " + $led3.stock)
$reRegister = Api "POST" "/api/repair/material" @{ taskId = $taskId; matId = $led.matId; useCount = "2" } $worker
Check "重新登记耗材成功" ($reRegister.code -eq 0) $reRegister.message
$finish2 = Api "POST" "/api/repair/finish" @{ taskId = $taskId; result = "更换高亮 LED 灯管并复测合格。" } $worker
Check "再次完成登记成功" ($finish2.code -eq 0) $finish2.message
$confirm = Api "POST" "/api/repair/confirm" @{ orderId = $orderId; pass = "true" } $reporter
Check "确认通过，报修单归档" ($confirm.code -eq 0 -and $confirm.data.status -eq 5) $confirm.message
$workerTasks = Api "GET" "/api/dispatch/myTasks?status=3" $null $worker
Check "维修人员可查询已完成任务" ($workerTasks.code -eq 0 -and $workerTasks.data.Count -ge 1) ("已完成任务 " + $workerTasks.data.Count + " 个")

Section "10. 评价与评价审核"
$eval = Api "POST" "/api/eval/submit" @{ orderId = $orderId; score = "5"; comment = "E2E 验证：响应及时，维修质量好" } $reporter
Check "提交评价成功" ($eval.code -eq 0) $eval.message
$evalId = $eval.data.evalId
$dup = Api "POST" "/api/eval/submit" @{ orderId = $orderId; score = "4"; comment = "重复评价" } $reporter
Check "重复评价被拒绝" ($dup.code -ne 0) $dup.message
$auditEval = Api "POST" "/api/stat/auditEval" @{ evalId = $evalId; auditStatus = "1" } $manager
Check "评价审核通过" ($auditEval.code -eq 0) $auditEval.message
$reply = Api "POST" "/api/eval/reply" @{ evalId = $evalId; reply = "感谢您的评价，我们会持续改进。" } $manager
Check "管理员回复评价成功" ($reply.code -eq 0) $reply.message

Section "11. 统计分析接口"
$stat = Api "GET" "/api/stat/report" $null $manager
Check "统计报表接口返回成功" ($stat.code -eq 0) ("报修总量 " + $stat.data.overview.total + "，完成率 " + $stat.data.overview.finishRate + "%")
Check "故障类型分析有数据" ($stat.data.category.Count -gt 0) ("类别 " + $stat.data.category.Count + " 个")
Check "维修人员绩效有数据" ($stat.data.worker.Count -gt 0) ("维修工 " + $stat.data.worker.Count + " 人")
Check "耗材成本统计大于 0" ([double]$stat.data.materialCost -gt 0) ("成本 " + $stat.data.materialCost + " 元")
Check "状态分布数据完整" ($stat.data.overview.statusDistribution.Count -eq 8)
Check "楼栋分布有数据" ($stat.data.building.Count -gt 0)
Check "月度趋势有数据" ($stat.data.monthly.Count -gt 0)
$avg = Api "GET" "/api/eval/statistics" $null $manager
Check "评价统计接口可用" ($avg.code -eq 0 -and $avg.data.avgScore -gt 0) ("平均分 " + $avg.data.avgScore)

Section "12. 报修人业务规则（撤销与催办）"
$order2 = Api "POST" "/api/report/submit" @{
    building = "图书馆"; floor = "1"; room = "大厅"; category = "网络"; description = "E2E 验证：无线网络无法连接"
} $reporter
Check "再次提交报修成功" ($order2.code -eq 0) ("报修单号 #" + $order2.data.orderId)
$urgeEarly = Api "POST" "/api/report/urge" @{ orderId = $order2.data.orderId; reason = "未审核即催办" } $reporter
Check "待审核状态不可催办" ($urgeEarly.code -ne 0) $urgeEarly.message
$cancel = Api "POST" "/api/report/cancel" @{ orderId = $order2.data.orderId; reason = "E2E 验证后撤销" } $reporter
Check "待审核状态可撤销" ($cancel.code -eq 0 -and $cancel.data.status -eq 6) $cancel.message
$cancelAgain = Api "POST" "/api/report/cancel" @{ orderId = $orderId; reason = "已完成的单不能撤销" } $reporter
Check "已完成的报修单不可撤销" ($cancelAgain.code -ne 0) $cancelAgain.message
$otherOrder = Api "POST" "/api/report/cancel" @{ orderId = "1"; reason = "越权撤销" } $teacher
Check "他人报修单不能撤销" ($otherOrder.code -eq 1004) $otherOrder.message
$teacherOrders = Api "GET" "/api/report/list" @{ pageSize = "50" } $teacher
$onlyMine = $true
foreach ($row in $teacherOrders.data.rows) { if ($row.reporterUsername -ne "teacher") { $onlyMine = $false } }
Check "报修人只能查询本人报修单" $onlyMine ("共 " + $teacherOrders.data.rows.Count + " 条")

Section "13. 系统管理员功能"
# 本次运行的唯一测试账号（持久化模式下重复执行不会与既有数据冲突）
$stamp = Get-Date -Format "MMddHHmmss"
$e2eUser = "e2e_" + $stamp
$e2eStudentNo = "E2E" + $stamp
# 手机号需满足 1[3-9] 后跟 9 位数字：用运行时刻的时分秒毫秒补足
$e2ePhone = "139" + (Get-Date).ToString("HHmmssfff").Substring(1, 8)
# 资料修改用的手机号同样必须唯一：写死常量会让脚本第二次运行就撞上上次运行的账号
# （表现为"该手机号已被其他账户使用"），这里用不同号段再次按运行时刻生成
$e2eProfilePhone = "137" + (Get-Date).ToString("HHmmssfff").Substring(1, 8)
$users = Api "GET" "/api/account/list" $null $admin
Check "账户列表查询成功" ($users.code -eq 0 -and $users.data.users.Count -gt 0) ("账户 " + $users.data.users.Count + " 个")
Check "在线会话统计可用" ($users.data.online -gt 0) ("在线 " + $users.data.online)
$register = Api "POST" "/api/account/register" @{
    username = $e2eUser; password = "e2epass123"; realName = "验证学生"
    studentNo = $e2eStudentNo; phone = $e2ePhone; role = "reporter"
} $null
Check "新用户注册成功并进入待审核" ($register.code -eq 0) $register.message
$dupRegister = Api "POST" "/api/account/register" @{
    username = $e2eUser; password = "e2epass123"; realName = "重复用户"
    studentNo = $e2eStudentNo + "X"; phone = $e2ePhone + "9"; role = "reporter"
} $null
Check "重复用户名注册被拒绝" ($dupRegister.code -ne 0) $dupRegister.message
$pending = Api "GET" "/api/account/pending" $null $admin
$target = $pending.data | Where-Object { $_.username -eq $e2eUser }
Check "待审核列表包含新注册用户" ($null -ne $target)
$loginBeforeAudit = Api "POST" "/api/account/login" @{ username = $e2eUser; password = "e2epass123" } $null
Check "未审核账户无法登录" ($loginBeforeAudit.code -ne 0) $loginBeforeAudit.message
$auditUser = Api "POST" "/api/account/audit" @{ userId = $target.userId; auditStatus = "1" } $admin
Check "实名审核通过" ($auditUser.code -eq 0) $auditUser.message
$newSession = Login $e2eUser "e2epass123"
Check "审核通过后可以登录" ($null -ne $newSession)
$notify = Api "GET" "/api/message/list" $null $newSession
$hasAuditMsg = $false
foreach ($m in $notify.data.messages) { if ($m.msgType -eq "审核") { $hasAuditMsg = $true } }
Check "用户收到审核结果通知" $hasAuditMsg
$lock = Api "POST" "/api/account/lock" @{ userId = $target.userId; locked = "true" } $admin
Check "锁定账户成功" ($lock.code -eq 0) $lock.message
$lockedLogin = Api "POST" "/api/account/login" @{ username = $e2eUser; password = "e2epass123" } $null
Check "锁定后无法登录" ($lockedLogin.code -ne 0) $lockedLogin.message
$unlock = Api "POST" "/api/account/lock" @{ userId = $target.userId; locked = "false" } $admin
Check "解锁账户成功" ($unlock.code -eq 0) $unlock.message
$baseData = Api "POST" "/api/account/saveBaseData" @{ type = "building"; value = "E2E测试楼"; sortNo = "99" } $admin
Check "新增基础数据成功" ($baseData.code -eq 0) $baseData.message
$baseDataList = Api "GET" "/api/account/baseData?type=building" $null $admin
Check "新增的楼栋出现在基础数据中" ($baseDataList.data.values -contains "E2E测试楼")
$delBase = Api "POST" "/api/account/saveBaseData" @{ type = "building"; value = "E2E测试楼"; delete = "true" } $admin
Check "删除基础数据成功" ($delBase.code -eq 0) $delBase.message

Section "14. 账户信息维护（表 1.4）"
$profile = Api "GET" "/api/account/profile" $null $newSession
Check "个人信息查询成功" ($profile.code -eq 0) ($profile.data.roleText)
$badPhone = Api "POST" "/api/account/modifyInfo" @{ phone = "12345"; realName = "验证学生" } $newSession
Check "手机号格式校验生效" ($badPhone.code -ne 0) $badPhone.message
$modify = Api "POST" "/api/account/modifyInfo" @{ phone = $e2eProfilePhone; realName = "验证学生" } $newSession
Check "修改账户信息成功" ($modify.code -eq 0) ($modify.message + "（新手机号 " + $e2eProfilePhone + "）")
$wrongOld = Api "POST" "/api/account/modifyPassword" @{ oldPassword = "wrong"; newPassword = "newpass456" } $newSession
Check "原密码错误被拒绝" ($wrongOld.code -ne 0) $wrongOld.message
$modifyPwd = Api "POST" "/api/account/modifyPassword" @{ oldPassword = "e2epass123"; newPassword = "newpass456"; confirmPassword = "newpass456" } $newSession
Check "修改密码成功" ($modifyPwd.code -eq 0) $modifyPwd.message
$newLogin = Api "POST" "/api/account/login" @{ username = $e2eUser; password = "newpass456" } $null
Check "新密码可以登录" ($newLogin.code -eq 0)

Section "15. 消息通知"
$messages = Api "GET" "/api/message/list" $null $reporter
Check "报修人可查询消息通知" ($messages.code -eq 0 -and $messages.data.messages.Count -gt 0) ("消息 " + $messages.data.messages.Count + " 条，未读 " + $messages.data.unread)
$readAll = Api "POST" "/api/message/readAll" @{} $reporter
Check "全部标记已读成功" ($readAll.code -eq 0) $readAll.message
$unread = Api "GET" "/api/message/unread" $null $reporter
Check "标记后未读数为 0" ($unread.data -eq 0)

Section "16. 维修监督与耗材管理"
$overdue = Api "GET" "/api/report/overdue" $null $manager
Check "超时监督接口可用" ($overdue.code -eq 0) ("超时未接单 " + $overdue.data.overdueTasks.Count + " 条，超时未确认 " + $overdue.data.overdueConfirms.Count + " 条")
$recalc = Api "POST" "/api/dispatch/recalculate" @{} $manager
Check "在单量校正成功" ($recalc.code -eq 0) $recalc.message
$remind = Api "POST" "/api/dispatch/remind" @{} $manager
Check "超时提醒执行成功" ($remind.code -eq 0) $remind.message
$saveMaterial = Api "POST" "/api/material/save" @{ matName = "E2E测试耗材"; spec = "T1"; stock = "10"; unitPrice = "3.50" } $manager
Check "新增耗材成功" ($saveMaterial.code -eq 0) $saveMaterial.message
$newMatId = $saveMaterial.data.matId
$replenish = Api "POST" "/api/material/addStock" @{ matId = $newMatId; count = "5" } $manager
Check "耗材补库成功" ($replenish.code -eq 0 -and $replenish.data.stock -eq 15) $replenish.message
$delMaterial = Api "POST" "/api/material/delete" @{ matId = $newMatId } $manager
Check "删除耗材成功" ($delMaterial.code -eq 0) $delMaterial.message
$workerLocation = Api "POST" "/api/repair/status" @{ status = "1"; location = "E2E测试位置" } $worker
Check "维修工更新在线状态与位置" ($workerLocation.code -eq 0) $workerLocation.message

Log ""
Log "==================== 端到端验证结果 ===================="
Log ("通过：" + $script:passed + " 项，失败：" + $script:failed + " 项")
if ($script:failed -gt 0) {
    $log | Out-File -FilePath $OutFile -Encoding utf8
    exit 1
}
Log "全部接口与业务规则验证通过 -> PASS"
$log | Out-File -FilePath $OutFile -Encoding utf8
exit 0

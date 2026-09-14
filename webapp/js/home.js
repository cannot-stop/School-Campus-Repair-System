/* 工作台脚本：按角色加载统计卡片与待处理事项（对应设计书 1.1.1 各角色职责） */
(function () {
    var me = App.shell({ key: 'home', title: '我的工作台' });
    if (!me) { return; }

    document.getElementById('greeting').textContent =
        '你好，' + (me.realName || me.username) + '（' + App.roleText(me.role) + '）。' +
        '当前登录时间 ' + App.formatDate(new Date().toISOString());

    if (me.role === 'reporter') {
        loadReporter();
    } else if (me.role === 'worker') {
        loadWorker();
    } else if (me.role === 'manager') {
        loadManager();
    } else {
        loadAdmin();
    }

    /* ------------------------------------------------------------ 报修人 */
    function loadReporter() {
        document.getElementById('listTitle').textContent = '我的最新报修单';
        document.getElementById('listTip').textContent = '仅展示最近 8 条，完整列表请进入「我的报修」';
        App.api('/api/report/statistics', {}).done(function (data) {
            renderStats([
                { label: '报修总数', value: data.total },
                { label: '处理中', value: data.processing, cls: 'warn' },
                { label: '已完成', value: data.finished, cls: 'success' },
                { label: '已撤销 / 驳回', value: data.canceled }
            ]);
        });
        App.api('/api/report/list', { pageSize: 8 }).done(function (data) {
            document.getElementById('listHead').innerHTML =
                '<tr><th>报修单号</th><th>报修地点</th><th>类别</th><th>故障描述</th>' +
                '<th>状态</th><th>维修人员</th><th>催办</th><th>提交时间</th></tr>';
            App.renderRows('listBody', data.rows, function (row) {
                return '<tr>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.escapeHtml(row.category) + '</td>' +
                    '<td>' + App.escapeHtml((row.description || '').substring(0, 24)) + '</td>' +
                    '<td>' + App.orderStatusTag(row.status) + '</td>' +
                    '<td>' + App.escapeHtml(row.workerName || '—') + '</td>' +
                    '<td>' + (row.urgeCount || 0) + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '</tr>';
            }, 8);
        });
    }

    /* ---------------------------------------------------------- 维修人员 */
    function loadWorker() {
        document.getElementById('listTitle').textContent = '待处理的维修任务';
        document.getElementById('listTip').textContent = '接单时限：派单后 2 小时；请及时反馈进度';
        App.api('/api/repair/statistics', {}).done(function (data) {
            renderStats([
                { label: '待接单', value: data.waiting, cls: 'warn' },
                { label: '维修中', value: data.repairing },
                { label: '待确认', value: data.pendingConfirm },
                { label: '已完成', value: data.finished, cls: 'success' },
                { label: '平均评分', value: data.avgScore ? data.avgScore.toFixed(1) : '—' },
                { label: '在单量', value: data.worker ? data.worker.currentOrders : 0 }
            ]);
        });
        App.api('/api/dispatch/myTasks', {}).done(function (rows) {
            var pending = rows.filter(function (row) { return row.status === 0 || row.status === 1; });
            document.getElementById('listHead').innerHTML =
                '<tr><th>任务号</th><th>报修单号</th><th>地点</th><th>类别</th><th>紧急</th>' +
                '<th>任务状态</th><th>派单时间</th><th>操作</th></tr>';
            App.renderRows('listBody', pending.slice(0, 8), function (row) {
                return '<tr>' +
                    '<td>#' + row.taskId + '</td>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.location || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.category || '') + '</td>' +
                    '<td>' + (row.priority === 1 ? '<span class="tag tag-red">紧急</span>' : '<span class="tag tag-gray">普通</span>') + '</td>' +
                    '<td>' + App.taskStatusTag(row.status) + '</td>' +
                    '<td>' + App.formatDate(row.dispatchTime) + '</td>' +
                    '<td><a class="btn btn-sm btn-primary" href="task.html?taskId=' + row.taskId + '">处理</a></td>' +
                    '</tr>';
            }, 8);
        });
    }

    /* -------------------------------------------------------- 维修管理员 */
    function loadManager() {
        document.getElementById('listTitle').textContent = '待审核与待派单报修单';
        document.getElementById('listTip').textContent = '审核通过后进入待派单，可人工派单或使用智能派单';
        App.api('/api/stat/overview', {}).done(function (data) {
            renderStats([
                { label: '报修总量', value: data.total },
                { label: '待审核', value: data.pendingAudit, cls: 'warn' },
                { label: '待派单', value: data.pendingDispatch, cls: 'warn' },
                { label: '处理中', value: data.processing },
                { label: '待确认', value: data.pendingConfirm },
                { label: '完成率(%)', value: data.finishRate, cls: 'success' },
                { label: '平均评分', value: data.avgScore ? data.avgScore.toFixed(1) : '—' },
                { label: '耗材成本(元)', value: data.materialCost }
            ]);
        });
        App.api('/api/report/list', { status: 0, pageSize: 8 }).done(function (data) {
            document.getElementById('listHead').innerHTML =
                '<tr><th>报修单号</th><th>报修人</th><th>地点</th><th>类别</th><th>故障描述</th>' +
                '<th>紧急</th><th>状态</th><th>提交时间</th></tr>';
            App.renderRows('listBody', data.rows, function (row) {
                return '<tr>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.reporterName || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.escapeHtml(row.category) + '</td>' +
                    '<td>' + App.escapeHtml((row.description || '').substring(0, 22)) + '</td>' +
                    '<td>' + (row.priority === 1 ? '<span class="tag tag-red">紧急</span>' : '普通') + '</td>' +
                    '<td>' + App.orderStatusTag(row.status) + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '</tr>';
            }, 8);
        });
    }

    /* ---------------------------------------------------------- 系统管理员 */
    function loadAdmin() {
        document.getElementById('listTitle').textContent = '待审核账户';
        document.getElementById('listTip').textContent = '审核通过后方可登录；可锁定异常账户';
        App.api('/api/account/list', {}).done(function (data) {
            renderStats([
                { label: '账户总数', value: data.statistics.total },
                { label: '报修人', value: data.statistics.reporter },
                { label: '维修人员', value: data.statistics.worker },
                { label: '维修管理员', value: data.statistics.manager },
                { label: '系统管理员', value: data.statistics.admin },
                { label: '待审核', value: data.statistics.pending, cls: 'warn' },
                { label: '当前在线会话', value: data.online, cls: 'success' }
            ]);
        });
        App.api('/api/account/pending', {}).done(function (rows) {
            document.getElementById('listHead').innerHTML =
                '<tr><th>用户名</th><th>姓名</th><th>学号/工号</th><th>手机号</th><th>角色</th><th>注册时间</th></tr>';
            App.renderRows('listBody', rows, function (row) {
                return '<tr>' +
                    '<td>' + App.escapeHtml(row.username) + '</td>' +
                    '<td>' + App.escapeHtml(row.realName) + '</td>' +
                    '<td>' + App.escapeHtml(row.studentNo) + '</td>' +
                    '<td>' + App.escapeHtml(row.phone) + '</td>' +
                    '<td>' + App.escapeHtml(row.roleText) + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '</tr>';
            }, 6);
        });
    }

    function renderStats(items) {
        document.getElementById('statGrid').innerHTML = items.map(function (item) {
            return '<div class="stat ' + (item.cls || '') + '">' +
                '<div class="label">' + App.escapeHtml(item.label) + '</div>' +
                '<div class="value">' + App.escapeHtml(String(item.value === undefined || item.value === null ? '0' : item.value)) + '</div>' +
                '</div>';
        }).join('');
    }
})();

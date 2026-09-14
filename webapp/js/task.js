/* 维修人员任务管理脚本（对应设计书 2.2.4 接单、2.2.5 维修管理模块设计） */
(function () {
    var me = App.shell({ key: 'task', title: '我的维修任务', roles: ['worker'] });
    if (!me) { return; }

    var autoOpenTask = new URLSearchParams(location.search).get('taskId');

    loadStats();
    loadTasks(autoOpenTask);

    document.getElementById('fStatus').onchange = function () { loadTasks(); };
    document.getElementById('btnLocation').onclick = function () {
        var location = document.getElementById('fLocation').value.trim();
        if (!location) { App.toast('请填写当前位置', 'error'); return; }
        App.api('/api/repair/status', { status: 1, location: location }).done(function () {
            App.toast('位置已更新', 'success');
            loadStats();
        });
    };

    function loadStats() {
        App.api('/api/repair/statistics', {}).done(function (data) {
            var worker = data.worker || {};
            document.getElementById('statGrid').innerHTML = [
                card('待接单', data.waiting, 'warn'),
                card('维修中', data.repairing, ''),
                card('待确认', data.pendingConfirm, ''),
                card('已完成', data.finished, 'success'),
                card('当前在单量', worker.currentOrders || 0, ''),
                card('平均评分', data.avgScore ? data.avgScore.toFixed(1) : '—', '')
            ].join('');
            document.getElementById('fLocation').value = worker.location || '';
        });
    }

    function card(label, value, cls) {
        return '<div class="stat ' + cls + '"><div class="label">' + label + '</div><div class="value">' +
            (value === undefined || value === null ? 0 : value) + '</div></div>';
    }

    function loadTasks(autoTaskId) {
        var status = document.getElementById('fStatus').value;
        App.api('/api/dispatch/myTasks', status === '' ? {} : { status: status }).done(function (rows) {
            App.renderRows('listBody', rows, function (row) {
                var actions = '<button class="btn btn-sm btn-primary act-handle" data-id="' + row.taskId + '">处理</button>';
                if (row.status === 0) {
                    actions += '<button class="btn btn-sm btn-success act-accept" data-id="' + row.taskId + '">接单</button>';
                }
                return '<tr>' +
                    '<td>#' + row.taskId + '</td>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.location || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.category || '') + '</td>' +
                    '<td>' + App.escapeHtml((row.description || '').substring(0, 18)) + '</td>' +
                    '<td>' + (row.priority === 1 ? '<span class="tag tag-red">紧急</span>' : '普通') + '</td>' +
                    '<td>' + App.taskStatusTag(row.status) + '</td>' +
                    '<td>' + App.formatDate(row.dispatchTime) + '</td>' +
                    '<td><div class="row-actions">' + actions + '</div></td>' +
                    '</tr>';
            }, 9);

            App.$$('.act-accept').forEach(function (btn) {
                btn.onclick = function () {
                    App.api('/api/dispatch/accept', { taskId: btn.getAttribute('data-id') }).done(function () {
                        App.toast('接单成功，请及时开始维修', 'success');
                        loadStats();
                        loadTasks();
                    });
                };
            });
            App.$$('.act-handle').forEach(function (btn) {
                btn.onclick = function () { openTask(btn.getAttribute('data-id')); };
            });

            if (autoTaskId) {
                openTask(autoTaskId);
                autoTaskId = null;
            }
        });
    }

    /* ---------------------------------------------------------- 任务处理弹窗 */
    function openTask(taskId) {
        App.api('/api/repair/detail', { taskId: taskId }, 'GET').done(function (data) {
            var task = data.task;
            var order = data.order;
            var materials = data.materials || [];
            var progressHtml = (data.progressList || []).map(function (item) {
                return '<li><div>' + App.escapeHtml(item.content) + '</div><div class="time">' + App.formatDate(item.createTime) + '</div></li>';
            }).join('');
            var usageHtml = (data.usageList || []).map(function (item) {
                return '<tr><td>' + App.escapeHtml(item.matName || '') + '</td><td>' + item.useCount + '</td><td>' + (item.amount || 0) + '</td></tr>';
            }).join('');
            var materialOptions = materials.map(function (item) {
                return '<option value="' + item.matId + '">' + App.escapeHtml(item.displayName || item.matName) +
                    '（库存 ' + item.stock + '，单价 ' + (item.unitPrice || 0) + '）</option>';
            }).join('');

            var body =
                '<div class="kv">' +
                '<div class="k">报修单号</div><div class="v">#' + order.orderId + '</div>' +
                '<div class="k">报修人</div><div class="v">' + App.escapeHtml(order.reporterName || '') + '（' + App.escapeHtml(order.reporterPhone || '') + '）</div>' +
                '<div class="k">报修地点</div><div class="v">' + App.escapeHtml(order.location) + '</div>' +
                '<div class="k">故障描述</div><div class="v">' + App.escapeHtml(order.description) + '</div>' +
                '<div class="k">任务状态</div><div class="v">' + App.taskStatusTag(task.status) + '</div>' +
                '<div class="k">接单时限</div><div class="v">派单后 ' + data.acceptDeadlineMinutes + ' 分钟内接单，结果确认时限 ' + data.confirmDeadlineHours + ' 小时</div>' +
                '</div>' +
                '<div class="section-title">维修处理</div>' +
                '<label>维修结果（提交完成时必填）<textarea id="mResult" placeholder="填写维修过程与结果">' + App.escapeHtml(task.result || '') + '</textarea></label>' +
                '<label>备注 / 延期原因<input type="text" id="mRemark" value="' + App.escapeHtml(task.remark || '') + '"></label>' +
                '<label>进度反馈<textarea id="mProgress" placeholder="填写当前进度，可留空"></textarea></label>' +
                '<div class="section-title">登记耗材</div>' +
                '<div id="usageRows">' + (usageHtml ? '<table class="table table-compact"><thead><tr><th>已登记耗材</th><th>数量</th><th>金额</th></tr></thead><tbody>' + usageHtml + '</tbody></table>' : '<div class="muted">尚未登记耗材</div>') + '</div>' +
                '<div style="display:flex;gap:8px;margin-top:10px">' +
                '  <select id="matSelect" style="flex:2">' + materialOptions + '</select>' +
                '  <input type="number" id="matCount" value="1" min="1" style="flex:1">' +
                '  <button class="btn btn-ghost" id="btnAddMaterial">登记耗材</button>' +
                '</div>' +
                '<div class="section-title">处理时间线</div>' +
                '<ul class="timeline">' + (progressHtml || '<li class="muted">暂无进度记录</li>') + '</ul>';

            var foot =
                '<button class="btn btn-ghost" id="btnFeedback">提交进度反馈</button>' +
                '<button class="btn btn-warn" id="btnTransfer">申请转单</button>' +
                '<button class="btn btn-danger" id="btnRefuse">申请退单</button>' +
                '<button class="btn btn-ghost" id="btnStart">开始维修</button>' +
                '<button class="btn btn-success" id="btnFinish">提交完成</button>';

            var mask = App.openModal('维修任务处理（任务 #' + taskId + '）', body, foot, { large: true });

            mask.querySelector('#btnStart').onclick = function () {
                App.api('/api/repair/start', { taskId: taskId, remark: mask.querySelector('#mRemark').value.trim() })
                    .done(function () {
                        App.toast('已记录开始维修', 'success');
                        App.closeModal();
                        loadStats();
                        loadTasks();
                    });
            };

            mask.querySelector('#btnFeedback').onclick = function () {
                App.api('/api/repair/feedback', {
                    taskId: taskId,
                    content: mask.querySelector('#mProgress').value.trim(),
                    delayReason: mask.querySelector('#mRemark').value.trim()
                }).done(function () {
                    App.toast('进度反馈已提交', 'success');
                    App.closeModal();
                    loadTasks();
                });
            };

            mask.querySelector('#btnAddMaterial').onclick = function () {
                var matId = mask.querySelector('#matSelect').value;
                var count = mask.querySelector('#matCount').value;
                App.api('/api/repair/material', { taskId: taskId, matId: matId, useCount: count })
                    .done(function () {
                        App.toast('耗材登记成功', 'success');
                        App.closeModal();
                        openTask(taskId);
                    });
            };

            mask.querySelector('#btnTransfer').onclick = function () {
                var reason = mask.querySelector('#mRemark').value.trim();
                if (!reason) { App.toast('请在备注中填写转单原因', 'error'); return; }
                App.api('/api/dispatch/transfer', { taskId: taskId, reason: reason }).done(function () {
                    App.toast('转单申请已提交，任务已退回派单池', 'success');
                    App.closeModal();
                    loadStats();
                    loadTasks();
                });
            };

            mask.querySelector('#btnRefuse').onclick = function () {
                var reason = mask.querySelector('#mRemark').value.trim();
                if (!reason) { App.toast('请在备注中填写退单原因', 'error'); return; }
                App.api('/api/dispatch/refuse', { taskId: taskId, reason: reason }).done(function () {
                    App.toast('退单申请已提交，任务已退回派单池', 'success');
                    App.closeModal();
                    loadStats();
                    loadTasks();
                });
            };

            mask.querySelector('#btnFinish').onclick = function () {
                var result = mask.querySelector('#mResult').value.trim();
                if (!result) { App.toast('请填写维修结果', 'error'); return; }
                App.api('/api/repair/finish', {
                    taskId: taskId,
                    result: result,
                    remark: mask.querySelector('#mRemark').value.trim()
                }).done(function () {
                    App.toast('维修完成登记成功，等待确认', 'success');
                    App.closeModal();
                    loadStats();
                    loadTasks();
                });
            };
        });
    }
})();

/* 报修审核脚本（对应设计书表 1.8 报修审核功能点） */
(function () {
    var me = App.shell({ key: 'audit', title: '报修审核', roles: ['manager', 'admin'] });
    if (!me) { return; }

    load();

    function load() {
        App.api('/api/report/statistics', {}).done(function (data) {
            renderStats(data);
        });
        App.api('/api/report/list', { status: 0, pageSize: 50 }).done(function (data) {
            App.renderRows('listBody', data.rows, function (row) {
                return '<tr>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.reporterName || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.reporterPhone || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.escapeHtml(row.category) + '</td>' +
                    '<td>' + App.escapeHtml((row.description || '').substring(0, 26)) + '</td>' +
                    '<td>' + (row.priority === 1 ? '<span class="tag tag-red">紧急</span>' : '普通') + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '<td><div class="row-actions">' +
                    '<button class="btn btn-sm btn-success act-accept" data-id="' + row.orderId + '">受理</button>' +
                    '<button class="btn btn-sm btn-danger act-reject" data-id="' + row.orderId + '">不受理</button>' +
                    '<a class="btn btn-sm btn-ghost" href="/report-detail.html?orderId=' + row.orderId + '">详情</a>' +
                    '</div></td>' +
                    '</tr>';
            }, 9);

            App.$$('.act-accept').forEach(function (btn) {
                btn.onclick = function () { acceptOrder(btn.getAttribute('data-id')); };
            });
            App.$$('.act-reject').forEach(function (btn) {
                btn.onclick = function () { rejectOrder(btn.getAttribute('data-id')); };
            });
        });

        App.api('/api/report/list', { pageSize: 20 }).done(function (data) {
            var rows = (data.rows || []).filter(function (row) {
                return row.status === 1 || row.status === 7;
            }).slice(0, 20);
            App.renderRows('historyBody', rows, function (row) {
                return '<tr>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.reporterName || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.orderStatusTag(row.status) + '</td>' +
                    '<td>' + App.escapeHtml(row.rejectReason || '—') + '</td>' +
                    '<td>' + App.formatDate(row.updateTime) + '</td>' +
                    '</tr>';
            }, 6);
        });
    }

    function renderStats(data) {
        document.getElementById('statGrid').innerHTML = [
            card('待审核', data.pendingAudit, 'warn'),
            card('待派单', data.pendingDispatch, 'warn'),
            card('维修中', data.repairing, ''),
            card('待确认', data.pendingConfirm, ''),
            card('已完成', data.finished, 'success'),
            card('报修总量', data.total, '')
        ].join('');
    }

    function card(label, value, cls) {
        return '<div class="stat ' + cls + '"><div class="label">' + label + '</div><div class="value">' +
            (value === undefined || value === null ? 0 : value) + '</div></div>';
    }

    function acceptOrder(orderId) {
        var mask = App.openModal('受理报修单 #' + orderId,
            '<label>优先级调整<select id="prioritySel"><option value="0">普通</option><option value="1">紧急</option></select></label>' +
            '<div class="muted">受理后报修单状态变为「待派单」，可在派单管理中派单。</div>',
            '<button class="btn btn-ghost" id="c0">取消</button><button class="btn btn-success" id="c1">确认受理</button>');
        mask.querySelector('#c0').onclick = App.closeModal;
        mask.querySelector('#c1').onclick = function () {
            App.api('/api/report/audit', {
                orderId: orderId,
                accept: true,
                priority: mask.querySelector('#prioritySel').value
            }).done(function (order, message) {
                App.closeModal();
                App.toast(message, 'success');
                load();
            });
        };
    }

    function rejectOrder(orderId) {
        var mask = App.openModal('不受理报修单 #' + orderId,
            '<label>退回原因（必填）<textarea id="rejectReason" placeholder="如：不属于维修范围 / 信息不完整 / 该设备已报废"></textarea></label>',
            '<button class="btn btn-ghost" id="c0">取消</button><button class="btn btn-danger" id="c1">确认驳回</button>');
        mask.querySelector('#c0').onclick = App.closeModal;
        mask.querySelector('#c1').onclick = function () {
            var reason = mask.querySelector('#rejectReason').value.trim();
            if (!reason) { App.toast('请填写退回原因', 'error'); return; }
            App.api('/api/report/audit', { orderId: orderId, accept: false, reason: reason }).done(function (order, message) {
                App.closeModal();
                App.toast(message, 'success');
                load();
            });
        };
    }
})();

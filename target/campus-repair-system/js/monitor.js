/* 维修监督脚本（对应设计书表 1.8 接单时限、表 1.9 确认时限与 1.1.1 监督职责） */
(function () {
    var me = App.shell({ key: 'monitor', title: '维修监督', roles: ['manager', 'admin'] });
    if (!me) { return; }

    document.getElementById('btnRemind').onclick = function () {
        App.api('/api/dispatch/remind', {}).done(function (data, message) {
            App.toast(message, 'success');
            load();
        });
    };

    function load() {
        App.api('/api/report/overdue', {}).done(function (data) {
            App.renderRows('overdueTaskBody', data.overdueTasks, function (row) {
                return '<tr>' +
                    '<td>#' + row.taskId + '</td>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.workerName || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.location || '') + '</td>' +
                    '<td>' + App.formatDate(row.dispatchTime) + '</td>' +
                    '<td>' + hoursSince(row.dispatchTime) + '</td>' +
                    '</tr>';
            }, 6);
            App.renderRows('overdueConfirmBody', data.overdueConfirms, function (row) {
                return '<tr>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.reporterName || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.escapeHtml(row.workerName || '') + '</td>' +
                    '<td>' + App.formatDate(row.finishTime) + '</td>' +
                    '</tr>';
            }, 5);
        });

        App.api('/api/report/list', { pageSize: 100 }).done(function (data) {
            var rows = (data.rows || []).filter(function (row) {
                return row.status === 1 || row.status === 2 || row.status === 3 || row.status === 4;
            });
            App.renderRows('trackBody', rows, function (row) {
                return '<tr>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.escapeHtml(row.category) + '</td>' +
                    '<td>' + App.orderStatusTag(row.status) + '</td>' +
                    '<td>' + App.escapeHtml(row.workerName || '—') + '</td>' +
                    '<td>' + App.formatDate(row.dispatchTime) + '</td>' +
                    '<td>' + (row.urgeCount || 0) + '</td>' +
                    '<td><a class="btn btn-sm btn-ghost" href="/report-detail.html?orderId=' + row.orderId + '">详情</a>' +
                    (row.status === 4 ? '<button class="btn btn-sm btn-success act-pass" data-id="' + row.orderId + '">确认归档</button>' : '') +
                    '</td>' +
                    '</tr>';
            }, 8);

            App.$$('.act-pass').forEach(function (btn) {
                btn.onclick = function () {
                    App.api('/api/repair/confirm', { orderId: btn.getAttribute('data-id'), pass: true }).done(function () {
                        App.toast('已确认归档', 'success');
                        load();
                    });
                };
            });
        });
    }

    function hoursSince(text) {
        if (!text) { return '—'; }
        var time = new Date(String(text).replace(' ', 'T')).getTime();
        if (isNaN(time)) { return '—'; }
        return ((Date.now() - time) / 3600000).toFixed(1);
    }

    load();
})();

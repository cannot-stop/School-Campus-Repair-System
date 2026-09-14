/* 提交报修脚本（对应设计书表 1.5 提交报修功能点） */
(function () {
    var me = App.shell({ key: 'report-submit', title: '提交报修', roles: ['reporter'] });
    if (!me) { return; }

    App.api('/api/report/options', {}).done(function (data) {
        var building = document.getElementById('building');
        var category = document.getElementById('category');
        building.innerHTML = (data.buildings || []).map(function (v) {
            return '<option value="' + App.escapeHtml(v) + '">' + App.escapeHtml(v) + '</option>';
        }).join('');
        category.innerHTML = (data.categories || []).map(function (v) {
            return '<option value="' + App.escapeHtml(v) + '">' + App.escapeHtml(v) + '</option>';
        }).join('');
    });

    var form = document.getElementById('reportForm');
    var alertBox = document.getElementById('alert');

    form.onsubmit = function (event) {
        event.preventDefault();
        var data = App.toQueryString(form);
        if (!data.building || !data.floor || !data.room || !data.category || !data.description) {
            alertBox.className = 'alert alert-error';
            alertBox.textContent = '请完整填写报修地点、类别与故障描述';
            return;
        }
        App.api('/api/report/submit', data).done(function (order) {
            alertBox.className = 'alert alert-success';
            alertBox.innerHTML = '报修提交成功，报修单号 <b>#' + order.orderId + '</b>，' +
                '当前状态：' + (App.ORDER_STATUS[order.status] || {}).text + '。' +
                ' <a href="/report-detail.html?orderId=' + order.orderId + '">查看详情</a>';
            form.reset();
            loadPending();
        }).fail(function (res) {
            alertBox.className = 'alert alert-error';
            alertBox.textContent = res.message || '提交失败';
        });
    };

    function loadPending() {
        App.api('/api/report/list', { pageSize: 10 }).done(function (data) {
            var rows = (data.rows || []).filter(function (row) {
                return row.status === 0 || row.status === 1 || row.status === 2 || row.status === 3 || row.status === 4;
            });
            App.renderRows('pendingBody', rows, function (row) {
                return '<tr>' +
                    '<td><a href="/report-detail.html?orderId=' + row.orderId + '">#' + row.orderId + '</a></td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.orderStatusTag(row.status) + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '</tr>';
            }, 4);
        });
    }

    loadPending();
})();

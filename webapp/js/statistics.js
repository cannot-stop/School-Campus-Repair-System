/* 统计分析脚本（对应设计书 2.2.6 StatService：维修统计、故障类型分析、绩效统计） */
(function () {
    var me = App.shell({ key: 'statistics', title: '统计分析', roles: ['manager', 'admin'] });
    if (!me) { return; }

    App.api('/api/stat/report', {}).done(function (data) {
        var overview = data.overview || {};
        document.getElementById('statGrid').innerHTML = [
            card('报修总量', overview.total, ''),
            card('已完成', overview.finished, 'success'),
            card('完成率(%)', overview.finishRate, 'success'),
            card('待审核', overview.pendingAudit, 'warn'),
            card('待派单', overview.pendingDispatch, 'warn'),
            card('处理中', overview.processing, ''),
            card('待确认', overview.pendingConfirm, ''),
            card('平均评分', overview.avgScore ? Number(overview.avgScore).toFixed(1) : '—', ''),
            card('平均处理时长(小时)', overview.avgHours, ''),
            card('耗材成本(元)', overview.materialCost, '')
        ].join('');

        App.renderRows('categoryBody', data.category, function (row) {
            return '<tr><td>' + App.escapeHtml(row.groupName) + '</td><td>' + row.orderCount + '</td><td>' +
                row.finishedCount + '</td><td>' + row.finishRate + '</td></tr>';
        }, 4);

        App.renderRows('buildingBody', data.building, function (row) {
            return '<tr><td>' + App.escapeHtml(row.groupName) + '</td><td>' + row.orderCount + '</td><td>' +
                row.finishedCount + '</td><td>' + row.finishRate + '</td></tr>';
        }, 4);

        App.renderRows('workerBody', data.worker, function (row) {
            return '<tr><td>' + App.escapeHtml(row.groupName) + '</td><td>' + row.orderCount + '</td><td>' +
                row.finishedCount + '</td><td>' + row.processingCount + '</td><td>' + row.finishRate + '</td><td>' +
                row.avgHours + '</td><td>' + row.avgScoreText + '</td><td>' + row.materialCost + '</td></tr>';
        }, 8);

        App.renderRows('monthlyBody', data.monthly, function (row) {
            return '<tr><td>' + App.escapeHtml(row.groupName) + '</td><td>' + row.orderCount + '</td><td>' +
                row.finishedCount + '</td><td>' + row.finishRate + '</td></tr>';
        }, 4);

        App.renderRows('materialBody', data.material, function (row) {
            return '<tr><td>' + App.escapeHtml(row.groupName) + '</td><td>' + row.orderCount + '</td><td>' +
                row.materialCost + '</td></tr>';
        }, 3);

        App.renderRows('statusBody', (overview.statusDistribution || []), function (row) {
            return '<tr><td>' + App.escapeHtml(row.statusText) + '</td><td>' + row.count + '</td></tr>';
        }, 2);
    });

    function card(label, value, cls) {
        var text = (value === undefined || value === null) ? '0' : value;
        return '<div class="stat ' + cls + '"><div class="label">' + label + '</div><div class="value">' + text + '</div></div>';
    }
})();

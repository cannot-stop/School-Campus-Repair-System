/* 派单管理脚本（对应设计书 2.2.4 人工派单、智能派单功能点设计） */
(function () {
    var me = App.shell({ key: 'dispatch', title: '派单管理', roles: ['manager', 'admin'] });
    if (!me) { return; }

    App.api('/api/account/baseData', { type: 'building' }).done(function (data) {
        (data.buildings || []).forEach(function (v) {
            document.getElementById('fBuilding').innerHTML += '<option value="' + App.escapeHtml(v) + '">' + App.escapeHtml(v) + '</option>';
        });
        (data.categories || []).forEach(function (v) {
            document.getElementById('fCategory').innerHTML += '<option value="' + App.escapeHtml(v) + '">' + App.escapeHtml(v) + '</option>';
        });
    });

    document.getElementById('btnRecalculate').onclick = function () {
        App.api('/api/dispatch/recalculate', {}).done(function (count, message) {
            App.toast(message, 'success');
            loadAll();
        });
    };
    document.getElementById('btnRemind').onclick = function () {
        App.api('/api/dispatch/remind', {}).done(function (data, message) {
            App.toast(message, 'success');
            loadAll();
        });
    };

    function loadAll() {
        loadOrders();
        loadWorkers();
    }

    function loadOrders() {
        App.api('/api/report/list', {
            status: 1,
            pageSize: 100,
            category: document.getElementById('fCategory').value,
            building: document.getElementById('fBuilding').value
        }).done(function (data) {
            App.renderRows('orderBody', data.rows, function (row) {
                return '<tr>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.reporterName || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.escapeHtml(row.category) + '</td>' +
                    '<td>' + App.escapeHtml((row.description || '').substring(0, 22)) + '</td>' +
                    '<td>' + (row.priority === 1 ? '<span class="tag tag-red">紧急</span>' : '普通') + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '<td><div class="row-actions">' +
                    '<button class="btn btn-sm btn-primary act-auto" data-id="' + row.orderId + '">智能派单</button>' +
                    '<button class="btn btn-sm btn-ghost act-manual" data-id="' + row.orderId + '">人工派单</button>' +
                    '<button class="btn btn-sm btn-ghost act-rank" data-id="' + row.orderId + '">查看匹配</button>' +
                    '</div></td>' +
                    '</tr>';
            }, 8);

            App.$$('.act-auto').forEach(function (btn) {
                btn.onclick = function () {
                    App.api('/api/dispatch/auto', { orderId: btn.getAttribute('data-id') }).done(function (data, message) {
                        var candidate = data.candidate;
                        App.toast(message + '：' + candidate.worker.name + '（匹配度 ' + candidate.totalScore + ' 分，' +
                            candidate.levelText + '）', 'success');
                        loadAll();
                    });
                };
            });
            App.$$('.act-manual').forEach(function (btn) {
                btn.onclick = function () { openManual(btn.getAttribute('data-id')); };
            });
            App.$$('.act-rank').forEach(function (btn) {
                btn.onclick = function () { openRank(btn.getAttribute('data-id')); };
            });
        });
    }

    function loadWorkers() {
        App.api('/api/dispatch/workers', {}).done(function (data) {
            App.renderRows('workerBody', data.workers, function (row) {
                return '<tr>' +
                    '<td>' + App.escapeHtml(row.name) + '</td>' +
                    '<td>' + App.escapeHtml(row.phone) + '</td>' +
                    '<td>' + App.escapeHtml(row.skillTags) + '</td>' +
                    '<td>' + (row.currentOrders || 0) + '</td>' +
                    '<td>' + App.escapeHtml(row.location || '—') + '</td>' +
                    '<td>' + (row.status === 1 ? '<span class="tag tag-green">在线</span>' : '<span class="tag tag-gray">离线</span>') + '</td>' +
                    '</tr>';
            }, 6);
        });
    }

    /* 智能派单匹配度明细（算法可解释性展示） */
    function openRank(orderId) {
        App.api('/api/dispatch/candidates', { orderId: orderId }).done(function (data) {
            var rows = (data.candidates || []).map(function (item, index) {
                return '<tr>' +
                    '<td>' + (index + 1) + '</td>' +
                    '<td>' + App.escapeHtml(item.worker.name) + '</td>' +
                    '<td>' + App.escapeHtml(item.worker.skillTags) + '</td>' +
                    '<td>' + item.worker.currentOrders + '</td>' +
                    '<td>' + (item.worker.status === 1 ? '在线' : '离线') + '</td>' +
                    '<td><b>' + item.totalScore + '</b></td>' +
                    '<td>' + item.skillScore + ' / ' + item.loadScore + ' / ' + item.onlineScore + ' / ' + item.urgentScore + '</td>' +
                    '<td>' + App.escapeHtml((item.reasons || []).join('；')) + '</td>' +
                    '<td>' + App.escapeHtml(item.levelText) + '</td>' +
                    '</tr>';
            }).join('');
            App.openModal('智能派单匹配度明细（报修单 #' + orderId + '）',
                '<div class="alert alert-info">评分模型共 100 分：技能匹配 40 分、在单量 30 分、在线状态 15 分、紧急单响应 15 分。</div>' +
                '<div style="overflow-x:auto"><table class="table table-compact"><thead><tr>' +
                '<th>排名</th><th>维修人员</th><th>技能</th><th>在单量</th><th>状态</th><th>总分</th>' +
                '<th>技能/在单/在线/紧急</th><th>推荐理由</th><th>结论</th></tr></thead><tbody>' + rows + '</tbody></table></div>',
                '<button class="btn btn-ghost" onclick="App.closeModal()">关闭</button>', { large: true });
        });
    }

    /* 人工派单 */
    function openManual(orderId) {
        App.api('/api/dispatch/workers', {}).done(function (data) {
            var options = (data.workers || []).map(function (item) {
                return '<option value="' + item.workerId + '">' + App.escapeHtml(item.name) + '（技能：' +
                    App.escapeHtml(item.skillTags) + '，在单量 ' + (item.currentOrders || 0) + '，' +
                    (item.status === 1 ? '在线' : '离线') + '）</option>';
            }).join('');
            var mask = App.openModal('人工派单（报修单 #' + orderId + '）',
                '<label>选择维修人员<select id="workerSelect">' + options + '</select></label>' +
                '<label>派单说明<input type="text" id="dispatchRemark" placeholder="如：就近派单"></label>' +
                '<div class="muted">仅可派给在线维修人员；离线人员需先在维修端切换为在线状态。</div>',
                '<button class="btn btn-ghost" id="cancelBtn">取消</button>' +
                '<button class="btn btn-primary" id="okBtn">确认派单</button>');
            mask.querySelector('#cancelBtn').onclick = App.closeModal;
            mask.querySelector('#okBtn').onclick = function () {
                App.api('/api/dispatch/manual', {
                    orderId: orderId,
                    workerId: mask.querySelector('#workerSelect').value,
                    remark: mask.querySelector('#dispatchRemark').value.trim()
                }).done(function (task, message) {
                    App.closeModal();
                    App.toast(message, 'success');
                    loadAll();
                });
            };
        });
    }

    loadAll();
})();

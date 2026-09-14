/* 报修单列表脚本（对应设计书 2.2.3.2 报修查询与撤销功能点） */
(function () {
    var me = App.shell({ key: 'report', title: '我的报修', roles: ['reporter'] });
    if (!me) { return; }

    var pageNum = 1;
    var pageSize = 10;
    var lastPages = 1;

    // 状态下拉
    var statusSelect = document.getElementById('fStatus');
    Object.keys(App.ORDER_STATUS).forEach(function (code) {
        var item = App.ORDER_STATUS[code];
        statusSelect.innerHTML += '<option value="' + code + '">' + item.text + '</option>';
    });

    // 楼栋与类别来自基础数据
    App.api('/api/report/options', {}).done(function (data) {
        (data.buildings || []).forEach(function (value) {
            document.getElementById('fBuilding').innerHTML += '<option value="' + App.escapeHtml(value) + '">' + App.escapeHtml(value) + '</option>';
        });
        (data.categories || []).forEach(function (value) {
            document.getElementById('fCategory').innerHTML += '<option value="' + App.escapeHtml(value) + '">' + App.escapeHtml(value) + '</option>';
        });
    });

    document.getElementById('btnSearch').onclick = function () { pageNum = 1; load(); };
    document.getElementById('btnReset').onclick = function () {
        ['fStatus', 'fCategory', 'fBuilding', 'fKeyword', 'fBegin', 'fEnd'].forEach(function (id) {
            document.getElementById(id).value = '';
        });
        pageNum = 1;
        load();
    };
    document.getElementById('btnPrev').onclick = function () {
        if (pageNum > 1) { pageNum--; load(); }
    };
    document.getElementById('btnNext').onclick = function () {
        if (pageNum < lastPages) { pageNum++; load(); }
    };

    function load() {
        var query = {
            pageNum: pageNum,
            pageSize: pageSize,
            status: document.getElementById('fStatus').value,
            category: document.getElementById('fCategory').value,
            building: document.getElementById('fBuilding').value,
            keyword: document.getElementById('fKeyword').value,
            beginTime: document.getElementById('fBegin').value,
            endTime: document.getElementById('fEnd').value
        };
        App.api('/api/report/list', query).done(function (data) {
            lastPages = data.pages || 1;
            document.getElementById('pageInfo').textContent = '第 ' + data.pageNum + ' / ' + lastPages + ' 页，共 ' + data.total + ' 条';
            App.renderRows('listBody', data.rows, function (row) {
                var actions = '<a class="btn btn-sm btn-ghost" href="/report-detail.html?orderId=' + row.orderId + '">详情</a>';
                if (row.cancelable) {
                    actions += '<button class="btn btn-sm btn-ghost act-cancel" data-id="' + row.orderId + '">撤销</button>';
                }
                if (row.urgable) {
                    actions += '<button class="btn btn-sm btn-warn act-urge" data-id="' + row.orderId + '">催办</button>';
                }
                if (row.evaluable) {
                    actions += '<button class="btn btn-sm btn-primary act-eval" data-id="' + row.orderId + '">评价</button>';
                }
                if (row.status === 4) {
                    actions += '<button class="btn btn-sm btn-success act-confirm" data-id="' + row.orderId + '">确认结果</button>';
                }
                return '<tr>' +
                    '<td>#' + row.orderId + '</td>' +
                    '<td>' + App.escapeHtml(row.location) + '</td>' +
                    '<td>' + App.escapeHtml(row.category) + '</td>' +
                    '<td>' + App.escapeHtml((row.description || '').substring(0, 20)) + '</td>' +
                    '<td>' + (row.priority === 1 ? '<span class="tag tag-red">紧急</span>' : '<span class="tag tag-gray">普通</span>') + '</td>' +
                    '<td>' + App.orderStatusTag(row.status) + '</td>' +
                    '<td>' + App.escapeHtml(row.workerName || '—') + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '<td><div class="row-actions">' + actions + '</div></td>' +
                    '</tr>';
            }, 9);
            bindActions();
        });
    }

    function bindActions() {
        App.$$('.act-cancel').forEach(function (btn) {
            btn.onclick = function () {
                var id = btn.getAttribute('data-id');
                App.confirmDialog('撤销报修', '撤销后报修单将终止处理，确定撤销报修单 #' + id + ' 吗？', function (reason) {
                    App.api('/api/report/cancel', { orderId: id, reason: reason }).done(function () {
                        App.toast('报修单已撤销', 'success');
                        load();
                    });
                }, '撤销原因');
            };
        });
        App.$$('.act-urge').forEach(function (btn) {
            btn.onclick = function () {
                var id = btn.getAttribute('data-id');
                App.confirmDialog('报修催办', '将向维修管理员与维修人员发送催办提醒。', function (reason) {
                    App.api('/api/report/urge', { orderId: id, reason: reason }).done(function () {
                        App.toast('已提交催办', 'success');
                        load();
                    });
                }, '催办说明');
            };
        });
        App.$$('.act-eval').forEach(function (btn) {
            btn.onclick = function () { openEval(btn.getAttribute('data-id')); };
        });
        App.$$('.act-confirm').forEach(function (btn) {
            btn.onclick = function () {
                var id = btn.getAttribute('data-id');
                var mask = App.openModal('确认维修结果',
                    '<div class="alert alert-info">请确认维修结果是否满意。确认通过后报修单归档；不通过将退回维修人员重新处理，并回滚已登记的耗材。</div>' +
                    '<label>确认意见 / 不通过原因<textarea id="confirmText" placeholder="通过时可不填"></textarea></label>',
                    '<button class="btn btn-danger" id="btnReject">不通过，退回</button>' +
                    '<button class="btn btn-success" id="btnPass">确认通过</button>');
                mask.querySelector('#btnPass').onclick = function () {
                    App.closeModal();
                    App.api('/api/repair/confirm', { orderId: id, pass: true }).done(function () {
                        App.toast('维修结果已确认', 'success');
                        load();
                    });
                };
                mask.querySelector('#btnReject').onclick = function () {
                    var reason = mask.querySelector('#confirmText').value.trim();
                    if (!reason) { App.toast('请填写不通过原因', 'error'); return; }
                    App.closeModal();
                    App.api('/api/repair/confirm', { orderId: id, pass: false, reason: reason }).done(function () {
                        App.toast('已退回重新处理', 'success');
                        load();
                    });
                };
            };
        });
    }

    function openEval(orderId) {
        var stars = '';
        for (var i = 1; i <= 5; i++) {
            stars += '<label style="display:inline-block;margin-right:12px"><input type="radio" name="score" value="' + i + '"' +
                (i === 5 ? ' checked' : '') + '> ' + i + ' 分</label>';
        }
        var mask = App.openModal('维修服务评价',
            '<label>评分（1～5 分）<div style="margin-top:6px">' + stars + '</div></label>' +
            '<label>评价内容<textarea id="evalComment" placeholder="请填写对本次维修服务的评价"></textarea></label>',
            '<button class="btn btn-ghost" id="evalCancel">取消</button>' +
            '<button class="btn btn-primary" id="evalOk">提交评价</button>');
        mask.querySelector('#evalCancel').onclick = App.closeModal;
        mask.querySelector('#evalOk').onclick = function () {
            var score = mask.querySelector('input[name=score]:checked').value;
            var comment = mask.querySelector('#evalComment').value.trim();
            App.api('/api/eval/submit', { orderId: orderId, score: score, comment: comment }).done(function () {
                App.closeModal();
                App.toast('评价提交成功，感谢反馈', 'success');
                load();
            });
        };
    }

    load();
})();

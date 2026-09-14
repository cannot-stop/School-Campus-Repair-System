/* 评价管理脚本（对应设计书 StatController.auditEval() 与评价审核回复要求） */
(function () {
    var me = App.shell({ key: 'evaluation', title: '评价管理' });
    if (!me) { return; }
    var canAudit = me.role === 'manager' || me.role === 'admin';

    document.getElementById('fAudit').onchange = load;

    function load() {
        App.api('/api/eval/statistics', {}).done(function (data) {
            document.getElementById('statGrid').innerHTML = [
                card('评价总数', data.total),
                card('平均评分', data.avgScore ? Number(data.avgScore).toFixed(1) : '—', 'success'),
                card('待审核评价', data.pendingAudit, 'warn')
            ].concat((data.distribution || []).map(function (row) {
                return card(row.score + ' 分', row.count);
            })).join('');
        });

        App.api('/api/eval/list', { pageSize: 100, auditStatus: document.getElementById('fAudit').value }).done(function (data) {
            App.renderRows('listBody', data.rows, function (row) {
                var actions = canAudit
                    ? '<button class="btn btn-sm btn-ghost act-reply" data-id="' + row.evalId + '">回复</button>' +
                      '<button class="btn btn-sm btn-success act-pass" data-id="' + row.evalId + '">通过</button>' +
                      '<button class="btn btn-sm btn-danger act-reject" data-id="' + row.evalId + '">驳回</button>'
                    : '<span class="muted">只读</span>';
                return '<tr>' +
                    '<td>' + row.evalId + '</td>' +
                    '<td>#' + (row.orderNo || row.orderId) + '</td>' +
                    '<td>' + App.escapeHtml(row.reporterName || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.workerName || '—') + '</td>' +
                    '<td>' + App.stars(row.score) + '</td>' +
                    '<td>' + App.escapeHtml(row.comment || '') + '</td>' +
                    '<td>' + App.escapeHtml(row.reply || '—') + '</td>' +
                    '<td>' + App.escapeHtml(row.auditStatusText || '') + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '<td><div class="row-actions">' + actions + '</div></td>' +
                    '</tr>';
            }, 10);

            App.$$('.act-reply').forEach(function (btn) {
                btn.onclick = function () {
                    var mask = App.openModal('回复评价',
                        '<label>回复内容（不超过 500 字）<textarea id="replyText"></textarea></label>',
                        '<button class="btn btn-ghost" id="c0">取消</button><button class="btn btn-primary" id="c1">提交回复</button>');
                    mask.querySelector('#c0').onclick = App.closeModal;
                    mask.querySelector('#c1').onclick = function () {
                        App.api('/api/eval/reply', {
                            evalId: btn.getAttribute('data-id'),
                            reply: mask.querySelector('#replyText').value.trim()
                        }).done(function () {
                            App.closeModal();
                            App.toast('回复成功', 'success');
                            load();
                        });
                    };
                };
            });
            App.$$('.act-pass').forEach(function (btn) {
                btn.onclick = function () {
                    App.api('/api/eval/audit', { evalId: btn.getAttribute('data-id'), auditStatus: 1 }).done(function () {
                        App.toast('评价已通过审核', 'success');
                        load();
                    });
                };
            });
            App.$$('.act-reject').forEach(function (btn) {
                btn.onclick = function () {
                    App.confirmDialog('驳回评价', '驳回后该评价不计入平均评分统计。', function (reason) {
                        if (!reason) { App.toast('请填写驳回原因', 'error'); return; }
                        App.api('/api/eval/audit', {
                            evalId: btn.getAttribute('data-id'),
                            auditStatus: 2,
                            reason: reason
                        }).done(function () {
                            App.toast('评价已驳回', 'success');
                            load();
                        });
                    }, '驳回原因');
                };
            });
        });
    }

    function card(label, value, cls) {
        return '<div class="stat ' + (cls || '') + '"><div class="label">' + label + '</div><div class="value">' +
            (value === undefined || value === null ? 0 : value) + '</div></div>';
    }

    load();
})();

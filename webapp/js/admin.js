/* 系统管理员脚本（对应设计书 1.1.1 系统管理员职责与 2.2.2.3 账户审核功能点） */
(function () {
    var me = App.shell({ key: 'admin', title: '用户与审核', roles: ['admin'] });
    if (!me) { return; }

    document.getElementById('fRole').onchange = load;

    function load() {
        App.api('/api/account/list', { role: document.getElementById('fRole').value }).done(function (data) {
            var statistics = data.statistics || {};
            document.getElementById('statGrid').innerHTML = [
                card('账户总数', statistics.total),
                card('报修人', statistics.reporter),
                card('维修人员', statistics.worker),
                card('维修管理员', statistics.manager),
                card('系统管理员', statistics.admin),
                card('待审核', statistics.pending, 'warn'),
                card('在线会话', data.online, 'success')
            ].join('');

            App.renderRows('userBody', data.users, function (row) {
                var locked = row.isLocked === 1;
                var actions = '<button class="btn btn-sm btn-ghost act-audit" data-id="' + row.userId + '" data-name="' +
                    App.escapeHtml(row.username) + '">审核</button>';
                if (row.role !== 'admin') {
                    actions += locked
                        ? '<button class="btn btn-sm btn-success act-unlock" data-id="' + row.userId + '">解锁</button>'
                        : '<button class="btn btn-sm btn-warn act-lock" data-id="' + row.userId + '">锁定</button>';
                }
                return '<tr>' +
                    '<td>' + row.userId + '</td>' +
                    '<td>' + App.escapeHtml(row.username) + '</td>' +
                    '<td>' + App.escapeHtml(row.realName) + '</td>' +
                    '<td>' + App.escapeHtml(row.studentNo) + '</td>' +
                    '<td>' + App.escapeHtml(row.phone) + '</td>' +
                    '<td>' + App.escapeHtml(row.roleText) + '</td>' +
                    '<td>' + auditTag(row.auditStatus) + '</td>' +
                    '<td>' + (locked ? '<span class="tag tag-red">已锁定</span>' : '<span class="tag tag-gray">正常</span>') + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '<td><div class="row-actions">' + actions + '</div></td>' +
                    '</tr>';
            }, 10);

            bind();
        });

        App.api('/api/account/pending', {}).done(function (rows) {
            App.renderRows('pendingBody', rows, function (row) {
                return '<tr>' +
                    '<td>' + App.escapeHtml(row.username) + '</td>' +
                    '<td>' + App.escapeHtml(row.realName) + '</td>' +
                    '<td>' + App.escapeHtml(row.studentNo) + '</td>' +
                    '<td>' + App.escapeHtml(row.phone) + '</td>' +
                    '<td>' + App.escapeHtml(row.roleText) + '</td>' +
                    '<td>' + App.formatDate(row.createTime) + '</td>' +
                    '<td><div class="row-actions">' +
                    '<button class="btn btn-sm btn-success act-pass" data-id="' + row.userId + '">通过</button>' +
                    '<button class="btn btn-sm btn-danger act-reject" data-id="' + row.userId + '">驳回</button>' +
                    '</div></td>' +
                    '</tr>';
            }, 7);

            App.$$('.act-pass').forEach(function (btn) {
                btn.onclick = function () {
                    App.api('/api/account/audit', { userId: btn.getAttribute('data-id'), auditStatus: 1 }).done(function () {
                        App.toast('审核通过，已通知用户', 'success');
                        load();
                    });
                };
            });
            App.$$('.act-reject').forEach(function (btn) {
                btn.onclick = function () {
                    App.confirmDialog('驳回实名审核', '驳回后该账户无法登录，请填写原因以便用户核实。', function (reason) {
                        if (!reason) { App.toast('请填写驳回原因', 'error'); return; }
                        App.api('/api/account/audit', {
                            userId: btn.getAttribute('data-id'),
                            auditStatus: 2,
                            reason: reason
                        }).done(function () {
                            App.toast('已驳回并通知用户', 'success');
                            load();
                        });
                    }, '驳回原因');
                };
            });
        });
    }

    function bind() {
        App.$$('.act-audit').forEach(function (btn) {
            btn.onclick = function () {
                var name = btn.getAttribute('data-name');
                var mask = App.openModal('账户审核（' + name + '）',
                    '<label>审核结果<select id="auditStatus"><option value="1">通过</option><option value="2">驳回</option></select></label>' +
                    '<label>驳回原因 / 备注<textarea id="auditReason" placeholder="驳回时必填"></textarea></label>',
                    '<button class="btn btn-ghost" id="c0">取消</button><button class="btn btn-primary" id="c1">提交审核</button>');
                mask.querySelector('#c0').onclick = App.closeModal;
                mask.querySelector('#c1').onclick = function () {
                    App.api('/api/account/audit', {
                        userId: btn.getAttribute('data-id'),
                        auditStatus: mask.querySelector('#auditStatus').value,
                        reason: mask.querySelector('#auditReason').value.trim()
                    }).done(function () {
                        App.closeModal();
                        App.toast('审核完成', 'success');
                        load();
                    });
                };
            };
        });
        App.$$('.act-lock').forEach(function (btn) {
            btn.onclick = function () {
                App.api('/api/account/lock', { userId: btn.getAttribute('data-id'), locked: true }).done(function () {
                    App.toast('账户已锁定', 'success');
                    load();
                });
            };
        });
        App.$$('.act-unlock').forEach(function (btn) {
            btn.onclick = function () {
                App.api('/api/account/lock', { userId: btn.getAttribute('data-id'), locked: false }).done(function () {
                    App.toast('账户已解锁', 'success');
                    load();
                });
            };
        });
    }

    function auditTag(status) {
        if (status === 1) { return '<span class="tag tag-green">已通过</span>'; }
        if (status === 2) { return '<span class="tag tag-red">已驳回</span>'; }
        return '<span class="tag tag-orange">待审核</span>';
    }

    function card(label, value, cls) {
        return '<div class="stat ' + (cls || '') + '"><div class="label">' + label + '</div><div class="value">' +
            (value === undefined || value === null ? 0 : value) + '</div></div>';
    }

    load();
})();

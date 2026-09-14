/* 基础数据维护脚本（对应设计书 1.1.1 系统管理员"维护楼栋、设备类型、维修工种等基础数据"） */
(function () {
    var me = App.shell({ key: 'base-data', title: '基础数据', roles: ['admin'] });
    if (!me) { return; }

    document.getElementById('fType').onchange = load;
    document.getElementById('btnAdd').onclick = function () {
        var type = document.getElementById('fType').value;
        var value = document.getElementById('fValue').value.trim();
        if (!value) { App.toast('请填写数据值', 'error'); return; }
        App.api('/api/account/saveBaseData', {
            type: type,
            value: value,
            sortNo: document.getElementById('fSort').value
        }).done(function () {
            document.getElementById('fValue').value = '';
            App.toast('基础数据已新增', 'success');
            load();
        });
    };

    function load() {
        var type = document.getElementById('fType').value;
        App.api('/api/account/baseData', { type: type }).done(function (data) {
            var values = data.values || [];
            if (!values.length) {
                document.getElementById('listBody').innerHTML = '<div class="empty">暂无数据</div>';
                return;
            }
            document.getElementById('listBody').innerHTML = values.map(function (item, index) {
                return '<span class="tag tag-blue" style="margin:0 8px 8px 0;padding:5px 12px">' +
                    (index + 1) + '. ' + App.escapeHtml(item) +
                    ' <a href="javascript:void(0)" class="act-del" data-value="' + App.escapeHtml(item) +
                    '" style="color:#b91c1c;text-decoration:none">×</a></span>';
            }).join('');

            App.$$('.act-del').forEach(function (link) {
                link.onclick = function () {
                    App.api('/api/account/saveBaseData', {
                        type: type,
                        value: link.getAttribute('data-value'),
                        delete: true
                    }).done(function () {
                        App.toast('已删除', 'success');
                        load();
                    });
                };
            });
        });
    }

    load();
})();

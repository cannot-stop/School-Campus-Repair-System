/* 耗材管理脚本（对应设计书表 2.14 耗材表与表 1.10 耗材登记业务规则） */
(function () {
    var me = App.shell({ key: 'material', title: '耗材管理' });
    if (!me) { return; }
    var canEdit = me.role === 'manager' || me.role === 'admin';

    var addBtn = document.getElementById('btnAdd');
    if (!canEdit) { addBtn.style.display = 'none'; }
    addBtn.onclick = function () { openForm(null); };

    function load() {
        App.api('/api/material/list', {}).done(function (data) {
            App.renderRows('listBody', data.materials, function (row) {
                var actions = canEdit
                    ? '<button class="btn btn-sm btn-ghost act-edit" data-id="' + row.matId + '">编辑</button>' +
                      '<button class="btn btn-sm btn-primary act-add" data-id="' + row.matId + '">补库</button>' +
                      '<button class="btn btn-sm btn-danger act-del" data-id="' + row.matId + '">删除</button>'
                    : '<span class="muted">只读</span>';
                return '<tr>' +
                    '<td>' + row.matId + '</td>' +
                    '<td>' + App.escapeHtml(row.matName) + '</td>' +
                    '<td>' + App.escapeHtml(row.spec || '—') + '</td>' +
                    '<td>' + (row.lowStock ? '<span class="tag tag-red">' + row.stock + '</span>' : row.stock) + '</td>' +
                    '<td>' + (row.unitPrice || 0) + '</td>' +
                    '<td><div class="row-actions">' + actions + '</div></td>' +
                    '</tr>';
            }, 6);

            var low = data.lowStock || [];
            document.getElementById('lowStockBody').innerHTML = low.length
                ? low.map(function (item) {
                    return '<span class="tag tag-red" style="margin-right:8px">' + App.escapeHtml(item.displayName) +
                        '：库存 ' + item.stock + '</span>';
                }).join('')
                : '<span class="muted">暂无库存不足的耗材</span>';

            bind(data.materials);
        });
    }

    function bind(materials) {
        App.$$('.act-edit').forEach(function (btn) {
            btn.onclick = function () {
                var id = btn.getAttribute('data-id');
                var target = materials.filter(function (item) { return String(item.matId) === String(id); })[0];
                openForm(target);
            };
        });
        App.$$('.act-add').forEach(function (btn) {
            btn.onclick = function () {
                var mask = App.openModal('耗材补库',
                    '<label>补库数量<input type="number" id="addCount" value="10" min="1"></label>',
                    '<button class="btn btn-ghost" id="c0">取消</button><button class="btn btn-primary" id="c1">确认补库</button>');
                mask.querySelector('#c0').onclick = App.closeModal;
                mask.querySelector('#c1').onclick = function () {
                    App.api('/api/material/addStock', {
                        matId: btn.getAttribute('data-id'),
                        count: mask.querySelector('#addCount').value
                    }).done(function (material, message) {
                        App.closeModal();
                        App.toast(message, 'success');
                        load();
                    });
                };
            };
        });
        App.$$('.act-del').forEach(function (btn) {
            btn.onclick = function () {
                App.api('/api/material/delete', { matId: btn.getAttribute('data-id') }).done(function () {
                    App.toast('耗材已删除', 'success');
                    load();
                });
            };
        });
    }

    function openForm(material) {
        var isEdit = !!(material && material.matId);
        var mask = App.openModal(isEdit ? '编辑耗材' : '新增耗材',
            '<label>耗材名称<input type="text" id="fMatName" value="' + (isEdit ? App.escapeHtml(material.matName) : '') + '"></label>' +
            '<label>规格型号<input type="text" id="fSpec" value="' + (isEdit ? App.escapeHtml(material.spec || '') : '') + '"></label>' +
            '<label>库存数量<input type="number" id="fStock" value="' + (isEdit ? material.stock : 0) + '" min="0"></label>' +
            '<label>单价（元）<input type="text" id="fPrice" value="' + (isEdit ? material.unitPrice : '0.00') + '"></label>',
            '<button class="btn btn-ghost" id="c0">取消</button><button class="btn btn-primary" id="c1">保存</button>');
        mask.querySelector('#c0').onclick = App.closeModal;
        mask.querySelector('#c1').onclick = function () {
            App.api('/api/material/save', {
                matId: isEdit ? material.matId : '',
                matName: mask.querySelector('#fMatName').value.trim(),
                spec: mask.querySelector('#fSpec').value.trim(),
                stock: mask.querySelector('#fStock').value,
                unitPrice: mask.querySelector('#fPrice').value.trim()
            }).done(function () {
                App.closeModal();
                App.toast('耗材信息已保存', 'success');
                load();
            });
        };
    }

    load();
})();

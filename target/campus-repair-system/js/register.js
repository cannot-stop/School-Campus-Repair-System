/* 注册页脚本（对应设计书表 1.2 账户注册功能点） */
(function () {
    var form = document.getElementById('registerForm');
    var alertBox = document.getElementById('alert');
    var roleSelect = document.getElementById('roleSelect');
    var skillRow = document.getElementById('skillRow');

    roleSelect.onchange = function () {
        skillRow.className = roleSelect.value === 'worker' ? '' : 'hidden';
    };

    function showAlert(message, type) {
        alertBox.className = 'alert alert-' + (type || 'error');
        alertBox.textContent = message;
    }

    form.onsubmit = function (event) {
        event.preventDefault();
        var data = App.toQueryString(form);
        if (data.password && data.password.length < 6) {
            showAlert('密码长度不能少于 6 位');
            return;
        }
        if (data.password && /^\d+$/.test(data.password)) {
            showAlert('密码不能为纯数字，请包含字母');
            return;
        }
        if (data.role === 'worker' && !data.skillTags) {
            showAlert('维修人员注册需填写技能标签，如：水电,木工');
            return;
        }
        App.api('/api/account/register', data).done(function (result) {
            showAlert('注册成功！账户已提交实名审核，审核通过后即可登录（用户名：' + result.username + '）', 'success');
            form.reset();
            skillRow.className = 'hidden';
            setTimeout(function () { location.href = App.url('/index.html'); }, 2200);
        }).fail(function (res) {
            showAlert(res.message || '注册失败');
        });
    };
})();

/* 登录页脚本（对应设计书表 1.3 登录功能点） */
(function () {
    if (App.token() && App.user()) {
        location.href = 'home.html';
        return;
    }

    var form = document.getElementById('loginForm');
    var alertBox = document.getElementById('alert');

    function showError(message) {
        alertBox.className = 'alert alert-error';
        alertBox.textContent = message;
    }

    form.onsubmit = function (event) {
        event.preventDefault();
        var data = App.toQueryString(form);
        if (!data.username || !data.password) {
            showError('请输入用户名和密码');
            return;
        }
        App.api('/api/account/login', data).done(function (result) {
            App.setSession(result.token, result);
            App.toast('登录成功，欢迎 ' + result.realName, 'success');
            setTimeout(function () { location.href = 'home.html'; }, 350);
        }).fail(function (res) {
            showError(res.message || '登录失败');
        });
    };

    // 展示运行模式，便于确认当前连接的是内存库还是 MySQL
    fetch(App.url('/api/system/info')).then(function (r) { return r.json(); }).then(function (res) {
        if (res && res.code === 0) {
            document.getElementById('systemInfo').textContent =
                '当前运行模式：' + (res.data.storageMode === 'memory' ? '内存库（演示）' : 'MySQL 持久化') +
                ' · 服务时间 ' + res.data.serverTime;
        }
    }).catch(function () { });
})();

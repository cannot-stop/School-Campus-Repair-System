/* ============================================================================
   校园报修系统 · 前端公共脚本
   职责：会话管理、接口调用、页面外壳、状态标签、弹窗与提示
   ========================================================================== */

var App = (function () {

    var TOKEN_KEY = 'crs_token';
    var USER_KEY = 'crs_user';

    /**
     * 应用上下文路径：兼容 Tomcat 部署在 /campus_repair_system_war 等非根上下文的情况。
     * 推导顺序：容器注入的 window.__CRS_BASE__（init.jsp）→ 由 js/common.js 自身地址推导
     * → 由页面地址推导。
     */
    var BASE = (function () {
        if (typeof window.__CRS_BASE__ === 'string') {
            return window.__CRS_BASE__.replace(/\/+$/, '');
        }
        try {
            var scripts = document.getElementsByTagName('script');
            for (var i = 0; i < scripts.length; i++) {
                var src = scripts[i].src || '';
                var idx = src.indexOf('/js/common.js');
                if (idx >= 0) {
                    return src.substring(0, idx).replace(/\/+$/, '');
                }
            }
        } catch (e) { /* 忽略 */ }
        var path = location.pathname || '/';
        var slash = path.lastIndexOf('/');
        if (slash > 0 && path.substring(slash + 1).indexOf('.') >= 0) {
            path = path.substring(0, slash);
        }
        return path.replace(/\/+$/, '');
    })();

    /**
     * 把应用内路径拼接为带上下文路径的地址。
     * 非字符串参数原样返回，保证 App.url() 只作用于字符串（防御性设计）。
     */
    function url(path) {
        if (typeof path !== 'string') { return path; }
        if (!path) { return BASE || '/'; }
        if (/^(https?:)?\/\//.test(path) || path.indexOf('data:') === 0) {
            return path;
        }
        if (path.charAt(0) !== '/') {
            path = '/' + path;
        }
        return BASE + path;
    }

    /* ------------------------------------------------------------ 会话 */
    function setSession(token, user) {
        localStorage.setItem(TOKEN_KEY, token);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        writeCookie(token);
    }

    /**
     * 写入会话令牌 Cookie。
     * 注意：必须与容器下发的 Cookie 保持一致（path 使用应用上下文路径），
     * 否则浏览器会同时发送两个同名 Cookie，服务器可能取到错误的那一个（表现为登录后接口报"未登录"）。
     */
    function writeCookie(value) {
        var cookiePath = (BASE || '') + '/';
        if (cookiePath === '//') { cookiePath = '/'; }
        document.cookie = 'CRS_TOKEN=' + value + '; path=' + cookiePath + '; max-age=7200';
        // 兼容：若曾经写入过根路径的同名 Cookie（旧版本遗留），一并清理，避免重复
        if (cookiePath !== '/') {
            document.cookie = 'CRS_TOKEN=; path=/; max-age=0';
        }
    }

    function token() {
        return localStorage.getItem(TOKEN_KEY) || '';
    }

    function user() {
        try {
            return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
        } catch (e) {
            return null;
        }
    }

    function clearSession() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        var cookiePath = (BASE || '') + '/';
        if (cookiePath === '//') { cookiePath = '/'; }
        document.cookie = 'CRS_TOKEN=; path=' + cookiePath + '; max-age=0';
        document.cookie = 'CRS_TOKEN=; path=/; max-age=0';
    }

    function logout() {
        api('/api/account/logout', {}).always(function () {
            clearSession();
            location.href = url('/index.html');
        });
    }

    /* ------------------------------------------------------------ 接口 */
    function api(path, data, method) {
        var options = {
            method: method || (data && Object.keys(data).length ? 'POST' : 'GET'),
            headers: { 'Content-Type': 'application/json;charset=UTF-8' },
            credentials: 'same-origin'
        };
        if (options.method === 'GET') {
            var query = toQuery(data);
            if (query) {
                path += (path.indexOf('?') < 0 ? '?' : '&') + query;
            }
        } else {
            options.body = JSON.stringify(data || {});
        }
        var chain = { done: [], fail: [], always: [] };
        fetch(url(path), options).then(function (response) {
            return response.json();
        }).then(function (result) {
            if (result && result.code === 0) {
                chain.done.forEach(function (fn) { fn(result.data, result.message); });
            } else if (result && result.code === 1003) {
                toast(result.message || '登录状态已失效', 'error');
                clearSession();
                setTimeout(function () { location.href = url('/index.html'); }, 800);
                chain.fail.forEach(function (fn) { fn(result); });
            } else {
                var message = (result && result.message) || '请求失败';
                toast(message, 'error');
                chain.fail.forEach(function (fn) { fn(result || { message: message }); });
            }
        }).catch(function (error) {
            toast('网络请求失败：' + error, 'error');
            chain.fail.forEach(function (fn) { fn({ message: String(error) }); });
        }).then(function () {
            chain.always.forEach(function (fn) { fn(); });
        });
        return {
            done: function (fn) { chain.done.push(fn); return this; },
            fail: function (fn) { chain.fail.push(fn); return this; },
            always: function (fn) { chain.always.push(fn); return this; }
        };
    }

    function toQuery(data) {
        if (!data) { return ''; }
        var parts = [];
        Object.keys(data).forEach(function (key) {
            var value = data[key];
            if (value === null || value === undefined || value === '') { return; }
            parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
        });
        return parts.join('&');
    }

    /* ------------------------------------------------------------ 工具 */
    function $(selector) { return document.querySelector(selector); }
    function $$(selector) { return Array.prototype.slice.call(document.querySelectorAll(selector)); }

    function escapeHtml(text) {
        if (text === null || text === undefined) { return ''; }
        return String(text).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }

    function toQueryString(form) {
        var data = {};
        new FormData(form).forEach(function (value, key) { data[key] = value; });
        return data;
    }

    function value(name) {
        var el = document.querySelector('[name="' + name + '"]');
        return el ? String(el.value).trim() : '';
    }

    function toast(message, type) {
        var box = document.getElementById('toastBox');
        if (!box) {
            box = document.createElement('div');
            box.id = 'toastBox';
            document.body.appendChild(box);
        }
        var div = document.createElement('div');
        div.className = 'toast ' + (type || 'info');
        div.textContent = message;
        box.appendChild(div);
        setTimeout(function () {
            div.style.opacity = '0';
            setTimeout(function () { div.remove(); }, 200);
        }, 2800);
    }

    /* ------------------------------------------------------------ 弹窗 */
    function openModal(title, bodyHtml, footHtml, options) {
        closeModal();
        var mask = document.createElement('div');
        mask.className = 'modal-mask';
        mask.innerHTML =
            '<div class="modal ' + ((options && options.large) ? 'modal-lg' : '') + '">' +
            '  <div class="modal-head"><span>' + escapeHtml(title) + '</span>' +
            '    <span class="modal-close" id="modalClose">×</span></div>' +
            '  <div class="modal-body">' + bodyHtml + '</div>' +
            '  <div class="modal-foot">' + (footHtml || '') + '</div>' +
            '</div>';
        document.body.appendChild(mask);
        mask.querySelector('#modalClose').onclick = closeModal;
        mask.addEventListener('click', function (event) {
            if (event.target === mask) { closeModal(); }
        });
        return mask;
    }

    function closeModal() {
        var exist = document.querySelector('.modal-mask');
        if (exist) { exist.remove(); }
    }

    function confirmDialog(title, message, onOk, reasonLabel) {
        var mask = openModal(title,
            '<div class="alert alert-info">' + escapeHtml(message) + '</div>' +
            '<label>' + escapeHtml(reasonLabel || '原因 / 备注（如需要）') +
            '<textarea id="confirmReason" placeholder="请填写原因"></textarea></label>',
            '<button class="btn btn-ghost" id="confirmCancel">取消</button>' +
            '<button class="btn btn-primary" id="confirmOk">确定</button>');
        mask.querySelector('#confirmCancel').onclick = closeModal;
        mask.querySelector('#confirmOk').onclick = function () {
            var reason = mask.querySelector('#confirmReason').value.trim();
            closeModal();
            onOk(reason);
        };
    }

    /* ------------------------------------------------------------ 字典 */
    var ORDER_STATUS = {
        0: { text: '待审核', cls: 'tag-orange' },
        1: { text: '待派单', cls: 'tag-blue' },
        2: { text: '已派单', cls: 'tag-cyan' },
        3: { text: '维修中', cls: 'tag-purple' },
        4: { text: '待确认', cls: 'tag-orange' },
        5: { text: '已完成', cls: 'tag-green' },
        6: { text: '已撤销', cls: 'tag-gray' },
        7: { text: '已驳回', cls: 'tag-red' }
    };

    var TASK_STATUS = {
        0: { text: '待接单', cls: 'tag-orange' },
        1: { text: '维修中', cls: 'tag-purple' },
        2: { text: '待确认', cls: 'tag-cyan' },
        3: { text: '已完成', cls: 'tag-green' },
        4: { text: '已转单', cls: 'tag-gray' },
        5: { text: '已退单', cls: 'tag-red' },
        6: { text: '已退回派单池', cls: 'tag-gray' }
    };

    function orderStatusTag(status) {
        var item = ORDER_STATUS[status] || { text: '未知', cls: 'tag-gray' };
        return '<span class="tag ' + item.cls + '">' + item.text + '</span>';
    }

    function taskStatusTag(status) {
        var item = TASK_STATUS[status] || { text: '未知', cls: 'tag-gray' };
        return '<span class="tag ' + item.cls + '">' + item.text + '</span>';
    }

    function roleText(role) {
        return { reporter: '报修人', worker: '维修人员', manager: '维修管理员', admin: '系统管理员' }[role] || role;
    }

    function stars(score) {
        var count = parseInt(score || 0, 10);
        var html = '';
        for (var i = 1; i <= 5; i++) {
            html += i <= count ? '★' : '☆';
        }
        return '<span class="stars">' + html + '</span>';
    }

    /* ------------------------------------------------------------ 页面外壳 */
    var NAV = {
        reporter: [
            { href: '/report.html', text: '我的报修', key: 'report' },
            { href: '/report-submit.html', text: '提交报修', key: 'report-submit' },
            { href: '/messages.html', text: '消息通知', key: 'messages' }
        ],
        worker: [
            { href: '/task.html', text: '我的维修任务', key: 'task' },
            { href: '/material.html', text: '耗材库存', key: 'material' },
            { href: '/messages.html', text: '消息通知', key: 'messages' }
        ],
        manager: [
            { href: '/audit.html', text: '报修审核', key: 'audit' },
            { href: '/dispatch.html', text: '派单管理', key: 'dispatch' },
            { href: '/monitor.html', text: '维修监督', key: 'monitor' },
            { href: '/statistics.html', text: '统计分析', key: 'statistics' },
            { href: '/evaluation.html', text: '评价管理', key: 'evaluation' },
            { href: '/material.html', text: '耗材管理', key: 'material' },
            { href: '/messages.html', text: '消息通知', key: 'messages' }
        ],
        admin: [
            { href: '/admin.html', text: '用户与审核', key: 'admin' },
            { href: '/base-data.html', text: '基础数据', key: 'base-data' },
            { href: '/statistics.html', text: '统计分析', key: 'statistics' },
            { href: '/material.html', text: '耗材管理', key: 'material' },
            { href: '/messages.html', text: '消息通知', key: 'messages' }
        ]
    };

    /**
     * 初始化页面外壳：校验登录与角色、渲染侧边栏与顶部栏。
     * 页面 HTML 中需包含 <aside id="sidebar"></aside> 与 <div class="topbar">…</div>。
     */
    function shell(options) {
        var opts = options || {};
        var current = user();
        if (!current || !token()) {
            location.href = url('/index.html');
            return null;
        }
        var allowRoles = opts.roles || [];
        if (allowRoles.length && allowRoles.indexOf(current.role) < 0) {
            document.body.innerHTML = '<div class="content"><div class="alert alert-error">当前角色（' +
                roleText(current.role) + '）无权访问该页面。</div>' +
                '<a class="btn btn-primary" href="' + url('/home.html') + '">返回我的工作台</a></div>';
            return null;
        }
        var items = NAV[current.role] || [];
        var navHtml = '<a href="' + url('/home.html') + '"' + (opts.key === 'home' ? ' class="active"' : '') + '>我的工作台</a>';
        navHtml += items.map(function (item) {
            var active = (item.key === opts.key) ? ' class="active"' : '';
            return '<a href="' + url(item.href) + '"' + active + '>' + item.text + '</a>';
        }).join('');

        var sidebar = document.getElementById('sidebar');
        if (sidebar) {
            sidebar.className = 'sidebar';
            sidebar.innerHTML =
                '<div class="brand">校园报修系统<small>Campus Repair v1.0.0</small></div>' +
                '<nav class="nav">' + navHtml + '</nav>' +
                '<div class="sidebar-footer" id="modeTip">运行模式加载中…</div>';
        }
        var topbar = document.getElementById('topbar');
        if (topbar) {
            topbar.innerHTML =
                '<h1>' + escapeHtml(opts.title || '工作台') + '</h1>' +
                '<div class="user-area">' +
                '  <span>' + escapeHtml(current.realName || current.username) + '（' + roleText(current.role) + '）</span>' +
                '  <span>未读 <span class="badge-dot" id="unreadBadge">0</span></span>' +
                '  <a href="javascript:void(0)" id="btnLogout">退出登录</a>' +
                '</div>';
            var btn = document.getElementById('btnLogout');
            if (btn) { btn.onclick = logout; }
        }
        refreshUnread();
        setInterval(refreshUnread, 30000);
        loadModeTip();
        return current;
    }

    function refreshUnread() {
        if (!token()) { return; }
        api('/api/message/unread', {}).done(function (count) {
            var badge = document.getElementById('unreadBadge');
            if (badge) { badge.textContent = count || 0; }
        });
    }

    function loadModeTip() {
        fetch(url('/api/system/info')).then(function (r) { return r.json(); }).then(function (res) {
            if (res && res.code === 0) {
                var tip = document.getElementById('modeTip');
                if (tip) {
                    tip.textContent = (res.data.storageMode === 'memory' ? '内存库演示模式' : 'MySQL 持久化模式') +
                        ' · 在线 ' + res.data.online;
                }
            }
        }).catch(function () { });
    }

    /* ------------------------------------------------------------ 渲染辅助 */
    function renderRows(tbodyId, rows, renderFn, colspan) {
        var tbody = document.getElementById(tbodyId);
        if (!tbody) { return; }
        if (!rows || !rows.length) {
            tbody.innerHTML = '<tr><td colspan="' + (colspan || 8) + '" class="empty">暂无数据</td></tr>';
            return;
        }
        tbody.innerHTML = rows.map(renderFn).join('');
    }

    function formatDate(text) {
        if (!text) { return '—'; }
        return String(text).replace('T', ' ').substring(0, 16);
    }

    return {
        api: api,
        url: url,
        base: BASE,
        token: token,
        user: user,
        setSession: setSession,
        clearSession: clearSession,
        logout: logout,
        shell: shell,
        $: $,
        $$: $$,
        escapeHtml: escapeHtml,
        toQueryString: toQueryString,
        value: value,
        toast: toast,
        openModal: openModal,
        closeModal: closeModal,
        confirmDialog: confirmDialog,
        orderStatusTag: orderStatusTag,
        taskStatusTag: taskStatusTag,
        roleText: roleText,
        stars: stars,
        renderRows: renderRows,
        formatDate: formatDate,
        ORDER_STATUS: ORDER_STATUS,
        TASK_STATUS: TASK_STATUS
    };
})();

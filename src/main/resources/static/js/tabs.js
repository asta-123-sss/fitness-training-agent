(function () {
  'use strict';

  var tabButtons = Array.prototype.slice.call(document.querySelectorAll('.tab-btn'));
  var sections = {
    chat: document.getElementById('tab-chat'),
    body: document.getElementById('tab-body'),
    diet: document.getElementById('tab-diet'),
    log: document.getElementById('tab-log')
  };

  tabButtons.forEach(function (btn) {
    btn.addEventListener('click', function () { activateTab(btn.getAttribute('data-tab')); });
  });

  function activateTab(name) {
    if (!sections[name]) { return; }
    tabButtons.forEach(function (b) {
      b.classList.toggle('tab-active', b.getAttribute('data-tab') === name);
    });
    Object.keys(sections).forEach(function (key) {
      sections[key].classList.toggle('hidden', key !== name);
    });
    // 切到某个业务模块时刷新其数据
    var refreshers = {
      body: window.BodyCalculator && window.BodyCalculator.refresh,
      diet: window.DietTool && window.DietTool.refresh,
      log: window.WorkoutLog && window.WorkoutLog.refresh
    };
    var fn = refreshers[name];
    if (typeof fn === 'function') { fn(); }
  }

  /* ================= 共享工具（供三个业务模块使用） ================= */

  async function api(url, options) {
    const res = await Auth.authFetch(url, options);
    if (res.status === 401) {
      Auth.redirectToLogin();
      throw new Error('未登录');
    }
    return res;
  }

  async function json(url, options) {
    const res = await api(url, options);
    return res.json();
  }

  var toastTimer = null;
  function toast(msg) {
    var t = document.getElementById('fitToast');
    if (!t) {
      t = document.createElement('div');
      t.id = 'fitToast';
      t.className = 'toast';
      document.body.appendChild(t);
    }
    t.textContent = msg;
    requestAnimationFrame(function () { t.classList.add('show'); });
    if (toastTimer) { clearTimeout(toastTimer); }
    toastTimer = setTimeout(function () { t.classList.remove('show'); }, 1800);
  }

  function esc(s) {
    return (s == null ? '' : String(s)).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function today() {
    var d = new Date();
    function p(n) { return n < 10 ? '0' + n : '' + n; }
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
  }

  function round1(v) { return Math.round(v * 10) / 10; }

  window.FitUtil = { api: api, json: json, toast: toast, esc: esc, today: today, round1: round1 };
})();

(function () {
  'use strict';

  const params = new URLSearchParams(location.search);
  const qrId = params.get('qrId');

  const loginPanel = document.getElementById('loginPanel');
  const confirmPanel = document.getElementById('confirmPanel');
  const loginBtn = document.getElementById('loginBtn');
  const confirmBtn = document.getElementById('confirmBtn');
  const cancelBtn = document.getElementById('cancelBtn');
  const nickname = document.getElementById('nickname');
  const statusCard = document.getElementById('statusCard');
  const statusText = document.getElementById('statusText');
  const errorCard = document.getElementById('errorCard');

  loginBtn.addEventListener('click', doLogin);
  confirmBtn.addEventListener('click', doConfirm);
  cancelBtn.addEventListener('click', () => showDone('已取消，可关闭本页面'));

  init();

  function init() {
    if (!qrId) { showError('缺少二维码标识，请重新扫码'); return; }
    if (Auth.isLoggedIn()) { loadMe(); } else { loginPanel.style.display = 'block'; }
  }

  async function loadMe() {
    try {
      const res = await Auth.authFetch('/api/auth/me');
      if (res.status === 401) { Auth.logout(); loginPanel.style.display = 'block'; return; }
      const data = await res.json();
      if (data && data.code === 0) {
        nickname.textContent = data.data.nickname || data.data.phone;
        confirmPanel.style.display = 'block';
      } else {
        loginPanel.style.display = 'block';
      }
    } catch (e) { loginPanel.style.display = 'block'; }
  }

  async function doLogin() {
    const phone = document.getElementById('phone').value.trim();
    const password = document.getElementById('password').value;
    if (!phone || !password) { showError('请填写手机号和密码'); return; }
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: phone, password: password })
      });
      const data = await res.json();
      if (data && data.code === 0) {
        Auth.setToken(data.data.token);
        loginPanel.style.display = 'none';
        hideError();
        loadMe();
      } else {
        showError((data && data.message) || '登录失败');
      }
    } catch (e) { showError('网络异常，请稍后重试'); }
  }

  async function doConfirm() {
    hideError();
    showDone('确认中…');
    try {
      const res = await Auth.authFetch('/api/auth/qr/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ qrId: qrId })
      });
      const data = await res.json();
      if (data && data.code === 0) {
        confirmPanel.style.display = 'none';
        showDone('已确认，电脑端即将登录 ✅');
      } else if (res.status === 401) {
        Auth.logout();
        loginPanel.style.display = 'block';
        statusCard.style.display = 'none';
        showError('登录已过期，请重新登录');
      } else {
        statusCard.style.display = 'none';
        confirmPanel.style.display = 'block';
        showError((data && data.message) || '确认失败');
      }
    } catch (e) {
      statusCard.style.display = 'none';
      confirmPanel.style.display = 'block';
      showError('网络异常，请稍后重试');
    }
  }

  function showDone(text) {
    statusCard.style.display = 'flex';
    const sp = statusCard.querySelector('.spinner');
    if (sp) { sp.style.display = 'none'; }
    statusText.textContent = text;
  }

  function showError(text) { errorCard.style.display = 'block'; errorCard.textContent = '⚠️ ' + text; }
  function hideError() { errorCard.style.display = 'none'; }
})();

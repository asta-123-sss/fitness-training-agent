(function () {
  'use strict';

  const tabs = document.querySelectorAll('.tab');
  const panels = {
    login: document.getElementById('panel-login'),
    register: document.getElementById('panel-register'),
    qr: document.getElementById('panel-qr')
  };
  const loginBtn = document.getElementById('loginBtn');
  const registerBtn = document.getElementById('registerBtn');
  const qrRefreshBtn = document.getElementById('qrRefreshBtn');
  const errorCard = document.getElementById('errorCard');
  const qrCode = document.getElementById('qrCode');
  const qrHint = document.getElementById('qrHint');

  let qrTimer = null;

  // 已登录则直接进主页
  if (Auth.isLoggedIn()) { location.href = '/index.html'; }

  tabs.forEach(tab => tab.addEventListener('click', () => switchTab(tab.dataset.tab)));
  loginBtn.addEventListener('click', doLogin);
  registerBtn.addEventListener('click', doRegister);
  qrRefreshBtn.addEventListener('click', startQr);

  function switchTab(name) {
    tabs.forEach(t => t.classList.toggle('active', t.dataset.tab === name));
    Object.keys(panels).forEach(k => { panels[k].style.display = k === name ? 'block' : 'none'; });
    hideError();
    if (name === 'qr') { startQr(); } else { stopQr(); }
  }

  async function doLogin() {
    const phone = document.getElementById('loginPhone').value.trim();
    const password = document.getElementById('loginPassword').value;
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
        location.href = '/index.html';
      } else {
        showError((data && data.message) || '登录失败');
      }
    } catch (e) { showError('网络异常，请稍后重试'); }
  }

  async function doRegister() {
    const phone = document.getElementById('regPhone').value.trim();
    const password = document.getElementById('regPassword').value;
    const nickname = document.getElementById('regNickname').value.trim();
    if (!phone || !password) { showError('请填写手机号和密码'); return; }
    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: phone, password: password, nickname: nickname })
      });
      const data = await res.json();
      if (data && data.code === 0) {
        Auth.setToken(data.data.token);
        location.href = '/index.html';
      } else {
        showError((data && data.message) || '注册失败');
      }
    } catch (e) { showError('网络异常，请稍后重试'); }
  }

  async function startQr() {
    stopQr();
    hideError();
    qrHint.textContent = '二维码生成中…';
    try {
      const res = await fetch('/api/auth/qr/create', { method: 'POST' });
      const data = await res.json();
      if (!data || data.code !== 0) { showError((data && data.message) || '二维码生成失败'); return; }
      const qrId = data.data.qrId;
      const url = location.origin + '/confirm.html?qrId=' + encodeURIComponent(qrId);
      qrCode.innerHTML = '';
      if (window.QRCode) {
        new QRCode(qrCode, { text: url, width: 200, height: 200, correctLevel: QRCode.CorrectLevel.M });
      } else {
        qrCode.textContent = '二维码库未加载';
      }
      qrHint.textContent = '请使用手机浏览器扫描二维码';
      pollQr(qrId);
    } catch (e) { showError('网络异常，请稍后重试'); }
  }

  function pollQr(qrId) {
    stopQr();
    qrTimer = setInterval(async () => {
      try {
        const res = await fetch('/api/auth/qr/status?qrId=' + encodeURIComponent(qrId));
        const data = await res.json();
        if (!data || data.code !== 0) { return; }
        const d = data.data;
        if (d.status === 'CONFIRMED' && d.token) {
          stopQr();
          Auth.setToken(d.token);
          location.href = '/index.html';
        } else if (d.status === 'EXPIRED') {
          stopQr();
          qrHint.textContent = '二维码已过期，请点击下方刷新';
          showError('二维码已过期，请刷新重试');
        }
      } catch (e) { /* 轮询偶发失败忽略 */ }
    }, 2000);
  }

  function stopQr() { if (qrTimer) { clearInterval(qrTimer); qrTimer = null; } }

  function showError(text) { errorCard.style.display = 'block'; errorCard.textContent = '⚠️ ' + text; }
  function hideError() { errorCard.style.display = 'none'; }
})();

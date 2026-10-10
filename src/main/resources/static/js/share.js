(function () {
  'use strict';

  const params = new URLSearchParams(location.search);
  const sessionId = params.get('sessionId');
  const conversationId = params.get('conversationId');

  const statusCard = document.getElementById('statusCard');
  const statusText = document.getElementById('statusText');
  const errorCard = document.getElementById('errorCard');
  const resultCard = document.getElementById('resultCard');
  const planContent = document.getElementById('planContent');
  const shareBox = document.getElementById('shareBox');
  const shareLink = document.getElementById('shareLink');
  const copyBtn = document.getElementById('copyBtn');
  const qrcode = document.getElementById('qrcode');

  let timer = null;

  copyBtn.addEventListener('click', copyLink);

  if (conversationId) {
    loadConversationShare(conversationId);
  } else if (sessionId) {
    timer = setInterval(poll, 2000);
    poll();
  } else {
    showError('缺少 sessionId 或 conversationId 参数');
  }

  function loadConversationShare(cid) {
    fetch('/api/conversation/' + encodeURIComponent(cid) + '/share')
      .then((r) => r.json())
      .then((data) => {
        if (!data || data.code !== 0) {
          showError((data && data.message) || '加载失败');
          return;
        }
        statusCard.style.display = 'none';
        resultCard.style.display = 'block';
        renderMarkdown(data.data.planMarkdown || '');
        buildShare();
      })
      .catch(() => showError('加载失败，请重试'));
  }

  function poll() {
    fetch('/api/task/' + encodeURIComponent(sessionId) + '/share')
      .then((r) => r.json())
      .then((data) => {
        if (!data || data.code !== 0) {
          showError((data && data.message) || '加载失败');
          clearInterval(timer);
          return;
        }
        const d = data.data;
        if (d.status === 'COMPLETED') {
          clearInterval(timer);
          statusCard.style.display = 'none';
          resultCard.style.display = 'block';
          renderMarkdown(d.planMarkdown || '');
          buildShare();
        } else if (d.status === 'FAILED') {
          clearInterval(timer);
          showError(d.errorMsg || '该方案生成失败');
        } else {
          showStatus(d.statusLabel + '…');
        }
      })
      .catch(() => { /* 偶发失败忽略，继续轮询 */ });
  }

  function renderMarkdown(md) {
    if (window.marked && typeof marked.parse === 'function') {
      planContent.innerHTML = marked.parse(md);
    } else if (window.marked) {
      planContent.innerHTML = marked(md);
    } else {
      planContent.textContent = md;
      planContent.style.whiteSpace = 'pre-wrap';
    }
  }

  function buildShare() {
    const url = location.href;
    shareLink.value = url;
    if (window.QRCode) {
      qrcode.innerHTML = '';
      new QRCode(qrcode, { text: url, width: 160, height: 160, correctLevel: QRCode.CorrectLevel.M });
    } else {
      qrcode.textContent = '二维码库未加载，请复制链接分享';
    }
    shareBox.style.display = 'flex';
  }

  function copyLink() {
    shareLink.select();
    try { document.execCommand('copy'); } catch (e) { /* ignore */ }
    copyBtn.textContent = '已复制';
    setTimeout(() => { copyBtn.textContent = '复制链接'; }, 1500);
  }

  function showStatus(text) {
    statusCard.style.display = 'flex';
    statusText.textContent = text;
  }

  function showError(text) {
    statusCard.style.display = 'none';
    errorCard.style.display = 'block';
    errorCard.textContent = '⚠️ ' + text;
  }
})();

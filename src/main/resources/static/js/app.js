(function () {
  'use strict';

  const input = document.getElementById('input');
  const submitBtn = document.getElementById('submitBtn');
  const chips = document.querySelectorAll('.chip');
  const attachToggle = document.getElementById('attachToggle');
  const attachMenu = document.getElementById('attachMenu');
  const uploadBtn = document.getElementById('uploadBtn');
  const fileInput = document.getElementById('fileInput');
  const attachmentList = document.getElementById('attachmentList');
  const sendIcon = document.getElementById('sendIcon');
  const sendSpinner = document.getElementById('sendSpinner');
  const loginBtn = document.getElementById('loginBtn');
  const userBar = document.getElementById('userBar');
  const userName = document.getElementById('userName');
  const logoutBtn = document.getElementById('logoutBtn');
  const historyCard = document.getElementById('historyCard');
  const historyList = document.getElementById('historyList');
  const newTaskBtn = document.getElementById('newTaskBtn');
  const chatScroll = document.getElementById('chatScroll');
  const chatList = document.getElementById('chatList');
  const welcomeBlock = document.getElementById('welcomeBlock');

  let pollTimer = null;
  let attachments = [];
  let streamTimer = null;
  let streamInterval = null;
  let ctxMenu = null;
  let toastTimer = null;
  let currentAssistant = null;
  let currentConversationId = null;

  chips.forEach(chip => {
    chip.addEventListener('click', () => {
      chips.forEach(c => c.classList.remove('chip-active'));
      chip.classList.add('chip-active');
      input.value = chip.dataset.text || '';
      autoResizeTextarea();
      input.focus();
    });
  });
  attachToggle.addEventListener('click', (e) => {
    e.stopPropagation();
    attachMenu.classList.toggle('hidden');
  });
  document.addEventListener('click', () => attachMenu.classList.add('hidden'));
  uploadBtn.addEventListener('click', () => {
    attachMenu.classList.add('hidden');
    fileInput.click();
  });
  fileInput.addEventListener('change', onFilesSelected);
  submitBtn.addEventListener('click', submit);
  logoutBtn.addEventListener('click', logout);
  loginBtn.addEventListener('click', () => { location.href = '/login.html'; });
  newTaskBtn.addEventListener('click', () => {
    reset();
    clearChat();
    currentConversationId = null;
    input.value = '';
    clearAttachments();
    enableSubmit();
    autoResizeTextarea();
    input.focus();
  });
  input.addEventListener('input', autoResizeTextarea);

  init();

  /** 带 401 跳转的请求封装。 */
  async function api(url, options) {
    const res = await Auth.authFetch(url, options);
    if (res.status === 401) {
      Auth.redirectToLogin();
      throw new Error('未登录');
    }
    return res;
  }

  async function init() {
    loadMe();
    loadHistory();
  }

  async function loadMe() {
    try {
      const res = await Auth.authFetch('/api/auth/me');
      if (res.status === 401) {
        loginBtn.style.display = 'block';
        return;
      }
      const data = await res.json();
      if (data && data.code === 0) {
        userName.textContent = data.data.nickname || data.data.phone;
        userBar.style.display = 'flex';
        loginBtn.style.display = 'none';
      }
    } catch (e) { /* ignore */ }
  }

  async function loadHistory() {
    try {
      const res = await Auth.authFetch('/api/conversation/list');
      if (res.status === 401) { return; }
      const data = await res.json();
      if (!data || data.code !== 0) { return; }
      const list = data.data || [];
      if (list.length === 0) {
        historyCard.style.display = 'none';
        return;
      }
      historyCard.style.display = 'block';
      historyList.innerHTML = '';
      list.forEach(item => {
        const el = document.createElement('div');
        el.className = 'history-item';
        el.setAttribute('data-cid', item.conversationId);
        const title = item.title || item.userInput || '';
        const isPinned = !!item.pinned;
        el.innerHTML =
          '<div class="history-head">' +
            '<span class="status-pill status-' + (item.status || '').toLowerCase() + '">' + escapeHtml(item.statusLabel || '') + '</span>' +
            '<div class="history-actions">' +
              (item.projectName ? '<span class="project-tag">' + escapeHtml(item.projectName) + '</span>' : '') +
              '<span class="history-time">' + escapeHtml(formatTime(item.createdAt)) + '</span>' +
              '<button class="history-more" type="button" title="更多">⋯</button>' +
            '</div>' +
          '</div>' +
          '<div class="history-text">' + (isPinned ? '<span class="pin-dot">📌</span> ' : '') + escapeHtml(shorten(title)) + '</div>';
        el.addEventListener('click', () => openConversation(item.conversationId));
        el.querySelector('.history-more').addEventListener('click', (e) => {
          e.stopPropagation();
          openCtxMenu(item, el.querySelector('.history-more'));
        });
        historyList.appendChild(el);
      });
    } catch (e) { /* ignore */ }
  }

  async function openConversation(conversationId) {
    reset();
    clearChat();
    currentConversationId = conversationId;
    try {
      const res = await api('/api/conversation/' + encodeURIComponent(conversationId));
      const data = await res.json();
      if (!data || data.code !== 0) {
        showError((data && data.message) || '加载对话失败');
        return;
      }
      const messages = data.data.messages || [];
      messages.forEach(m => {
        addUserMessage(m.userInput || '');
        const a = addAssistantMessage();
        a.rawText = m.planMarkdown || '';
        if (m.status === 'COMPLETED') {
          a.copy.classList.remove('hidden');
          currentAssistant = a;
          renderMarkdown(m.planMarkdown || '');
          currentAssistant = null;
        } else {
          a.content.innerHTML = '<div class="text-red-600">⚠️ ' + escapeHtml(m.statusLabel || '该轮未完成') + '</div>';
        }
      });
      scrollToBottom(true);
    } catch (e) {
      showError('加载对话失败，请重试');
    }
  }

  async function logout() {
    try { await api('/api/auth/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
    Auth.logout();
    location.reload();
  }

  /** 组装提交内容：自由文本 + 附件。 */
  function buildMessage() {
    const free = input.value.trim();
    const att = buildAttachmentsText();
    return [free, att].filter(Boolean).join('\n\n');
  }

  function buildAttachmentsText() {
    const parts = [];
    attachments.forEach(att => {
      if (att.type === 'text' && att.text) {
        parts.push('【附件：' + att.name + '】\n' + att.text);
      } else if (att.type === 'image') {
        parts.push('【已上传图片：' + att.name + '】');
      }
    });
    return parts.join('\n\n');
  }

  function onFilesSelected() {
    const files = Array.from(fileInput.files || []);
    fileInput.value = '';
    files.forEach(handleFile);
  }

  function handleFile(file) {
    const type = file.type || '';
    const isImage = type.indexOf('image/') === 0;
    const isText = type.indexOf('text/') === 0
      || type === 'application/json'
      || /\.(txt|md|csv|json)$/i.test(file.name);
    if (!isImage && !isText) {
      showError('仅支持图片或文本文件（txt / md / csv / json）');
      return;
    }
    const att = {
      id: Date.now() + '-' + Math.random().toString(36).slice(2),
      name: file.name,
      size: file.size,
      type: isImage ? 'image' : 'text',
      uploading: true,
      url: null,
      text: ''
    };
    attachments.push(att);
    renderAttachments();
    if (isImage) { uploadImage(att, file); } else { readText(att, file); }
  }

  async function uploadImage(att, file) {
    try {
      const fd = new FormData();
      fd.append('file', file);
      const res = await api('/api/upload', { method: 'POST', body: fd });
      const data = await res.json();
      if (!data || data.code !== 0) {
        throw new Error((data && data.message) || '上传失败');
      }
      att.url = data.data.url;
      att.uploading = false;
    } catch (e) {
      attachments = attachments.filter(a => a.id !== att.id);
      showError('图片上传失败，请重试');
    }
    renderAttachments();
  }

  function readText(att, file) {
    if (file.size > 512 * 1024) {
      attachments = attachments.filter(a => a.id !== att.id);
      showError('文本文件过大（超过 512KB）');
      renderAttachments();
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      att.text = String(reader.result || '').trim();
      att.uploading = false;
      renderAttachments();
    };
    reader.onerror = () => {
      attachments = attachments.filter(a => a.id !== att.id);
      showError('文件读取失败');
      renderAttachments();
    };
    reader.readAsText(file);
  }

  function renderAttachments() {
    if (attachments.length === 0) {
      attachmentList.style.display = 'none';
      attachmentList.innerHTML = '';
      return;
    }
    attachmentList.style.display = 'flex';
    attachmentList.innerHTML = '';
    attachments.forEach(att => {
      const chip = document.createElement('div');
      chip.className = 'flex items-center gap-2 rounded-xl border border-gray-200 bg-gray-50 py-1.5 pl-2 pr-1.5';
      if (att.type === 'image') {
        if (att.uploading) {
          chip.innerHTML = '<span class="flex h-8 w-8 items-center justify-center rounded-lg bg-gray-200 text-gray-400"><svg class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg></span>';
        } else {
          chip.innerHTML = '<img src="' + escapeHtml(att.url || '') + '" alt="" class="h-8 w-8 rounded-lg object-cover">';
        }
      } else {
        chip.innerHTML = '<span class="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-50 text-sm">📄</span>';
      }
      chip.innerHTML += '<span class="max-w-[140px] truncate text-xs text-gray-600">' + escapeHtml(att.name) + '</span>'
        + '<button type="button" class="flex h-5 w-5 items-center justify-center rounded-full text-gray-400 transition hover:bg-gray-200 hover:text-gray-600" data-id="' + att.id + '">×</button>';
      chip.querySelector('button[data-id]').addEventListener('click', () => removeAttachment(att.id));
      attachmentList.appendChild(chip);
    });
  }

  function removeAttachment(id) {
    attachments = attachments.filter(a => a.id !== id);
    renderAttachments();
  }

  function clearAttachments() {
    attachments = [];
    renderAttachments();
  }

  async function submit() {
    const message = buildMessage();
    if (!message) { showError('请先填写你的健身需求'); return; }

    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    clearStream();

    addUserMessage(message);
    currentAssistant = addAssistantMessage();
    showStatus('待解析…');
    input.value = '';
    autoResizeTextarea();
    setSubmitLoading(true);

    try {
      if (!currentConversationId) {
        const cr = await api('/api/conversation', { method: 'POST' });
        const cd = await cr.json();
        if (!cd || cd.code !== 0) {
          throw new Error((cd && cd.message) || '创建对话失败');
        }
        currentConversationId = cd.data.conversationId;
      }
      const res = await api('/api/task/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: message, conversationId: currentConversationId })
      });
      const data = await res.json();
      if (!data || data.code !== 0) {
        showError((data && data.message) || '提交失败，请重试');
        enableSubmit();
        return;
      }
      startPolling(data.data.sessionId);
      loadHistory();
      clearAttachments();
    } catch (e) {
      showError('网络异常，请稍后重试');
      enableSubmit();
    }
  }

  function startPolling(sessionId) {
    showStatus('待解析…');
    pollTimer = setInterval(() => poll(sessionId), 2000);
  }

  async function poll(sessionId) {
    try {
      const res = await api('/api/task/' + encodeURIComponent(sessionId));
      const data = await res.json();
      if (!data || data.code !== 0) { return; }
      const d = data.data;
      showStatus(d.statusLabel + '…');

      if (d.status === 'COMPLETED') {
        clearInterval(pollTimer);
        loadResult(sessionId);
      } else if (d.status === 'FAILED') {
        clearInterval(pollTimer);
        showError(d.errorMsg || '生成失败，请重试');
        enableSubmit();
      }
    } catch (e) {
      // 轮询偶发失败忽略，继续下一轮
    }
  }

  async function loadResult(sessionId, instant) {
    try {
      const res = await api('/api/task/' + encodeURIComponent(sessionId) + '/result');
      const data = await res.json();
      if (!data || data.code !== 0) {
        showError((data && data.message) || '获取结果失败');
        return;
      }
      const userInput = data.data.userInput || '';
      const md = data.data.planMarkdown || '';
      if (!currentAssistant) {
        if (userInput) { addUserMessage(userInput); }
        currentAssistant = addAssistantMessage();
      }
      hideStatus();
      currentAssistant.rawText = md;
      currentAssistant.copy.classList.remove('hidden');
      if (instant) {
        renderMarkdown(md);
        scrollToBottom(true);
      } else {
        streamPlan(md);
        scrollToBottom(true);
      }
      loadHistory();
    } catch (e) {
      showError('获取结果失败，请重试');
    } finally {
      enableSubmit();
    }
  }

  function renderMarkdown(md) {
    const el = currentAssistant ? currentAssistant.content : null;
    if (!el) { return; }
    if (window.marked && typeof marked.parse === 'function') {
      el.innerHTML = marked.parse(md);
    } else if (window.marked) {
      el.innerHTML = marked(md);
    } else {
      el.textContent = md;
      el.style.whiteSpace = 'pre-wrap';
    }
  }

  /** 流式输出：先展示“思考中”，再一行一行渲染 Markdown。 */
  function streamPlan(md, onDone) {
    clearStream();
    const lines = (md || '').split('\n');
    if (lines.length === 0 || (lines.length === 1 && lines[0].trim() === '')) {
      renderMarkdown('');
      if (onDone) { onDone(); }
      return;
    }
    let i = 0;
    if (currentAssistant) { currentAssistant.content.innerHTML = thinkingHtml(); }
    const lineInterval = Math.max(15, Math.min(40, Math.round(3200 / lines.length)));
    streamTimer = setTimeout(() => {
      streamInterval = setInterval(() => {
        i++;
        renderMarkdown(lines.slice(0, i).join('\n'));
        scrollToBottom(false);
        if (i >= lines.length) {
          clearStream();
          if (onDone) { onDone(); }
        }
      }, lineInterval);
    }, 900);
  }

  function thinkingHtml() {
    return '<div class="flex items-center gap-2 text-sm text-gray-500">'
      + '<div class="h-4 w-4 shrink-0 animate-spin rounded-full border-2 border-gray-200 border-t-blue-600"></div>'
      + '<span>正在思考…</span></div>';
  }

  function clearStream() {
    if (streamTimer) { clearTimeout(streamTimer); streamTimer = null; }
    if (streamInterval) { clearInterval(streamInterval); streamInterval = null; }
  }

  function showStatus(text) {
    if (!currentAssistant) { currentAssistant = addAssistantMessage(); }
    currentAssistant.status.classList.remove('hidden');
    currentAssistant.statusText.textContent = text;
  }

  function hideStatus() {
    if (currentAssistant) { currentAssistant.status.classList.add('hidden'); }
  }

  function showError(text) {
    if (currentAssistant) {
      currentAssistant.status.classList.add('hidden');
      currentAssistant.content.innerHTML = '<div class="text-red-600">⚠️ ' + escapeHtml(text) + '</div>';
      currentAssistant = null;
    } else {
      const row = document.createElement('div');
      row.className = 'flex items-start gap-3';
      row.innerHTML = '<div class="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-red-50 text-base">⚠️</div>'
        + '<div class="min-w-0 flex-1 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">' + escapeHtml(text) + '</div>';
      chatList.appendChild(row);
    }
    hideWelcome();
    scrollToBottom(true);
  }

  function reset() {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    clearStream();
    currentAssistant = null;
  }

  function setSubmitLoading(loading) {
    submitBtn.disabled = loading;
    sendIcon.classList.toggle('hidden', loading);
    sendSpinner.classList.toggle('hidden', !loading);
  }

  function enableSubmit() {
    setSubmitLoading(false);
  }

  /* ================= 对话消息渲染 ================= */

  function clearChat() {
    chatList.innerHTML = '';
    currentAssistant = null;
    welcomeBlock.classList.remove('hidden');
  }

  function hideWelcome() {
    welcomeBlock.classList.add('hidden');
  }

  function addUserMessage(text) {
    hideWelcome();
    const row = document.createElement('div');
    row.className = 'flex justify-end';
    const bubble = document.createElement('div');
    bubble.className = 'max-w-[85%] whitespace-pre-wrap break-words rounded-2xl rounded-tr-md bg-gray-900 px-4 py-2.5 text-sm leading-relaxed text-white';
    bubble.textContent = text;
    row.appendChild(bubble);
    chatList.appendChild(row);
    scrollToBottom(true);
    return row;
  }

  function addAssistantMessage() {
    hideWelcome();
    const row = document.createElement('div');
    row.className = 'flex items-start gap-3';
    row.innerHTML =
      '<div class="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-50 text-base">🏋️</div>' +
      '<div class="min-w-0 flex-1">' +
        '<div class="status-row hidden flex items-center gap-2 py-1 text-sm text-gray-500">' +
          '<div class="h-4 w-4 shrink-0 animate-spin rounded-full border-2 border-gray-200 border-t-blue-600"></div>' +
          '<span class="status-text"></span>' +
        '</div>' +
        '<div class="markdown-body content"></div>' +
        '<div class="mt-2 flex justify-start">' +
          '<button type="button" class="copy-btn hidden flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-2.5 py-1 text-xs text-gray-500 transition hover:border-gray-300 hover:bg-gray-50 hover:text-gray-700">' +
            '<svg class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>' +
            '<span>复制</span>' +
          '</button>' +
        '</div>' +
      '</div>';
    chatList.appendChild(row);
    scrollToBottom(true);
    const obj = {
      row: row,
      status: row.querySelector('.status-row'),
      statusText: row.querySelector('.status-text'),
      content: row.querySelector('.content'),
      copy: row.querySelector('.copy-btn'),
      rawText: ''
    };
    obj.copy.addEventListener('click', () => {
      const text = obj.rawText || obj.content.innerText || '';
      copyToClipboard(text)
        .then(() => showToast('已复制'))
        .catch(() => showToast('复制失败'));
    });
    return obj;
  }

  function scrollToBottom(force) {
    const el = chatScroll;
    requestAnimationFrame(() => {
      const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 120;
      if (force || nearBottom) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }

  function autoResizeTextarea() {
    const max = 200;
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, max) + 'px';
    input.style.overflowY = input.scrollHeight > max ? 'auto' : 'hidden';
  }

  function formatTime(iso) {
    if (!iso) { return ''; }
    const d = new Date(iso);
    if (isNaN(d.getTime())) { return ''; }
    const p = n => (n < 10 ? '0' + n : '' + n);
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
  }

  function shorten(text) {
    if (!text) { return ''; }
    return text.length > 30 ? text.slice(0, 30) + '…' : text;
  }

  function escapeHtml(text) {
    return (text || '').replace(/[&<>"']/g, c => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  }

  /* ================= 历史条目「更多」上下文菜单 ================= */

  function openCtxMenu(item, anchor) {
    closeCtxMenu();
    ctxMenu = document.createElement('div');
    ctxMenu.className = 'ctx-menu';
    ctxMenu.innerHTML = buildCtxMenuHtml(item);
    document.body.appendChild(ctxMenu);

    // 事件委托：处理所有 data-action（含动态重建的「新建项目」按钮）
    ctxMenu.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) { return; }
      e.stopPropagation();
      if (btn.dataset.action === 'new-project') {
        showNewProjectInput(btn, item);
      } else {
        handleCtxAction(btn.dataset.action, item);
      }
    });

    loadProjectsIntoMenu(ctxMenu, item);
    positionCtxMenu(ctxMenu, anchor);

    document.addEventListener('mousedown', onDocDown, true);
    document.addEventListener('keydown', onEsc, true);
  }

  function closeCtxMenu() {
    if (!ctxMenu) { return; }
    ctxMenu.remove();
    ctxMenu = null;
    document.removeEventListener('mousedown', onDocDown, true);
    document.removeEventListener('keydown', onEsc, true);
  }

  function onDocDown(e) {
    if (ctxMenu && !ctxMenu.contains(e.target)) { closeCtxMenu(); }
  }

  function onEsc(e) {
    if (e.key === 'Escape') { closeCtxMenu(); }
  }

  function buildCtxMenuHtml(item) {
    const pinned = !!item.pinned;
    return '' +
      '<div class="ctx-item" data-action="pin">' + (pinned ? '取消置顶' : '置顶') + '<span class="ctx-kbd">Ctrl+Shift+P</span></div>' +
      '<div class="ctx-item" data-action="rename">重命名<span class="ctx-kbd">Ctrl+Shift+R</span></div>' +
      '<div class="ctx-item has-sub">在新对话中继续<span class="ctx-arrow">›</span>' +
        '<div class="ctx-sub">' +
          '<div class="ctx-item" data-action="continue-question">携带问题</div>' +
          '<div class="ctx-item" data-action="continue-plan">携带方案</div>' +
        '</div>' +
      '</div>' +
      '<div class="ctx-item has-sub">移动到项目<span class="ctx-arrow">›</span>' +
        '<div class="ctx-sub">' +
          '<div class="ctx-sub-projects" data-role="projects"><div class="ctx-empty">加载中…</div></div>' +
          '<div class="ctx-sub-footer"><div class="ctx-item" data-action="new-project">＋ 新建项目</div></div>' +
        '</div>' +
      '</div>' +
      '<div class="ctx-item has-sub">复制<span class="ctx-arrow">›</span>' +
        '<div class="ctx-sub">' +
          '<div class="ctx-item" data-action="copy-text">复制文本</div>' +
          '<div class="ctx-item" data-action="copy-link">复制链接</div>' +
        '</div>' +
      '</div>' +
      '<div class="ctx-divider"></div>' +
      '<div class="ctx-item" data-action="archive">归档对话<span class="ctx-kbd">Ctrl+Shift+H</span></div>' +
      '<div class="ctx-item ctx-danger" data-action="delete">删除</div>';
  }

  function positionCtxMenu(menu, anchor) {
    const r = anchor.getBoundingClientRect();
    const mr = menu.getBoundingClientRect();
    const pad = 8;
    let left = r.left;
    let top = r.bottom + 4;
    if (left + mr.width > window.innerWidth - pad) {
      left = Math.max(pad, window.innerWidth - mr.width - pad);
    }
    if (top + mr.height > window.innerHeight - pad) {
      top = Math.max(pad, window.innerHeight - mr.height - pad);
    }
    menu.style.left = left + 'px';
    menu.style.top = top + 'px';
  }

  async function loadProjectsIntoMenu(menu, item) {
    const box = menu.querySelector('[data-role="projects"]');
    if (!box) { return; }
    try {
      const projects = await fetchProjects();
      if (!projects.length) {
        box.innerHTML = '<div class="ctx-empty">暂无项目</div>';
        return;
      }
      box.innerHTML = '';
      projects.forEach(p => {
        const el = document.createElement('div');
        el.className = 'ctx-item';
        el.innerHTML = '<span>' + escapeHtml(p.name) + '</span>' +
          (item.projectName === p.name ? '<span class="ctx-check">✓</span>' : '');
        el.addEventListener('click', (e) => {
          e.stopPropagation();
          moveToProject(item, p.id);
        });
        box.appendChild(el);
      });
    } catch (e) {
      box.innerHTML = '<div class="ctx-empty">加载失败</div>';
    }
  }

  async function fetchProjects() {
    const res = await api('/api/project/list');
    const data = await res.json();
    return (data && data.code === 0 && data.data) ? data.data : [];
  }

  function showNewProjectInput(btn, item) {
    const footer = btn.parentElement;
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'ctx-new-project-input';
    input.placeholder = '输入项目名，回车创建';
    footer.innerHTML = '';
    footer.appendChild(input);
    input.focus();
    let done = false;
    const finish = () => {
      if (done) { return; }
      done = true;
      footer.innerHTML = '<div class="ctx-item" data-action="new-project">＋ 新建项目</div>';
    };
    input.addEventListener('keydown', async (e) => {
      e.stopPropagation();
      if (e.key === 'Enter') {
        const name = input.value.trim();
        if (!name) { finish(); return; }
        try {
          const project = await createProject(name);
          finish();
          await moveToProject(item, project.id);
        } catch (err) {
          showToast('新建项目失败');
          finish();
        }
      } else if (e.key === 'Escape') {
        finish();
      }
    });
    input.addEventListener('blur', finish);
  }

  async function createProject(name) {
    const res = await api('/api/project', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name })
    });
    const data = await res.json();
    if (!data || data.code !== 0) { throw new Error((data && data.message) || '创建失败'); }
    return data.data;
  }

  async function moveToProject(item, projectId) {
    try {
      const res = await api('/api/conversation/' + encodeURIComponent(item.conversationId) + '/move', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ projectId: projectId })
      });
      const data = await res.json();
      if (!data || data.code !== 0) { throw new Error(); }
      closeCtxMenu();
      loadHistory();
    } catch (e) {
      showToast('移动失败，请重试');
    }
  }

  function handleCtxAction(action, item) {
    closeCtxMenu();
    switch (action) {
      case 'pin': togglePin(item); break;
      case 'rename': startRename(item); break;
      case 'archive': archiveTask(item); break;
      case 'delete': deleteTask(item); break;
      case 'continue-question': continueInNew(item, 'question'); break;
      case 'continue-plan': continueInNew(item, 'plan'); break;
      case 'copy-text': copyText(item); break;
      case 'copy-link': copyLinkFor(item); break;
    }
  }

  async function togglePin(item) {
    try {
      const res = await api('/api/conversation/' + encodeURIComponent(item.conversationId) + '/pin', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pinned: !item.pinned })
      });
      const data = await res.json();
      if (!data || data.code !== 0) { throw new Error(); }
      loadHistory();
    } catch (e) { showToast('操作失败，请重试'); }
  }

  async function archiveTask(item) {
    try {
      const res = await api('/api/conversation/' + encodeURIComponent(item.conversationId) + '/archive', { method: 'POST' });
      const data = await res.json();
      if (!data || data.code !== 0) { throw new Error(); }
      loadHistory();
    } catch (e) { showToast('归档失败，请重试'); }
  }

  async function deleteTask(item) {
    if (!confirm('确定删除该对话？删除后不可恢复。')) { return; }
    try {
      const res = await api('/api/conversation/' + encodeURIComponent(item.conversationId), { method: 'DELETE' });
      const data = await res.json();
      if (!data || data.code !== 0) { throw new Error(); }
      loadHistory();
    } catch (e) { showToast('删除失败，请重试'); }
  }

  function startRename(item) {
    const el = historyList.querySelector('.history-item[data-cid="' + item.conversationId + '"]');
    if (!el) { return; }
    const textEl = el.querySelector('.history-text');
    const current = item.title || item.userInput || '';
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'history-rename-input';
    input.value = current;
    textEl.innerHTML = '';
    textEl.appendChild(input);
    input.focus();
    input.select();
    let done = false;
    const finish = async (save) => {
      if (done) { return; }
      done = true;
      const val = input.value.trim();
      if (save && val && val !== current) {
        try {
          const res = await api('/api/conversation/' + encodeURIComponent(item.conversationId) + '/rename', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title: val })
          });
          const data = await res.json();
          if (!data || data.code !== 0) { showToast((data && data.message) || '重命名失败'); }
        } catch (e) { showToast('重命名失败，请重试'); }
      }
      loadHistory();
    };
    input.addEventListener('click', (e) => e.stopPropagation());
    input.addEventListener('keydown', (e) => {
      e.stopPropagation();
      if (e.key === 'Enter') { finish(true); }
      else if (e.key === 'Escape') { finish(false); }
    });
    input.addEventListener('blur', () => finish(true));
  }

  async function continueInNew(item, mode) {
    let text = item.userInput || '';
    if (mode === 'plan') {
      try {
        const res = await api('/api/conversation/' + encodeURIComponent(item.conversationId));
        const data = await res.json();
        if (!data || data.code !== 0) { showToast('获取方案失败'); return; }
        const messages = data.data.messages || [];
        const last = messages[messages.length - 1];
        if (!last || last.status !== 'COMPLETED' || !last.planMarkdown) {
          showToast('该对话暂无已完成的方案');
          return;
        }
        text = last.planMarkdown.trim();
      } catch (e) { showToast('获取方案失败，请重试'); return; }
    }
    reset();
    clearChat();
    currentConversationId = null;
    clearAttachments();
    input.value = text;
    autoResizeTextarea();
    enableSubmit();
    input.focus();
  }

  async function copyText(item) {
    let text = item.userInput || '';
    try {
      const res = await api('/api/conversation/' + encodeURIComponent(item.conversationId));
      const data = await res.json();
      if (data && data.code === 0) {
        const messages = data.data.messages || [];
        text = messages.map(m => {
          const q = m.userInput ? ('问：' + m.userInput) : '';
          const a = m.planMarkdown ? ('答：\n' + m.planMarkdown) : '';
          return [q, a].filter(Boolean).join('\n');
        }).join('\n\n---\n\n');
      }
    } catch (e) { /* 忽略，回退到仅复制首条输入 */ }
    try {
      await copyToClipboard(text);
      showToast('已复制');
    } catch (e) { showToast('复制失败'); }
  }

  async function copyLinkFor(item) {
    const url = location.origin + '/share.html?conversationId=' + encodeURIComponent(item.conversationId);
    try {
      await copyToClipboard(url);
      showToast('链接已复制');
    } catch (e) { showToast('复制失败'); }
  }

  async function copyToClipboard(text) {
    if (navigator.clipboard && window.isSecureContext && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text);
      return;
    }
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.opacity = '0';
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
  }

  function showToast(msg) {
    let t = document.getElementById('toast');
    if (!t) {
      t = document.createElement('div');
      t.id = 'toast';
      t.className = 'toast';
      document.body.appendChild(t);
    }
    t.textContent = msg;
    requestAnimationFrame(() => t.classList.add('show'));
    if (toastTimer) { clearTimeout(toastTimer); }
    toastTimer = setTimeout(() => t.classList.remove('show'), 1600);
  }
})();

(function () {
  'use strict';

  const TOKEN_KEY = 'fitness_token';

  function getToken() {
    try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; }
  }

  function setToken(token) {
    try {
      if (token) { localStorage.setItem(TOKEN_KEY, token); }
      else { localStorage.removeItem(TOKEN_KEY); }
    } catch (e) { /* ignore */ }
  }

  function isLoggedIn() {
    return !!getToken();
  }

  function logout() {
    setToken(null);
  }

  function redirectToLogin() {
    setToken(null);
    if (location.pathname.indexOf('login.html') === -1) {
      location.href = '/login.html';
    }
  }

  /** 带 Bearer 鉴权头的 fetch 封装（不自动跳转，401 由调用方决定如何处理）。 */
  async function authFetch(url, options) {
    options = options || {};
    options.headers = options.headers || {};
    const token = getToken();
    if (token) {
      options.headers['Authorization'] = 'Bearer ' + token;
    }
    return fetch(url, options);
  }

  window.Auth = { getToken, setToken, isLoggedIn, logout, redirectToLogin, authFetch };
})();

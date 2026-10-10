(function () {
  'use strict';

  const U = function () { return window.FitUtil; };
  const $ = function (id) { return document.getElementById(id); };

  $('workoutDate').value = U().today();

  $('workoutSaveBtn').addEventListener('click', save);

  function numOrNull(v) {
    if (v === '' || v == null) { return null; }
    const n = Number(v);
    return isNaN(n) ? null : n;
  }

  function intOrNull(v) {
    if (v === '' || v == null) { return null; }
    const n = parseInt(v, 10);
    return isNaN(n) ? null : n;
  }

  async function save() {
    const exercise = $('workoutExercise').value.trim();
    if (!exercise) { U().toast('请填写动作名称'); return; }
    const req = {
      workoutDate: $('workoutDate').value || U().today(),
      bodyPart: $('workoutBodyPart').value,
      exerciseName: exercise,
      weightKg: numOrNull($('workoutWeight').value),
      sets: intOrNull($('workoutSets').value),
      reps: $('workoutReps').value.trim() || null,
      feeling: $('workoutFeeling').value.trim() || null
    };
    try {
      const data = await U().json('/api/workout', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(req)
      });
      if (!data || data.code !== 0) { throw new Error((data && data.message) || '保存失败'); }
      $('workoutExercise').value = '';
      $('workoutWeight').value = '';
      $('workoutSets').value = '';
      $('workoutReps').value = '';
      $('workoutFeeling').value = '';
      U().toast('打卡成功');
      refresh();
    } catch (e) {
      U().toast(e.message || '保存失败，请重试');
    }
  }

  async function refresh() {
    try {
      const [stats, list] = await Promise.all([
        U().json('/api/workout/stats'),
        U().json('/api/workout/list')
      ]);
      renderStats(stats && stats.code === 0 ? stats.data : null);
      renderList(list && list.code === 0 ? (list.data || []) : []);
    } catch (e) { /* 未登录忽略 */ }
  }

  function renderStats(s) {
    if (!s) { return; }
    const parts = s.byBodyPart || {};
    const partHtml = Object.keys(parts).map(function (k) {
      return '<span class="rounded-full bg-blue-50 px-3 py-1 text-sm text-blue-700">' + U().esc(k) + ' × ' + parts[k] + '</span>';
    }).join('');
    $('workoutStats').innerHTML =
      '<div class="grid grid-cols-2 gap-3 sm:grid-cols-3">' +
        stat('累计训练', s.totalCount, '次') +
        stat('本周训练', s.weekCount, '次') +
        '<div class="col-span-2 rounded-xl border border-gray-200 bg-white p-4 sm:col-span-1">' +
          '<div class="text-xs text-gray-500">各部位分布</div>' +
          '<div class="mt-2 flex flex-wrap gap-2">' + (partHtml || '<span class="text-xs text-gray-400">暂无</span>') + '</div>' +
        '</div>' +
      '</div>';
  }

  function stat(label, value, unit) {
    return '<div class="rounded-xl border border-gray-200 bg-white p-4 text-center">' +
      '<div class="text-xs text-gray-500">' + U().esc(label) + '</div>' +
      '<div class="mt-1 text-2xl font-bold text-gray-900">' + U().esc(value == null ? '0' : String(value)) + '</div>' +
      '<div class="text-xs text-gray-400">' + U().esc(unit) + '</div>' +
    '</div>';
  }

  function renderList(list) {
    const box = $('workoutList');
    box.innerHTML = '';
    if (!list.length) {
      box.innerHTML = '<div class="text-sm text-gray-400">暂无训练记录。</div>';
      return;
    }
    const wrap = document.createElement('div');
    wrap.className = 'space-y-2';
    list.forEach(function (w) {
      const detail = [
        w.weightKg != null ? w.weightKg + 'kg' : '',
        w.sets != null ? w.sets + '组' : '',
        w.reps ? w.reps + '次' : ''
      ].filter(Boolean).join(' · ');
      const row = document.createElement('div');
      row.className = 'rounded-xl border border-gray-100 bg-white px-4 py-3';
      row.innerHTML =
        '<div class="flex items-center justify-between">' +
          '<span class="text-sm font-semibold text-gray-900">' + U().esc(w.exerciseName) + '</span>' +
          '<span class="text-xs text-gray-400">' + U().esc(w.workoutDate || '') + '</span>' +
        '</div>' +
        '<div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-gray-500">' +
          '<span class="rounded-full bg-gray-100 px-2 py-0.5">' + U().esc(w.bodyPart || '') + '</span>' +
          (detail ? '<span>' + U().esc(detail) + '</span>' : '') +
        '</div>' +
        (w.feeling ? '<div class="mt-2 text-xs text-gray-500">💬 ' + U().esc(w.feeling) + '</div>' : '');
      wrap.appendChild(row);
    });
    box.appendChild(wrap);
  }

  window.WorkoutLog = { refresh: refresh };
})();

(function () {
  'use strict';

  const U = function () { return window.FitUtil; };
  const $ = function (id) { return document.getElementById(id); };

  let selectedFood = null; // 食物对象（每 100g 营养）

  $('dietDate').value = U().today();

  $('foodSearchBtn').addEventListener('click', searchFood);
  $('foodQuery').addEventListener('keydown', function (e) { if (e.key === 'Enter') { searchFood(); } });
  $('foodCalcBtn').addEventListener('click', showNutrition);
  $('dietAddBtn').addEventListener('click', addRecord);
  $('dietDate').addEventListener('change', refresh);

  async function searchFood() {
    const q = $('foodQuery').value.trim();
    if (!q) { U().toast('请输入食物名称'); return; }
    try {
      const data = await U().json('/api/diet/food/search?q=' + encodeURIComponent(q));
      if (!data || data.code !== 0) { throw new Error(); }
      const list = data.data || [];
      const box = $('foodSearchResults');
      box.innerHTML = '';
      if (!list.length) {
        box.innerHTML = '<div class="text-sm text-gray-400">未找到相关食物，换个关键词试试。</div>';
        return;
      }
      const wrap = document.createElement('div');
      wrap.className = 'flex flex-wrap gap-2';
      list.forEach(function (f) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'rounded-full border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-700 transition hover:border-blue-400 hover:text-blue-600';
        btn.textContent = f.name;
        btn.addEventListener('click', function () { selectFood(f); });
        wrap.appendChild(btn);
      });
      box.appendChild(wrap);
    } catch (e) {
      U().toast('搜索失败，请重试');
    }
  }

  function selectFood(f) {
    selectedFood = f;
    $('foodDetail').classList.remove('hidden');
    $('dietFoodName').value = f.name;
    showNutrition();
  }

  function showNutrition() {
    if (!selectedFood) { U().toast('请先搜索并选择一个食物'); return; }
    const weight = parseInt($('foodWeight').value, 10);
    if (!weight || weight <= 0) { U().toast('请输入有效重量'); return; }
    const f = selectedFood;
    const k = weight / 100;
    const cal = U().round1(f.calories * k);
    const protein = U().round1(f.protein * k);
    const carbs = U().round1(f.carbs * k);
    const fat = U().round1(f.fat * k);
    $('foodNutrition').innerHTML =
      '<div class="text-sm font-medium text-gray-900">' + U().esc(f.name) + ' · ' + weight + ' 克</div>' +
      '<div class="mt-2 grid grid-cols-4 gap-2 text-center">' +
        nut('热量', cal, '千卡') + nut('蛋白质', protein, '克') + nut('碳水', carbs, '克') + nut('脂肪', fat, '克') +
      '</div>' +
      '<div class="mt-2 text-xs text-gray-400">每 100 克：' + f.calories + ' 千卡 · 蛋白 ' + f.protein + 'g · 碳水 ' + f.carbs + 'g · 脂肪 ' + f.fat + 'g</div>';
  }

  function nut(label, value, unit) {
    return '<div class="rounded-lg bg-white px-2 py-2">' +
      '<div class="text-xs text-gray-500">' + U().esc(label) + '</div>' +
      '<div class="mt-0.5 font-semibold text-gray-900">' + value + '</div>' +
      '<div class="text-[11px] text-gray-400">' + U().esc(unit) + '</div>' +
    '</div>';
  }

  async function addRecord() {
    const foodName = $('dietFoodName').value.trim();
    const weight = parseInt($('dietWeight').value, 10);
    if (!foodName) { U().toast('请填写食物名（可从查询结果选择）'); return; }
    if (!weight || weight <= 0) { U().toast('请输入重量'); return; }
    const req = {
      recordDate: $('dietDate').value || U().today(),
      mealType: $('dietMeal').value,
      foodName: foodName,
      weightGrams: weight
    };
    try {
      const data = await U().json('/api/diet/record', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(req)
      });
      if (!data || data.code !== 0) { throw new Error((data && data.message) || '添加失败'); }
      $('dietFoodName').value = '';
      $('dietWeight').value = '';
      U().toast('已添加');
      refresh();
    } catch (e) {
      U().toast(e.message || '添加失败，请重试');
    }
  }

  async function refresh() {
    const date = $('dietDate').value || U().today();
    try {
      const data = await U().json('/api/diet/summary?date=' + encodeURIComponent(date));
      if (!data || data.code !== 0) { return; }
      render(data.data);
    } catch (e) { /* 未登录忽略 */ }
  }

  function render(s) {
    if (!s) { return; }
    const target = s.targetCalories;
    let diff = '';
    if (target != null) {
      const d = Math.round((s.totalCalories || 0) - target);
      diff = '<span class="font-medium ' + (d <= 0 ? 'text-green-600' : 'text-red-500') + '">' +
        (d <= 0 ? '低于目标 ' : '超出目标 ') + Math.abs(d) + ' 千卡</span>';
    }
    $('dietSummary').innerHTML =
      '<div class="rounded-xl bg-gray-50 p-4">' +
        '<div class="flex flex-wrap items-center gap-x-6 gap-y-2">' +
          '<div><span class="text-xs text-gray-500">总热量</span><span class="ml-2 text-xl font-bold text-gray-900">' + U().round1(s.totalCalories) + '</span><span class="text-xs text-gray-400"> 千卡</span></div>' +
          '<div><span class="text-xs text-gray-500">蛋白质</span><span class="ml-2 font-semibold">' + U().round1(s.totalProtein) + 'g</span></div>' +
          '<div><span class="text-xs text-gray-500">碳水</span><span class="ml-2 font-semibold">' + U().round1(s.totalCarbs) + 'g</span></div>' +
          '<div><span class="text-xs text-gray-500">脂肪</span><span class="ml-2 font-semibold">' + U().round1(s.totalFat) + 'g</span></div>' +
          (target != null ? '<div><span class="text-xs text-gray-500">目标</span><span class="ml-2 font-semibold">' + target + ' 千卡</span></div>' : '') +
          (diff ? '<div class="text-sm">' + diff + '</div>' : '') +
        '</div>' +
      '</div>';
    renderRecords(s.records || []);
  }

  function renderRecords(records) {
    const box = $('dietRecords');
    if (!records.length) {
      box.innerHTML = '<div class="text-sm text-gray-400">当天暂无记录。</div>';
      return;
    }
    box.innerHTML = '';
    const list = document.createElement('div');
    list.className = 'space-y-2';
    records.forEach(function (r) {
      const row = document.createElement('div');
      row.className = 'flex items-center justify-between rounded-xl border border-gray-100 px-3 py-2';
      row.innerHTML =
        '<div class="min-w-0">' +
          '<span class="text-xs text-gray-400">' + U().esc(r.mealType || '') + '</span> ' +
          '<span class="text-sm font-medium text-gray-800">' + U().esc(r.foodName) + '</span> ' +
          '<span class="text-xs text-gray-400">' + U().esc(r.weightGrams) + 'g</span>' +
        '</div>' +
        '<div class="flex items-center gap-3">' +
          '<span class="text-sm font-semibold text-gray-700">' + U().round1(r.calories) + ' 千卡</span>' +
          '<button type="button" class="text-xs text-gray-400 hover:text-red-500">删除</button>' +
        '</div>';
      row.querySelector('button').addEventListener('click', function () { removeRecord(r.id); });
      list.appendChild(row);
    });
    box.appendChild(list);
  }

  async function removeRecord(id) {
    try {
      const data = await U().json('/api/diet/record/' + id, { method: 'DELETE' });
      if (!data || data.code !== 0) { throw new Error(); }
      refresh();
    } catch (e) { U().toast('删除失败，请重试'); }
  }

  window.DietTool = { refresh: refresh };
})();

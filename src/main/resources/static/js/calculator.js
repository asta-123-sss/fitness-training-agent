(function () {
  'use strict';

  const U = function () { return window.FitUtil; };
  const $ = function (id) { return document.getElementById(id); };

  const genderEl = $('bodyGender');
  const ageEl = $('bodyAge');
  const heightEl = $('bodyHeight');
  const weightEl = $('bodyWeight');
  const activityEl = $('bodyActivity');
  const goalEl = $('bodyGoal');
  const resultEl = $('bodyResult');

  $('bodyCalcBtn').addEventListener('click', calculate);
  $('bodySaveBtn').addEventListener('click', save);

  function collect() {
    const age = parseInt(ageEl.value, 10);
    const height = parseFloat(heightEl.value);
    const weight = parseFloat(weightEl.value);
    if (!age || !height || !weight) {
      U().toast('请填写年龄、身高、体重');
      return null;
    }
    return {
      gender: genderEl.value,
      age: age,
      heightCm: height,
      weightKg: weight,
      activityLevel: activityEl.value,
      goal: goalEl.value
    };
  }

  async function calculate() {
    const req = collect();
    if (!req) { return; }
    try {
      const data = await U().json('/api/body/calculate', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(req)
      });
      if (!data || data.code !== 0) { throw new Error((data && data.message) || '计算失败'); }
      render(data.data);
    } catch (e) {
      U().toast(e.message || '计算失败，请重试');
    }
  }

  async function save() {
    const req = collect();
    if (!req) { return; }
    try {
      const data = await U().json('/api/body/profile', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(req)
      });
      if (!data || data.code !== 0) { throw new Error((data && data.message) || '保存失败'); }
      render(data.data);
      U().toast('已保存到档案');
    } catch (e) {
      U().toast(e.message || '保存失败，请重试');
    }
  }

  function render(r) {
    if (!r) { return; }
    const targets = (r.targets || []).map(function (t) {
      const active = r.goal && t.goal === r.goal;
      return '<div class="rounded-xl border p-4 ' + (active ? 'border-blue-300 bg-blue-50' : 'border-gray-100 bg-gray-50') + '">' +
        '<div class="text-sm font-semibold text-gray-900">' + U().esc(t.goal) + (active ? '<span class="ml-1 text-xs font-normal text-blue-600">当前目标</span>' : '') + '</div>' +
        '<div class="mt-1 text-2xl font-bold text-gray-900">' + t.calories + '<span class="text-sm font-normal text-gray-500"> 千卡/天</span></div>' +
        '<div class="mt-2 text-xs text-gray-600">蛋白质 ' + t.protein + 'g · 碳水 ' + t.carbs + 'g · 脂肪 ' + t.fat + 'g</div>' +
      '</div>';
    }).join('');

    resultEl.innerHTML =
      '<div class="grid grid-cols-3 gap-3">' +
        metric('BMI', r.bmi, r.bmiCategory || '') +
        metric('基础代谢 BMR', r.bmr, '千卡/天') +
        metric('总消耗 TDEE', r.tdee, '千卡/天') +
      '</div>' +
      '<div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">' + targets + '</div>' +
      '<div class="mt-4 rounded-xl bg-gray-50 p-4 text-sm leading-relaxed text-gray-700">' + interpret(r) + '</div>';
    resultEl.classList.remove('hidden');
  }

  function metric(label, value, unit) {
    return '<div class="rounded-xl border border-gray-100 bg-white p-4 text-center">' +
      '<div class="text-xs text-gray-500">' + U().esc(label) + '</div>' +
      '<div class="mt-1 text-2xl font-bold text-gray-900">' + U().esc(value == null ? '' : String(value)) + '</div>' +
      '<div class="text-xs text-gray-400">' + U().esc(unit) + '</div>' +
    '</div>';
  }

  function interpret(r) {
    let bmiNote;
    if (r.bmi < 18.5) { bmiNote = 'BMI 偏低，属于偏瘦范围，建议以增肌为目标并保证热量盈余。'; }
    else if (r.bmi < 24) { bmiNote = 'BMI 处于正常范围，可依据目标（减脂/维持/增肌）灵活安排饮食与训练。'; }
    else if (r.bmi < 28) { bmiNote = 'BMI 处于超重范围，建议优先减脂，控制热量摄入并配合力量训练。'; }
    else { bmiNote = 'BMI 属于肥胖范围，建议以减脂为目标，循序渐进地调整饮食与运动。'; }
    return '解读：' + bmiNote +
      ' 基础代谢 BMR 约 ' + r.bmr + ' 千卡/天，是你完全静止时消耗的热量；结合你的日常活动量，每日总消耗 TDEE 约 ' + r.tdee + ' 千卡/天。' +
      (r.targetCalories != null ? ' 你当前目标「' + U().esc(r.goal) + '」对应的推荐摄入为 ' + r.targetCalories + ' 千卡/天。' : '') +
      ' 蛋白质建议按目标取 1.6–2.0 克/公斤体重，脂肪约占 25% 热量，其余由碳水补齐。';
  }

  /** 打开该 Tab 时回填已保存档案。 */
  async function refresh() {
    try {
      const data = await U().json('/api/body/profile');
      if (!data || data.code !== 0 || !data.data) { return; }
      const p = data.data;
      genderEl.value = p.gender || '男';
      ageEl.value = p.age || '';
      heightEl.value = p.heightCm || '';
      weightEl.value = p.weightKg || '';
      activityEl.value = p.activityLevel || '轻度';
      goalEl.value = p.goal || '维持';
      render(p);
    } catch (e) { /* 未登录或未保存忽略 */ }
  }

  window.BodyCalculator = { refresh: refresh };
})();

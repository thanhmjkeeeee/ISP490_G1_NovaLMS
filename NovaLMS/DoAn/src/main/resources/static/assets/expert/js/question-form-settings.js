/**
 * Kỹ năng, CEFR, loại câu hỏi — lấy từ /api/settings (SKILL, CEFR_LEVEL, QUESTION_TYPE).
 * Dùng chung form tạo/sửa câu hỏi, filter ngân hàng, wizard, quiz.
 */
(function (global) {
  'use strict';

  const state = {
    skills: [],
    cefr: [],
    types: [],
    loaded: false
  };

  const LEGACY_TYPES_BY_SKILL = {
    LISTENING: ['MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI', 'FILL_IN_BLANK', 'MATCHING'],
    READING: ['MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI', 'FILL_IN_BLANK', 'MATCHING'],
    WRITING: ['WRITING'],
    SPEAKING: ['SPEAKING']
  };

  const TYPE_LABEL_FALLBACK = {
    MULTIPLE_CHOICE_SINGLE: 'Multiple Choice (1 đáp án)',
    MULTIPLE_CHOICE_MULTI: 'Multiple Choice (nhiều đáp án)',
    FILL_IN_BLANK: 'Fill in the Blank',
    MATCHING: 'Matching',
    WRITING: 'Writing',
    SPEAKING: 'Speaking'
  };

  function esc(s) {
    if (s == null) return '';
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function pickData(json) {
    return json && json.status === 200 && Array.isArray(json.data) ? json.data : [];
  }

  async function load() {
    const base = '/api/settings?activeOnly=true&type=';
    const [sk, ce, qt] = await Promise.all([
      fetch(base + encodeURIComponent('SKILL')).then((r) => r.json()),
      fetch(base + encodeURIComponent('CEFR_LEVEL')).then((r) => r.json()),
      fetch(base + encodeURIComponent('QUESTION_TYPE')).then((r) => r.json())
    ]);
    state.skills = pickData(sk);
    state.cefr = pickData(ce);
    state.types = pickData(qt);
    state.loaded = true;
  }

  function fallbackSkills(mode) {
    const all = [
      { value: 'READING', name: 'Reading / Đọc hiểu' },
      { value: 'LISTENING', name: 'Listening / Nghe hiểu' },
      { value: 'WRITING', name: 'Writing / Viết' },
      { value: 'SPEAKING', name: 'Speaking / Nói' }
    ];
    if (mode === 'GROUP') {
      return all.filter((s) => s.value === 'READING' || s.value === 'LISTENING');
    }
    return all;
  }

  function fallbackCefr() {
    return ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'].map((v) => ({
      value: v,
      name: v + ' — ' + (v.startsWith('A') ? 'Beginner/Elementary' : v.startsWith('B') ? 'Intermediate' : 'Advanced')
    }));
  }

  function fallbackTypes() {
    return [
      { value: 'MULTIPLE_CHOICE_SINGLE', name: TYPE_LABEL_FALLBACK.MULTIPLE_CHOICE_SINGLE },
      { value: 'MULTIPLE_CHOICE_MULTI', name: TYPE_LABEL_FALLBACK.MULTIPLE_CHOICE_MULTI },
      { value: 'FILL_IN_BLANK', name: TYPE_LABEL_FALLBACK.FILL_IN_BLANK },
      { value: 'MATCHING', name: TYPE_LABEL_FALLBACK.MATCHING },
      { value: 'WRITING', name: TYPE_LABEL_FALLBACK.WRITING },
      { value: 'SPEAKING', name: TYPE_LABEL_FALLBACK.SPEAKING }
    ];
  }

  function allowedTypeCodesForSkill(skill) {
    const su = (skill || 'READING').toUpperCase();
    return LEGACY_TYPES_BY_SKILL[su] || LEGACY_TYPES_BY_SKILL.READING;
  }

  /** Loại câu hỏi (value + label) sau khi lọc theo kỹ năng */
  function questionTypesForSkill(skill) {
    const allow = new Set(allowedTypeCodesForSkill(skill));
    const src = state.types.length ? state.types : fallbackTypes();
    const out = [];
    const seen = new Set();
    for (const s of src) {
      const value = (s.value || '').trim().toUpperCase();
      if (!value || !allow.has(value) || seen.has(value)) continue;
      seen.add(value);
      out.push({ value, name: (s.name && String(s.name).trim()) || TYPE_LABEL_FALLBACK[value] || value });
    }
    if (!out.length) {
      allowedTypeCodesForSkill(skill).forEach((value) => {
        out.push({ value, name: TYPE_LABEL_FALLBACK[value] || value });
      });
    }
    return out;
  }

  function typeLabel(code) {
    const u = (code || '').toUpperCase();
    const from = state.types.find((x) => (x.value || '').trim().toUpperCase() === u);
    if (from) return (from.name && String(from.name).trim()) || from.value || u;
    return TYPE_LABEL_FALLBACK[u] || code || '';
  }

  function fillSelect(selectEl, items, options) {
    if (!selectEl) return;
    const opts = options || {};
    let html = '';
    if (opts.includeEmpty) {
      html += `<option value="">${esc(opts.emptyLabel || '')}</option>`;
    }
    for (const s of items || []) {
      const v = s.value != null ? String(s.value) : '';
      const n = (s.name != null && String(s.name).trim()) ? String(s.name) : v;
      html += `<option value="${esc(v)}">${esc(n)}</option>`;
    }
    selectEl.innerHTML = html;
  }

  /** mode: 'SINGLE' | 'GROUP' — GROUP chỉ READING/LISTENING */
  function fillSkillSelectByMode(selectEl, mode) {
    const groupCodes = new Set(['READING', 'LISTENING']);
    let list = state.skills.length
      ? state.skills.filter((s) => {
          const v = (s.value || '').trim().toUpperCase();
          if (!v) return false;
          if (mode === 'GROUP') return groupCodes.has(v);
          return true;
        })
      : fallbackSkills(mode);
    if (!list.length) list = fallbackSkills(mode);
    fillSelect(selectEl, list);
  }

  /** Tất cả kỹ năng (chỉnh sửa câu hỏi / filter) */
  function fillSkillSelectAll(selectEl, options) {
    let list = state.skills.length ? state.skills : fallbackSkills('SINGLE');
    if (!list.length) list = fallbackSkills('SINGLE');
    fillSelect(selectEl, list, options);
  }

  function fillCefrSelect(selectEl, options) {
    let list = state.cefr.length ? state.cefr : fallbackCefr();
    fillSelect(selectEl, list, options);
  }

  function getSkillOptionsHtml(selectedValue) {
    const sel = (selectedValue || '').trim().toUpperCase();
    const list = state.skills.length ? state.skills : fallbackSkills('SINGLE');
    return list
      .map((s) => {
        const v = (s.value || '').trim().toUpperCase();
        const optSel = v === sel ? ' selected' : '';
        return `<option value="${esc(v)}"${optSel}>${esc(s.name || v)}</option>`;
      })
      .join('');
  }

  function getCefrOptionsHtml(selectedValue) {
    const sel = (selectedValue || '').trim().toUpperCase();
    const list = state.cefr.length ? state.cefr : fallbackCefr();
    return list
      .map((s) => {
        const v = (s.value || '').trim().toUpperCase();
        const optSel = v === sel ? ' selected' : '';
        return `<option value="${esc(v)}"${optSel}>${esc(s.name || v)}</option>`;
      })
      .join('');
  }

  /** AI Quick: thêm tùy chọn MIXED + các SKILL từ setting */
  function fillAiQuickSkillSelect(selectEl) {
    if (!selectEl) return;
    const mix = '<option value="MIXED">Tất cả / Mixed</option>';
    const list = state.skills.length ? state.skills : fallbackSkills('SINGLE');
    const rest = list
      .map((s) => {
        const v = (s.value || '').trim();
        return `<option value="${esc(v)}">${esc(s.name || v)}</option>`;
      })
      .join('');
    selectEl.innerHTML = mix + rest;
  }

  /**
   * Filter loại câu từ setting; tùy chọn thêm PASSAGE (ngân hàng đề).
   * @param opts {@code { includePassage: false }} để ẩn PASSAGE (vd. phê duyệt).
   */
  function fillFilterTypeSelect(selectEl, opts) {
    if (!selectEl) return;
    const includePassage = !opts || opts.includePassage !== false;
    let html = '<option value="">Tất cả</option>';
    const src = state.types.length ? state.types : fallbackTypes();
    for (const s of src) {
      const v = (s.value || '').trim().toUpperCase();
      if (!v) continue;
      html += `<option value="${esc(v)}">${esc((s.name && String(s.name).trim()) || v)}</option>`;
    }
    if (includePassage) {
      html += `<option value="PASSAGE">Bộ bài đọc/nghe (Passage)</option>`;
    }
    selectEl.innerHTML = html;
  }

  function getQuestionTypeOptionsHtml(skill, selectedValue) {
    const cur = selectedValue != null ? String(selectedValue).toUpperCase() : '';
    return questionTypesForSkill(skill)
      .map((t) => {
        const sel = t.value === cur ? ' selected' : '';
        return `<option value="${esc(t.value)}"${sel}>${esc(t.name || t.value)}</option>`;
      })
      .join('');
  }

  global.NovaQuestionFormSettings = {
    load,
    get loaded() {
      return state.loaded;
    },
    get skills() {
      return state.skills;
    },
    get cefrLevels() {
      return state.cefr;
    },
    get questionTypes() {
      return state.types;
    },
    allowedTypeCodesForSkill,
    questionTypesForSkill,
    typeLabel,
    fillSkillSelectByMode,
    fillSkillSelectAll,
    fillCefrSelect,
    getQuestionTypeOptionsHtml,
    getSkillOptionsHtml,
    getCefrOptionsHtml,
    fillFilterTypeSelect,
    fillAiQuickSkillSelect,
    esc
  };
})(typeof window !== 'undefined' ? window : this);

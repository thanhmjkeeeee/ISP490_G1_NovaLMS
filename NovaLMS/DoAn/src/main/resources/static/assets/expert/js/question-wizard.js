/* ================================================================
   question-wizard.js — NovaLMS Expert Question Group Wizard
   Handles 4-step wizard: Group Setup → Add Questions → Preview
   → Save & Publish
   ================================================================ */

'use strict';

// ── State ──────────────────────────────────────────────────────
let currentStep = 1;
let sessionHasData = false;

let step1Data = {
  mode: 'PASSAGE_BASED',
  passageContent: '',
  audioUrl: '',
  imageUrl: '',
  topic: '',
  skill: 'READING',
  cefrLevel: 'B1',
  tags: []
};

let accumulatedQuestions = [];
let validationResult = null;
let selectedSaveMode = 'DRAFT';
let selectedVisibility = 'EXPERT_BANK';

// ── Init ────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  loadStepData();

  document.getElementById('btnLeaveWizard')?.addEventListener('click', () => {
    if (sessionHasData) {
      if (!confirm('Bạn có tiến độ chưa lưu. Rời khỏi wizard?')) return;
      abandonWizard();
    } else {
      window.location.href = '/expert/question-bank';
    }
  });

  window.addEventListener('beforeunload', (e) => {
    if (sessionHasData) {
      e.preventDefault();
      e.returnValue = '';
    }
  });

  initTagsInput();

  document.getElementById('s1Skill')?.addEventListener('change', () => {
    step1Data.skill = document.getElementById('s1Skill').value;
    renderAITypeCheckboxes();
  });

  document.getElementById('excelFile')?.addEventListener('change', (e) => {
    const file = e.target.files[0];
    const nameEl = document.getElementById('excelFileName');
    if (nameEl) nameEl.textContent = file ? file.name : '';
  });

  document.getElementById('mType')?.addEventListener('change', onManualTypeChange);

  renderAITypeCheckboxes();
});

// ── Step Navigation ─────────────────────────────────────────────
function goToStep(n) {
  if (n < 1 || n > 4) return;
  currentStep = n;
  updateStepUI();
  window.scrollTo({ top: 0, behavior: 'smooth' });

  if (n === 2) renderAITypeCheckboxes();
  if (n === 3) renderStep3Preview();
  if (n === 4) renderStep4Summary();
}

function updateStepUI() {
  for (let i = 1; i <= 4; i++) {
    const el = document.getElementById(`wizard-step${i}`);
    if (el) el.style.display = i === currentStep ? '' : 'none';
  }

  for (let i = 1; i <= 4; i++) {
    const bubble = document.getElementById(`stepbubble${i}`);
    if (!bubble) continue;
    bubble.classList.remove('active', 'completed');
    if (i < currentStep) bubble.classList.add('completed');
    else if (i === currentStep) bubble.classList.add('active');
  }

  for (const [from, to] of [[1,2],[2,3],[3,4]]) {
    const line = document.getElementById(`stepline${from}-${to}`);
    if (!line) continue;
    line.classList.remove('active', 'completed');
    if (to <= currentStep) line.classList.add('completed');
    else if (from < currentStep) line.classList.add('active');
  }
}

// ── Tags Input ──────────────────────────────────────────────────
function initTagsInput() {
  const container = document.getElementById('tagsContainer');
  const input = document.getElementById('tagInput');
  if (!container || !input) return;

  renderTags();

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      const val = input.value.trim().replace(/,/g, '');
      if (val && step1Data.tags.length < 10 && !step1Data.tags.includes(val)) {
        step1Data.tags.push(val);
        renderTags();
      }
      input.value = '';
    } else if (e.key === 'Backspace' && !input.value && step1Data.tags.length > 0) {
      step1Data.tags.pop();
      renderTags();
    }
  });

  container.addEventListener('click', (e) => {
    if (e.target.classList.contains('tag-remove')) {
      const tag = e.target.dataset.tag;
      step1Data.tags = step1Data.tags.filter(t => t !== tag);
      renderTags();
    }
  });
}

function renderTags() {
  const container = document.getElementById('tagsContainer');
  const input = document.getElementById('tagInput');
  if (!container) return;
  container.querySelectorAll('.tag-chip').forEach(el => el.remove());
  step1Data.tags.forEach(tag => {
    const chip = document.createElement('span');
    chip.className = 'tag-chip';
    chip.innerHTML = `${escapeHtml(tag)}<span class="tag-remove" data-tag="${escapeHtml(tag)}">&times;</span>`;
    container.insertBefore(chip, input);
  });
}

// ── Step 1 ─────────────────────────────────────────────────────
function selectGroupType(type, cardEl) {
  step1Data.mode = type;
  document.querySelectorAll('.mode-card').forEach(c => {
    if (c.id === 'cardPassage' || c.id === 'cardTopic') c.classList.remove('selected');
  });
  cardEl.classList.add('selected');

  const passageSection = document.getElementById('passageSection');
  const topicSection = document.getElementById('topicSection');
  if (type === 'PASSAGE_BASED') {
    passageSection.style.display = '';
    topicSection.style.display = 'none';
  } else {
    passageSection.style.display = 'none';
    topicSection.style.display = '';
  }
}

async function submitStep1() {
  step1Data.mode = document.querySelector('input[name="groupType"]:checked')?.value || 'PASSAGE_BASED';
  step1Data.passageContent = document.getElementById('passageContent')?.value || '';
  step1Data.audioUrl = document.getElementById('audioUrl')?.value || '';
  step1Data.imageUrl = document.getElementById('imageUrl')?.value || '';
  step1Data.topic = document.getElementById('topicName')?.value || '';
  step1Data.skill = document.getElementById('s1Skill')?.value || 'READING';
  step1Data.cefrLevel = document.getElementById('s1Cefr')?.value || 'B1';

  if (step1Data.mode === 'PASSAGE_BASED' && !step1Data.passageContent.trim()) {
    showStepError('s1Error', 'Passage content is required for passage-based groups.');
    return;
  }
  if (step1Data.mode === 'TOPIC_BASED' && !step1Data.topic.trim()) {
    showStepError('s1Error', 'Topic name is required for topic-based groups.');
    return;
  }

  try {
    const res = await fetch('/api/v1/expert/questions/wizard/step1', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(step1Data)
    });
    const result = await res.json();
    if (!res.ok) {
      showStepError('s1Error', result.message || result.error || 'Step 1 submission failed.');
      return;
    }
    sessionHasData = true;
    goToStep(2);
  } catch (e) {
    showStepError('s1Error', 'Network error: ' + e.message);
  }
}

function showStepError(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.style.display = 'block';
}

// ── Step 2 ─────────────────────────────────────────────────────
function renderAITypeCheckboxes() {
  const container = document.getElementById('aiTypeCheckboxes');
  if (!container) return;
  const types = getTypesForSkill(step1Data.skill);
  container.innerHTML = types.map(t => `
    <div class="form-check">
      <input class="form-check-input" type="checkbox" value="${t}" id="aiType_${t}" checked>
      <label class="form-check-label small" for="aiType_${t}">${typeLabel(t)}</label>
    </div>
  `).join('');
}

function getTypesForSkill(skill) {
  const skillMap = {
    LISTENING: ['MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI', 'FILL_IN_BLANK', 'MATCHING'],
    READING:   ['MULTIPLE_CHOICE_SINGLE', 'MULTIPLE_CHOICE_MULTI', 'FILL_IN_BLANK', 'MATCHING'],
    WRITING:   ['WRITING'],
    SPEAKING:  ['SPEAKING']
  };
  return skillMap[skill] || [];
}

function typeLabel(t) {
  const map = {
    MULTIPLE_CHOICE_SINGLE: 'MC (Single)',
    MULTIPLE_CHOICE_MULTI: 'MC (Multi)',
    FILL_IN_BLANK: 'Fill Blank',
    MATCHING: 'Matching',
    WRITING: 'Writing',
    SPEAKING: 'Speaking'
  };
  return map[t] || t;
}

function cssClass(t) {
  return 'q-type-' + t;
}

// AI Generate
async function step2GenerateAI() {
  const topic = document.getElementById('aiTopic')?.value.trim();
  const qty = parseInt(document.getElementById('aiQty')?.value) || 5;
  const selectedTypes = Array.from(document.querySelectorAll('#aiTypeCheckboxes input:checked')).map(el => el.value);
  const mode = document.querySelector('input[name="aiMode"]:checked')?.value || 'NORMAL';

  if (!topic) { showError('aiStep2Error', 'Please enter a topic.'); return; }
  if (!qty || qty < 1 || qty > 50) { showError('aiStep2Error', 'Quantity must be between 1 and 50.'); return; }
  if (!selectedTypes.length) { showError('aiStep2Error', 'Select at least one question type.'); return; }

  const body = {
    sourceType: 'AI_GENERATE',
    aiTopic: topic,
    aiQuantity: qty,
    aiQuestionTypes: selectedTypes,
    aiMode: mode
  };

  const loadingEl = document.getElementById('aiStep2Loading');
  const resultsEl = document.getElementById('aiStep2Results');
  const errorEl = document.getElementById('aiStep2Error');
  errorEl.style.display = 'none';
  resultsEl.style.display = 'none';
  loadingEl.style.display = 'block';

  try {
    const formData = new FormData();
    formData.append('sourceType', 'AI_GENERATE');
    formData.append('aiTopic', topic);
    formData.append('aiQuantity', qty);
    formData.append('aiMode', mode);
    selectedTypes.forEach(t => formData.append('aiQuestionTypes', t));

    const res = await fetch('/api/v1/expert/questions/wizard/step2', {
      method: 'POST',
      body: formData
    });
    const result = await res.json();
    loadingEl.style.display = 'none';

    if (!res.ok) {
      showError('aiStep2Error', result.message || result.error || 'Generation failed.');
      return;
    }

    const data = result.data || {};
    const questions = data.questions || [];
    const errors = data.errors || [];

    renderAIStep2Results(questions, errors);

  } catch (e) {
    loadingEl.style.display = 'none';
    showError('aiStep2Error', 'Network error: ' + e.message);
  }
}

function renderAIStep2Results(questions, errors) {
  const resultsEl = document.getElementById('aiStep2Results');
  resultsEl.style.display = 'block';

  let html = '';

  if (errors && errors.length > 0) {
    html += `<div class="alert alert-warning mb-2"><i class="bi bi-exclamation-triangle me-1"></i> ${escapeHtml(errors[0].message || errors[0])}</div>`;
  }

  if (questions.length === 0) {
    resultsEl.innerHTML = html + '<div class="alert alert-info">No questions generated. Try a different topic.</div>';
    return;
  }

  questions.forEach(q => {
    q._source = 'AI';
    q._tempId = 'ai_' + Date.now() + '_' + Math.random();
  });
  accumulatedQuestions = [...accumulatedQuestions, ...questions];
  updateAccumulatedList();
  updateQuestionCount();

  resultsEl.innerHTML = html + `
    <div class="alert alert-success mb-2">
      <i class="bi bi-check-circle me-1"></i>
      Generated ${questions.length} question(s). Added to the list below.
    </div>
    <div class="list-group mb-2">${questions.map(q => `
      <div class="list-group-item d-flex justify-content-between align-items-center py-2">
        <div class="d-flex gap-2 flex-wrap align-items-center">
          <span class="badge q-type-badge ${cssClass(q.questionType)}">${typeLabel(q.questionType)}</span>
          <span class="badge q-skill-badge">${q.skill || step1Data.skill}</span>
          <span class="small text-muted">${escapeHtml((q.content || '').substring(0, 80))}${q.content && q.content.length > 80 ? '...' : ''}</span>
        </div>
        <button class="btn btn-sm btn-outline-danger" onclick="removeAccumulated('${q._tempId}')">
          <i class="bi bi-trash"></i>
        </button>
      </div>
    `).join('')}</div>
  `;
}

// Excel Import
async function step2UploadExcel() {
  const type = document.getElementById('excelType')?.value;
  const fileInput = document.getElementById('excelFile');
  const file = fileInput?.files[0];

  if (!type) { showError('excelStep2Error', 'Select a question type.'); return; }
  if (!file) { showError('excelStep2Error', 'Select an Excel file.'); return; }

  const formData = new FormData();
  formData.append('sourceType', 'EXCEL_IMPORT');
  formData.append('excelFile', file);
  formData.append('excelQuestionType', type);

  const loadingEl = document.getElementById('excelStep2Loading');
  const errorEl = document.getElementById('excelStep2Error');
  const previewEl = document.getElementById('excelStep2Preview');
  errorEl.style.display = 'none';
  previewEl.style.display = 'none';
  loadingEl.style.display = 'block';

  try {
    const res = await fetch('/api/v1/expert/questions/wizard/step2', {
      method: 'POST',
      body: formData
    });
    const result = await res.json();
    loadingEl.style.display = 'none';

    if (!res.ok) {
      showError('excelStep2Error', result.message || result.error || 'Upload failed.');
      return;
    }

    const data = result.data || {};
    const questions = data.questions || [];

    if (!questions.length) {
      showError('excelStep2Error', 'No valid rows found in the file.');
      return;
    }

    questions.forEach(q => {
      q._source = 'Excel';
      q._tempId = 'excel_' + Date.now() + '_' + Math.random();
    });
    accumulatedQuestions = [...accumulatedQuestions, ...questions];
    updateAccumulatedList();
    updateQuestionCount();

    previewEl.style.display = 'block';
    previewEl.innerHTML = `<div class="alert alert-success"><i class="bi bi-check-circle me-1"></i> Imported ${questions.length} question(s) from Excel.</div>`;

  } catch (e) {
    loadingEl.style.display = 'none';
    showError('excelStep2Error', 'Network error: ' + e.message);
  }
}

function step2DownloadTemplate() {
  const type = document.getElementById('excelType')?.value;
  if (!type) { alert('Select a question type first.'); return; }
  window.location.href = `/api/v1/expert/questions/excel/template?type=${encodeURIComponent(type)}`;
}

// Manual Add
function showManualForm() {
  document.getElementById('manualForm').style.display = '';
  resetManualFormFields();
  onManualTypeChange();
}

function hideManualForm() {
  document.getElementById('manualForm').style.display = 'none';
}

function resetManualFormFields() {
  const fields = ['mContent', 'mCorrectAnswer', 'mExplanation'];
  fields.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  document.getElementById('mOptionsList').innerHTML = `
    <div class="input-group input-group-sm mb-1">
      <div class="input-group-text"><input type="checkbox" class="form-check-input option-cb" onclick="syncOptionCorrect(this)"></div>
      <input type="text" class="form-control option-text" placeholder="Option A">
      <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeOption(this)"><i class="bi bi-x"></i></button>
    </div>
    <div class="input-group input-group-sm mb-1">
      <div class="input-group-text"><input type="checkbox" class="form-check-input option-cb" onclick="syncOptionCorrect(this)"></div>
      <input type="text" class="form-control option-text" placeholder="Option B">
      <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeOption(this)"><i class="bi bi-x"></i></button>
    </div>
  `;
  initMatchingPairs();
}

function onManualTypeChange() {
  const type = document.getElementById('mType')?.value;
  const optionsSection = document.getElementById('mOptionsSection');
  const correctAnswerRow = document.getElementById('mCorrectAnswerRow');
  const matchingSection = document.getElementById('mMatchingSection');
  const isMC = type === 'MULTIPLE_CHOICE_SINGLE' || type === 'MULTIPLE_CHOICE_MULTI';

  if (optionsSection) optionsSection.style.display = isMC ? '' : 'none';
  if (correctAnswerRow) correctAnswerRow.style.display = (isMC || type === 'FILL_IN_BLANK') ? '' : 'none';
  if (matchingSection) matchingSection.style.display = type === 'MATCHING' ? '' : 'none';
}

function addManualOption() {
  const list = document.getElementById('mOptionsList');
  const idx = list.children.length;
  const letter = String.fromCharCode(65 + idx);
  list.insertAdjacentHTML('beforeend', `
    <div class="input-group input-group-sm mb-1">
      <div class="input-group-text"><input type="checkbox" class="form-check-input option-cb" onclick="syncOptionCorrect(this)"></div>
      <input type="text" class="form-control option-text" placeholder="Option ${letter}">
      <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeOption(this)"><i class="bi bi-x"></i></button>
    </div>
  `);
}

function removeOption(btn) {
  const list = document.getElementById('mOptionsList');
  if (list.children.length <= 2) return;
  btn.closest('.input-group').remove();
}

function syncOptionCorrect(cb) {
  const type = document.getElementById('mType')?.value;
  if (type === 'MULTIPLE_CHOICE_SINGLE') {
    document.querySelectorAll('.option-cb').forEach(c => { if (c !== cb) c.checked = false; });
  }
}

function initMatchingPairs() {
  const container = document.getElementById('mMatchingPairs');
  if (!container) return;
  container.innerHTML = `
    <div class="mb-1 d-flex gap-2 align-items-center">
      <input type="text" class="form-control form-control-sm" placeholder="Left item" style="width:40%">
      <i class="bi bi-arrow-right text-muted"></i>
      <input type="text" class="form-control form-control-sm" placeholder="Right item" style="width:40%">
    </div>
    <div class="mb-1 d-flex gap-2 align-items-center">
      <input type="text" class="form-control form-control-sm" placeholder="Left item" style="width:40%">
      <i class="bi bi-arrow-right text-muted"></i>
      <input type="text" class="form-control form-control-sm" placeholder="Right item" style="width:40%">
    </div>
  `;
}

function addMatchingPair() {
  const container = document.getElementById('mMatchingPairs');
  container.insertAdjacentHTML('beforeend', `
    <div class="mb-1 d-flex gap-2 align-items-center">
      <input type="text" class="form-control form-control-sm" placeholder="Left item" style="width:40%">
      <i class="bi bi-arrow-right text-muted"></i>
      <input type="text" class="form-control form-control-sm" placeholder="Right item" style="width:40%">
    </div>
  `);
}

function submitManualQuestion() {
  const type = document.getElementById('mType')?.value;
  const content = document.getElementById('mContent')?.value.trim();

  if (!content) { alert('Question content is required.'); return; }

  const question = {
    questionType: type,
    content,
    explanation: document.getElementById('mExplanation')?.value?.trim() || '',
    _source: 'Manual',
    _tempId: 'manual_' + Date.now() + '_' + Math.random()
  };

  if (type === 'MULTIPLE_CHOICE_SINGLE' || type === 'MULTIPLE_CHOICE_MULTI') {
    const rows = document.querySelectorAll('#mOptionsList .input-group');
    const options = [];
    let correctFound = false;
    rows.forEach(row => {
      const text = row.querySelector('.option-text')?.value?.trim();
      const correct = !!row.querySelector('.option-cb')?.checked;
      if (text) {
        options.push({ title: text, correct });
        if (correct) correctFound = true;
      }
    });
    if (options.length < 2) { alert('Add at least 2 options.'); return; }
    if (!correctFound) { alert('Select the correct answer.'); return; }
    question.options = options;
  } else if (type === 'FILL_IN_BLANK') {
    question.options = [{
      title: document.getElementById('mCorrectAnswer')?.value?.trim() || '',
      correct: true
    }];
  } else if (type === 'MATCHING') {
    const pairs = [];
    document.querySelectorAll('#mMatchingPairs .d-flex').forEach(row => {
      const inputs = row.querySelectorAll('input');
      if (inputs[0]?.value?.trim() && inputs[1]?.value?.trim()) {
        pairs.push(inputs[0].value.trim() + ':' + inputs[1].value.trim());
      }
    });
    question.matchingPairs = pairs;
  }

  accumulatedQuestions.push(question);
  updateAccumulatedList();
  updateQuestionCount();
  hideManualForm();
}

function removeAccumulated(tempId) {
  accumulatedQuestions = accumulatedQuestions.filter(q => q._tempId !== tempId);
  updateAccumulatedList();
  updateQuestionCount();
}

function updateAccumulatedList() {
  const list = document.getElementById('step2AccumulatedList');
  if (!list) return;

  if (!accumulatedQuestions.length) {
    list.innerHTML = `
      <div class="text-center text-muted py-4 border rounded bg-light">
        <i class="bi bi-inbox fs-3 d-block mb-2"></i>
        No questions added yet. Use the tabs above to add questions.
      </div>`;
    return;
  }

  list.innerHTML = accumulatedQuestions.map((q, i) => `
    <div class="list-group-item d-flex justify-content-between align-items-start py-2 q-preview-card bg-white">
      <div class="d-flex align-items-start gap-2 flex-grow-1">
        <span class="text-muted small fw-bold mt-1">${i + 1}.</span>
        <div class="flex-grow-1">
          <div class="d-flex gap-1 flex-wrap mb-1">
            <span class="badge q-type-badge ${cssClass(q.questionType)}">${typeLabel(q.questionType)}</span>
            <span class="badge q-skill-badge">${q.skill || step1Data.skill}</span>
            <span class="badge bg-light text-dark">${q.cefrLevel || step1Data.cefrLevel}</span>
            <span class="badge bg-secondary-subtle text-secondary small">${q._source || '—'}</span>
          </div>
          <p class="mb-1 small">${escapeHtml(q.content || '')}</p>
          ${q.options && q.options.length ? `<ul class="mb-0 small text-muted">${q.options.map(o => `<li class="${o.correct ? 'text-success fw-bold' : ''}">${escapeHtml(o.title)} ${o.correct ? '✓' : ''}</li>`).join('')}</ul>` : ''}
          ${q.matchingPairs && q.matchingPairs.length ? `<p class="mb-0 small text-muted fst-italic">${q.matchingPairs.length} matching pairs</p>` : ''}
        </div>
      </div>
      <button class="btn btn-sm btn-outline-danger mt-1 flex-shrink-0" onclick="removeAccumulated('${q._tempId}')">
        <i class="bi bi-x"></i>
      </button>
    </div>
  `).join('');
}

function updateQuestionCount() {
  const n = accumulatedQuestions.length;
  const countEl = document.getElementById('s2QuestionCount');
  const totalEl = document.getElementById('totalAddedCount');
  if (countEl) countEl.textContent = `${n} question${n !== 1 ? 's' : ''}`;
  if (totalEl) totalEl.textContent = n;
}

async function submitStep2() {
  if (!accumulatedQuestions.length) {
    alert('Add at least one question before continuing.');
    return;
  }

  // Build WizardStep2DTO with MANUAL source — use dedicated JSON endpoint
  const dto = {
    sourceType: 'MANUAL',
    manualQuestions: accumulatedQuestions.map(q => ({
      content: q.content,
      questionType: q.questionType,
      explanation: q.explanation || null,
      options: q.options ? q.options.map(o => ({
        title: o.title,
        correct: o.correct,
        matchTarget: o.matchTarget || null
      })) : null,
      matchingPairs: q.matchingPairs || null
    }))
  };

  try {
    const res = await fetch('/api/v1/expert/questions/wizard/step2/manual', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(dto)
    });
    const result = await res.json();
    if (!res.ok) {
      alert(result.message || result.error || 'Step 2 submission failed.');
      return;
    }
    sessionHasData = true;
    goToStep(3);
  } catch (e) {
    alert('Network error: ' + e.message);
  }
}

// ── Step 3 ─────────────────────────────────────────────────────
function renderStep3Preview() {
  const list = document.getElementById('step3QuestionList');
  if (!list) return;

  list.innerHTML = accumulatedQuestions.map((q, i) => `
    <div class="card mb-2 q-preview-card" id="s3q${i}">
      <div class="card-body py-2">
        <div class="d-flex gap-2 mb-1">
          <span class="badge bg-dark text-white">${i + 1}</span>
          <span class="badge q-type-badge ${cssClass(q.questionType)}">${typeLabel(q.questionType)}</span>
          <span class="badge q-skill-badge">${q.skill || step1Data.skill}</span>
          <span class="badge bg-light text-dark">${q.cefrLevel || step1Data.cefrLevel}</span>
          <span class="badge bg-secondary-subtle text-secondary small">${q._source || '—'}</span>
        </div>
        <p class="mb-1 small fw-semibold">${escapeHtml(q.content || '')}</p>
        ${q.questionType === 'MATCHING' && q.matchingPairs && q.matchingPairs.length ? `
          <table class="table table-sm table-borderless mb-0 mt-2" style="font-size:0.8rem;">
            <thead><tr><th class="w-50 text-muted">Vế trái</th><th class="w-50 text-muted">Vế phải</th></tr></thead>
            <tbody>
              ${q.matchingPairs.map(pair => {
                const colonIdx = pair.lastIndexOf(':');
                const left  = colonIdx > -1 ? pair.substring(0, colonIdx) : pair;
                const right = colonIdx > -1 ? pair.substring(colonIdx + 1) : '';
                return `<tr><td class="text-secondary">${escapeHtml(left)}</td><td class="text-secondary">${escapeHtml(right)}</td></tr>`;
              }).join('')}
            </tbody>
          </table>` : ''}
        ${(q.questionType !== 'MATCHING' && q.options && q.options.length) ? `<ul class="mb-0 small text-muted">${q.options.map(o => `<li class="${o.correct ? 'text-success fw-bold' : ''}">${escapeHtml(o.title)} ${o.correct ? '✓' : ''}</li>`).join('')}</ul>` : ''}
        ${q.explanation ? `<p class="mb-0 small text-muted fst-italic">Explanation: ${escapeHtml(q.explanation)}</p>` : ''}
      </div>
    </div>
  `).join('');

  validateStep3();
}

async function validateStep3() {
  const area = document.getElementById('step3ValidationArea');
  area.innerHTML = `
    <div class="text-center py-3 text-muted">
      <div class="spinner-border spinner-border-sm text-primary me-2"></div>
      Validating questions...
    </div>`;

  try {
    // Step 3 validation is called without posting questions — the service
    // reads from session. But the JS doesn't have access to session questions
    // so we need to use /validate which reads from session on the server side.
    const res = await fetch('/api/v1/expert/questions/wizard/validate', {
      method: 'POST',
      headers: { 'Accept': 'application/json' }
    });
    const result = await res.json();
    validationResult = result.data || result;
    renderValidationResults();
  } catch (e) {
    area.innerHTML = `<div class="alert alert-danger"><i class="bi bi-x-circle me-1"></i> Validation error: ${e.message}</div>`;
  }
}

function renderValidationResults() {
  const area = document.getElementById('step3ValidationArea');
  const btn = document.getElementById('btnContinueStep4');
  if (!area) return;

  const isClean = validationResult && validationResult.isClean;
  const groupErrors = validationResult && validationResult.groupErrors ? validationResult.groupErrors : [];
  const groupWarnings = validationResult && validationResult.groupWarnings ? validationResult.groupWarnings : [];
  const questions = validationResult && validationResult.questions ? validationResult.questions : [];

  let html = '';

  if (isClean) {
    html += `
      <div class="alert alert-success d-flex align-items-center gap-2 mb-3">
        <i class="bi bi-check-circle-fill"></i>
        <div>
          <strong>All questions validated successfully.</strong>
          ${validationResult.totalQuestions !== undefined ? ` (${validationResult.totalQuestions} questions)` : ''}
        </div>
      </div>`;
    if (btn) { btn.disabled = false; btn.classList.remove('disabled'); }
  } else {
    if (groupErrors.length > 0) {
      html += `
        <div class="alert alert-danger mb-3">
          <div class="d-flex align-items-center gap-2 mb-2">
            <i class="bi bi-x-circle-fill"></i>
            <strong>${groupErrors.length} error(s) found — please fix before saving.</strong>
          </div>
          <ul class="mb-0 ps-3 small">${groupErrors.map(e => `<li>${escapeHtml(e.message || e)}</li>`).join('')}</ul>
        </div>`;
    }

    // Per-question errors
    const perQuestionErrors = questions.filter(q => q.errors && q.errors.length > 0);
    if (perQuestionErrors.length > 0) {
      html += `<div class="alert alert-danger mb-3">
        <i class="bi bi-x-circle-fill me-1"></i>
        <strong>${perQuestionErrors.length} question(s) have errors.</strong>
      </div>`;
      perQuestionErrors.forEach((q, qi) => {
        const card = document.getElementById(`s3q${qi}`);
        if (card) card.style.borderLeft = '4px solid #dc3545';
      });
    }

    if (groupWarnings.length > 0) {
      html += `
        <div class="alert alert-warning mb-3">
          <div class="d-flex align-items-center gap-2 mb-1">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <strong>${groupWarnings.length} warning(s)</strong>
          </div>
          <ul class="mb-0 ps-3 small">${groupWarnings.map(w => `<li>${escapeHtml(w.message || w)}</li>`).join('')}</ul>
        </div>`;
    }

    if (btn) { btn.disabled = true; btn.classList.add('disabled'); }
  }

  area.innerHTML = html;
}

// ── Step 4 ─────────────────────────────────────────────────────
function selectSaveMode(mode, cardEl) {
  selectedSaveMode = mode;
  document.querySelectorAll('#cardDraft,#cardPublish').forEach(c => c.classList.remove('selected'));
  cardEl.classList.add('selected');
}

function selectVisibility(vis, cardEl) {
  selectedVisibility = vis;
  document.querySelectorAll('#cardExpertBank,#cardPrivate').forEach(c => c.classList.remove('selected'));
  cardEl.classList.add('selected');
}

function renderStep4Summary() {
  const topic = step1Data.mode === 'PASSAGE_BASED'
    ? ((step1Data.passageContent || '').substring(0, 60) + ((step1Data.passageContent || '').length > 60 ? '...' : ''))
    : step1Data.topic;
  document.getElementById('summaryTopic').textContent = topic || '—';
  document.getElementById('summarySkill').textContent = step1Data.skill;
  document.getElementById('summaryCefr').textContent = step1Data.cefrLevel;
  document.getElementById('summaryQuestionCount').textContent = accumulatedQuestions.length;

  const tagsEl = document.getElementById('summaryTags');
  if (tagsEl) {
    tagsEl.innerHTML = step1Data.tags.length
      ? step1Data.tags.map(t => `<span class="badge bg-secondary">${escapeHtml(t)}</span>`).join(' ')
      : '<span class="text-muted small">No tags</span>';
  }
}

async function submitStep4() {
  const btn = document.getElementById('btnSaveFinish');
  const errorEl = document.getElementById('step4Error');
  const loadingEl = document.getElementById('step4Loading');

  errorEl.style.display = 'none';
  loadingEl.style.display = '';
  if (btn) btn.disabled = true;

  const payload = {
    status: selectedSaveMode,
    source: selectedVisibility
  };

  try {
    const res = await fetch('/api/v1/expert/questions/wizard/save', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();
    loadingEl.style.display = 'none';

    if (!res.ok) {
      showError('step4Error', result.message || result.error || 'Save failed.');
      if (btn) btn.disabled = false;
      return;
    }

    sessionHasData = false;
    alert('Question group saved successfully!');
    window.location.href = '/expert/question-bank';

  } catch (e) {
    loadingEl.style.display = 'none';
    showError('step4Error', 'Network error: ' + e.message);
    if (btn) btn.disabled = false;
  }
}

// ── Session helpers ─────────────────────────────────────────────
async function loadStepData() {
  try {
    const res = await fetch('/api/v1/expert/questions/wizard/step-data');
    if (!res.ok) return;
    const result = await res.json();
    const data = result.data || result;
    if (!data) return;

    if (data.step1) {
      step1Data = { ...step1Data, ...data.step1 };
    }
    if (data.questions && data.questions.length > 0) {
      accumulatedQuestions = data.questions.map((q, i) => ({ ...q, _tempId: 'recovery_' + i, _source: 'Loaded' }));
      sessionHasData = true;
      updateAccumulatedList();
      updateQuestionCount();
    }
    if (data.validationResult) {
      validationResult = data.validationResult;
    }
    if (data.step1 && currentStep > 1) {
      updateStepUI();
    }
  } catch (e) {
    // Fresh wizard on error
  }
}

async function abandonWizard() {
  try {
    await fetch('/api/v1/expert/questions/wizard/abandon', { method: 'GET' });
  } catch (e) { /* ignore */ }
  sessionHasData = false;
  window.location.href = '/expert/question-bank';
}

// ── Utilities ──────────────────────────────────────────────────
function showError(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.style.display = 'block';
}

function escapeHtml(s) {
  if (!s) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

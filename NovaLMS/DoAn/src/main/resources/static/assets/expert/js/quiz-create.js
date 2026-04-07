// E:\workspace\ISP490_G1_NovaLMS\NovaLMS\DoAn\src\main\resources\static\assets\expert\js\quiz-create.js

// ── State ─────────────────────────────────────────────────────────────────────
let _quizId = null;
let _currentBankPage = 0;
let _quizQuestions = [];
let _currentMode = 'quiz';   // 'quiz' | 'assignment'
let _expandedGroups = new Set();
let _lastBankResult = null;
let optionCount = 0;

// ── Bank browser (shared) ─────────────────────────────────────────────────────
function bankInit(mode) {
    _currentMode = mode;
    bankLoad(0);
    loadQuizQuestions();
}

async function bankLoad(page) {
    _currentBankPage = page;
    const params = new URLSearchParams();
    const skill = document.getElementById('bankSkill')?.value || '';
    const cefr  = document.getElementById('bankCefr')?.value || '';
    const kw    = document.getElementById('bankKeyword')?.value || '';
    if (skill) params.append('skill', skill);
    if (cefr)  params.append('cefrLevel', cefr);
    if (kw)    params.append('keyword', kw);
    params.append('status', 'PUBLISHED');
    params.append('page', page);
    params.append('size', 10);
    try {
        const res = await fetch(`/api/v1/expert/question-bank?${params}`);
        _lastBankResult = await res.json();
        bankRender(_lastBankResult);
    } catch(e) { console.error(e); }
}

function bankRender(result) {
    const tbody = document.getElementById('bankQuestionsBody');
    if (!result || result.status !== 200 || !result.data?.items?.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">Không tìm thấy câu hỏi.</td></tr>';
        const pg = document.getElementById('bankPagination');
        if (pg) pg.innerHTML = '';
        return;
    }
    const existingIds = new Set(_quizQuestions.map(q => q.questionId));
    tbody.innerHTML = result.data.items.map(q => {
        const added = existingIds.has(q.id);
        return `<tr>
            <td class="ps-3">
                <div class="small text-truncate" style="max-width:240px" title="${q.content||''}">${(q.content||'').substring(0,80)}${(q.content||'').length>80?'…':''}</div>
                <div class="extra-small text-muted">${q.skill||'—'} • ${typeName(q.questionType)}</div>
            </td>
            <td><span class="badge bg-light text-dark">${typeName(q.questionType)}</span></td>
            <td><span class="badge bg-info text-dark">${q.cefrLevel||''}</span></td>
            <td class="text-end pe-3">
                ${added
                    ? '<span class="badge bg-success rounded-pill px-3"><i class="bi bi-check-lg"></i> Đã thêm</span>'
                    : `<button class="btn btn-sm btn-primary rounded-pill px-3" onclick="bankAdd(${q.id},'${q.type||'SINGLE'}')">
                        <i class="bi bi-plus-lg"></i> Thêm
                       </button>`
                }
            </td>
        </tr>`;
    }).join('');

    const pg = document.getElementById('bankPagination');
    if (pg) {
        let html = '';
        for (let i = 0; i < result.data.totalPages; i++) {
            html += `<li class="page-item ${i===_currentBankPage?'active':''}">
                <a class="page-link" href="javascript:void(0)" onclick="bankLoad(${i})">${i+1}</a>
            </li>`;
        }
        pg.innerHTML = html;
    }
}

async function bankAdd(id, type) {
    const payload = { questionId: id, itemType: type, points: 1.0 };
    if (_currentMode === 'assignment') {
        const skill = document.getElementById('bankSkill')?.value;
        if (skill) payload.skill = skill;
    }
    const endpoint = _currentMode === 'assignment'
        ? `/api/v1/expert/quizzes/${_quizId}/section/questions`
        : `/api/v1/expert/quizzes/${_quizId}/questions`;
    try {
        const res = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (res.ok) { loadQuizQuestions(); }
        else { const r = await res.json(); alert(r.message || 'Không thể thêm.'); }
    } catch(e) { alert('Lỗi: ' + e.message); }
}

// ── Load current quiz questions ───────────────────────────────────────────────
async function loadQuizQuestions() {
    if (!_quizId) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}`);
        const result = await res.json();
        if (result.status === 200 && result.data) {
            _quizQuestions = result.data.questions || [];
            renderAddedQuestions();
            bankRender(_lastBankResult);
        }
    } catch(e) { console.error(e); }
}

// ── Render added questions panel ─────────────────────────────────────────────
function renderAddedQuestions() {
    const container = document.getElementById('addedQuestionsList');
    const badge = document.getElementById('currentCountBadge');
    if (!container) return;

    const groups = new Map(), lones = [];
    _quizQuestions.forEach(q => {
        if (q.groupId) {
            if (!groups.has(q.groupId)) groups.set(q.groupId, { id: q.groupId, content: q.groupContent, items: [] });
            groups.get(q.groupId).items.push(q);
        } else { lones.push(q); }
    });
    const total = groups.size + lones.length;
    if (badge) badge.textContent = `${total} câu`;

    if (total === 0) {
        container.innerHTML = `<div class="text-center py-5 text-muted"><i class="bi bi-inbox fs-1 d-block mb-2"></i>Chưa có câu hỏi nào.</div>`;
        return;
    }

    let html = '';
    groups.forEach(g => {
        const expanded = _expandedGroups.has(g.id);
        html += `<div class="card mb-3 border" style="border-left:5px solid #0d6efd !important;border-radius:12px">
            <div class="card-header bg-white d-flex justify-content-between align-items-center py-2">
                <div class="d-flex align-items-center gap-2">
                    <button class="btn btn-sm btn-light rounded-circle p-0" style="width:24px;height:24px" onclick="toggleGroup(${g.id})">
                        <i class="bi ${expanded?'bi-dash':'bi-plus'}"></i>
                    </button>
                    <span class="small fw-bold text-primary"><i class="bi bi-journal-text me-1"></i>PASSAGE</span>
                </div>
                <button class="btn btn-sm btn-link text-danger p-0" onclick="removeGroup(${g.id})"><i class="bi bi-trash"></i> Gỡ</button>
            </div>
            <div class="card-body pt-0">
                <div class="small text-muted fst-italic border-bottom pb-2 mb-2">${g.content||'—'}</div>
                <div class="${expanded?'':'d-none'}">
                    ${g.items.map((sq,i) => `
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div class="small"><span class="badge bg-light text-dark me-1">${i+1}</span> ${sq.questionContent}</div>
                            <button class="btn btn-sm btn-outline-danger border-0" onclick="removeQuestion(${sq.questionId})"><i class="bi bi-trash"></i></button>
                        </div>`).join('')}
                </div>
            </div>
        </div>`;
    });

    lones.forEach(q => {
        html += `<div class="card mb-2 border shadow-none" style="border-radius:12px">
            <div class="card-body p-3 d-flex justify-content-between align-items-start">
                <div>
                    <div class="small fw-bold text-success mb-1">CÂU LẺ • ${typeName(q.questionType)}</div>
                    <div class="small text-truncate" style="max-width:380px">${q.questionContent||'—'}</div>
                </div>
                <button class="btn btn-sm btn-outline-danger border-0" onclick="removeQuestion(${q.questionId})"><i class="bi bi-trash"></i></button>
            </div>
        </div>`;
    });
    container.innerHTML = html;
}

function toggleGroup(id) {
    _expandedGroups.has(id) ? _expandedGroups.delete(id) : _expandedGroups.add(id);
    renderAddedQuestions();
}

async function removeQuestion(questionId) {
    if (!confirm('Gỡ câu hỏi này?')) return;
    const endpoint = _currentMode === 'assignment'
        ? `/api/v1/expert/quizzes/${_quizId}/section/questions/${questionId}`
        : `/api/v1/expert/quizzes/${_quizId}/questions/${questionId}`;
    try {
        const res = await fetch(endpoint, { method: 'DELETE' });
        if (res.ok) loadQuizQuestions();
    } catch(e) { alert('Lỗi: ' + e.message); }
}

async function removeGroup(groupId) {
    if (!confirm('Gỡ toàn bộ Passage?')) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/groups/${groupId}`, { method: 'DELETE' });
        if (res.ok) loadQuizQuestions();
    } catch(e) { alert('Lỗi: ' + e.message); }
}

// ── Inline question creation (for assignment skill sections) ────────────────────
function initInlineQuestionForm(skill) {
    const form = document.getElementById('inlineQuestionForm');
    if (!form) return;
    const qType = document.getElementById('questionType');
    const mcDiv = document.getElementById('mcOptions');

    qType?.addEventListener('change', () => {
        const isMC = qType.value === 'MULTIPLE_CHOICE_SINGLE' || qType.value === 'MULTIPLE_CHOICE_MULTI';
        if (mcDiv) mcDiv.style.display = isMC ? 'block' : 'none';
        if (isMC && optionCount === 0) { addOption(); addOption(); addOption(); addOption(); }
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(form);
        const isMC = qType.value === 'MULTIPLE_CHOICE_SINGLE' || qType.value === 'MULTIPLE_CHOICE_MULTI';
        const isMulti = qType.value === 'MULTIPLE_CHOICE_MULTI';

        let options = [];
        if (isMC) {
            const correctMap = {};
            const sel = isMulti ? '[name=correctMulti]:checked' : '[name=correctSingle]:checked';
            document.querySelectorAll(sel).forEach(r => { correctMap[r.value] = true; });
            document.querySelectorAll('.option-text').forEach((inp, i) => {
                options.push({ title: inp.value, correct: !!correctMap[inp.dataset.idx], orderIndex: i });
            });
        }

        const payload = {
            content: fd.get('content'),
            questionType: qType.value,
            skill: skill,
            cefrLevel: fd.get('cefrLevel'),
            explanation: fd.get('explanation') || null,
            status: 'PUBLISHED',
            source: 'EXPERT_BANK',
            options: isMC ? options : []
        };

        const resp = await fetch('/api/v1/expert/question-bank', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await resp.json();
        if (resp.ok && data.data?.questionId) {
            const addEndpoint = _currentMode === 'assignment'
                ? `/api/v1/expert/quizzes/${_quizId}/section/questions`
                : `/api/v1/expert/quizzes/${_quizId}/questions`;
            await fetch(addEndpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ questionIds: [data.data.questionId], skill: skill, itemType: 'SINGLE' })
            });
            form.reset();
            optionCount = 0;
            if (document.getElementById('optionsList')) document.getElementById('optionsList').innerHTML = '';
            loadQuizQuestions();
        } else {
            alert('Lỗi: ' + (data.message || 'Không thể tạo câu hỏi'));
        }
    });
}

function addOption() {
    if (!document.getElementById('optionsList')) return;
    const qType = document.getElementById('questionType');
    const isMulti = qType?.value === 'MULTIPLE_CHOICE_MULTI';
    const idx = optionCount++;
    document.getElementById('optionsList').insertAdjacentHTML('beforeend', `
        <div class="input-group input-group-sm mb-1">
            <div class="input-group-text">
                <input class="form-check-input m-0 correct-check" type="${isMulti?'checkbox':'radio'}"
                       name="${isMulti?'correctMulti':'correctSingle'}" value="${idx}">
            </div>
            <input type="text" class="form-control option-text" placeholder="Đáp án ${idx+1}" data-idx="${idx}">
            <button type="button" class="btn btn-outline-danger" onclick="this.parentElement.remove()"><i class="bi bi-x"></i></button>
        </div>`);
}

function typeName(type) {
    return ({
        MULTIPLE_CHOICE_SINGLE:'Trắc nghiệm (1)',
        MULTIPLE_CHOICE_MULTI:'Trắc nghiệm (n)',
        FILL_IN_BLANK:'Điền từ', MATCHING:'Nối đáp án',
        WRITING:'Viết', SPEAKING:'Nói'
    })[type] || type || '';
}

// ── Load skill summary badges (assignment preview section 3) ──────────────────
async function loadSkillSummaryBadges() {
    if (!_quizId) return;
    try {
        const res = await fetch(`/api/v1/expert/quizzes/${_quizId}/skills`);
        const result = await res.json();
        if (!result.data) return;
        let total = 0;
        const missing = [];
        ['LISTENING','READING','SPEAKING','WRITING'].forEach(s => {
            const d = result.data[s] || {};
            const count = d.questionCount || 0;
            total += count;
            const badge = document.getElementById('badge-' + s);
            if (badge) {
                badge.textContent = badge.textContent.split(':')[0] + ': ' + count;
                badge.className = 'badge rounded-pill px-3 py-2 ' + (count > 0 ? 'bg-success' : 'bg-danger');
            }
            if (count === 0) missing.push(s);
        });
        const summaryEl = document.getElementById('previewSummary');
        if (summaryEl) summaryEl.textContent = `Tổng: ${total} câu hỏi`;
        const warnEl = document.getElementById('missingSkillsWarning');
        if (warnEl) warnEl.textContent = missing.length ? '⚠️ Thiếu: ' + missing.join(', ') : '';
    } catch(e) { console.error(e); }
}

// ── Helper: submit quiz (create or update) ─────────────────────────────────────
async function submitQuiz(status) {
    const cat = document.getElementById('quizCategory')?.value;
    if (!cat) { alert('Vui lòng chọn loại quiz.'); return; }
    const title = document.getElementById('title')?.value;
    if (!title) { alert('Vui lòng nhập tên Quiz'); return; }

    const body = {
        title,
        description: document.getElementById('description')?.value || null,
        quizCategory: cat,
        courseId: parseInt(document.getElementById('courseId')?.value) || null,
        moduleId: parseInt(document.getElementById('moduleId')?.value) || _moduleId || null,
        lessonId: parseInt(document.getElementById('lessonId')?.value) || null,
        timeLimitMinutes: parseInt(document.getElementById('timeLimitMinutes')?.value) || null,
        passScore: parseFloat(document.getElementById('passScore')?.value) || null,
        maxAttempts: parseInt(document.getElementById('maxAttempts')?.value) || null,
        questionOrder: document.getElementById('questionOrder')?.value || 'FIXED',
        showAnswerAfterSubmit: document.getElementById('showAnswerAfterSubmit')?.checked || false,
        status: status || 'DRAFT'
    };

    if (['COURSE_ASSIGNMENT','MODULE_ASSIGNMENT'].includes(cat)) {
        const limits = {};
        ['LISTENING','READING','SPEAKING','WRITING'].forEach(s => {
            const v = parseInt(document.getElementById('timeLimit' + s)?.value);
            if (v) limits[s] = v;
        });
        if (Object.keys(limits).length) body.timeLimitPerSkill = limits;
    }

    try {
        const method = _quizId ? 'PUT' : 'POST';
        const url    = _quizId ? `/api/v1/expert/quizzes/${_quizId}` : '/api/v1/expert/quizzes';
        const res    = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const result = await res.json();
        if (res.ok) {
            _quizId = result.data?.quizId || _quizId;
            return true;
        } else {
            alert(result.message || 'Có lỗi xảy ra.');
            return false;
        }
    } catch(e) { alert('Lỗi: ' + e.message); return false; }
}

// ── Load courses dropdown ─────────────────────────────────────────────────────
async function loadCourses() {
    const sel = document.getElementById('courseId');
    if (!sel || sel.options.length > 1) return;
    sel.innerHTML = '<option value="">-- Đang tải... --</option>';
    try {
        const res = await fetch('/api/v1/expert/modules/courses');
        const result = await res.json();
        if (result.status === 200 && result.data?.length) {
            sel.innerHTML = '<option value="">-- Chọn khóa học --</option>';
            result.data.forEach(c => {
                sel.innerHTML += `<option value="${c.courseId}">${c.courseName || c.title || 'Course #' + c.courseId}</option>`;
            });
        } else { sel.innerHTML = '<option value="">-- Không có khóa học --</option>'; }
    } catch(e) { sel.innerHTML = '<option value="">-- Lỗi tải --</option>'; }
}


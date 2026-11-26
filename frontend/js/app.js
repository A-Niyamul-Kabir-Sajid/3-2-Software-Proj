(() => {
  const $ = (s, r = document) => r.querySelector(s);
  const D = window.DB;
  const ROLE_ORDER = ['ADMIN', 'CR', 'TEACHER', 'STUDENT'];
  const DAY_ORDER = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  const DAY_SHORT = {
    SUNDAY: 'Sun', MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed',
    THURSDAY: 'Thu', FRIDAY: 'Fri', SATURDAY: 'Sat'
  };

  const primaryRole = u => (u && ROLE_ORDER.find(r => (u.roles || []).includes(r))) || 'STUDENT';
  const hasRole = r => !!((Api.user() && Api.user().roles) || []).includes(r);
  const isCr = () => hasRole('CR');
  const canManageSubjects = () => hasRole('CR') || hasRole('ADMIN');
  const canPostNotice = () => hasRole('CR') || hasRole('TEACHER');
  const canModifyNotice = n => {
    const u = Api.user();
    return !!u && (hasRole('ADMIN') || (n.author && n.author.id === u.id));
  };

  const state = {
    role: primaryRole(Api.user()),
    tag: null,
    edit: false,
    editSlotId: null,
    editNoticeId: null,
    noticeQuery: '',
    routine: null,
    subjects: [],
    noticesPage: null,
    loading: false,
    error: '',
    statusMsg: '',
    authMode: 'login'
  };

  const canEdit = () => state.role !== 'STUDENT';
  const canTeach = () => state.role === 'TEACHER';
  const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  const fmtErr = ex => (ex && ex.fieldErrors && ex.fieldErrors.length)
    ? ex.fieldErrors.map(fe => `${fe.field}: ${fe.message}`).join(' · ')
    : ((ex && ex.message) || 'Something went wrong');

  const msg = m => `<div class="msg ${m.me ? 'me' : ''} ${m.ai ? 'ai' : ''}"><small>${esc(m.u)}</small>${esc(m.t)}</div>`;

  const noticeCard = (n, showActions = false) => {
    const date = (n.createdAt || '').slice(0, 10);
    const authorName = n.author ? n.author.name : 'Unknown';
    return `<div class="card">
      <div class="notice-head">
        <h3>${esc(n.title)}</h3>
        ${n.pinned ? '<span class="tag on" style="margin:0">Pinned</span>' : ''}
      </div>
      <p>${esc(n.body || '')}</p>
      <div class="meta">
        <span>${esc(authorName)}${date ? ' · ' + esc(date) : ''}</span>
        ${showActions && canModifyNotice(n)
          ? `<span class="row" style="margin:0"><button class="btn sm ghost" data-edit-notice="${n.id}">Edit</button><button class="btn sm danger" data-del-notice="${n.id}">Delete</button></span>`
          : ''}
      </div>
    </div>`;
  };

  const titles = { home: 'Home', routine: 'Routine', notices: 'Notices', assignments: 'Assignments', forms: 'Forms', chat: 'Chat', assistant: 'AI Assistant' };

  function flattenRoutineSlots(routine) {
    if (!routine || !Array.isArray(routine.days)) return [];
    const list = [];
    routine.days.forEach(d => (d.slots || []).forEach(s => list.push({ ...s, day: d.day })));
    return list;
  }

  function findSlotById(id) {
    return flattenRoutineSlots(state.routine).find(s => String(s.id) === String(id)) || null;
  }

  function renderRoutineEditor() {
    const editing = state.editSlotId ? findSlotById(state.editSlotId) : null;
    const subjects = state.subjects || [];
    const selectedSubjectId = editing ? editing.subject.id : (subjects[0] ? subjects[0].id : '');
    const selectedSubject = subjects.find(s => String(s.id) === String(selectedSubjectId));
    const teachers = selectedSubject ? (selectedSubject.teachers || []) : [];

    const subjectOptions = subjects.length
      ? subjects.map(s => `<option value="${s.id}" ${String(s.id) === String(selectedSubjectId) ? 'selected' : ''}>${esc(s.code)} — ${esc(s.title)}</option>`).join('')
      : '<option value="">No subjects yet (add one below)</option>';

    const teacherOptions = '<option value="">Unassigned</option>' + teachers.map(t =>
      `<option value="${t.id}" ${editing && editing.teacher && editing.teacher.id === t.id ? 'selected' : ''}>${esc(t.name)}</option>`
    ).join('');

    return `<div class="card stack" style="margin-top:16px">
      <h3>${editing ? 'Edit routine slot' : 'Add routine slot'}</h3>
      <form id="slotForm" class="stack">
        <div class="row-wrap">
          <div class="field-col">
            <label for="slotDay">Day</label>
            <select id="slotDay" name="day" class="in" required>
              ${DAY_ORDER.map(d => `<option value="${d}" ${(editing ? editing.day : 'SUNDAY') === d ? 'selected' : ''}>${DAY_SHORT[d]} (${d})</option>`).join('')}
            </select>
          </div>
          <div class="field-col">
            <label for="slotStart">Start</label>
            <input id="slotStart" name="startTime" type="time" value="${esc(editing ? editing.start : '08:00')}" required>
          </div>
          <div class="field-col">
            <label for="slotEnd">End</label>
            <input id="slotEnd" name="endTime" type="time" value="${esc(editing ? editing.end : '08:50')}" required>
          </div>
          <div class="field-col">
            <label for="slotRoom">Room</label>
            <input id="slotRoom" name="room" placeholder="e.g. 301" maxlength="30" value="${esc(editing ? editing.room : '')}" required>
          </div>
        </div>
        <div class="row-wrap">
          <div class="field-col">
            <label for="slotSubject">Subject</label>
            <select id="slotSubject" name="subjectId" class="in" required>${subjectOptions}</select>
          </div>
          <div class="field-col">
            <label for="slotTeacher">Teacher (optional)</label>
            <select id="slotTeacher" name="teacherId" class="in">${teacherOptions}</select>
          </div>
        </div>
        <div class="row-wrap">
          <button class="btn" type="submit" ${subjects.length ? '' : 'disabled'}>${editing ? 'Save slot' : 'Add slot'}</button>
          ${editing ? '<button class="btn ghost" type="button" id="cancelSlotEdit">Cancel edit</button>' : ''}
        </div>
      </form>
      ${canManageSubjects() ? `
        <hr style="border:0;border-top:1px solid var(--line);margin:6px 0">
        <h3 style="margin-bottom:0">Subjects (${subjects.length})</h3>
        <form id="subjectForm" class="row-wrap">
          <input name="code" placeholder="Code (e.g. CSE 3201)" maxlength="20" required style="max-width:190px">
          <input name="title" placeholder="Subject title" maxlength="120" required>
          <button class="btn sm" type="submit">Add subject</button>
        </form>
        ${subjects.length ? `<div class="row-wrap">${subjects.map(s => `<span class="tag">${esc(s.code)} · ${esc(s.title)} <button type="button" data-del-subject="${s.id}" aria-label="Delete ${esc(s.code)}" style="background:none;border:0;color:inherit;cursor:pointer;padding:0 0 0 4px">✕</button></span>`).join('')}</div>` : ''}
      ` : ''}
    </div>`;
  }

  const views = {
    auth() {
      const reg = state.authMode === 'register';
      return `<h1>${reg ? 'Create your <em>account</em>' : 'Welcome <em>back</em>'}</h1>
      <p class="sub">${reg ? 'Students and CRs can register here. Teachers are added by the admin.' : 'Log in to see your routine, notices and assignments.'}</p>
      <form id="authForm" class="card auth" novalidate>
        ${reg ? '<input name="name" placeholder="Full name" autocomplete="name" required maxlength="100">' : ''}
        <input name="email" type="email" placeholder="Email" autocomplete="email" required>
        <input name="password" type="password" placeholder="Password${reg ? ' (8+ chars, letters and digits)' : ''}" autocomplete="${reg ? 'new-password' : 'current-password'}" required>
        ${reg ? '<select name="role" aria-label="Role"><option value="STUDENT">Student</option><option value="CR">CR</option></select>' : ''}
        <p class="err" id="authErr" role="alert"></p>
        <button class="btn" type="submit">${reg ? 'Register' : 'Log in'}</button>
        <button class="btn ghost" type="button" id="authSwitch">${reg ? 'I already have an account' : 'Create an account'}</button>
      </form>`;
    },
    home() {
      const notices = (state.noticesPage && state.noticesPage.content) || [];
      const noticeCount = state.noticesPage ? state.noticesPage.totalElements : notices.length;
      return `<h1>Your class,<br><em>in sync.</em></h1>
      <p class="sub">Routine, notices, assignments and forms in one installable app that works offline.</p>
      <div class="hero-grid">
        <div class="card"><div class="stat">${D.assignments.filter(a => !a.done).length}</div><p>Pending assignments</p></div>
        <div class="card"><div class="stat">${D.forms.filter(f => !f.done).length}</div><p>Open forms</p></div>
        <div class="card"><div class="stat">${noticeCount}</div><p>Notices</p></div>
        <div class="card"><div class="stat">${state.subjects.length}</div><p>Subjects</p></div>
      </div>
      <h2>Latest notices</h2>
      ${state.loading ? '<p class="sub">Loading…</p>' : `<div class="grid">${notices.slice(0, 4).map(n => noticeCard(n, false)).join('') || '<p class="sub">No notices yet.</p>'}</div>`}`;
    },
    routine() {
      const ed = state.edit && isCr();
      const routine = state.routine || { days: [], totalSlots: 0 };
      const byDay = Object.fromEntries((routine.days || []).map(d => [d.day, d.slots || []]));

      const activeDays = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY'];
      ['FRIDAY', 'SATURDAY'].forEach(d => { if ((byDay[d] || []).length) activeDays.push(d); });

      const timeKeys = Array.from(new Set(
        flattenRoutineSlots(routine).map(s => `${s.start}–${s.end}`)
      )).sort();

      const rows = timeKeys.map(range => `<tr><th>${esc(range)}</th>${activeDays.map(day => {
        const matches = (byDay[day] || []).filter(s => `${s.start}–${s.end}` === range);
        if (!matches.length) return '<td></td>';
        return `<td>${matches.map(s => `<div class="slot-item">
          <b>${esc(s.subject.code)}</b>
          <span>${esc(s.subject.title)}</span><br>
          <span>Rm ${esc(s.room)}${s.teacher ? ' · ' + esc(s.teacher.name) : ''}</span>
          ${ed ? `<div class="slot-actions">
            <button class="btn sm ghost" type="button" data-edit-slot="${s.id}">Edit</button>
            <button class="btn sm danger" type="button" data-del-slot="${s.id}">Delete</button>
          </div>` : ''}
        </div>`).join('')}</td>`;
      }).join('')}</tr>`).join('');

      return `<h1>Class <em>routine</em></h1>
      <p class="sub">Weekly schedule (${routine.totalSlots} slots). ${isCr() ? 'CR can edit slots or undo the last change.' : 'View only.'}</p>
      ${isCr() ? `<div class="row-wrap">
        <button class="btn" type="button" id="toggleEdit">${ed ? 'Close editor' : 'Routine editor'}</button>
        <button class="btn ghost" type="button" id="undoRoutine">Undo last change</button>
      </div>` : ''}
      ${state.error ? `<p class="err" style="margin-top:10px" role="alert">${esc(state.error)}</p>` : ''}
      ${state.statusMsg ? `<p class="ok-msg" style="margin-top:10px">${esc(state.statusMsg)}</p>` : ''}
      ${ed ? renderRoutineEditor() : ''}
      <div style="overflow:auto;margin-top:16px">
        ${state.loading && !state.routine
          ? '<p class="sub">Loading routine…</p>'
          : (timeKeys.length
            ? `<table><tr><th>Time</th>${activeDays.map(d => `<th>${DAY_SHORT[d]}</th>`).join('')}</tr>${rows}</table>`
            : '<div class="card"><p>No routine slots scheduled yet.</p></div>')}
      </div>`;
    },
    notices() {
      const notices = (state.noticesPage && state.noticesPage.content) || [];
      const editing = state.editNoticeId ? notices.find(n => String(n.id) === String(state.editNoticeId)) : null;

      return `<h1>Notice <em>board</em></h1>
      <p class="sub">Official notices published by CR and teachers.</p>
      <form id="noticeSearchForm" class="row-wrap" style="margin-bottom:16px">
        <input id="noticeQuery" name="q" placeholder="Search notices by title or body…" value="${esc(state.noticeQuery)}" style="max-width:420px">
        <button class="btn sm" type="submit">Search</button>
        ${state.noticeQuery ? '<button class="btn sm ghost" type="button" id="clearNoticeQuery">Clear</button>' : ''}
      </form>
      ${(canPostNotice() || editing) ? `<form id="noticeForm" class="card stack" style="margin-bottom:18px">
        <h3>${editing ? 'Edit notice' : 'Publish a notice'}</h3>
        <input id="nt" name="title" placeholder="Notice title" maxlength="150" value="${esc(editing ? editing.title : '')}" required>
        <textarea id="nb" name="body" rows="3" placeholder="Notice details (optional)" maxlength="5000">${esc(editing ? (editing.body || '') : '')}</textarea>
        <div class="row-wrap" style="justify-content:space-between">
          <label class="chk"><input type="checkbox" id="np" name="pinned" ${editing && editing.pinned ? 'checked' : ''}> Pin this notice to top</label>
          <div class="row-wrap">
            ${editing ? '<button class="btn ghost sm" type="button" id="cancelNoticeEdit">Cancel</button>' : ''}
            <button class="btn" type="submit">${editing ? 'Save notice' : 'Publish'}</button>
          </div>
        </div>
      </form>` : ''}
      ${state.error ? `<p class="err" role="alert">${esc(state.error)}</p>` : ''}
      ${state.statusMsg ? `<p class="ok-msg">${esc(state.statusMsg)}</p>` : ''}
      ${state.loading && !state.noticesPage
        ? '<p class="sub">Loading notices…</p>'
        : `<div class="grid" style="margin-top:18px">${notices.map(n => noticeCard(n, true)).join('') || '<p class="sub">No notices found.</p>'}</div>`}`;
    },
    assignments() {
      const subjectNames = Array.from(new Set([
        ...(state.subjects || []).map(s => s.code),
        ...D.assignments.map(a => a.subject)
      ]));
      const list = D.assignments.filter(a => !state.tag || a.subject === state.tag);
      const tags = `<span class="tag ${!state.tag ? 'on' : ''}" data-tag="">All</span>` +
        subjectNames.map(s => `<span class="tag ${state.tag === s ? 'on' : ''}" data-tag="${esc(s)}">${esc(s)}</span>`).join('');
      const card = a => `<div class="card ${a.done ? 'done' : ''}"><h3>${esc(a.title)}</h3>
        <span class="tag on">${esc(a.subject)}</span><p style="margin-top:8px">${esc(a.link)} · by ${esc(a.by)}</p>
        ${a.comments.filter(c => !c.priv || canTeach()).map(c => `<div class="comment ${c.priv ? 'priv' : ''}">${esc(c.t)}</div>`).join('')}
        <div class="row"><input data-c="${a.id}" placeholder="Comment…"><select class="in" data-v="${a.id}" style="flex:0 0 auto"><option value="0">Public</option>${canTeach() ? '<option value="1">Teacher only</option>' : ''}</select><button class="btn sm" data-cm="${a.id}">Post</button></div>
        <div class="meta"><span>Due ${esc(a.due)}</span><button class="btn sm ghost" data-done="${a.id}">${a.done ? 'Mark pending' : 'Mark complete'}</button></div></div>`;
      const pend = list.filter(a => !a.done), done = list.filter(a => a.done);
      return `<h1>Assign<em>ments</em></h1><p class="sub">Tagged by subject so you can sort and organise.</p>
      ${canEdit() ? `<div class="card" style="margin-bottom:18px"><h3>New assignment (${esc(state.role)})</h3><div class="row"><input id="at" placeholder="Title"><input id="al" placeholder="Link or document name"><select class="in" id="as" style="flex:0 0 auto">${subjectNames.map(s => `<option>${esc(s)}</option>`).join('')}</select><button class="btn" id="addAsg">Add</button></div></div>` : ''}
      <div>${tags}</div><h2>Pending</h2><div class="grid">${pend.map(card).join('') || '<p class="sub">Nothing pending 🎉</p>'}</div>
      <h2>Completed</h2><div class="grid">${done.map(card).join('') || '<p class="sub">None yet</p>'}</div>`;
    },
    forms() {
      return `<h1>Forms</h1><p class="sub">Fill-ups and registrations posted by CR.</p><div class="grid">
      ${D.forms.map(f => `<div class="card ${f.done ? 'done' : ''}"><h3>${esc(f.title)}</h3><div class="meta"><span>Due ${esc(f.due)}</span>
      <button class="btn sm ghost" data-form="${f.id}">${f.done ? 'Done' : 'Mark filled'}</button></div></div>`).join('')}</div>`;
    },
    chat() {
      return `<h1>Group <em>chat</em></h1><p class="sub">Real-time class room (mock data for now).</p>
      <div class="card chat" id="msgs">${D.chat.map(msg).join('')}</div>
      <div class="row"><input id="ci" placeholder="Message class…"><button class="btn" id="send">Send</button></div>`;
    },
    assistant() {
      return `<h1>AI <em>assistant</em></h1><p class="sub">Type one prompt. I'll understand it and ask for confirmation before changing anything.</p>
      <div class="card chat" id="msgs"><div class="msg ai"><small>Assistant</small>Hi! Ask about the class, or (CR / Teacher) tell me what to update.</div></div>
      <div class="row"><input id="ci" placeholder="Ask or instruct…"><button class="btn" id="aiSend">Send</button></div>`;
    }
  };

  function renderSession() {
    const u = Api.user();
    $('#who').hidden = $('#logout').hidden = !u;
    if (u) $('#who').textContent = `${u.name} · ${state.role}`;
  }

  let activePage = null;

  function currentPage() {
    const page = location.hash.slice(1) || 'home';
    return views[page] ? page : 'home';
  }

  async function fetchHomeData() {
    const [noticesRes, subjectsRes] = await Promise.allSettled([
      Api.get('/notices?page=0&size=4'),
      Api.get('/subjects')
    ]);
    if (noticesRes.status === 'fulfilled') state.noticesPage = noticesRes.value;
    if (subjectsRes.status === 'fulfilled') state.subjects = subjectsRes.value || [];
  }

  async function fetchRoutineData() {
    const [routineRes, subjectsRes] = await Promise.allSettled([
      Api.get('/routine'),
      Api.get('/subjects')
    ]);
    if (routineRes.status === 'fulfilled') state.routine = routineRes.value;
    else state.error = fmtErr(routineRes.reason);
    if (subjectsRes.status === 'fulfilled') state.subjects = subjectsRes.value || [];
  }

  async function fetchNoticesData() {
    const q = state.noticeQuery ? `&q=${encodeURIComponent(state.noticeQuery)}` : '';
    try {
      state.noticesPage = await Api.get(`/notices?page=0&size=20${q}`);
    } catch (ex) {
      state.error = fmtErr(ex);
    }
  }

  async function loadPage(v) {
    state.loading = true;
    state.error = '';
    draw(v);
    if (v === 'home') await fetchHomeData();
    else if (v === 'routine') await fetchRoutineData();
    else if (v === 'notices') await fetchNoticesData();
    else if (v === 'assignments' && !state.subjects.length) {
      try { state.subjects = (await Api.get('/subjects')) || []; } catch { /* ignore */ }
    }
    state.loading = false;
    if (currentPage() === v && Api.isLoggedIn()) draw(v);
  }

  function draw(v) {
    renderSession();
    $('#nav').innerHTML = Object.keys(titles).map(k => `<a href="#${k}" class="${k === v ? 'on' : ''}">${titles[k]}</a>`).join('');
    $('#view').innerHTML = views[v]();
    bind(v);
    const m = $('#msgs'); if (m) m.scrollTop = m.scrollHeight;
  }

  function render() {
    renderSession();
    if (!Api.isLoggedIn()) {
      activePage = null;
      $('#nav').innerHTML = '';
      $('#view').innerHTML = views.auth();
      bindAuth();
      return;
    }
    state.role = primaryRole(Api.user());
    const v = currentPage();
    if (v !== activePage) {
      activePage = v;
      state.error = '';
      state.statusMsg = '';
      loadPage(v);
    } else {
      draw(v);
    }
  }

  function bindRoutine(on) {
    on('#toggleEdit', () => {
      state.edit = !state.edit;
      state.editSlotId = null;
      state.error = '';
      state.statusMsg = '';
      draw('routine');
    });

    on('#undoRoutine', async () => {
      state.error = '';
      state.statusMsg = '';
      try {
        const res = await Api.post('/routine/undo');
        if (res && res.queued) state.statusMsg = 'Undo queued offline — will sync when online.';
        else { state.routine = res; state.statusMsg = 'Reverted last routine change.'; }
      } catch (ex) {
        state.error = fmtErr(ex);
      }
      draw('routine');
    });

    const subjectSel = $('#slotSubject');
    const teacherSel = $('#slotTeacher');
    if (subjectSel && teacherSel) {
      subjectSel.onchange = () => {
        const subj = (state.subjects || []).find(s => String(s.id) === subjectSel.value);
        const ts = subj ? (subj.teachers || []) : [];
        teacherSel.innerHTML = '<option value="">Unassigned</option>' +
          ts.map(t => `<option value="${t.id}">${esc(t.name)}</option>`).join('');
      };
    }

    on('#cancelSlotEdit', () => {
      state.editSlotId = null;
      state.error = '';
      draw('routine');
    });

    const slotForm = $('#slotForm');
    if (slotForm) {
      slotForm.onsubmit = async e => {
        e.preventDefault();
        state.error = '';
        state.statusMsg = '';
        const f = Object.fromEntries(new FormData(slotForm));
        const body = {
          day: f.day,
          startTime: f.startTime,
          endTime: f.endTime,
          room: f.room.trim(),
          subjectId: Number(f.subjectId),
          teacherId: f.teacherId ? Number(f.teacherId) : null
        };
        try {
          const res = state.editSlotId
            ? await Api.put(`/routine/slots/${state.editSlotId}`, body)
            : await Api.post('/routine/slots', body);
          if (res && res.queued) {
            state.statusMsg = 'Slot change queued offline — will sync when online.';
          } else {
            state.statusMsg = state.editSlotId ? 'Slot updated.' : 'Slot added.';
            state.editSlotId = null;
            await fetchRoutineData();
          }
        } catch (ex) {
          state.error = fmtErr(ex);
        }
        draw('routine');
      };
    }

    document.querySelectorAll('[data-edit-slot]').forEach(btn => {
      btn.onclick = () => {
        state.editSlotId = btn.dataset.editSlot;
        state.error = '';
        state.statusMsg = '';
        draw('routine');
      };
    });

    document.querySelectorAll('[data-del-slot]').forEach(btn => {
      btn.onclick = async () => {
        state.error = '';
        state.statusMsg = '';
        try {
          const res = await Api.del(`/routine/slots/${btn.dataset.delSlot}`);
          if (res && res.queued) state.statusMsg = 'Slot deletion queued offline.';
          else {
            if (String(state.editSlotId) === btn.dataset.delSlot) state.editSlotId = null;
            state.statusMsg = 'Slot deleted.';
            await fetchRoutineData();
          }
        } catch (ex) {
          state.error = fmtErr(ex);
        }
        draw('routine');
      };
    });

    const subjectForm = $('#subjectForm');
    if (subjectForm) {
      subjectForm.onsubmit = async e => {
        e.preventDefault();
        state.error = '';
        state.statusMsg = '';
        const f = Object.fromEntries(new FormData(subjectForm));
        try {
          const res = await Api.post('/subjects', { code: f.code.trim(), title: f.title.trim(), teacherIds: [] });
          if (res && res.queued) state.statusMsg = 'Subject queued offline.';
          else {
            state.statusMsg = `Subject ${res.code} added.`;
            await fetchRoutineData();
          }
        } catch (ex) {
          state.error = fmtErr(ex);
        }
        draw('routine');
      };
    }

    document.querySelectorAll('[data-del-subject]').forEach(btn => {
      btn.onclick = async () => {
        state.error = '';
        state.statusMsg = '';
        try {
          const res = await Api.del(`/subjects/${btn.dataset.delSubject}`);
          if (res && res.queued) state.statusMsg = 'Subject deletion queued offline.';
          else {
            state.statusMsg = 'Subject deleted.';
            await fetchRoutineData();
          }
        } catch (ex) {
          state.error = fmtErr(ex);
        }
        draw('routine');
      };
    });
  }

  function bindNotices(on) {
    const searchForm = $('#noticeSearchForm');
    if (searchForm) {
      searchForm.onsubmit = async e => {
        e.preventDefault();
        state.noticeQuery = ($('#noticeQuery').value || '').trim();
        state.error = '';
        state.statusMsg = '';
        await fetchNoticesData();
        draw('notices');
      };
    }

    on('#clearNoticeQuery', async () => {
      state.noticeQuery = '';
      state.error = '';
      state.statusMsg = '';
      await fetchNoticesData();
      draw('notices');
    });

    on('#cancelNoticeEdit', () => {
      state.editNoticeId = null;
      state.error = '';
      draw('notices');
    });

    const noticeForm = $('#noticeForm');
    if (noticeForm) {
      noticeForm.onsubmit = async e => {
        e.preventDefault();
        state.error = '';
        state.statusMsg = '';
        const body = {
          title: ($('#nt').value || '').trim(),
          body: ($('#nb').value || '').trim(),
          pinned: $('#np').checked
        };
        if (!body.title) {
          state.error = 'Notice title is required';
          draw('notices');
          return;
        }
        try {
          const res = state.editNoticeId
            ? await Api.put(`/notices/${state.editNoticeId}`, body)
            : await Api.post('/notices', body);
          if (res && res.queued) {
            state.statusMsg = 'Notice saved offline — will sync when online.';
          } else {
            state.statusMsg = state.editNoticeId ? 'Notice updated.' : 'Notice published.';
            state.editNoticeId = null;
            await fetchNoticesData();
          }
        } catch (ex) {
          state.error = fmtErr(ex);
        }
        draw('notices');
      };
    }

    document.querySelectorAll('[data-edit-notice]').forEach(btn => {
      btn.onclick = () => {
        state.editNoticeId = btn.dataset.editNotice;
        state.error = '';
        state.statusMsg = '';
        draw('notices');
      };
    });

    document.querySelectorAll('[data-del-notice]').forEach(btn => {
      btn.onclick = async () => {
        state.error = '';
        state.statusMsg = '';
        try {
          const res = await Api.del(`/notices/${btn.dataset.delNotice}`);
          if (res && res.queued) state.statusMsg = 'Notice deletion queued offline.';
          else {
            if (String(state.editNoticeId) === btn.dataset.delNotice) state.editNoticeId = null;
            state.statusMsg = 'Notice deleted.';
            await fetchNoticesData();
          }
        } catch (ex) {
          state.error = fmtErr(ex);
        }
        draw('notices');
      };
    });
  }

  function bind(v) {
    const on = (sel, fn) => { const e = $(sel); if (e) e.addEventListener('click', fn); };
    if (v === 'routine') bindRoutine(on);
    if (v === 'notices') bindNotices(on);
    if (v === 'assignments') {
      document.querySelectorAll('[data-tag]').forEach(e => e.onclick = () => { state.tag = e.dataset.tag || null; draw('assignments'); });
      document.querySelectorAll('[data-done]').forEach(e => e.onclick = () => { const a = D.assignments.find(x => x.id == e.dataset.done); a.done = !a.done; draw('assignments'); });
      document.querySelectorAll('[data-cm]').forEach(e => e.onclick = () => {
        const id = e.dataset.cm, t = $(`[data-c="${id}"]`).value.trim(); if (!t) return;
        D.assignments.find(x => x.id == id).comments.push({ t, priv: $(`[data-v="${id}"]`).value === '1' }); draw('assignments');
      });
      on('#addAsg', () => {
        const t = $('#at').value.trim(); if (!t) return;
        D.assignments.unshift({ id: Date.now(), title: t, subject: $('#as').value, by: state.role === 'CR' ? 'CR' : 'Teacher', due: '—', link: $('#al').value || 'no attachment', done: false, comments: [] }); draw('assignments');
      });
    }
    if (v === 'forms') document.querySelectorAll('[data-form]').forEach(e => e.onclick = () => { const f = D.forms.find(x => x.id == e.dataset.form); f.done = !f.done; draw('forms'); });
    if (v === 'chat') {
      const send = () => { const i = $('#ci'); if (!i.value.trim()) return; D.chat.push({ u: 'You', t: i.value, me: true }); draw('chat'); };
      on('#send', send); $('#ci').onkeydown = e => e.key === 'Enter' && send();
    }
    if (v === 'assistant') bindAssistant(on);
  }

  function bindAuth() {
    const form = $('#authForm'), err = $('#authErr');
    $('#authSwitch').onclick = () => { state.authMode = state.authMode === 'login' ? 'register' : 'login'; render(); };
    form.onsubmit = async e => {
      e.preventDefault();
      const f = Object.fromEntries(new FormData(form));
      const btn = form.querySelector('[type=submit]');
      err.textContent = ''; btn.disabled = true;
      try {
        if (state.authMode === 'register') await Api.register(f.name, f.email, f.password, f.role);
        const user = await Api.login(f.email, f.password);
        state.role = primaryRole(user); state.authMode = 'login';
        location.hash = '#home'; render();
      } catch (ex) {
        err.textContent = fmtErr(ex);
      } finally { btn.disabled = false; }
    };
  }

  function bindAssistant(on) {
    const add = h => { $('#msgs').insertAdjacentHTML('beforeend', h); $('#msgs').scrollTop = 1e6; };
    const say = t => add(msg({ u: 'Assistant', ai: true, t }));
    const send = () => {
      const i = $('#ci'), q = i.value.trim(); if (!q) return; i.value = '';
      add(msg({ u: 'You', t: q, me: true }));
      if (!canEdit() && /move|change|update|add/i.test(q)) return say('Only CR or Teacher can change data. I can answer questions though.');
      say('AI assistant backend integration will be available once E5 is implemented.');
    };
    on('#aiSend', send); $('#ci').onkeydown = e => e.key === 'Enter' && send();
  }

  $('#logout').onclick = async () => {
    await Api.logout();
    state.edit = false; state.editSlotId = null; state.editNoticeId = null;
    state.routine = null; state.subjects = []; state.noticesPage = null;
    render();
  };
  addEventListener('api:unauthorized', () => { state.edit = false; render(); });
  addEventListener('api:synced', () => {
    const v = currentPage();
    if (Api.isLoggedIn() && (v === 'routine' || v === 'notices' || v === 'home')) loadPage(v);
  });
  addEventListener('hashchange', render);
  const net = () => { const n = $('#net'); n.textContent = navigator.onLine ? 'Online' : 'Offline'; n.className = 'pill ' + (navigator.onLine ? 'ok' : 'off'); };
  addEventListener('online', net); addEventListener('offline', net); net();
  let dp; addEventListener('beforeinstallprompt', e => { e.preventDefault(); dp = e; $('#install').hidden = false; });
  $('#install').onclick = async () => { dp.prompt(); await dp.userChoice; $('#install').hidden = true; };
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
  render();
})();

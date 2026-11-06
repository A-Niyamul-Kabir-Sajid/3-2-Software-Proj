(() => {
  const $ = (s, r = document) => r.querySelector(s);
  const D = window.DB;
  const state = { role: 'STUDENT', tag: null, edit: false, ai: null };
  const canEdit = () => state.role !== 'STUDENT';
  const canTeach = () => state.role === 'TEACHER';
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  const msg = m => `<div class="msg ${m.me ? 'me' : ''} ${m.ai ? 'ai' : ''}"><small>${esc(m.u)}</small>${esc(m.t)}</div>`;
  const noticeCard = n => `<div class="card"><h3>${esc(n.title)}</h3><p>${esc(n.body || '')}</p><div class="meta"><span>${esc(n.by)}</span><span>${n.date}</span></div></div>`;
  const titles = { home: 'Home', routine: 'Routine', notices: 'Notices', assignments: 'Assignments', forms: 'Forms', chat: 'Chat', assistant: 'AI Assistant' };

  const views = {
    home() {
      return `<h1>Your class,<br><em>in sync.</em></h1>
      <p class="sub">Routine, notices, assignments and forms in one installable app that works offline.</p>
      <div class="hero-grid">
        <div class="card"><div class="stat">${D.assignments.filter(a => !a.done).length}</div><p>Pending assignments</p></div>
        <div class="card"><div class="stat">${D.forms.filter(f => !f.done).length}</div><p>Open forms</p></div>
        <div class="card"><div class="stat">${D.notices.length}</div><p>Notices</p></div>
        <div class="card"><div class="stat">${D.subjects.length}</div><p>Subjects</p></div>
      </div>
      <h2>Latest notices</h2><div class="grid">${D.notices.map(noticeCard).join('')}</div>`;
    },
    routine() {
      const r = D.routine, ed = state.edit && canEdit();
      const rows = r.slots.map(s => `<tr><th>${s}</th>${r.days.map(d => {
        const c = r.cells[`${d}-${s}`];
        return `<td ${ed ? `contenteditable="true" data-k="${d}-${s}"` : ''}>${c ? `<b>${esc(c[0])}</b><span>${esc(c[1])}</span>` : ''}</td>`;
      }).join('')}</tr>`).join('');
      return `<h1>Class <em>routine</em></h1><p class="sub">Latest routine. ${canEdit() ? 'CR can edit it.' : 'View only.'}</p>
      ${canEdit() ? `<button class="btn" id="toggleEdit">${ed ? 'Save routine' : 'Routine editor'}</button>` : ''}
      <div style="overflow:auto;margin-top:16px"><table><tr><th></th>${r.days.map(d => `<th>${d}</th>`).join('')}</tr>${rows}</table></div>`;
    },
    notices() {
      return `<h1>Notice <em>board</em></h1><p class="sub">Official notices uploaded by CR and teachers.</p>
      ${canEdit() ? `<div class="row"><input id="nt" placeholder="Notice title"><button class="btn" id="addNotice">Upload</button></div>` : ''}
      <div class="grid" style="margin-top:18px">${D.notices.map(noticeCard).join('')}</div>`;
    },
    assignments() {
      const list = D.assignments.filter(a => !state.tag || a.subject === state.tag);
      const tags = `<span class="tag ${!state.tag ? 'on' : ''}" data-tag="">All</span>` +
        D.subjects.map(s => `<span class="tag ${state.tag === s ? 'on' : ''}" data-tag="${s}">${s}</span>`).join('');
      const card = a => `<div class="card ${a.done ? 'done' : ''}"><h3>${esc(a.title)}</h3>
        <span class="tag on">${a.subject}</span><p style="margin-top:8px">${esc(a.link)} · by ${a.by}</p>
        ${a.comments.filter(c => !c.priv || canTeach()).map(c => `<div class="comment ${c.priv ? 'priv' : ''}">${esc(c.t)}</div>`).join('')}
        <div class="row"><input data-c="${a.id}" placeholder="Comment…"><select class="in" data-v="${a.id}" style="flex:0 0 auto"><option value="0">Public</option>${canTeach() ? '<option value="1">Teacher only</option>' : ''}</select><button class="btn sm" data-cm="${a.id}">Post</button></div>
        <div class="meta"><span>Due ${a.due}</span><button class="btn sm ghost" data-done="${a.id}">${a.done ? 'Mark pending' : 'Mark complete'}</button></div></div>`;
      const pend = list.filter(a => !a.done), done = list.filter(a => a.done);
      return `<h1>Assign<em>ments</em></h1><p class="sub">Tagged by subject so you can sort and organise.</p>
      ${canEdit() ? `<div class="card" style="margin-bottom:18px"><h3>New assignment (${state.role})</h3><div class="row"><input id="at" placeholder="Title"><input id="al" placeholder="Link or document name"><select class="in" id="as" style="flex:0 0 auto">${D.subjects.map(s => `<option>${s}</option>`).join('')}</select><button class="btn" id="addAsg">Add</button></div></div>` : ''}
      <div>${tags}</div><h2>Pending</h2><div class="grid">${pend.map(card).join('') || '<p class="sub">Nothing pending 🎉</p>'}</div>
      <h2>Completed</h2><div class="grid">${done.map(card).join('') || '<p class="sub">None yet</p>'}</div>`;
    },
    forms() {
      return `<h1>Forms</h1><p class="sub">Fill-ups and registrations posted by CR.</p><div class="grid">
      ${D.forms.map(f => `<div class="card ${f.done ? 'done' : ''}"><h3>${esc(f.title)}</h3><div class="meta"><span>Due ${f.due}</span>
      <button class="btn sm ghost" data-form="${f.id}">${f.done ? 'Done' : 'Mark filled'}</button></div></div>`).join('')}</div>`;
    },
    chat() {
      return `<h1>Group <em>chat</em></h1><p class="sub">Real-time class room (mock data for now).</p>
      <div class="card chat" id="msgs">${D.chat.map(msg).join('')}</div>
      <div class="row"><input id="ci" placeholder="Message class…"><button class="btn" id="send">Send</button></div>`;
    },
    assistant() {
      return `<h1>AI <em>assistant</em></h1><p class="sub">Type one prompt. I'll understand it and ask for confirmation before changing anything. Try: <i>move Monday AI to 12:00</i></p>
      <div class="card chat" id="msgs"><div class="msg ai"><small>Assistant</small>Hi! Ask about the class, or (CR / Teacher) tell me what to update.</div></div>
      <div class="row"><input id="ci" placeholder="Ask or instruct…"><button class="btn" id="aiSend">Send</button></div>`;
    }
  };

  function render() {
    const page = location.hash.slice(1) || 'home';
    const v = views[page] ? page : 'home';
    $('#nav').innerHTML = Object.keys(titles).map(k => `<a href="#${k}" class="${k === v ? 'on' : ''}">${titles[k]}</a>`).join('');
    $('#view').innerHTML = views[v]();
    bind(v);
    const m = $('#msgs'); if (m) m.scrollTop = m.scrollHeight;
  }

  function bind(v) {
    const on = (sel, fn) => { const e = $(sel); if (e) e.addEventListener('click', fn); };
    if (v === 'routine') on('#toggleEdit', () => {
      if (state.edit) document.querySelectorAll('td[data-k]').forEach(td => {
        const t = td.innerText.trim().split('\n').filter(Boolean);
        if (t.length) D.routine.cells[td.dataset.k] = [t[0], t[1] || '']; else delete D.routine.cells[td.dataset.k];
      });
      state.edit = !state.edit; render();
    });
    if (v === 'notices') on('#addNotice', () => {
      const t = $('#nt').value.trim(); if (!t) return;
      D.notices.unshift({ id: Date.now(), title: t, body: '', by: state.role, date: new Date().toISOString().slice(0, 10) }); render();
    });
    if (v === 'assignments') {
      document.querySelectorAll('[data-tag]').forEach(e => e.onclick = () => { state.tag = e.dataset.tag || null; render(); });
      document.querySelectorAll('[data-done]').forEach(e => e.onclick = () => { const a = D.assignments.find(x => x.id == e.dataset.done); a.done = !a.done; render(); });
      document.querySelectorAll('[data-cm]').forEach(e => e.onclick = () => {
        const id = e.dataset.cm, t = $(`[data-c="${id}"]`).value.trim(); if (!t) return;
        D.assignments.find(x => x.id == id).comments.push({ t, priv: $(`[data-v="${id}"]`).value === '1' }); render();
      });
      on('#addAsg', () => {
        const t = $('#at').value.trim(); if (!t) return;
        D.assignments.unshift({ id: Date.now(), title: t, subject: $('#as').value, by: state.role === 'CR' ? 'CR' : 'Teacher', due: '—', link: $('#al').value || 'no attachment', done: false, comments: [] }); render();
      });
    }
    if (v === 'forms') document.querySelectorAll('[data-form]').forEach(e => e.onclick = () => { const f = D.forms.find(x => x.id == e.dataset.form); f.done = !f.done; render(); });
    if (v === 'chat') {
      const send = () => { const i = $('#ci'); if (!i.value.trim()) return; D.chat.push({ u: 'You', t: i.value, me: true }); render(); };
      on('#send', send); $('#ci').onkeydown = e => e.key === 'Enter' && send();
    }
    if (v === 'assistant') bindAssistant(on);
  }

  function bindAssistant(on) {
    const add = h => { $('#msgs').insertAdjacentHTML('beforeend', h); $('#msgs').scrollTop = 1e6; };
    const say = t => add(msg({ u: 'Assistant', ai: true, t }));
    const send = () => {
      const i = $('#ci'), q = i.value.trim(); if (!q) return; i.value = '';
      add(msg({ u: 'You', t: q, me: true }));
      if (!canEdit() && /move|change|update|add/i.test(q)) return say('Only CR or Teacher can change data. I can answer questions though.');
      const mv = q.match(/move\s+(\w+)\s+(.+?)\s+to\s+(\d{1,2}):?(\d{2})?/i);
      if (mv) {
        const R = D.routine, day = R.days.find(d => d.toLowerCase() === mv[1].slice(0, 3).toLowerCase());
        const time = mv[3].padStart(2, '0') + ':' + (mv[4] || '00');
        const from = day && Object.keys(R.cells).find(k => k.startsWith(day + '-') && R.cells[k][0].toLowerCase() === mv[2].toLowerCase());
        const to = `${day}-${time}`;
        if (from && R.slots.includes(time) && !R.cells[to]) {
          add(`<div class="msg ai"><small>Assistant</small>I'll move <b>${esc(R.cells[from][0])}</b> from ${from.replace('-', ' ')} to ${day} ${time}.
            <div class="confirm">Confirm this change?<div class="row"><button class="btn sm" id="yes">Confirm</button><button class="btn sm ghost" id="no">Cancel</button></div></div></div>`);
          $('#yes').onclick = () => { R.cells[to] = R.cells[from]; delete R.cells[from]; $('.confirm').remove(); say('✅ Routine updated.'); };
          $('#no').onclick = () => { $('.confirm').remove(); say('Cancelled, nothing changed.'); };
          return;
        }
        return say("I couldn't find a matching class or the slot is taken — can you clarify? (Flagged to CR/Sir.)");
      }
      say("I'm not sure what you mean yet. Give the day, subject and new time. Unresolved questions go to the CR/Sir control panel.");
    };
    on('#aiSend', send); $('#ci').onkeydown = e => e.key === 'Enter' && send();
  }

  $('#role').onchange = e => { state.role = e.target.value; state.edit = false; render(); };
  addEventListener('hashchange', render);
  const net = () => { const n = $('#net'); n.textContent = navigator.onLine ? 'Online' : 'Offline'; n.className = 'pill ' + (navigator.onLine ? 'ok' : 'off'); };
  addEventListener('online', net); addEventListener('offline', net); net();
  let dp; addEventListener('beforeinstallprompt', e => { e.preventDefault(); dp = e; $('#install').hidden = false; });
  $('#install').onclick = async () => { dp.prompt(); await dp.userChoice; $('#install').hidden = true; };
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
  render();
})();

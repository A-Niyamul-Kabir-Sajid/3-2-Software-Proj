// Mock data (Flyweight-style shared subject tags). Replaced by api.js calls once backend is ready.
window.DB = {
  subjects: ['DBMS', 'Software Engg', 'Networks', 'AI', 'Math'],
  routine: {
    days: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu'],
    slots: ['08:00', '09:00', '10:00', '11:00', '12:00'],
    cells: {
      'Sun-08:00': ['DBMS', 'Rm 301'], 'Sun-10:00': ['Networks', 'Rm 204'],
      'Mon-09:00': ['Software Engg', 'Rm 301'], 'Mon-11:00': ['AI', 'Lab 2'],
      'Tue-08:00': ['Math', 'Rm 101'], 'Tue-10:00': ['DBMS', 'Rm 301'],
      'Wed-09:00': ['Networks', 'Lab 1'], 'Wed-12:00': ['AI', 'Rm 204'],
      'Thu-10:00': ['Software Engg', 'Rm 301']
    }
  },
  notices: [
    { id: 1, title: 'Mid-term schedule published', body: 'Mid-terms start 20 Nov. Check the routine for rooms.', by: 'CR', date: '2026-11-04' },
    { id: 2, title: 'Class test postponed', body: 'DBMS CT moved to Tuesday.', by: 'Sir (DBMS)', date: '2026-11-05' }
  ],
  assignments: [
    { id: 1, title: 'ER Diagram for Library System', subject: 'DBMS', by: 'Teacher', due: '2026-11-12', link: 'doc: er-task.pdf', done: false,
      comments: [{ t: 'Draw using any tool.', priv: false }, { t: 'Check plagiarism on Ali & Rafi.', priv: true }] },
    { id: 2, title: 'SRS Document Draft', subject: 'Software Engg', by: 'CR', due: '2026-11-15', link: 'link: drive/srs', done: true, comments: [] },
    { id: 3, title: 'Subnetting Worksheet', subject: 'Networks', by: 'Teacher', due: '2026-11-10', link: 'doc: subnet.pdf', done: false, comments: [] }
  ],
  forms: [
    { id: 1, title: 'Tour registration', due: '2026-11-18', done: false },
    { id: 2, title: 'Exam form fill-up', due: '2026-11-09', done: true }
  ],
  chat: [
    { u: 'Rafi', t: 'Anyone has DBMS notes?' }, { u: 'Mim', t: 'Posted in assignments tab 👍' }
  ]
};

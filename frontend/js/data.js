// Mock data for modules not yet migrated to the backend API (E3: assignments/forms, E4: chat).
window.DB = {
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

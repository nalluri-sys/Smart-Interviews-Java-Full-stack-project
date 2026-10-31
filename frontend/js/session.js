(() => {
  const config = JSON.parse(localStorage.getItem('smartInterviewSessionConfig') || '{}');
  const role = config.role || 'Data Analyst';
  const questionCount = config.questions || 5;
  const bank = {
    'Data Analyst': ['Tell me about a project where you used data to solve a business problem.', 'How would you handle missing values in a dataset?', 'Explain the difference between INNER JOIN and LEFT JOIN.', 'Which metrics would you use to measure a product feature?', 'How do you communicate a complex finding to a non-technical stakeholder?'],
    'Frontend Developer': ['Describe a frontend project you are proud of.', 'How do you improve web performance?', 'Explain the difference between state and props.', 'How do you make a UI accessible?', 'How do you handle API loading and error states?'],
    'Product Manager': ['How would you prioritize competing product requests?', 'Describe a product decision driven by user research.', 'How do you define success for a new feature?', 'Tell me about a time you handled disagreement.', 'How would you improve an onboarding flow?']
  };
  const questions = (bank[role] || bank['Data Analyst']).slice(0, questionCount);
  while (questions.length < questionCount) questions.push(`Tell me about a key skill you would bring to a ${role} role.`);
  let current = 0;
  const answered = [];
  let remaining = Number.parseInt(config.duration, 10) * 60 || 900;
  const $ = (id) => document.getElementById(id);
  $('roomRole').textContent = `${role} Interview`;
  $('sessionDetails').textContent = `${config.difficulty || 'Medium'} · ${config.experience || 'Fresher'} · ${(config.language || 'English').split(' ')[0]}`;
  const render = () => {
    $('questionCounter').textContent = `QUESTION ${current + 1} OF ${questions.length}`;
    $('questionText').textContent = questions[current];
    $('answerInput').value = answered[current]?.answer || '';
    const percent = Math.round((answered.length / questions.length) * 100);
    $('progressPercent').textContent = `${percent}%`;
    $('progressBar').style.width = `${percent}%`;
    $('answeredCount').textContent = `${answered.length} of ${questions.length} answered`;
  };
  const finish = () => {
    if (!answered.length) return;
    const score = Math.min(98, Math.max(45, Math.round(answered.reduce((total, item) => total + Math.min(item.answer.length / 7, 100), 0) / answered.length)));
    const completedAt = new Date().toISOString();
    const activity = JSON.parse(localStorage.getItem('smartInterviewActivity') || '{}');
    const activityDate = completedAt.slice(0, 10);
    activity[activityDate] = (Number(activity[activityDate]) || 0) + 1;
    localStorage.setItem('smartInterviewActivity', JSON.stringify(activity));
    localStorage.setItem('smartInterviewLatestReport', JSON.stringify({ role, questions, answers: answered, overallScore: score, completedAt }));
    window.location.href = 'report.html';
  };
  $('submitButton').addEventListener('click', () => {
    const answer = $('answerInput').value.trim();
    if (!answer) { $('answerInput').focus(); $('sessionStatus').textContent = 'Add an answer before submitting.'; return; }
    answered[current] = { question: questions[current], answer };
    if (current < questions.length - 1) { current += 1; render(); } else finish();
  });
  $('skipButton').addEventListener('click', () => { if (current < questions.length - 1) { current += 1; render(); } else finish(); });
  $('endButton').addEventListener('click', () => { if (answered.length && window.confirm('End this interview and view your report?')) finish(); });
  window.setInterval(() => { remaining -= 1; const minutes = Math.floor(Math.max(remaining, 0) / 60); const seconds = Math.max(remaining, 0) % 60; $('timer').textContent = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`; if (remaining <= 0) finish(); }, 1000);
  render();
})();

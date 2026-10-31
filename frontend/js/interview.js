document.querySelectorAll('.choice').forEach((choice) => {
  choice.addEventListener('click', () => {
    choice.parentElement.querySelectorAll('.choice').forEach((item) => item.classList.remove('selected'));
    choice.classList.add('selected');
  });
});
document.querySelector('#setupForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const selected = (name) => form.querySelector(`[data-setting="${name}"].selected`)?.textContent.trim() || '';
  const session = {
    role: form.querySelector('select')?.value || 'Data Analyst',
    experience: selected('experience') || 'Fresher',
    difficulty: selected('difficulty') || 'Medium',
    duration: selected('duration') || '15 mins',
    questions: Number(selected('questions') || 5),
    language: form.querySelectorAll('select')[1]?.value || 'English (United States)',
    startedAt: new Date().toISOString()
  };
  localStorage.setItem('smartInterviewSessionConfig', JSON.stringify(session));
  window.location.href = 'interview-room.html';
});

document.querySelectorAll('.hero .btn').forEach((button) => {
  button.addEventListener('click', () => { window.location.href = 'interview.html'; });
});

document.querySelectorAll('[data-count]').forEach((counter) => {
  const target = Number(counter.dataset.count);
  const suffix = counter.textContent.replace(/[\d.+-]/g, '');
  const duration = 650;
  const start = performance.now();
  const update = (time) => {
    const progress = Math.min((time - start) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    counter.textContent = `${Math.round(target * eased)}${suffix}`;
    if (progress < 1) requestAnimationFrame(update);
  };
  requestAnimationFrame(update);
});

const latestReport = JSON.parse(localStorage.getItem('smartInterviewLatestReport') || 'null');
if (latestReport) {
  const values = document.querySelectorAll('.stats .value');
  const score = latestReport.overallScore;
  if (values[0]) values[0].textContent = '1';
  if (values[1]) values[1].textContent = `${score}%`;
  if (values[2]) values[2].textContent = `${score}%`;
  if (values[3]) values[3].textContent = '0%';
  const recent = document.querySelector('#recent-interviews .empty');
  if (recent) {
    recent.innerHTML = `<strong>${latestReport.role}</strong><br><span class="eyebrow">Latest interview · ${score}%</span><br><a class="btn ghost" href="report.html" style="margin-top:12px">View report</a>`;
  }
  const performance = document.querySelector('.performance-empty');
  if (performance) performance.innerHTML = `Your latest score is <strong>${score}%</strong>.<br><a href="report.html">Review your feedback →</a>`;
}

const report = JSON.parse(localStorage.getItem('smartInterviewLatestReport') || 'null');
const downloadButton = document.querySelector('#downloadReport');

if (report) {
  document.querySelector('#reportMeta').textContent = `${report.role} · ${new Date(report.completedAt).toLocaleDateString()} · ${report.questions.length} Questions`;
  document.querySelector('#reportScore').textContent = report.overallScore;
  document.querySelector('#reportTitle').textContent = 'Interview complete';
  document.querySelector('#reportSummary').textContent = 'Your first practice session is complete. Keep practicing to improve your score.';
  document.querySelector('#startReportButton').textContent = 'Practice Again  →';
  const breakdown = document.querySelector('#breakdown');
  breakdown.className = 'report-breakdown';
  breakdown.innerHTML = `<div class="progress-row"><div><div class="progress-label"><span>Answer completeness</span><b>${report.overallScore}%</b></div><div class="bar"><span style="width:${report.overallScore}%"></span></div></div></div><p class="eyebrow">${report.answers.length} answer(s) submitted successfully.</p>`;
  document.querySelector('#feedback').innerHTML = '<div class="callout">✓ <strong>Keep going</strong><br>Your responses have been saved. Complete more sessions to receive stronger personalized feedback.</div>';
  downloadButton.disabled = false;
}

downloadButton?.addEventListener('click', () => window.print());

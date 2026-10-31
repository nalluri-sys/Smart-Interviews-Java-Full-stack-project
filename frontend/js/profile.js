document.querySelectorAll('.tabs a').forEach((tab) => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tabs a').forEach((item) => item.classList.remove('active'));
    tab.classList.add('active');
  });
});

document.querySelectorAll('.toggle').forEach((toggle) => {
  toggle.addEventListener('click', () => toggle.classList.toggle('on'));
});

const heatmap = document.querySelector('#activity-heatmap');
const activityTotal = document.querySelector('#activity-total');

if (heatmap) {
  const activity = JSON.parse(localStorage.getItem('smartInterviewActivity') || '{}');
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  let total = 0;

  for (let index = 83; index >= 0; index -= 1) {
    const date = new Date(today);
    date.setDate(today.getDate() - index);
    const key = date.toISOString().slice(0, 10);
    const count = Number(activity[key]) || 0;
    const level = count === 0 ? 0 : Math.min(4, count);
    total += count;

    const cell = document.createElement('button');
    cell.type = 'button';
    cell.className = `heat-cell level-${level}`;
    cell.setAttribute('role', 'gridcell');
    cell.setAttribute('aria-label', `${count} practice session${count === 1 ? '' : 's'} on ${date.toLocaleDateString()}`);
    cell.title = cell.getAttribute('aria-label');
    heatmap.appendChild(cell);
  }

  if (activityTotal) {
    activityTotal.textContent = `${total} session${total === 1 ? '' : 's'}`;
  }
}

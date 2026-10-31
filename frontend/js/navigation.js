(() => {
  const currentPage = window.location.pathname.split(/[\\/]/).pop() || 'index.html';
  const pageLinks = {
    'My Interviews': 'my-interviews.html',
    Profile: 'profile.html',
    Settings: 'profile.html#settings'
  };

  document.querySelectorAll('.nav a, .side-bottom a').forEach((link) => {
    const label = link.textContent.trim().replace(/^[^\w]+/, '');
    const target = link.getAttribute('href') || '#';
    if (pageLinks[label] && (target === '#' || target.includes('dashboard.html#'))) {
      link.href = pageLinks[label];
    }
    const targetPage = link.href.split('/').pop().split('#')[0];
    const isActive = targetPage === currentPage;
    link.classList.toggle('active', isActive);
    if (isActive) link.setAttribute('aria-current', 'page');
  });

  const logout = document.querySelector('[data-action="logout"]');
  logout?.addEventListener('click', (event) => {
    event.preventDefault();
    window.location.href = 'login.html';
  });
})();

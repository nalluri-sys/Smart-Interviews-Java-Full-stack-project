const registerForm = document.querySelector('#registerForm');

if (registerForm) {
  const tabs = registerForm.querySelectorAll('.source-tab');
  const panels = registerForm.querySelectorAll('.source-panel');
  const resumeInput = registerForm.querySelector('#resumeInput');
  const fileStatus = registerForm.querySelector('#fileStatus');
  const preview = registerForm.querySelector('#analysisPreview');
  const analysisText = registerForm.querySelector('#analysisText');
  let source = 'resume';
  let resumeName = '';
  let resumeText = '';

  const showPreview = (text) => {
    preview.hidden = false;
    analysisText.textContent = text;
  };

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      source = tab.dataset.source;
      tabs.forEach((item) => item.classList.toggle('active', item === tab));
      panels.forEach((panel) => panel.classList.toggle('active', panel.dataset.panel === source));
      if (source === 'manual') {
        showPreview('Your role, skills and goals will be used to suggest interview topics and question difficulty.');
      } else if (!resumeName) {
        preview.hidden = true;
      }
    });
  });

  resumeInput.addEventListener('change', async () => {
    const file = resumeInput.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      resumeInput.value = '';
      fileStatus.textContent = 'This file is larger than 5 MB. Please choose a smaller resume.';
      fileStatus.classList.add('error-text');
      return;
    }
    resumeName = file.name;
    fileStatus.classList.remove('error-text');
    fileStatus.textContent = `${file.name} selected. Analysing your profile…`;
    showPreview('AI profile analysis is ready. Your interview will be tailored to the experience and skills found in this resume.');
    if (file.type === 'text/plain' || file.name.toLowerCase().endsWith('.txt')) {
      resumeText = await file.text();
    }
    window.setTimeout(() => {
      fileStatus.textContent = `${file.name} selected · Ready for profile analysis`;
    }, 650);
  });

  registerForm.addEventListener('submit', (event) => {
    event.preventDefault();
    const password = registerForm.elements.password.value;
    if (password !== registerForm.elements.confirmPassword.value) {
      registerForm.elements.confirmPassword.setCustomValidity('Passwords do not match.');
    } else {
      registerForm.elements.confirmPassword.setCustomValidity('');
    }
    if (!registerForm.reportValidity()) return;

    const data = Object.fromEntries(new FormData(registerForm).entries());
    delete data.password;
    delete data.confirmPassword;
    delete data.consent;
    data.profileSource = source;
    data.resumeName = resumeName;
    data.resumeText = resumeText;
    data.profileStatus = 'pending-ai-analysis';
    localStorage.setItem('smartInterviewProfile', JSON.stringify(data));
    window.location.href = 'login.html?registered=1';
  });
}

const loginForm = document.querySelector('#loginForm');
loginForm?.addEventListener('submit', (event) => {
  event.preventDefault();
  window.location.href = 'dashboard.html';
});

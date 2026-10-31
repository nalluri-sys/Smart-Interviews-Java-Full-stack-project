const VoiceCoach = {
  listening: false,
  toggle() {
    this.listening = !this.listening;
    return this.listening;
  }
};

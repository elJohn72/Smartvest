let audioContext: AudioContext | null = null;

const getAudioContext = (): AudioContext | null => {
  if (typeof window === 'undefined') {
    return null;
  }

  const AudioCtx = window.AudioContext || (window as typeof window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!AudioCtx) {
    return null;
  }

  if (!audioContext) {
    audioContext = new AudioCtx();
  }

  return audioContext;
};

export const playSosAlertSound = (): void => {
  const ctx = getAudioContext();
  if (!ctx) {
    return;
  }

  void ctx.resume();

  const playBeep = (startOffset: number, frequency: number) => {
    const oscillator = ctx.createOscillator();
    const gain = ctx.createGain();
    oscillator.type = 'square';
    oscillator.frequency.value = frequency;
    gain.gain.value = 0.12;
    oscillator.connect(gain);
    gain.connect(ctx.destination);
    const start = ctx.currentTime + startOffset;
    oscillator.start(start);
    oscillator.stop(start + 0.18);
  };

  playBeep(0, 880);
  playBeep(0.22, 660);
  playBeep(0.44, 880);
};

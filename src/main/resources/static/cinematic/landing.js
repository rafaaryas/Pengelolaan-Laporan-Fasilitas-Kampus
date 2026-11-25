let canvas = document.querySelector('#cakra-canvas');
const control = document.querySelector('#cakra-motion');
const root = document.documentElement;
const mask = document.querySelector('.landing-mask');
const motion = matchMedia('(prefers-reduced-motion: reduce)');
const sessionKey = 'um-peduli-cakra-seen';

let pending, player, script;
let paused = false;
let failed = false;

function hasSeenOpening() {
  try { return Boolean(sessionStorage.getItem(sessionKey)); }
  catch { return false; }
}

function markSeen() {
  try { sessionStorage.setItem(sessionKey, '1'); }
  catch { /* Storage can be disabled. */ }
}

function showPoster() {
  delete root.dataset.cakra;
  control.hidden = true;
  mask.style.removeProperty('opacity');
}
function releaseScene() {
  pending?.abort();
  pending = undefined;
  player?.dispose();
  player = undefined;
  showPoster();
}
function useFallback() {
  failed = true;
  releaseScene();
}

function loadTimeline() {
  if (!script) {
    script = new Promise((resolve, reject) => {
      const library = document.createElement('script');
      library.src = new URL('./vendor/gsap.min.js', import.meta.url);
      library.onload = resolve;
      library.onerror = reject;
      document.head.append(library);
    });
  }
  return script;
}

function afterFirstPaint() {
  return new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
}

async function enhance() {
  if (pending || player || failed || document.hidden) return;
  if (motion.matches || navigator.connection?.saveData) {
    showPoster();
    return;
  }

  const returning = hasSeenOpening();
  const request = new AbortController();
  pending = request;
  if (!returning) root.dataset.cakra = 'loading';

  try {
    await afterFirstPaint();
    request.signal.throwIfAborted();
    const [{ createCinematic }] = await Promise.all([import('./cinematic.js'), loadTimeline()]);
    request.signal.throwIfAborted();

    // A new canvas owns its own context even if a previous load is still unwinding.
    const nextCanvas = canvas.cloneNode();
    canvas.replaceWith(nextCanvas);
    canvas = nextCanvas;
    let lastScrim;
    const shot = await createCinematic(nextCanvas, {
      startAtEnd: returning,
      paused,
      signal: request.signal,
      onFailure: useFallback,
      onScrim(strength) {
        if (request.signal.aborted || canvas !== nextCanvas) return;
        const value = strength.toFixed(2);
        if (value === lastScrim) return;
        mask.style.opacity = value;
        lastScrim = value;
      }
    });
    if (request.signal.aborted) {
      shot.dispose();
      return;
    }

    player = shot;
    root.dataset.cakra = 'ready';
    control.hidden = false;
    markSeen();
  } catch {
    if (!request.signal.aborted) useFallback();
  } finally {
    if (pending === request) pending = undefined;
  }
}

control.addEventListener('click', () => {
  if (!player) return;
  paused = !paused;
  if (paused) player.pause();
  else player.play();

  const label = paused ? 'Lanjutkan animasi' : 'Jeda animasi';
  control.toggleAttribute('data-paused', paused);
  control.setAttribute('aria-label', label);
  control.title = label;
});

motion.addEventListener('change', () => {
  if (motion.matches) releaseScene();
  else void enhance();
});
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) void enhance();
});
window.addEventListener('pagehide', releaseScene);
window.addEventListener('pageshow', event => {
  if (event.persisted) void enhance();
});
void enhance();

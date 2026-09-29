function truckerFindSavedVoice(command) {
  if (location.origin !== 'https://truckerfindapp.github.io' ||
      !location.pathname.startsWith('/Trucker-Find--web/')) return 'untrusted';
  if (typeof supabaseClient === 'undefined' || typeof mapsDirections !== 'function' || !document.body) return 'loading';
  if (window.__tfSavedVoiceCancel) window.__tfSavedVoiceCancel();
  const controller = new AbortController();
  let cancelled = false;
  const shade = document.createElement('div');
  shade.style.cssText = 'position:fixed;inset:0;z-index:2147483647;background:#000b;display:flex;align-items:center;justify-content:center;padding:20px';
  const panel = document.createElement('section');
  panel.setAttribute('role', 'dialog'); panel.setAttribute('aria-modal', 'true');
  panel.setAttribute('aria-label', 'Saved Areas voice search');
  panel.style.cssText = 'background:#10273a;color:white;padding:20px;border-radius:18px;width:100%;max-width:430px;max-height:80vh;overflow:auto;font:16px Arial';
  const status = document.createElement('p'); status.setAttribute('role', 'status');
  status.textContent = 'Looking up your Saved Areas…';
  const choices = document.createElement('div');
  const close = document.createElement('button'); close.textContent = 'CLOSE';
  close.style.cssText = 'width:100%;padding:14px;margin-top:14px;border:0;border-radius:10px;background:#203b50;color:white;font-weight:bold';
  const previousFocus = document.activeElement;
  function dismiss() {
    cancelled = true; controller.abort(); shade.remove();
    document.removeEventListener('keydown', onKey);
    if (previousFocus && previousFocus.isConnected) previousFocus.focus();
  }
  function onKey(event) {
    if (event.key === 'Escape') dismiss();
    if (event.key === 'Tab') {
      const buttons = [...panel.querySelectorAll('button:not(:disabled)')];
      const first = buttons[0], last = buttons[buttons.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    }
  }
  window.__tfSavedVoiceCancel = dismiss;
  close.onclick = dismiss;
  panel.append(status, choices, close); shade.append(panel); document.body.append(shade);
  document.addEventListener('keydown', onKey); close.focus();
  const normalize = value => String(value || '').normalize('NFKD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase().replace(/['’]/g, '').replace(/[^\p{L}\p{N}]+/gu, ' ').trim().replace(/\s+/g, ' ');
  let name = normalize(command).replace(/^please /, '').replace(/ please$/, '')
    .replace(/^(?:hey trucker find |trucker find )/, '')
    .replace(/^(?:go to |navigate to |take me to |directions to |open |show me |show |find |search for )/, '')
    .replace(/^(?:my |the )/, '');
  const listAll = /^(?:saved areas|saved places|saved locations)$/.test(name);
  name = name.replace(/^(?:saved area |saved place |saved location )/, '');
  async function bounded(promise) {
    let timer;
    try {
      return await Promise.race([promise, new Promise((_, reject) => {
        timer = setTimeout(() => reject(new Error('timeout')), 15000);
      })]);
    } finally { clearTimeout(timer); }
  }
  async function currentAccount() {
    const response = await bounded(supabaseClient.auth.getUser());
    if (response.error) throw response.error;
    return response.data.user;
  }
  async function navigate(place, owner) {
    try {
      const user = await currentAccount();
      if (cancelled) return;
      if (!user || user.id !== owner) {
        choices.replaceChildren(); status.textContent = 'Your account changed. Try voice search again.'; return;
      }
      // Re-read the selected row so a deleted or renamed destination is not used.
      const result = await bounded(supabaseClient.from('saved_places').select('id,place_name,address')
        .eq('user_id', owner).eq('id', place.id).maybeSingle().abortSignal(controller.signal));
      if (cancelled) return;
      if (result.error) throw result.error;
      if (!result.data) { status.textContent = 'This Saved Area was removed. Try voice search again.'; return; }
      const latestUser = await currentAccount();
      if (cancelled) return;
      if (!latestUser || latestUser.id !== owner) { choices.replaceChildren(); status.textContent = 'Your account changed. Try voice search again.'; return; }
      const destination = (result.data.address || result.data.place_name || '').trim();
      if (!destination) { status.textContent = 'This Saved Area has no destination. Edit it and try again.'; return; }
      mapsDirections(destination);
      dismiss();
    } catch (error) {
      if (!cancelled) status.textContent = 'Could not open this Saved Area. Check your connection and try again.';
    }
  }
  (async () => {
    try {
      const user = await currentAccount();
      if (cancelled) return;
      if (!user) { status.textContent = 'Sign in to Trucker Find, then try your Saved Area voice command again.'; return; }
      const places = [];
      for (let offset = 0; ; offset += 500) {
        const result = await bounded(supabaseClient.from('saved_places').select('id,place_name,address')
          .eq('user_id', user.id).order('id', {ascending: true}).range(offset, offset + 499)
          .abortSignal(controller.signal));
        if (cancelled) return;
        if (result.error) throw result.error;
        places.push(...(result.data || []));
        if (!result.data || result.data.length < 500) break;
      }
      const exact = places.filter(place => normalize(place.place_name) === name);
      const matches = listAll ? places : exact.length ? exact : places.filter(place => name && normalize(place.place_name).includes(name));
      if (!matches.length) {
        status.textContent = places.length ? 'No Saved Area matched “' + command + '”. Try “Go to saved area” followed by its saved name, or say “Show saved areas”.' : 'You have no Saved Areas yet. Add one in Trucker Find first.';
      } else if (!listAll && matches.length === 1) {
        status.textContent = 'Opening ' + matches[0].place_name + '…';
        await navigate(matches[0], user.id);
      } else {
        status.textContent = listAll ? 'Choose a Saved Area, or close this and say its name.' : 'More than one Saved Area matches. Choose the destination:';
        matches.forEach(place => {
          const button = document.createElement('button');
          button.textContent = place.place_name + ' — ' + (place.address || 'Saved area');
          button.style.cssText = 'display:block;width:100%;padding:14px;margin:8px 0;text-align:left;background:#168fe8;color:white;border:0;border-radius:10px;font-size:16px';
          button.onclick = async () => {
            [...choices.children].forEach(item => item.disabled = true);
            await navigate(place, user.id);
            [...choices.children].forEach(item => item.disabled = false);
          };
          choices.append(button);
        });
        choices.firstChild.focus();
      }
    } catch (error) {
      if (!cancelled) status.textContent = 'Could not load Saved Areas. Check your connection and sign-in, then try again.';
    }
  })();
  return 'accepted';
}

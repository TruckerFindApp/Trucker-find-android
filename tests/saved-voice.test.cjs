const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const test = require('node:test');
const source = fs.readFileSync(require('node:path').join(__dirname, '../app/src/main/assets/trucker-find-saved-voice.js'), 'utf8');

async function run(command, rows, options = {}) {
  class Element {
    constructor(tag) { this.tag = tag; this.style = {}; this.children = []; this.textContent = ''; this.isConnected = true; }
    append(...items) { this.children.push(...items); }
    setAttribute() {}
    focus() { document.activeElement = this; }
    remove() { this.isConnected = false; }
    replaceChildren(...items) { this.children = items; }
    get firstChild() { return this.children[0]; }
  }
  const document = { body: new Element('body'), createElement: tag => new Element(tag), addEventListener() {}, removeEventListener() {} };
  const opened = [], calls = [];
  let authCalls = 0;
  const client = {
    auth: { getUser: async () => ({ data: { user: options.signedOut || (options.switchAccount && ++authCalls > 1) ? null : {id:'owner'} } }) },
    from(table) {
      const query = { table, filters: {}, start: 0, end: rows.length, single: false };
      const builder = {
        select() { return this; },
        eq(key, value) { query.filters[key] = value; return this; },
        order() { return this; },
        range(start, end) { query.start = start; query.end = end; return this; },
        maybeSingle() { query.single = true; return this; },
        abortSignal() {
          calls.push(query);
          const data = query.single ? (options.deleted ? null : rows.find(x => x.id === query.filters.id)) : rows.slice(query.start, query.end + 1);
          return Promise.resolve({ data, error: options.queryError ? new Error('network') : null });
        }
      };
      return builder;
    }
  };
  const context = vm.createContext({ document, window:{}, location:{origin:options.origin || 'https://truckerfindapp.github.io',pathname:'/Trucker-Find--web/'},
    supabaseClient: client, mapsDirections: x => opened.push(x), AbortController, setTimeout, clearTimeout });
  const status = vm.runInContext('(' + source + ')(' + JSON.stringify(command) + ')', context);
  for (let i = 0; i < 20; i++) await new Promise(resolve => setImmediate(resolve));
  const panel = document.body.firstChild?.firstChild;
  return { status, opened, calls, panel, message: panel?.firstChild.textContent };
}
const home = {id:'1',place_name:'Home',address:'123 Main St, Austin TX'};
test('saved destination opens its address and enforces account filters', async () => {
  const r = await run('Go to saved area Home', [home]);
  assert.deepEqual(r.opened, [home.address]);
  assert.ok(r.calls.every(q => q.table === 'saved_places' && q.filters.user_id === 'owner'));
});
test('spoken names ignore punctuation and accents', async () => {
  const r = await run('navigate to saved area joes cafe', [{...home,place_name:"Joe’s Café"}]);
  assert.equal(r.opened.length, 1);
});
test('duplicate names require a choice', async () => {
  const r = await run('Home', [home,{...home,id:'2',address:'Dallas TX'}]);
  assert.equal(r.opened.length, 0); assert.match(r.message, /More than one/);
  assert.equal(r.panel.children[1].children.length, 2);
  await r.panel.children[1].children[1].onclick();
  assert.deepEqual(r.opened, ['Dallas TX']);
});
test('exact names take precedence over partial matches', async () => {
  const r = await run('Home', [home,{...home,id:'2',place_name:'Home terminal'}]);
  assert.deepEqual(r.opened, [home.address]);
});
test('unknown destination never opens a guessed search', async () => {
  const r = await run('not my home', [home]); assert.equal(r.opened.length, 0); assert.match(r.message, /No Saved Area matched/);
});
test('signed out does not query saved locations', async () => {
  const r = await run('Home', [home], {signedOut:true}); assert.equal(r.calls.length, 0); assert.match(r.message, /Sign in/);
});
test('saved area beyond first 1000 rows is available', async () => {
  const rows = Array.from({length:1001}, (_, i) => ({id:String(i),place_name:'Stop '+i,address:'Address '+i}));
  const r = await run('Go to saved area Stop 1000', rows); assert.deepEqual(r.opened, ['Address 1000']);
  assert.equal(r.calls.filter(q => !q.single).length, 3);
});
test('show saved areas lists choices without navigating', async () => {
  const r = await run('show saved areas', [home]); assert.equal(r.opened.length, 0); assert.match(r.message, /Choose a Saved Area/);
});
test('network errors do not navigate', async () => {
  const r = await run('Home', [home], {queryError:true}); assert.equal(r.opened.length, 0); assert.match(r.message, /Could not load/);
});
test('account changes and deleted places do not navigate', async () => {
  assert.equal((await run('Home', [home], {switchAccount:true})).opened.length, 0);
  assert.equal((await run('Home', [home], {deleted:true})).opened.length, 0);
});
test('script refuses other web origins', async () => {
  const r = await run('Home', [home], {origin:'https://example.com'}); assert.equal(r.status, 'untrusted'); assert.equal(r.calls.length, 0);
});

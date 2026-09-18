const assert = require('node:assert/strict');
const { readFile } = require('node:fs/promises');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const names = [
  'updateLastCandlestick', 'addCandlesticksAtTheEnd', 'addCandlesticksAtTheStart',
  'addOrderLine', 'removeOrderLine', 'updateOrderLine', 'getOrderLines',
  'addBuySellMark', 'removeBuySellMark', 'updateBuySellMark', 'getBuySellMarks',
];

async function mount(platform) {
  const context = vm.createContext({});
  const ref = { current: null };
  const calls = [];
  const host = { tag: 42 };
  const ids = Object.fromEntries(names.map((name, i) => [name, i + 1]));
  const react = {
    forwardRef: component => component,
    useRef: value => ({ current: value }),
    useImperativeHandle: (target, create) => { target.current = create(); },
    createElement: (type, props) => ({ type, props }),
  };
  const modules = {
    react: { default: react, ...react },
    'react-native': {
      requireNativeComponent: name => name,
      Platform: { OS: platform },
      findNodeHandle: view => { calls.push(['lookup', view]); return view?.tag ?? null; },
      UIManager: {
        getViewManagerConfig: () => ({ Commands: ids }),
        dispatchViewManagerCommand: (...args) => { calls.push(['legacy', ...args]); },
      },
    },
    'react-native/Libraries/Utilities/codegenNativeCommands': {
      default: ({ supportedCommands }) => Object.fromEntries(supportedCommands.map(name => [
        name, (view, ...args) => { calls.push(['host', view, name, args]); },
      ])),
    },
  };
  const source = await readFile(path.join(__dirname, '../index.js'), 'utf8');
  const module = new vm.SourceTextModule(source, { context });
  await module.link(name => {
    const values = modules[name];
    return new vm.SyntheticModule(Object.keys(values), function () {
      for (const [key, value] of Object.entries(values)) this.setExport(key, value);
    }, { context });
  });
  await module.evaluate();
  const element = module.namespace.default({ testID: 'chart' }, ref);
  element.props.ref.current = host;
  return { api: ref.current, nativeRef: element.props.ref, host, calls, ids, element };
}

for (const platform of ['android', 'ios']) {
  for (const name of names) {
    test(`${platform}: ${name} preserves payload and uses the correct dispatch path`, async () => {
      const { api, host, calls, ids } = await mount(platform);
      const payload = { time: 123, close: 42 };
      const args = name.startsWith('get') ? [] : [payload];
      api[name](...args);
      if (platform === 'android') {
        assert.equal(calls.length, 1);
        assert.equal(calls[0][0], 'host');
        assert.equal(calls[0][1], host);
        assert.equal(calls[0][2], name);
      } else {
        assert.equal(calls.length, 2);
        assert.equal(calls[0][0], 'lookup');
        assert.equal(calls[1][0], 'legacy');
        assert.equal(calls[1][1], host.tag);
        assert.equal(calls[1][2], ids[name]);
      }
      const sent = calls.at(-1)[3];
      assert.equal(sent.length, args.length);
      if (args.length) assert.equal(sent[0], payload);
    });
  }

  test(`${platform}: retained API cannot dispatch after native ref detaches`, async () => {
    const { api, nativeRef, calls } = await mount(platform);
    nativeRef.current = null;
    for (const name of names) {
      const result = api[name]({ time: 123 });
      if (name.startsWith('get')) assert.equal(result.length, 0);
    }
    assert.equal(calls.length, 0);
  });

  test(`${platform}: retained API reads the current view after replacement`, async () => {
    const { api, nativeRef, calls } = await mount(platform);
    const replacement = { tag: 84 };
    nativeRef.current = replacement;
    api.updateLastCandlestick({ close: 12 });
    assert.equal(calls[0][1], replacement);
  });
}

test('independent charts dispatch to their own native refs', async () => {
  const first = await mount('android');
  const second = await mount('android');
  first.nativeRef.current = null;
  first.api.addCandlesticksAtTheEnd([]);
  second.api.addCandlesticksAtTheEnd([]);
  assert.equal(first.calls.length, 0);
  assert.equal(second.calls[0][1], second.host);
});

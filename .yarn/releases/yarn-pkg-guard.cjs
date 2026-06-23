#!/usr/bin/env node
/**
 * pkg-guard pre-install wrapper around the bundled Yarn 4 release.
 *
 * Yarn 4 does not run the root `preinstall` script, so the old
 * `preinstall: pkg-guard` hook stopped firing. This file is what `yarnPath`
 * (and the classic `.yarnrc` `yarn-path`) point to, so EVERY `yarn`
 * invocation — local or CI, corepack or Yarn 1 — goes through it.
 *
 * For dependency-mutating commands (install/add/up/dedupe, or bare `yarn`)
 * it runs @elliottech/pkg-guard FIRST and aborts before Yarn touches the
 * project if pkg-guard finds malware/vulns. Otherwise it transparently
 * delegates to the real Yarn release.
 *
 * Bypass with SKIP_PKG_GUARD=1 (pkg-guard also honors the per-check
 * SKIP_PKG_*_CHECK vars).
 */
const { spawnSync } = require(`child_process`);
const path = require(`path`);

const REAL_YARN = path.join(__dirname, `yarn-4.15.0.cjs`);
const args = process.argv.slice(2);
const mutatesDeps = args.length === 0 || [`install`, `add`, `up`, `dedupe`].includes(args[0]);

if (mutatesDeps && !process.env.SKIP_PKG_GUARD) {
  const result = spawnSync(`npx`, [`--yes`, `@elliottech/pkg-guard@latest`], {
    stdio: `inherit`,
    shell: process.platform === `win32`,
    // Yarn 4 doesn't set npm_config_argv (which pkg-guard reads to detect
    // `yarn add <pkg>`). Synthesize it from our argv so explicitly-added
    // packages are checked, matching Yarn 1's behavior.
    env: { ...process.env, npm_config_argv: process.env.npm_config_argv || JSON.stringify({ original: args }) },
  });
  if (result.error) {
    // pkg-guard couldn't even be launched (e.g. npx missing). Fail closed —
    // don't install unchecked. Use SKIP_PKG_GUARD=1 to bypass deliberately.
    console.error(`[pkg-guard] failed to run: ${result.error.message} — blocking install (set SKIP_PKG_GUARD=1 to bypass)`);
    process.exit(1);
  } else if (result.status !== 0) {
    // Malware/vuln found (or a block) — abort BEFORE Yarn installs anything.
    process.exit(result.status);
  }
}

// Hand off to the real Yarn release without re-triggering this wrapper.
process.env.YARN_IGNORE_PATH = `1`;
require(REAL_YARN);

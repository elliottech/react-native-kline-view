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
  });
  if (result.error) {
    // pkg-guard couldn't be launched (e.g. npx missing) — warn, don't block.
    console.warn(`[pkg-guard] skipped: ${result.error.message}`);
  } else if (result.status !== 0) {
    // Malware/vuln found (or a block) — abort BEFORE Yarn installs anything.
    process.exit(result.status);
  }
}

// Hand off to the real Yarn release without re-triggering this wrapper.
process.env.YARN_IGNORE_PATH = `1`;
require(REAL_YARN);

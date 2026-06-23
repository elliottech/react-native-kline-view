/**
 * Runs @elliottech/pkg-guard after every install.
 *
 * Yarn 4 does not execute the root project's npm-style `preinstall` script, so
 * the pkg-guard supply-chain checks (malware + vulnerability scans; the age
 * check is also enforced natively via `npmMinimalAgeGate` in .yarnrc.yml) would
 * otherwise stop running. This plugin restores the old `preinstall` behavior by
 * invoking pkg-guard from the `afterAllInstalled` hook, locally and in CI.
 *
 * Bypass with SKIP_PKG_GUARD=1 (and the per-check SKIP_PKG_*_CHECK vars that
 * pkg-guard itself honors).
 */
const { spawnSync } = require(`child_process`);

module.exports = {
  name: `plugin-pkg-guard`,
  factory: () => ({
    hooks: {
      afterAllInstalled() {
        if (process.env.SKIP_PKG_GUARD) return;

        const result = spawnSync(`npx`, [`--yes`, `@elliottech/pkg-guard@latest`], {
          stdio: `inherit`,
          shell: process.platform === `win32`,
        });

        if (result.error) {
          // pkg-guard couldn't be launched at all (e.g. npx missing) — warn, don't block.
          console.warn(`[pkg-guard] skipped: ${result.error.message}`);
          return;
        }
        if (result.status !== 0) {
          throw new Error(`pkg-guard blocked the install (exit code ${result.status})`);
        }
      },
    },
  }),
};

// Room on wasmJs talks to SQLite through a web worker (core/database/sqlite-worker/worker.js).
// The worker is referenced as "tuavaga-sqlite-worker/worker.js" from Kotlin; this alias points
// webpack straight at the source file, and the extra module path lets the worker resolve
// @sqlite.org/sqlite-wasm from the Kotlin-managed node_modules.
const path = require("path");
const rootDir = path.resolve(__dirname, "../../../..");

config.resolve = config.resolve || {};
config.resolve.alias = Object.assign({}, config.resolve.alias, {
    "tuavaga-sqlite-worker": path.resolve(rootDir, "core/database/sqlite-worker"),
});
config.resolve.modules = (config.resolve.modules || ["node_modules"]).concat([
    path.resolve(__dirname, "../../node_modules"),
]);

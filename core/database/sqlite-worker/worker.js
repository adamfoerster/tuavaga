// Web worker backing androidx.sqlite's WebWorkerSQLiteDriver (used by Room on wasmJs).
//
// Protocol (see androidx.sqlite.driver.web.WebWorkerSQLiteDriver KDoc):
//   request  { id, data: { cmd: "open" | "prepare" | "step" | "close", ... } }
//   response { id, data } or { id, error }
//
// Storage is OPFS through the "opfs-sahpool" VFS, which works without COOP/COEP
// headers. If OPFS is unavailable (e.g. private browsing), it falls back to an
// in-memory database so the app still runs, just without persistence.
import sqlite3InitModule from "@sqlite.org/sqlite-wasm";

const databases = new Map();
const statements = new Map();
let nextDatabaseId = 1;
let nextStatementId = 1;

const ready = (async () => {
    const sqlite3 = await sqlite3InitModule();
    let pool = null;
    try {
        pool = await sqlite3.installOpfsSAHPoolVfs({ name: "tuavaga-sahpool" });
    } catch (e) {
        console.warn("[sqlite-worker] OPFS indisponível, usando banco em memória:", e);
    }
    return { sqlite3, pool };
})();

function open({ sqlite3, pool }, fileName) {
    const db = pool
        ? new pool.OpfsSAHPoolDb("/" + fileName)
        : new sqlite3.oo1.DB(":memory:", "c");
    const databaseId = nextDatabaseId++;
    databases.set(databaseId, db);
    return { databaseId };
}

function prepare({ sqlite3 }, databaseId, sql) {
    const db = databases.get(databaseId);
    if (!db) throw new Error(`Database ${databaseId} is not open`);
    const stmt = db.prepare(sql);
    const statementId = nextStatementId++;
    statements.set(statementId, { stmt, databaseId });
    // Not stmt.getColumnNames(): it throws for statements without result columns (INSERT, DELETE...).
    const columnNames = Array.from({ length: stmt.columnCount }, (_, i) =>
        sqlite3.capi.sqlite3_column_name(stmt.pointer, i),
    );
    return {
        statementId,
        parameterCount: stmt.parameterCount,
        columnNames,
    };
}

function toTransferable(value) {
    return typeof value === "bigint" ? Number(value) : value;
}

function step({ sqlite3 }, statementId, bindings) {
    const entry = statements.get(statementId);
    if (!entry) throw new Error(`Statement ${statementId} is not prepared`);
    const { stmt } = entry;
    stmt.reset(true);
    if (bindings && bindings.length > 0) {
        // Sparse JS arrays leave holes for unbound params; SQLite treats those as NULL.
        stmt.bind(Array.from(bindings, (v) => (v === undefined ? null : v)));
    }
    const rows = [];
    let columnTypes = [];
    const columnCount = stmt.columnCount;
    while (stmt.step()) {
        if (rows.length === 0) {
            columnTypes = Array.from({ length: columnCount }, (_, i) =>
                sqlite3.capi.sqlite3_column_type(stmt.pointer, i),
            );
        }
        const row = new Array(columnCount);
        for (let i = 0; i < columnCount; i++) row[i] = toTransferable(stmt.get(i));
        rows.push(row);
    }
    stmt.reset();
    return { rows, columnTypes };
}

function close({ statementId, databaseId }) {
    if (statementId != null) {
        statements.get(statementId)?.stmt.finalize();
        statements.delete(statementId);
    }
    if (databaseId != null) {
        for (const [id, entry] of statements) {
            if (entry.databaseId === databaseId) {
                entry.stmt.finalize();
                statements.delete(id);
            }
        }
        databases.get(databaseId)?.close();
        databases.delete(databaseId);
    }
}

self.onmessage = async (event) => {
    const { id, data } = event.data;
    try {
        const env = await ready;
        let result;
        switch (data.cmd) {
            case "open":
                result = open(env, data.fileName);
                break;
            case "prepare":
                result = prepare(env, data.databaseId, data.sql);
                break;
            case "step":
                result = step(env, data.statementId, data.bindings);
                break;
            case "close":
                close(data);
                return; // one-way command, no response
            default:
                throw new Error(`Unknown command: ${data.cmd}`);
        }
        self.postMessage({ id, data: result });
    } catch (e) {
        self.postMessage({ id, error: e?.message ?? String(e) });
    }
};

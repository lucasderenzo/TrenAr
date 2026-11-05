import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import m from "../../docs/js/versiones.js";

const VERSIONES = m.VERSIONES;

test("32 entradas y 9 hitos", () => {
  assert.equal(VERSIONES.length, 32);
  assert.equal(VERSIONES.filter((v) => v.hito).length, 9);
});

test("versiones únicas", () => {
  const versiones = VERSIONES.map((v) => v.version);
  assert.equal(new Set(versiones).size, 32);
});

test("fechas ISO en orden no decreciente", () => {
  let anterior = "";
  for (const { fecha } of VERSIONES) {
    assert.match(fecha, /^\d{4}-\d{2}-\d{2}$/);
    // Fecha válida: el round-trip UTC debe devolver la misma cadena (descarta 2026-02-30, etc.).
    assert.equal(new Date(fecha + "T00:00:00Z").toISOString().slice(0, 10), fecha);
    assert.ok(fecha >= anterior, `${fecha} es anterior a ${anterior}`);
    anterior = fecha;
  }
});

test("textos en los tres idiomas", () => {
  for (const { texto } of VERSIONES) {
    for (const idioma of ["es", "en", "pt"]) {
      assert.equal(typeof texto[idioma], "string");
      assert.ok(texto[idioma].trim().length > 0, `falta texto en ${idioma}`);
    }
  }
});

test("la última coincide con la app", () => {
  const gradle = fs.readFileSync(new URL("../../app/build.gradle.kts", import.meta.url), "utf8");
  const app = gradle.match(/versionName = "([^"]+)"/)[1];
  assert.equal(VERSIONES.at(-1).version, app);
});

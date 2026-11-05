import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import t from "../../docs/js/textos.js";
import v from "../../docs/js/versiones.js";

const { TEXTOS } = t;
const { VERSIONES } = v;
const html = fs.readFileSync(new URL("../../docs/index.html", import.meta.url), "utf8");
const LINK = "https://github.com/lucasderenzo/TrenAr/releases/latest/download/TrenAr.apk";
// Claves que solo pone JavaScript, así que no aparecen en el HTML.
const SOLO_JS = ["hist.ver_menos"];

test("mismas claves en los tres idiomas", () => {
  const es = Object.keys(TEXTOS.es).sort();
  assert.deepEqual(Object.keys(TEXTOS.en).sort(), es);
  assert.deepEqual(Object.keys(TEXTOS.pt).sort(), es);
});

test("sin textos vacíos", () => {
  for (const idioma of ["es", "en", "pt"]) {
    for (const [clave, valor] of Object.entries(TEXTOS[idioma])) {
      assert.ok(String(valor).trim().length > 0, `${idioma}.${clave} está vacío`);
    }
  }
});

test("el HTML solo usa claves existentes y usa todas las de TEXTOS.es", () => {
  const usadas = new Set([...html.matchAll(/data-i18n(?:-[a-z]+)?="([^"]+)"/g)].map((m) => m[1]));
  for (const clave of usadas) assert.ok(clave in TEXTOS.es, `clave inexistente en el HTML: ${clave}`);
  for (const clave of Object.keys(TEXTOS.es)) {
    if (SOLO_JS.includes(clave)) continue;
    assert.ok(usadas.has(clave), `clave sin usar en el HTML: ${clave}`);
  }
});

test("respaldo sin JavaScript: los 9 hitos están escritos en el HTML", () => {
  const bloque = html.match(/<ol id="linea-tiempo"[^>]*>([\s\S]*?)<\/ol>/);
  assert.ok(bloque, 'falta <ol id="linea-tiempo">');
  assert.equal((bloque[1].match(/<li/g) || []).length, 9);
  const versiones = [...bloque[1].matchAll(/<span class="v">([^<]+)<\/span>/g)].map((m) => m[1]);
  assert.deepEqual(versiones, VERSIONES.filter((x) => x.hito).map((x) => x.version));
});

test("descarga apunta al Release (portada y cierre)", () => {
  assert.ok(html.split(LINK).length - 1 >= 2);
});

test("ganchos para la Tarea 5", () => {
  assert.match(html, /<html lang="es">/);
  for (const i of ["es", "en", "pt"]) assert.match(html, new RegExp(`<button[^>]*class="idioma(?: on)?"[^>]*data-idioma="${i}"`));
  assert.match(html, /id="version-descarga"/);
  assert.match(html, /<button[^>]*id="ver-todas"/);
  const orden = ["textos.js", "versiones.js", "idioma.js", "linea-tiempo.js"].map((s) => html.indexOf(`js/${s}`));
  assert.ok(orden.every((n) => n > 0));
  assert.deepEqual([...orden].sort((a, b) => a - b), orden);
});

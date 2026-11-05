import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import t from "../../docs/js/textos.js";
import v from "../../docs/js/versiones.js";
import lt from "../../docs/js/linea-tiempo.js";

const { TEXTOS } = t;
const { VERSIONES } = v;
const { completar, ultimaVersion, versionCorta, semanasEntre } = lt;
const css = fs.readFileSync(new URL("../../docs/css/estilos.css", import.meta.url), "utf8");
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
  const usadas = new Set([...html.matchAll(/data-i18n(?:-[a-z-]+)?="([^"]+)"/g)].map((m) => m[1]));
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
  assert.match(html, /<html lang="es" class="sin-js">/);
  for (const i of ["es", "en", "pt"]) assert.match(html, new RegExp(`<button[^>]*class="idioma(?: on)?"[^>]*data-idioma="${i}"`));
  assert.match(html, /id="version-descarga"/);
  assert.match(html, /<button[^>]*id="ver-todas"/);
  const orden = ["textos.js", "versiones.js", "idioma.js", "linea-tiempo.js"].map((s) => html.indexOf(`js/${s}`));
  assert.ok(orden.every((n) => n > 0));
  assert.deepEqual([...orden].sort((a, b) => a - b), orden);
});

test("docs/ solo contiene la web", () => {
  const permitidos = new Set(["index.html", ".nojekyll", "css", "js", "img"]);
  const raiz = new URL("../../docs/", import.meta.url);
  for (const nombre of fs.readdirSync(raiz)) assert.ok(permitidos.has(nombre), `no debería publicarse: docs/${nombre}`);
  const recorrer = (dir) => fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? recorrer(new URL(`${e.name}/`, dir)) : [e.name]);
  const md = recorrer(raiz).filter((n) => n.endsWith(".md"));
  assert.deepEqual(md, [], "hay archivos .md dentro de docs/");
});

test("sin JavaScript no se ven los controles que no hacen nada", () => {
  assert.match(html, /<html lang="es" class="sin-js">/);
  assert.match(css, /\.sin-js \.lang,\s*\.sin-js #ver-todas\s*\{\s*display:\s*none/);
});

test("el respaldo estático de la versión no queda viejo", () => {
  const ultima = ultimaVersion(VERSIONES);
  const version = html.match(/data-i18n="hero\.version">([^<]*)</);
  assert.equal(version[1], completar(TEXTOS.es["hero.version"], { version: ultima }));
  const titulo = html.match(/data-i18n="hist\.titulo">([^<]*)</);
  assert.equal(titulo[1], completar(TEXTOS.es["hist.titulo"], { version: versionCorta(ultima), semanas: semanasEntre(VERSIONES) }));
});

test("todos los textos ES escritos en el HTML coinciden con TEXTOS.es", () => {
  const ultima = ultimaVersion(VERSIONES);
  const valores = { version: ultima, semanas: semanasEntre(VERSIONES) };
  const esperado = (clave) => completar(TEXTOS.es[clave], clave === "hist.titulo" ? { ...valores, version: versionCorta(ultima) } : valores);
  const decodificar = (x) => x.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&amp;/g, "&");
  let revisados = 0;
  for (const m of html.matchAll(/<(\w+)\s[^>]*?data-i18n(-html)?="([^"]+)"[^>]*>([\s\S]*?)<\/\1>/g)) {
    const [, , esHtml, clave, interior] = m;
    assert.equal(esHtml ? interior : decodificar(interior), esperado(clave), `HTML desactualizado para ${clave}`);
    revisados++;
  }
  for (const m of html.matchAll(/<[^>]*?data-i18n-(content|aria-label)="([^"]+)"[^>]*>/g)) {
    const valor = m[0].match(new RegExp(`\\s${m[1]}="([^"]*)"`));
    assert.ok(valor, `falta el atributo ${m[1]} para ${m[2]}`);
    assert.equal(decodificar(valor[1]), esperado(m[2]), `atributo desactualizado para ${m[2]}`);
    revisados++;
  }
  assert.ok(revisados > 40);
});

test("accesibilidad: selector de idioma agrupado y línea de tiempo como lista", () => {
  assert.match(html, /<div class="lang" role="group" data-i18n-aria-label="nav\.idioma" aria-label="Idioma">/);
  assert.match(html, /<ol id="linea-tiempo" class="tl" role="list">/);
  assert.equal(TEXTOS.es["nav.idioma"], "Idioma");
  assert.equal(TEXTOS.en["nav.idioma"], "Language");
  assert.equal(TEXTOS.pt["nav.idioma"], "Idioma");
});

test("el botón ver todas arranca cerrado y las flechas las dibuja el CSS", () => {
  assert.match(html, /<button[^>]*id="ver-todas"[^>]*aria-expanded="false"/);
  for (const idioma of ["es", "en", "pt"]) {
    assert.doesNotMatch(TEXTOS[idioma]["hist.ver_todas"], /[▾▴]/);
    assert.doesNotMatch(TEXTOS[idioma]["hist.ver_menos"], /[▾▴]/);
  }
  assert.match(css, /#ver-todas::after\s*\{\s*content:\s*" ▾"/);
  assert.match(css, /#ver-todas\[aria-expanded="true"\]::after\s*\{\s*content:\s*" ▴"/);
});

test("textos corregidos en la revisión final", () => {
  assert.ok(TEXTOS.en["pie.aviso"].includes("on the platform"));
  assert.equal(TEXTOS.en["hist.ver_menos"], "Show less");
  assert.equal(TEXTOS.es["inst.p3.texto"], "“Permitir desde esta fuente”.");
  assert.equal(TEXTOS.en["inst.p3.texto"], "“Allow from this source”.");
  assert.equal(TEXTOS.pt["inst.p3.texto"], "“Permitir desta fonte”.");
  assert.equal(TEXTOS.pt["inst.titulo"], "Pronto em um minuto.");
  assert.equal(TEXTOS.en["func.salir.texto"], "Tells you when to leave based on your walking time.");
  assert.equal(TEXTOS.en["inst.p1.texto"], "Tap the button on your Android phone.");
  assert.equal(TEXTOS.pt["inst.p1.texto"], "Toque no botão no seu Android.");
});

test("css: scroll suave solo con movimiento permitido, .meta conserva su estilo y foco visible", () => {
  assert.doesNotMatch(css, /^html\s*\{\s*scroll-behavior:\s*smooth/m);
  assert.match(css, /@media \(prefers-reduced-motion:\s*no-preference\)\s*\{\s*html\s*\{\s*scroll-behavior:\s*smooth/);
  assert.doesNotMatch(css, /^\s*\.hero p\s*[,{]/m);
  assert.match(css, /\.hero \.bajada/);
  assert.match(css, /\.idioma:focus-visible\s*\{\s*outline:\s*2px solid var\(--amber\)/);
});

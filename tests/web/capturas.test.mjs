import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import t from "../../docs/js/textos.js";

const { TEXTOS } = t;
const html = fs.readFileSync(new URL("../../docs/index.html", import.meta.url), "utf8");
const CAPTURAS = { "estacion.png": "img.estacion", "cuenta.png": "img.cuenta", "mapa.png": "img.mapa" };

test("las capturas existen y están en la página con alt traducible", () => {
  for (const [archivo, clave] of Object.entries(CAPTURAS)) {
    assert.ok(fs.existsSync(new URL(`../../docs/img/${archivo}`, import.meta.url)), `falta docs/img/${archivo}`);
    const img = html.match(new RegExp(`<img[^>]*src="img/${archivo.replace(".", "\\.")}"[^>]*>`));
    assert.ok(img, `index.html no muestra img/${archivo}`);
    assert.match(img[0], new RegExp(`data-i18n-alt="${clave.replace(".", "\\.")}"`));
    assert.match(img[0], new RegExp(`alt="${TEXTOS.es[clave]}"`));
    assert.match(img[0], /width="1080" height="2400"/);
  }
});

test("solo queda el lugar reservado de ¡Salí ya!", () => {
  assert.equal((html.match(/class="shot-ph"/g) || []).length, 1);
});

test("metadatos para compartir el link (Open Graph)", () => {
  assert.match(html, /<meta property="og:title" content="[^"]+">/);
  assert.match(html, /<meta property="og:description" content="[^"]+">/);
  assert.match(html, /<meta property="og:image" content="https:\/\/lucasderenzo\.github\.io\/TrenAr\/img\/estacion\.png">/);
});

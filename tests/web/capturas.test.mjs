import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import t from "../../docs/js/textos.js";

const { TEXTOS } = t;
const html = fs.readFileSync(new URL("../../docs/index.html", import.meta.url), "utf8");
// archivo → [clave del alt, ancho, alto]. La de «¡Salí ya!» es del Redmi (720×1600, misma proporción 9:20).
const CAPTURAS = {
  "estacion.png": ["img.estacion", 1080, 2400],
  "cuenta.png": ["img.cuenta", 1080, 2400],
  "mapa.png": ["img.mapa", 1080, 2400],
  "sali-ya.png": ["img.salir", 720, 1600],
};

test("las capturas existen y están en la página con alt traducible", () => {
  for (const [archivo, [clave, ancho, alto]] of Object.entries(CAPTURAS)) {
    assert.ok(fs.existsSync(new URL(`../../docs/img/${archivo}`, import.meta.url)), `falta docs/img/${archivo}`);
    const img = html.match(new RegExp(`<img[^>]*src="img/${archivo.replace(".", "\\.")}"[^>]*>`));
    assert.ok(img, `index.html no muestra img/${archivo}`);
    assert.match(img[0], new RegExp(`data-i18n-alt="${clave.replace(".", "\\.")}"`));
    assert.match(img[0], new RegExp(`alt="${TEXTOS.es[clave]}"`));
    assert.match(img[0], new RegExp(`width="${ancho}" height="${alto}"`));
  }
});

test("no queda ningún lugar reservado", () => {
  assert.equal((html.match(/class="shot-ph"/g) || []).length, 0);
  assert.doesNotMatch(html, /Captura nueva/);
});

test("metadatos para compartir el link (Open Graph)", () => {
  assert.match(html, /<meta property="og:title" content="[^"]+">/);
  assert.match(html, /<meta property="og:description" content="[^"]+">/);
  assert.match(html, /<meta property="og:image" content="https:\/\/lucasderenzo\.github\.io\/TrenAr\/img\/estacion\.png">/);
});

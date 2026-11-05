import { test } from "node:test";
import assert from "node:assert/strict";
import idioma from "../../docs/js/idioma.js";
import lt from "../../docs/js/linea-tiempo.js";
import textos from "../../docs/js/textos.js";
import versiones from "../../docs/js/versiones.js";

// Los scripts del navegador comparten ámbito global: se lo damos también en Node.
Object.assign(globalThis, textos, versiones, lt);

// Elemento falso mínimo: lo justo para lo que usa el código de DOM.
function elemento(atributos = {}) {
  const clases = new Set((atributos.class || "").split(" ").filter(Boolean));
  const attrs = { ...atributos };
  return {
    textContent: "",
    innerHTML: "",
    get attributes() { return Object.entries(attrs).map(([name, value]) => ({ name, value })); },
    getAttribute: (n) => (n in attrs ? attrs[n] : null),
    setAttribute: (n, v) => { attrs[n] = String(v); },
    classList: {
      contains: (c) => clases.has(c),
      toggle: (c, fuerza) => { (fuerza ?? !clases.has(c)) ? clases.add(c) : clases.delete(c); },
    },
  };
}

function documentoFalso() {
  const version = elemento({ "data-i18n": "hero.version" });
  const meta = elemento({ "data-i18n-content": "meta.descripcion", content: "x" });
  const botones = ["es", "en", "pt"].map((l) => elemento({ class: "idioma", "data-idioma": l, "aria-pressed": "false" }));
  const lista = elemento({ id: "linea-tiempo" });
  const verTodas = elemento({ id: "ver-todas", "data-i18n": "hist.ver_todas" });
  const grupo = elemento({ "data-i18n-aria-label": "nav.idioma", "aria-label": "Idioma" });
  const todos = [version, meta, grupo, ...botones, lista, verTodas];
  const porSelector = {
    "*": todos,
    "[data-i18n]": [version, verTodas],
    "[data-i18n-html]": [],
    ".idioma": botones,
    "#linea-tiempo": [lista],
    "#ver-todas": [verTodas],
  };
  return {
    version, meta, grupo, botones, lista, verTodas,
    documentElement: elemento({ lang: "es" }),
    querySelectorAll: (s) => porSelector[s] || [],
    querySelector: (s) => (porSelector[s] || [])[0] || null,
  };
}

test("aplicarIdioma traduce textos, atributos y botones", () => {
  const doc = documentoFalso();
  idioma.aplicarIdioma(doc, "en");
  assert.equal(doc.version.textContent, "Version 1.22.0 · ~22 MB");
  assert.equal(doc.meta.getAttribute("content"), textos.TEXTOS.en["meta.descripcion"]);
  assert.deepEqual(doc.botones.map((b) => b.getAttribute("aria-pressed")), ["false", "true", "false"]);
  assert.equal(doc.documentElement.getAttribute("lang"), "en");
});

test("montarLineaTiempo arma las 32 entradas", () => {
  const doc = documentoFalso();
  lt.montarLineaTiempo(doc, "es");
  assert.equal((doc.lista.innerHTML.match(/<li/g) || []).length, 32);
});

test("aplicarIdioma traduce atributos con guión como aria-label", () => {
  const doc = documentoFalso();
  idioma.aplicarIdioma(doc, "en");
  assert.equal(doc.grupo.getAttribute("aria-label"), "Language");
});

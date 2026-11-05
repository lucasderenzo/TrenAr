import { test } from "node:test";
import assert from "node:assert/strict";
import m from "../../docs/js/idioma.js";

test("usa el guardado si es válido", () => assert.equal(m.elegirIdioma("en", ["pt-BR"]), "en"));
test("ignora guardado inválido", () => assert.equal(m.elegirIdioma("fr", ["pt-BR"]), "pt"));
test("pt-BR → pt", () => assert.equal(m.elegirIdioma(null, ["pt-BR"]), "pt"));
test("en-US → en", () => assert.equal(m.elegirIdioma(null, ["en-US"]), "en"));
test("fr → es", () => assert.equal(m.elegirIdioma(null, ["fr-FR"]), "es"));
test("lista vacía → es", () => assert.equal(m.elegirIdioma(null, []), "es"));
test("gana el primero compatible", () => assert.equal(m.elegirIdioma(null, ["fr-FR", "pt-BR", "en"]), "pt"));
test("acepta string suelto (navigator.language)", () => assert.equal(m.elegirIdioma(null, "en-GB"), "en"));
test("undefined → es", () => assert.equal(m.elegirIdioma(undefined, undefined), "es"));
test("códigos raros", () => {
  assert.equal(m.elegirIdioma(null, ["PT-br"]), "pt");
  assert.equal(m.elegirIdioma(null, ["en_GB"]), "en");
});
test("storage que tira error", () => {
  const roto = { getItem() { throw new Error("x"); }, setItem() { throw new Error("x"); } };
  assert.equal(m.leerIdiomaGuardado(roto), null);
  assert.doesNotThrow(() => m.guardarIdioma(roto, "en"));
});
test("guarda y lee con la clave trenar-idioma", () => {
  const datos = {};
  const s = { getItem: (k) => datos[k] ?? null, setItem: (k, v) => { datos[k] = v; } };
  m.guardarIdioma(s, "pt");
  assert.equal(datos["trenar-idioma"], "pt");
  assert.equal(m.leerIdiomaGuardado(s), "pt");
});

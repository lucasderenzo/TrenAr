import { test } from "node:test";
import assert from "node:assert/strict";
import lt from "../../docs/js/linea-tiempo.js";
import versiones from "../../docs/js/versiones.js";

const { VERSIONES } = versiones;
const { formatearFecha, versionCorta, ultimaVersion, semanasEntre, completar, htmlLineaTiempo } = lt;

test("formatearFecha en es, en y pt", () => {
  assert.equal(formatearFecha("2026-09-30", "es"), "30 sep");
  assert.equal(formatearFecha("2026-09-30", "en"), "Sep 30");
  assert.equal(formatearFecha("2026-09-30", "pt"), "30 set");
  assert.equal(formatearFecha("2026-11-03", "es"), "3 nov");
});

test("formatearFecha no corre el día por la zona horaria", () => {
  assert.equal(formatearFecha("2026-01-01", "es"), "1 ene");
  assert.equal(formatearFecha("2026-12-31", "en"), "Dec 31");
});

test("versionCorta deja los dos primeros números", () => {
  assert.equal(versionCorta("1.22.0"), "1.22");
  assert.equal(versionCorta("1.0"), "1.0");
});

test("ultimaVersion y semanasEntre", () => {
  assert.equal(ultimaVersion(VERSIONES), "1.22.0");
  assert.equal(semanasEntre(VERSIONES), 5);
});

test("completar reemplaza cada {clave}", () => {
  assert.equal(
    completar("De la idea a la {version} en {semanas} semanas.", { version: "1.22", semanas: 5 }),
    "De la idea a la 1.22 en 5 semanas.",
  );
});

test("htmlLineaTiempo: 32 entradas, 9 hitos y 23 ocultas", () => {
  const html = htmlLineaTiempo(VERSIONES, "es");
  assert.equal(html.split('<li class="stop').length - 1, 32);
  assert.equal(html.split('<li class="stop hito"').length - 1, 9);
  assert.equal(html.split('<li class="stop oculta"').length - 1, 23);
});

test("htmlLineaTiempo: el primer li tiene versión y fecha", () => {
  const html = htmlLineaTiempo(VERSIONES, "es");
  const primero = html.slice(0, html.indexOf("</li>"));
  assert.ok(primero.includes('<span class="v">1.0</span>'));
  assert.ok(primero.includes("30 sep"));
});

test("htmlLineaTiempo escapa & < > \" en el texto", () => {
  const html = htmlLineaTiempo(
    [{ version: "9.9", fecha: "2026-01-01", hito: true, texto: { es: "<b>&", en: "x", pt: "x" } }],
    "es",
  );
  assert.ok(html.includes("&lt;b&gt;&amp;"));
  assert.ok(!html.includes("<b>"));
});

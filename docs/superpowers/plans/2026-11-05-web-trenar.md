# Web de TrenAr — Plan de implementación

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendado) o superpowers:executing-plans para implementar este plan tarea por tarea. Los pasos usan casillas (`- [ ]`) para el seguimiento.

**Objetivo:** Web estática de portfolio de TrenAr en `docs/` (GitHub Pages), en ES/EN/PT, con descarga de la APK y línea de tiempo de versiones.

**Arquitectura:**
- Una sola página HTML con los textos en español escritos adentro.
- Scripts clásicos sin dependencias: textos por idioma, elección de idioma, datos de versiones y armado de la línea de tiempo. Cada script exporta sus funciones puras con `module.exports` para probarlas con `node:test`.
- La lógica que toca el DOM es mínima y se verifica a mano en el navegador.

**Stack:** HTML + CSS + JavaScript sin frameworks. Pruebas con `node --test` (Node 26). Tipografías Archivo e Inter de Google Fonts.

**Especificación:** `docs/superpowers/specs/2026-11-04-web-trenar-design.md` (leerla entera antes de empezar).

## Restricciones globales

- Todo texto visible, comentario y mensaje de commit va **en español**. Las traducciones EN/PT viven solo en `docs/js/textos.js` y `docs/js/versiones.js`.
- **Sin dependencias, sin compilación, sin `fetch`.** Cada `.js` de `docs/js/` es un script clásico: declara sus funciones y constantes a nivel de archivo y termina con `if (typeof module !== "undefined") module.exports = { … };`.
- Pruebas: `node --test "tests/web/*.test.mjs"`. Importan los scripts como CommonJS: `import idioma from "../../docs/js/idioma.js";`.
- Node en Git Bash: anteponer `export PATH="/c/Program Files/nodejs:$PATH";` si `node` no se encuentra.
- Idiomas válidos: `es`, `en`, `pt`. Clave de `localStorage`: `trenar-idioma`.
- Link de descarga, siempre este: `https://github.com/lucasderenzo/TrenAr/releases/latest/download/TrenAr.apk`.
- Colores:
  - Navy `#0A1B30`, Navy2 `#14345C`, azul `#1E5AA8`, celeste `#4A90D9`, cyan `#37C2FF`, amarillo `#FFC53D`.
  - Fondo `#F6F8FC`, tinte `#E6EEF8`, tinta `#11161D`, tinta2 `#44505E`, borde `#C9D4E3`.
- Colores de línea (franja):
  - Mitre `#0EA5E0`, Sarmiento `#D6007E`, Roca `#2E9E5B`, San Martín `#F57C00`.
  - Belgrano Sur `#F9A825`, Belgrano Norte `#8E24AA`, Urquiza `#00ACC1`, Tren de la Costa `#26A69A`.
- Referencia visual aprobada: `C:\Users\WDS\.claude\projects\AppTrenes\.superpowers\brainstorm\1497-1793846301\content\estructura-v3.html`. De ahí salen el **texto en español**, la estructura, el CSS y el SVG del tren. Sacar solo lo que es del boceto: etiquetas rosas `.num` y `.new`, el aviso `.top`, el marco `.page` y el fondo gris.
- Commits con emoji inicial, como el historial del repo, y al final la línea `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Foco de revisión

1. **`localStorage` que tira error** (Safari en modo privado): la página carga y el selector de idioma funciona igual, solo que no recuerda la elección. → Prueba en Tarea 1.
2. **Navegador sin `navigator.languages`** (solo `navigator.language`), o con códigos raros como `PT-br` o `en_GB`: se elige el idioma correcto. → Prueba en Tarea 1.
3. **JavaScript desactivado o con error:** la página se ve completa en español, *incluida* la línea de tiempo (los 9 hitos están escritos en el HTML como respaldo). → Prueba en Tarea 4.
4. **Pantalla de 320 px** (celulares chicos): sin desplazamiento horizontal, el botón de descarga entra entero y los trenes animados no ensanchan la página. → Verificación en Tarea 5.
5. **Versión nueva de la app sin actualizar la web:** si `build.gradle.kts` sube de versión y `versiones.js` no, una prueba falla. → Prueba en Tarea 2.

---

### Tarea 1: Base y elección de idioma

**Archivos:**
- Borrar: `docs/index.html` y `docs/img/` (la web vieja, sin commitear).
- Crear: `docs/.nojekyll` (vacío), `docs/js/idioma.js`, `tests/web/idioma.test.mjs`.

**Interfaces:**
- Produce, en `docs/js/idioma.js`:
  - `IDIOMAS = ["es","en","pt"]` y `CLAVE_GUARDADO = "trenar-idioma"`.
  - `elegirIdioma(guardado: string|null|undefined, preferidos: string[]|string|undefined) → "es"|"en"|"pt"`.
  - `leerIdiomaGuardado(storage) → string|null` (cualquier error → `null`).
  - `guardarIdioma(storage, idioma: string) → void` (traga cualquier error).

- [ ] **Paso 1: Escribir las pruebas que fallan** en `tests/web/idioma.test.mjs`:

```js
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
```

- [ ] **Paso 2: Correr y ver que fallan.**
  Correr: `node --test "tests/web/*.test.mjs"`.
  Esperado: FAIL (`Cannot find module …/docs/js/idioma.js`).

- [ ] **Paso 3: Implementar `docs/js/idioma.js`** con las funciones de *Interfaces*. Para normalizar cada preferido: pasarlo a minúsculas y quedarse con lo anterior al primer `-` o `_`.

- [ ] **Paso 4: Correr y ver que pasan.**
  Correr: `node --test "tests/web/*.test.mjs"`.
  Esperado: 12 pruebas OK.

- [ ] **Paso 5: Commit.**
  `git add -A docs/.nojekyll docs/js/idioma.js tests/web/idioma.test.mjs` y `git commit -m "🌐 Elección de idioma de la web (es/en/pt)"`.

---

### Tarea 2: Datos de versiones

**Archivos:**
- Crear: `docs/js/versiones.js`, `tests/web/versiones.test.mjs`.

**Interfaces:**
- Produce `VERSIONES: Array<{ version: string, fecha: "AAAA-MM-DD", hito: boolean, texto: { es, en, pt } }>`, ordenada de vieja a nueva.

**Contenido exacto** (versión, fecha, ★ = hito, texto ES). El EN y el PT los traduce quien implementa, con el mismo tono corto.

| Versión | Fecha | Hito | Texto ES |
|---|---|---|---|
| 1.0 | 2026-09-30 | ★ | Primera versión: arribos en vivo, estaciones cercanas y favoritas. |
| 1.1 | 2026-09-30 | ★ | Barra de cinco pestañas, mapa de la red en vivo y alertas. |
| 1.1.1 | 2026-10-01 | | Trenes en vivo diferenciados y acentos corregidos. |
| 1.2.0 | 2026-10-01 | ★ | Ecobici en vivo en el mapa. |
| 1.3.0 | 2026-10-01 | ★ | Seguir un tren con cuenta regresiva y “¡Salí ya!”. |
| 1.4.0 | 2026-10-02 | | Filtros y zoom en el mapa; Cercanas con tren y Ecobici. |
| 1.5.0 | 2026-10-02 | ★ | Llegan Urquiza y Belgrano Norte, y “mi ubicación” en el mapa. |
| 1.6.0 | 2026-10-02 | | Horarios programados y mapa de la estación. |
| 1.7.0 | 2026-10-13 | | Horarios exactos de Belgrano Norte desde el PDF oficial. |
| 1.8.0 | 2026-10-13 | ★ | Horarios exactos de Urquiza desde el PDF oficial. |
| 1.9.0 | 2026-10-14 | | “Cómo llego”, Ecobici favoritas y cercanía configurable. |
| 1.10.0 | 2026-10-14 | | La posición del tren en el mapa de cada estación. |
| 1.11.0 | 2026-10-14 | | Radio de búsqueda configurable y “¡Salí ya!” con horarios programados. |
| 1.11.1 | 2026-10-14 | | Corrección del radio guardado en Cercanas. |
| 1.12.0 | 2026-10-14 | | Distancia a la estación y “¡Salí ya!” más preciso. |
| 1.12.1 | 2026-10-14 | | “Sin radio” muestra todas las estaciones. |
| 1.13.0 | 2026-10-14 | ★ | En español, inglés y portugués. |
| 1.13.1 | 2026-10-15 | | Los feriados usan el horario de domingo. |
| 1.14.0 | 2026-10-16 | | Íconos de tren, cabeceras y posición estimada en el mapa. |
| 1.14.1 | 2026-10-19 | | El tren mira hacia donde va. |
| 1.15.0 | 2026-10-23 | | Botón de sugerencias y reportes. |
| 1.16.1 | 2026-10-27 | | Ubicación escrita a mano en Cercanas. |
| 1.16.2 | 2026-10-28 | | Arribos coherentes con los trenes del mapa. |
| 1.17.0 | 2026-10-28 | ★ | La línea entera, con estaciones y trenes, en el mapa de cada estación. |
| 1.17.1 | 2026-10-29 | | Destinos y ramales que ya no se cortan. |
| 1.18.0 | 2026-10-29 | | Estaciones terminales marcadas en el mapa. |
| 1.19.0 | 2026-10-29 | | Nombres de las estaciones en el mapa según el zoom. |
| 1.20.0 | 2026-10-29 | | El nombre de la línea, en su color, bajo cada estación. |
| 1.21.0 | 2026-10-30 | | Modo oscuro. |
| 1.21.1 | 2026-11-02 | | El mapa de la red mueve los trenes en vivo. |
| 1.21.2 | 2026-11-02 | | El minimapa de la estación también anima los trenes. |
| 1.22.0 | 2026-11-03 | ★ | La estación se actualiza cada 10 segundos, como el mapa en vivo. |

- [ ] **Paso 1: Escribir las pruebas que fallan** en `tests/web/versiones.test.mjs`:
  - `"32 entradas y 9 hitos"`: `VERSIONES.length === 32` y `filter(v => v.hito).length === 9`.
  - `"versiones únicas"`: `new Set(versiones).size === 32`.
  - `"fechas ISO en orden no decreciente"`: todas cumplen `/^\d{4}-\d{2}-\d{2}$/`, son fechas válidas y cada una es `>=` la anterior.
  - `"textos en los tres idiomas"`: `texto.es`, `texto.en` y `texto.pt` son strings con `trim().length > 0`.
  - `"la última coincide con la app"`: lee `app/build.gradle.kts` con `fs.readFileSync(new URL("../../app/build.gradle.kts", import.meta.url), "utf8")`, extrae `/versionName = "([^"]+)"/` y compara con `VERSIONES.at(-1).version` (hoy, `"1.22.0"`).

- [ ] **Paso 2: Correr y ver que fallan.** Esperado: FAIL (módulo inexistente).

- [ ] **Paso 3: Crear `docs/js/versiones.js`** con la tabla de arriba y `module.exports = { VERSIONES }`.

- [ ] **Paso 4: Correr y ver que pasan** (las de la Tarea 1 también).

- [ ] **Paso 5: Commit:** `🗓️ Datos de la línea de tiempo de versiones`.

---

### Tarea 3: Armado de la línea de tiempo (funciones puras)

**Archivos:**
- Crear: `docs/js/linea-tiempo.js`, `tests/web/linea-tiempo.test.mjs`.

**Interfaces:**
- Consume: la forma de `VERSIONES` (Tarea 2).
- Produce, en `docs/js/linea-tiempo.js`:
  - `formatearFecha(iso: string, idioma) → string`. Usa tablas de meses propias, **no `Intl`** (cambia entre motores): es `ene feb mar abr may jun jul ago sep oct nov dic`, en `Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec`, pt `jan fev mar abr mai jun jul ago set out nov dez`. Formato es/pt `"30 sep"`, en `"Sep 30"`. Sin ceros a la izquierda.
  - `versionCorta(version: string) → string`: los dos primeros números (`"1.22.0"` → `"1.22"`, `"1.0"` → `"1.0"`).
  - `ultimaVersion(versiones) → string`: la `version` de la última entrada.
  - `semanasEntre(versiones) → number`: `Math.round(días entre la primera y la última fecha / 7)`.
  - `completar(plantilla: string, valores: object) → string`: reemplaza cada `{clave}` por `valores[clave]`.
  - `htmlLineaTiempo(versiones, idioma) → string`: un `<li>` por versión, en orden, con la forma `<li class="stop[ hito][ oculta]"><span class="v">{version}</span><span class="d">{fecha}</span><p>{texto}</p></li>`. Las que no son hito llevan `oculta`. Escapa `& < > "` en el texto.

- [ ] **Paso 1: Escribir las pruebas que fallan** (importan también `VERSIONES`):
  - `formatearFecha("2026-09-30","es") === "30 sep"`, `("2026-09-30","en") === "Sep 30"`, `("2026-09-30","pt") === "30 set"` y `("2026-11-03","es") === "3 nov"`.
  - `versionCorta("1.22.0") === "1.22"` y `versionCorta("1.0") === "1.0"`.
  - `ultimaVersion(VERSIONES) === "1.22.0"` y `semanasEntre(VERSIONES) === 5`.
  - `completar("De la idea a la {version} en {semanas} semanas.", { version: "1.22", semanas: 5 }) === "De la idea a la 1.22 en 5 semanas."`.
  - Con `html = htmlLineaTiempo(VERSIONES, "es")`:
    - 32 `<li class="stop`, 9 con `hito` y 23 con `oculta`.
    - El primer `<li>` contiene `<span class="v">1.0</span>` y `30 sep`.
  - Escapado: `htmlLineaTiempo([{ version: "9.9", fecha: "2026-01-01", hito: true, texto: { es: "<b>&", en: "x", pt: "x" } }], "es")` contiene `&lt;b&gt;&amp;` y no contiene `<b>`.

- [ ] **Paso 2: Correr y ver que fallan.**

- [ ] **Paso 3: Implementar las funciones de *Interfaces*.**

- [ ] **Paso 4: Correr y ver que pasan.**

- [ ] **Paso 5: Commit:** `🚉 Armado de la línea de tiempo`.

---

### Tarea 4: Página, estilos y textos en tres idiomas

**Archivos:**
- Crear: `docs/index.html`, `docs/css/estilos.css`, `docs/js/textos.js`, `tests/web/textos.test.mjs`.

**Interfaces:**
- Produce `TEXTOS = { es: {…}, en: {…}, pt: {…} }` con **exactamente** estas claves (los valores ES salen del boceto v3; los placeholders `{version}`/`{semanas}` los completa la Tarea 5):
  - `meta.titulo`, `meta.descripcion`
  - `nav.funciones`, `nav.instalar`, `nav.historia`
  - `hero.titulo` *(HTML, con `<em>`)*, `hero.bajada`, `hero.descargar`, `hero.requisito`, `hero.version` = `"Versión {version} · ~22 MB"`
  - `func.kicker`, `func.titulo`, `func.cuenta.titulo`, `func.cuenta.texto`, `func.mapa.titulo`, `func.mapa.texto`, `func.salir.titulo`, `func.salir.texto`
  - `tarjeta.cercanas.titulo`, `tarjeta.cercanas.texto`, y lo mismo para `favoritas`, `alertas`, `widget`, `ecobici` y `oscuro`
  - `inst.kicker`, `inst.titulo`, `inst.p1.titulo`, `inst.p1.texto` … `inst.p4.titulo`, `inst.p4.texto`
  - `hist.kicker`, `hist.titulo` = `"De la idea a la {version} en {semanas} semanas."`, `hist.bajada`, `hist.ver_todas`, `hist.ver_menos`
  - `pie.aviso`, `pie.creditos`
- Produce en `index.html` los ganchos para la Tarea 5:
  - `<html lang="es">`.
  - Botones `<button class="idioma" data-idioma="es|en|pt">`.
  - Contenedores `<span id="version-descarga">` y `<ol id="linea-tiempo">`.
  - Botón `<button id="ver-todas">`.
  - Scripts al final del `<body>`, en este orden: `textos.js`, `versiones.js`, `idioma.js`, `linea-tiempo.js`.
  - Elementos traducibles con `data-i18n="clave"` (texto) o `data-i18n-html="clave"` (HTML).
  - El `<title>` y la `meta description` llevan `data-i18n` con las claves `meta.*`.

- [ ] **Paso 1: Escribir las pruebas que fallan** en `tests/web/textos.test.mjs`:
  - `"mismas claves en los tres idiomas"`: los `Object.keys` ordenados de `es`, `en` y `pt` son iguales.
  - `"sin textos vacíos"`: todo valor tiene `trim().length > 0`.
  - `"el HTML solo usa claves existentes"`: toda captura de `/data-i18n(?:-html)?="([^"]+)"/g` sobre `docs/index.html` existe en `TEXTOS.es`, y todas las claves de `TEXTOS.es` se usan al menos una vez en el HTML.
  - `"respaldo sin JavaScript"`: dentro de `<ol id="linea-tiempo">…</ol>` del HTML hay 9 `<li`, y sus `<span class="v">` son, en orden, las versiones hito de `VERSIONES`.
  - `"descarga apunta al Release"`: el HTML contiene el link de descarga de las *Restricciones globales* al menos dos veces (portada y cierre).

- [ ] **Paso 2: Correr y ver que fallan.**

- [ ] **Paso 3: Escribir `docs/js/textos.js`** con las claves de arriba (ES del boceto v3, más EN y PT).

- [ ] **Paso 4: Escribir `docs/index.html` y `docs/css/estilos.css`** a partir del boceto v3, sin lo que es solo del boceto (ver *Restricciones globales*). Detalles:
  - El header usa el ícono real de la app (el SVG de `app/src/main/res/drawable/ic_launcher_foreground.xml` sobre fondo `#14345C`) y un favicon SVG igual.
  - Las capturas son `<img src="img/estacion.png">`, `img/mapa.png` e `img/sali-ya.png`. Mientras no existan, el `<div class="shot-ph">` del boceto queda como lugar reservado: la Tarea 6 lo reemplaza.
  - En 'Cómo se hizo', el `<ol id="linea-tiempo">` trae los 9 hitos en español ya escritos, con el formato de `htmlLineaTiempo`, y el botón `#ver-todas`.
  - `.oculta { display: none; }` salvo dentro de `#linea-tiempo.abierta`.
  - Responsive y `prefers-reduced-motion` como dice la especificación (§4). Las pistas de los trenes llevan `overflow: hidden` para no ensanchar la página.

- [ ] **Paso 5: Correr las pruebas y ver que pasan** (todas las tareas).

- [ ] **Paso 6: Commit:** `🖼️ Página de TrenAr con textos en tres idiomas`.

---

### Tarea 5: Conectar idioma y línea de tiempo en el navegador

**Archivos:**
- Modificar: `docs/js/idioma.js` (agregar la parte de DOM), `docs/js/linea-tiempo.js` (agregar el montaje).

**Interfaces:**
- Consume: todo lo anterior.
- Produce:
  - `aplicarIdioma(doc, idioma)`:
    - Recorre `[data-i18n]` (`textContent`) y `[data-i18n-html]` (`innerHTML`) con `TEXTOS[idioma]`, después de completar los placeholders con `completar`:
      - En `hero.version`, `{version}` es la versión completa (`ultimaVersion`).
      - En `hist.titulo`, `{version}` es `versionCorta(ultimaVersion(VERSIONES))` y `{semanas}` es `semanasEntre(VERSIONES)`.
    - Actualiza `<html lang>` (`es-AR`, `en`, `pt-BR`), el `<title>` y la `meta description`.
    - Marca `aria-pressed="true"` en el botón de idioma activo.
    - Rearma `#linea-tiempo` con `htmlLineaTiempo(VERSIONES, idioma)`, manteniendo la clase `abierta` y el texto de `#ver-todas` según el estado.
  - Arranque (`DOMContentLoaded`, solo si existe `document`):
    - `idioma = elegirIdioma(leerIdiomaGuardado(localStorage), navigator.languages || navigator.language)` → `aplicarIdioma`.
    - Clic en `.idioma` → `guardarIdioma` + `aplicarIdioma`.
    - Clic en `#ver-todas` → alterna `abierta` en `#linea-tiempo` y el texto `hist.ver_todas` / `hist.ver_menos`, con `aria-expanded`.
  - Acceder a `localStorage` puede tirar error: usar `try { window.localStorage } catch { null }` y pasarlo a las funciones de la Tarea 1.

- [ ] **Paso 1: Implementar lo de *Interfaces*.** Las pruebas de `node` deben seguir pasando: el código de DOM no corre si no existe `document`.

- [ ] **Paso 2: Correr las pruebas.**
  Correr: `node --test "tests/web/*.test.mjs"`.
  Esperado: todo OK.

- [ ] **Paso 3: Verificar en el navegador.** Servir `docs/` (configuración `web` de `.claude/launch.json`, puerto 8765) y comprobar:
  - A 1280 px y a 375 px: ES → EN → PT cambian **todos** los textos, sin claves a la vista ni `{version}` sin completar.
  - Recargar mantiene el idioma elegido.
  - "Ver todas las versiones" muestra las 32 y se puede plegar.
  - El botón de descarga dice "Versión 1.22.0 · ~22 MB".
  - A **320 px**: `document.documentElement.scrollWidth === innerWidth`, y el botón entra entero.
  - Con `localStorage` roto (`Object.defineProperty(window, "localStorage", { get() { throw 1 } })` antes de cargar los scripts, o en una ventana privada): la página funciona.
  - Con JavaScript desactivado: se ve todo en español, con los 9 hitos.
  - Sin errores en la consola.

- [ ] **Paso 4: Commit:** `🔁 Cambio de idioma y "ver todas las versiones" en la web`.

---

### Tarea 6: Capturas nuevas de la v1.22 *(solo en horario de servicio, 6 a 22 h)*

**Archivos:**
- Crear: `docs/img/estacion.png`, `docs/img/mapa.png`, `docs/img/sali-ya.png`.
- Modificar: `docs/index.html` (cambiar cada `.shot-ph` por su `<img>`, con `alt` traducible: claves nuevas `img.estacion`, `img.mapa` e `img.salir` en `TEXTOS` y en el HTML).

- [ ] **Paso 1: Preparar el emulador** `trenar`:
  - Arrancar con `C:/Android/sdk/emulator/emulator.exe -avd trenar` (en segundo plano).
  - Instalar `app/build/outputs/apk/debug/app-debug.apk`, compilado de `main` con `./gradlew :app:assembleDebug` (versión 1.22.0).
  - Usar `C:/Android/sdk/platform-tools/adb.exe`. La pantalla es de 1080×2400.
- [ ] **Paso 2: Sacar las capturas** con `adb exec-out screencap -p > archivo.png`:
  - `estacion.png`: buscar "Once" (lupa de Cercanas) y esperar a ver arribos con cuenta regresiva.
  - `mapa.png`: pestaña Mapa, con trenes visibles.
  - `sali-ya.png`: el aviso "¡Salí ya!" en una estación cercana. El GPS del emulador no siempre responde a `adb emu geo fix`; si falla, usar "Ingresar dirección" en Cercanas con una dirección a unas 8 cuadras de la estación.
  - Mirar cada captura antes de usarla: tiene que haber trenes y no tiene que haber diálogos ni teclado.
- [ ] **Paso 3: Cambiar los lugares reservados** por las `<img>` y agregar las claves `img.*` en los tres idiomas.
- [ ] **Paso 4: Correr las pruebas** (la de claves del HTML cubre las nuevas) **y verificar en el navegador** que las tres imágenes cargan.
- [ ] **Paso 5: Commit:** `📸 Capturas nuevas de la v1.22 en la web`.

---

### Tarea 7: Publicar *(después del merge a `main`, y solo con el OK explícito de Lucas en el chat)*

- [ ] **Paso 1: Pedirle OK a Lucas** para dos cosas, que son públicas: crear el Release `v1.22.0` y activar GitHub Pages.
- [ ] **Paso 2: Crear el Release.**
  - Copiar la APK de `main` a `TrenAr.apk`.
  - Correr `gh release create v1.22.0 TrenAr.apk --repo lucasderenzo/TrenAr --title "TrenAr 1.22.0" --notes "<texto de la 1.22.0 en VERSIONES>"`.
- [ ] **Paso 3: Activar Pages:** `gh api -X POST repos/lucasderenzo/TrenAr/pages -f "source[branch]=main" -f "source[path]=/docs"`.
- [ ] **Paso 4: Verificar.**
  - `curl -sIL https://github.com/lucasderenzo/TrenAr/releases/latest/download/TrenAr.apk | grep -i "^HTTP"` termina en `200`.
  - `https://lucasderenzo.github.io/TrenAr/` carga la web, después de que el deploy de Pages termine (puede tardar unos minutos).

# Web de TrenAr — Especificación de diseño

- **Fecha:** 2026-11-04
- **Autor:** Lucas De Renzo (dirección) + Claude (diseño)
- **Estado:** pendiente de revisión de Lucas
- **Boceto aprobado:** `.superpowers/brainstorm/1497-1793846301/content/estructura-v3.html` (local, no versionado)

## 1. Objetivo

Una web de **portfolio** de TrenAr que cumple dos cosas, en este orden de importancia visual:

1. **Mostrar la app como producto** y permitir **descargar la APK** (sin formularios).
2. Mostrar **cómo se hizo**, mediante una **línea de tiempo de versiones**.

Quedan **fuera de alcance**: la historia humano + IA, los desafíos técnicos, el stack, los números y el link al código. Lo único que aparece de eso son los créditos en el footer.

## 2. Público y criterio de éxito

- **Público:** quien recibe el link para ver lo que hizo Lucas (portfolio). Puede entrar desde cualquier país, por eso hay tres idiomas.
- **Éxito:**
  - Al entrar se entiende en segundos qué es TrenAr.
  - La APK se descarga con un toque desde un Android.
  - La línea de tiempo muestra la evolución del proyecto.
  - Se ve bien en celular y en compu, en ES, EN y PT.

## 3. Publicación

- **GitHub Pages** del repo público `lucasderenzo/TrenAr`, rama `main`, carpeta `/docs`.
- URL: `https://lucasderenzo.github.io/TrenAr/`.
- Pages se activa **al final y con el OK explícito de Lucas** (deja la web pública).

## 4. Estructura de la página (aprobada en el boceto v3)

De arriba hacia abajo:

1. **Header.** Barra azul oscuro (`#0A1B30`) con borde inferior amarillo (`#FFC53D`, 4 px). Tiene:
   - A la izquierda, el logo (ícono de la app + "Tren**Ar**", con "Ar" en celeste `#4A90D9`).
   - A la derecha, links a *Funciones*, *Instalar* y *Cómo se hizo* (ocultos en celular) y el selector **ES · EN · PT**.
2. **Portada.** Degradado azul (`#14345C` → `#0F2440`). Tiene:
   - Título "¿Cuándo llega *tu tren?*" (la segunda parte en amarillo) y una frase corta.
   - Botón amarillo **Descargar APK** con "Android 8.0 o superior", y debajo "Versión X · ~22 MB".
   - Un celular con la captura de la estación con arribos.
   - Abajo, unas **vías con el tren animado** que cruza de izquierda a derecha (tren claro, ventanillas celestes, farol amarillo).
3. **Franja de colores de las líneas.** 8 px de alto, con los colores de `LineColors.kt`: Mitre `#0EA5E0`, Sarmiento `#D6007E`, Roca `#2E9E5B`, San Martín `#F57C00`, Belgrano Sur `#F9A825`, Belgrano Norte `#8E24AA`, Urquiza `#00ACC1`, Tren de la Costa `#26A69A`.
4. **Sección 01 · Funciones** (fondo blanco):
   - Tres bloques con captura: *Cuenta regresiva*, *El tren en el mapa* y *¡Salí ya!*.
   - Seis tarjetas: Cercanas (GPS o dirección), Favoritas, Alertas, Widget, Ecobici, Modo oscuro + idiomas.
5. **Vía separadora** con un tren azul oscuro que va hacia la derecha.
6. **Sección 02 · Instalación** (fondo celeste claro `#E6EEF8`): cuatro pasos (Descargá → Abrí el archivo → Permití la instalación → ¡A viajar!).
7. **Vía separadora** con un tren que va hacia la izquierda.
8. **Sección 03 · Cómo se hizo** (fondo azul `#14345C`, texto claro):
   - Título "De la idea a la 1.22 en 5 semanas" (versión y semanas salen de los datos).
   - Línea de tiempo vertical dibujada como vía, con cada versión como estación. Los hitos van con borde amarillo.
   - Botón **Ver todas las versiones** que despliega el resto.
9. **Footer.** Bloque azul oscuro con borde superior amarillo: logo, aviso "app no oficial… verificá siempre en el andén" y "Hecha por Lucas De Renzo + Claude · © 2026".

Cada sección tiene un encabezado con número (`01`, `02`, `03` en recuadro amarillo), un *kicker* en mayúsculas y una línea divisoria.

**Tipografías:** Archivo (títulos) e Inter (texto), de Google Fonts.

**Movimiento:** con `prefers-reduced-motion: reduce`, los trenes quedan quietos sobre las vías.

**Responsive:**
- Debajo de 760 px, la portada y las funciones pasan a una columna, y las tarjetas y los pasos a dos columnas.
- El botón de descarga ocupa todo el ancho.
- Nunca hay desplazamiento horizontal.

## 5. Archivos

Todo en `docs/`. Reemplaza la web anterior (`docs/index.html` y `docs/img/`, sin commitear, se borran).

Los documentos de diseño viven en `notas/superpowers/`, fuera de `docs/`, para que Pages no los publique.

| Archivo | Responsabilidad |
|---|---|
| `docs/index.html` | Estructura y textos en **español** escritos en el HTML. Cada texto traducible lleva `data-i18n="clave"`. |
| `docs/css/estilos.css` | Estilos, secciones, vías y animación del tren. |
| `docs/js/textos.js` | Objeto `TEXTOS = { es: {...}, en: {...}, pt: {...} }` con las mismas claves en los tres idiomas. |
| `docs/js/idioma.js` | `elegirIdioma(guardado, idiomasNavegador)` (función pura) + aplicar textos al DOM + guardar la elección. |
| `docs/js/versiones.js` | Lista `VERSIONES = [{ version, fecha, hito, texto: { es, en, pt } }]`, ordenada de vieja a nueva. |
| `docs/js/linea-tiempo.js` | Dibuja la línea de tiempo (hitos visibles y resto desplegable) y completa la versión del botón de descarga. |
| `docs/img/*.png` | Capturas nuevas de la v1.22 (iguales para los tres idiomas). |
| `docs/.nojekyll` | Que GitHub Pages sirva los archivos tal cual. |
| `tests/web/*.test.mjs` | Pruebas automáticas con `node:test` (fuera de `docs/` para no publicarlas). |

**Sin dependencias ni compilación.** Los scripts se cargan con `<script>` comunes (sin `fetch`), así la página también funciona abierta como archivo local. Los `.js` exportan a `window` en el navegador y vía `module.exports` en Node, para poder probarlos.

## 6. Idiomas

- **Por defecto:** el HTML ya está en español. Si falla JavaScript, la página se ve completa en español.
- **Primera visita:** `elegirIdioma(null, navigator.languages)` recorre los idiomas del navegador y devuelve el primero que empiece con `pt` → `pt`, o con `en` → `en`. Si no hay ninguno, devuelve `es`. Por ejemplo, `["fr-FR", "pt-BR"]` → `pt`.
- **Elección manual:** tocar ES / EN / PT aplica los textos, actualiza `<html lang>` y el `<title>`, y guarda en `localStorage` (clave `trenar-idioma`). La próxima visita usa lo guardado.
- **Guardado inválido o inaccesible:** `localStorage` puede tirar error (modo privado), y el valor guardado puede ser desconocido. En esos casos se ignora y se usa la detección por navegador. Todo acceso a `localStorage` va en `try/catch`.
- Los textos con formato (por ejemplo, el título con `<em>`) usan `data-i18n-html` y se insertan como HTML. Son textos propios, no del usuario.

## 7. Línea de tiempo

- **Datos:** 32 entradas, **una por commit de `main`**, desde "TrenAr v1.0" (2026-09-30) hasta la 1.22.0 (2026-11-03). Cada commit es una versión:
  - 21 commits con el número en el mensaje: 1.0, 1.1, 1.1.1, 1.2.0 … 1.15.0.
  - 11 PRs (#2 → #12), cada uno con su `versionName` (verificado en el historial de `app/build.gradle.kts`): 1.16.1, 1.16.2, 1.17.0, 1.17.1, 1.18.0, 1.19.0, 1.20.0, 1.21.0, 1.21.1, 1.21.2 y 1.22.0.
  - El texto de cada entrada es una frase corta y amigable basada en el mensaje del commit (sin emojis ni números de PR), en ES, EN y PT.
- **Hitos visibles (9):**
  - 1.0 (primera versión)
  - 1.1 (mapa de red y alertas)
  - 1.2.0 (Ecobici)
  - 1.3.0 ("¡Salí ya!")
  - 1.5.0 (Urquiza y Belgrano Norte)
  - 1.8.0 (horarios exactos de PDF oficiales)
  - 1.13.0 (tres idiomas)
  - 1.17.0 (la línea entera en el mapa de cada estación)
  - 1.22.0 (estación que se actualiza cada 10 s, como el mapa en vivo)

  El resto aparece con "Ver todas las versiones".
- **Fechas:** se muestran en formato corto, según el idioma: "30 sep" / "Sep 30" / "30 set". Se arman con tablas de meses propias, no con `Intl`, porque `Intl` da "sept" en algunos motores.
- **Título:** "De la idea a la {versión corta} en {semanas} semanas", con los dos valores calculados de los datos (hoy: "1.22" y 5).
- **Botón de descarga:** "Versión X" usa la `version` de la **última** entrada.

## 8. Descarga de la APK

- Link fijo: `https://github.com/lucasderenzo/TrenAr/releases/latest/download/TrenAr.apk`.
- Se crea el **Release `v1.22.0`** con la APK compilada de `main` (firma debug compartida con el VPS, así actualiza sobre la que Lucas ya tiene), subida con el nombre **`TrenAr.apk`**.
- Para cada versión futura, alcanza con un Release nuevo con el mismo nombre de archivo, más una entrada en `versiones.js`.
- El Release se crea **al final y con el OK explícito de Lucas** (es público).

## 9. Capturas

- Se sacan del emulador `trenar` con la v1.22.0, **en horario de servicio** (a partir de las 5–6 h). A la noche no hay trenes, y las pantallas quedarían en "Sin arribos".
- Se necesitan tres:
  - Estación con arribos y cuenta regresiva (Once/Sarmiento), que va en la portada y en "Cuenta regresiva".
  - Mapa de red en vivo con trenes.
  - "¡Salí ya!" (requiere ubicación cerca de la estación).
- Se guardan en `docs/img/` como PNG de 1080×2400.
- Hasta tenerlas, la página muestra lugares reservados. La tarea de capturas es independiente del resto.

## 10. Pruebas

**Automáticas** (`node --test tests/web/`):

1. `textos`: ES, EN y PT tienen exactamente el mismo conjunto de claves, y ningún valor está vacío.
2. `textos ↔ HTML`: cada `data-i18n` / `data-i18n-html` de `index.html` existe en `TEXTOS.es`.
3. `elegirIdioma`:
   - Guardado válido → se usa.
   - Guardado inválido → se ignora.
   - `pt-BR` → `pt`; `en-US` → `en`; `fr` → `es`; lista vacía → `es`.
   - El primer idioma compatible gana.
4. `versiones`:
   - Fechas ISO válidas en orden no decreciente y versiones sin repetir.
   - Textos en los tres idiomas.
   - 32 entradas y exactamente 9 hitos.
5. `versiones ↔ app`: la última `version` es igual al `versionName` de `app/build.gradle.kts`.

**Manuales** (antes de decir "listo"):

- Navegador a 1280 px y a 375 px, en los tres idiomas.
- Sin scroll horizontal, los trenes animados funcionando, y "Ver todas" desplegando y plegando.
- Ante el pedido de reducir movimiento, los trenes quietos.
- Después de publicar el Release, el link de descarga devuelve la APK (HTTP 200 después de la redirección).

## 11. Fuera de alcance (YAGNI)

- Formularios, analítica, cookies.
- Dominio propio.
- Generador de sitios, frameworks.
- Capturas por idioma.
- Mostrar la versión leyéndola de la API de GitHub.
- Modo oscuro de la web (la portada y las secciones oscuras ya dan contraste).

// Elección de idioma de la web (es/en/pt).
// Funciones puras y acceso seguro a localStorage. Sin dependencias.

const IDIOMAS = ["es", "en", "pt"];
const CLAVE_GUARDADO = "trenar-idioma";

// Devuelve el idioma a usar: primero el guardado si es válido, después el
// primer preferido compatible (pt-BR → pt, en_GB → en) y, si no hay ninguno, "es".
function elegirIdioma(guardado, preferidos) {
  if (IDIOMAS.includes(guardado)) return guardado;
  const lista = typeof preferidos === "string" ? [preferidos] : Array.isArray(preferidos) ? preferidos : [];
  for (const preferido of lista) {
    const base = String(preferido).toLowerCase().split(/[-_]/)[0];
    if (IDIOMAS.includes(base)) return base;
  }
  return "es";
}

// Lee el idioma guardado. Si el storage falla (por ejemplo Safari en modo privado), devuelve null.
function leerIdiomaGuardado(storage) {
  try {
    return storage.getItem(CLAVE_GUARDADO) ?? null;
  } catch {
    return null;
  }
}

// Guarda el idioma elegido. Si el storage falla, la página sigue funcionando sin recordar la elección.
function guardarIdioma(storage, idioma) {
  try {
    storage.setItem(CLAVE_GUARDADO, idioma);
  } catch {
    // Sin storage disponible: no hay nada que hacer.
  }
}

// Valor de <html lang> para cada idioma.
const LANG_HTML = { es: "es-AR", en: "en", pt: "pt-BR" };

// Aplica un idioma a toda la página. Usa TEXTOS, VERSIONES y las funciones de
// linea-tiempo.js, que ya están cargadas cuando esto corre (después de DOMContentLoaded).
function aplicarIdioma(doc, idioma) {
  const textos = TEXTOS[idioma];
  const ultima = ultimaVersion(VERSIONES);
  const valores = { version: ultima, semanas: semanasEntre(VERSIONES) };
  // En el título de la historia {version} es la versión corta; en el resto, la completa.
  const texto = (clave) => completar(textos[clave], clave === "hist.titulo" ? { ...valores, version: versionCorta(ultima) } : valores);

  doc.documentElement.setAttribute("lang", LANG_HTML[idioma]);
  doc.querySelectorAll("[data-i18n]").forEach((el) => { el.textContent = texto(el.getAttribute("data-i18n")); });
  doc.querySelectorAll("[data-i18n-html]").forEach((el) => { el.innerHTML = texto(el.getAttribute("data-i18n-html")); });
  // Atributos traducibles: data-i18n-X="clave" setea el atributo X (por ejemplo content).
  doc.querySelectorAll("*").forEach((el) => {
    Array.from(el.attributes).forEach((atributo) => {
      const m = /^data-i18n-(.+)$/.exec(atributo.name);
      if (m && m[1] !== "html") el.setAttribute(m[1], texto(atributo.value));
    });
  });
  doc.querySelectorAll(".idioma").forEach((boton) => {
    const activo = boton.getAttribute("data-idioma") === idioma;
    boton.setAttribute("aria-pressed", String(activo));
    boton.classList.toggle("on", activo);
  });
  montarLineaTiempo(doc, idioma);
}

// Arranque en el navegador. En Node (pruebas) no hay document y no corre.
if (typeof document !== "undefined") {
  document.addEventListener("DOMContentLoaded", () => {
    // Acceder a localStorage puede tirar error (modo privado): se sigue sin recordar.
    let almacen = null;
    try { almacen = window.localStorage; } catch { almacen = null; }
    aplicarIdioma(document, elegirIdioma(leerIdiomaGuardado(almacen), navigator.languages || navigator.language));

    document.querySelectorAll(".idioma").forEach((boton) => {
      boton.addEventListener("click", () => {
        const elegido = boton.getAttribute("data-idioma");
        guardarIdioma(almacen, elegido);
        aplicarIdioma(document, elegido);
      });
    });

    const verTodas = document.querySelector("#ver-todas");
    if (verTodas) {
      verTodas.addEventListener("click", () => {
        document.querySelector("#linea-tiempo").classList.toggle("abierta");
        montarLineaTiempo(document, document.documentElement.getAttribute("lang").slice(0, 2));
      });
    }
  });
}

if (typeof module !== "undefined") module.exports = { IDIOMAS, CLAVE_GUARDADO, elegirIdioma, leerIdiomaGuardado, guardarIdioma, aplicarIdioma };

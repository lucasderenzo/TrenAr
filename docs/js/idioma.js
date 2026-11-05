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

if (typeof module !== "undefined") module.exports = { IDIOMAS, CLAVE_GUARDADO, elegirIdioma, leerIdiomaGuardado, guardarIdioma };

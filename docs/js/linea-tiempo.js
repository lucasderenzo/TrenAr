// Armado de la línea de tiempo de la web (es/en/pt).
// Funciones puras: transforman la lista VERSIONES en textos y HTML. Sin DOM ni dependencias.

// Nombres de meses propios, sin Intl (cambia entre motores).
const MESES = {
  es: ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"],
  en: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
  pt: ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"],
};

const MS_POR_DIA = 24 * 60 * 60 * 1000;

// Separa "AAAA-MM-DD" sin pasar por Date, para no depender de la zona horaria.
function partirFecha(iso) {
  const [anio, mes, dia] = String(iso).split("-").map(Number);
  return { anio, mes, dia };
}

// "2026-09-30" → "30 sep" (es/pt) o "Sep 30" (en). Sin ceros a la izquierda.
function formatearFecha(iso, idioma) {
  const { mes, dia } = partirFecha(iso);
  const nombre = (MESES[idioma] || MESES.es)[mes - 1];
  return idioma === "en" ? `${nombre} ${dia}` : `${dia} ${nombre}`;
}

// "1.22.0" → "1.22". "1.0" → "1.0".
function versionCorta(version) {
  return String(version).split(".").slice(0, 2).join(".");
}

// La version de la última entrada de la lista.
function ultimaVersion(versiones) {
  return versiones[versiones.length - 1].version;
}

// Semanas (redondeadas) entre la primera y la última fecha de la lista.
function semanasEntre(versiones) {
  const primera = partirFecha(versiones[0].fecha);
  const ultima = partirFecha(versiones[versiones.length - 1].fecha);
  const dias = (Date.UTC(ultima.anio, ultima.mes - 1, ultima.dia) - Date.UTC(primera.anio, primera.mes - 1, primera.dia)) / MS_POR_DIA;
  return Math.round(dias / 7);
}

// Reemplaza cada {clave} de la plantilla por valores[clave]. Si falta la clave, deja el {clave} tal cual.
function completar(plantilla, valores) {
  return plantilla.replace(/\{(\w+)\}/g, (coincidencia, clave) =>
    Object.prototype.hasOwnProperty.call(valores, clave) ? String(valores[clave]) : coincidencia
  );
}

// Escapa los caracteres que rompen el HTML. El & va primero para no escaparlo dos veces.
function escaparHtml(texto) {
  return String(texto).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// Un <li> por versión, en el orden de la lista. Las que no son hito llevan "oculta".
function htmlLineaTiempo(versiones, idioma) {
  return versiones
    .map((v) => {
      const clase = v.hito ? "stop hito" : "stop oculta";
      return `<li class="${clase}"><span class="v">${escaparHtml(v.version)}</span><span class="d">${formatearFecha(v.fecha, idioma)}</span><p>${escaparHtml(v.texto[idioma])}</p></li>`;
    })
    .join("\n");
}

// Rearma la lista de #linea-tiempo en el idioma dado y ajusta el botón #ver-todas
// (texto y aria-expanded) según esté abierta o no. Usa TEXTOS y VERSIONES globales.
function montarLineaTiempo(doc, idioma) {
  const lista = doc.querySelector("#linea-tiempo");
  const boton = doc.querySelector("#ver-todas");
  if (lista) lista.innerHTML = htmlLineaTiempo(VERSIONES, idioma);
  if (lista && boton) {
    const abierta = lista.classList.contains("abierta");
    boton.textContent = TEXTOS[idioma][abierta ? "hist.ver_menos" : "hist.ver_todas"];
    boton.setAttribute("aria-expanded", String(abierta));
  }
}

if (typeof module !== "undefined") module.exports = { formatearFecha, versionCorta, ultimaVersion, semanasEntre, completar, htmlLineaTiempo, montarLineaTiempo };

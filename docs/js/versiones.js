// Historial de versiones de la app TrenAr, de vieja a nueva. Alimenta la línea de tiempo.
// hito: true marca las versiones grandes (★ en la especificación).
// La última versión debe coincidir con versionName de app/build.gradle.kts.

const VERSIONES = [
  { version: "1.0", fecha: "2026-09-30", hito: true, texto: { es: "Primera versión: arribos en vivo, estaciones cercanas y favoritas.", en: "First version: live arrivals, nearby and favorite stations.", pt: "Primeira versão: chegadas ao vivo, estações próximas e favoritas." } },
  { version: "1.1", fecha: "2026-09-30", hito: true, texto: { es: "Barra de cinco pestañas, mapa de la red en vivo y alertas.", en: "Five-tab bar, live network map and alerts.", pt: "Barra de cinco abas, mapa da rede ao vivo e alertas." } },
  { version: "1.1.1", fecha: "2026-10-01", hito: false, texto: { es: "Trenes en vivo diferenciados y acentos corregidos.", en: "Live trains told apart and accents fixed.", pt: "Trens ao vivo diferenciados e acentos corrigidos." } },
  { version: "1.2.0", fecha: "2026-10-01", hito: true, texto: { es: "Ecobici en vivo en el mapa.", en: "Live Ecobici on the map.", pt: "Ecobici ao vivo no mapa." } },
  { version: "1.3.0", fecha: "2026-10-01", hito: true, texto: { es: "Seguir un tren con cuenta regresiva y “¡Salí ya!”.", en: "Follow a train with a countdown and “Leave now!”.", pt: "Acompanhe um trem com contagem regressiva e “Saia já!”." } },
  { version: "1.4.0", fecha: "2026-10-02", hito: false, texto: { es: "Filtros y zoom en el mapa; Cercanas con tren y Ecobici.", en: "Map filters and zoom; Nearby with train and Ecobici.", pt: "Filtros e zoom no mapa; Próximas com trem e Ecobici." } },
  { version: "1.5.0", fecha: "2026-10-02", hito: true, texto: { es: "Llegan Urquiza y Belgrano Norte, y “mi ubicación” en el mapa.", en: "Urquiza and Belgrano Norte arrive, plus “my location” on the map.", pt: "Chegam Urquiza e Belgrano Norte, e “minha localização” no mapa." } },
  { version: "1.6.0", fecha: "2026-10-02", hito: false, texto: { es: "Horarios programados y mapa de la estación.", en: "Scheduled times and station map.", pt: "Horários programados e mapa da estação." } },
  { version: "1.7.0", fecha: "2026-10-13", hito: false, texto: { es: "Horarios exactos de Belgrano Norte desde el PDF oficial.", en: "Exact Belgrano Norte timetables from the official PDF.", pt: "Horários exatos de Belgrano Norte pelo PDF oficial." } },
  { version: "1.8.0", fecha: "2026-10-13", hito: true, texto: { es: "Horarios exactos de Urquiza desde el PDF oficial.", en: "Exact Urquiza timetables from the official PDF.", pt: "Horários exatos de Urquiza pelo PDF oficial." } },
  { version: "1.9.0", fecha: "2026-10-14", hito: false, texto: { es: "“Cómo llego”, Ecobici favoritas y cercanía configurable.", en: "“How do I get there”, favorite Ecobici and adjustable proximity.", pt: "“Como chego”, Ecobici favoritas e proximidade configurável." } },
  { version: "1.10.0", fecha: "2026-10-14", hito: false, texto: { es: "La posición del tren en el mapa de cada estación.", en: "The train's position on each station's map.", pt: "A posição do trem no mapa de cada estação." } },
  { version: "1.11.0", fecha: "2026-10-14", hito: false, texto: { es: "Radio de búsqueda configurable y “¡Salí ya!” con horarios programados.", en: "Adjustable search radius and “Leave now!” with scheduled times.", pt: "Raio de busca configurável e “Saia já!” com horários programados." } },
  { version: "1.11.1", fecha: "2026-10-14", hito: false, texto: { es: "Corrección del radio guardado en Cercanas.", en: "Fix for the saved radius in Nearby.", pt: "Correção do raio salvo em Próximas." } },
  { version: "1.12.0", fecha: "2026-10-14", hito: false, texto: { es: "Distancia a la estación y “¡Salí ya!” más preciso.", en: "Distance to the station and a more precise “Leave now!”.", pt: "Distância até a estação e “Saia já!” mais preciso." } },
  { version: "1.12.1", fecha: "2026-10-14", hito: false, texto: { es: "“Sin radio” muestra todas las estaciones.", en: "“No radius” shows all stations.", pt: "“Sem raio” mostra todas as estações." } },
  { version: "1.13.0", fecha: "2026-10-14", hito: true, texto: { es: "En español, inglés y portugués.", en: "In Spanish, English and Portuguese.", pt: "Em espanhol, inglês e português." } },
  { version: "1.13.1", fecha: "2026-10-15", hito: false, texto: { es: "Los feriados usan el horario de domingo.", en: "Public holidays use the Sunday timetable.", pt: "Feriados usam o horário de domingo." } },
  { version: "1.14.0", fecha: "2026-10-16", hito: false, texto: { es: "Íconos de tren, cabeceras y posición estimada en el mapa.", en: "Train icons, line ends and estimated position on the map.", pt: "Ícones de trem, cabeceiras e posição estimada no mapa." } },
  { version: "1.14.1", fecha: "2026-10-19", hito: false, texto: { es: "El tren mira hacia donde va.", en: "The train faces where it's heading.", pt: "O trem olha para onde está indo." } },
  { version: "1.15.0", fecha: "2026-10-23", hito: false, texto: { es: "Botón de sugerencias y reportes.", en: "Suggestions and reports button.", pt: "Botão de sugestões e relatórios." } },
  { version: "1.16.1", fecha: "2026-10-27", hito: false, texto: { es: "Ubicación escrita a mano en Cercanas.", en: "Manually entered location in Nearby.", pt: "Localização digitada à mão em Próximas." } },
  { version: "1.16.2", fecha: "2026-10-28", hito: false, texto: { es: "Arribos coherentes con los trenes del mapa.", en: "Arrivals consistent with the trains on the map.", pt: "Chegadas coerentes com os trens do mapa." } },
  { version: "1.17.0", fecha: "2026-10-28", hito: true, texto: { es: "La línea entera, con estaciones y trenes, en el mapa de cada estación.", en: "The whole line, with stations and trains, on each station's map.", pt: "A linha inteira, com estações e trens, no mapa de cada estação." } },
  { version: "1.17.1", fecha: "2026-10-29", hito: false, texto: { es: "Destinos y ramales que ya no se cortan.", en: "Destinations and branches no longer get cut off.", pt: "Destinos e ramais que não são mais cortados." } },
  { version: "1.18.0", fecha: "2026-10-29", hito: false, texto: { es: "Estaciones terminales marcadas en el mapa.", en: "Terminal stations marked on the map.", pt: "Estações terminais marcadas no mapa." } },
  { version: "1.19.0", fecha: "2026-10-29", hito: false, texto: { es: "Nombres de las estaciones en el mapa según el zoom.", en: "Station names on the map depending on zoom.", pt: "Nomes das estações no mapa conforme o zoom." } },
  { version: "1.20.0", fecha: "2026-10-29", hito: false, texto: { es: "El nombre de la línea, en su color, bajo cada estación.", en: "The line name, in its color, below each station.", pt: "O nome da linha, na sua cor, abaixo de cada estação." } },
  { version: "1.21.0", fecha: "2026-10-30", hito: false, texto: { es: "Modo oscuro.", en: "Dark mode.", pt: "Modo escuro." } },
  { version: "1.21.1", fecha: "2026-11-02", hito: false, texto: { es: "El mapa de la red mueve los trenes en vivo.", en: "The network map moves trains live.", pt: "O mapa da rede move os trens ao vivo." } },
  { version: "1.21.2", fecha: "2026-11-02", hito: false, texto: { es: "El minimapa de la estación también anima los trenes.", en: "The station minimap also animates the trains.", pt: "O minimapa da estação também anima os trens." } },
  { version: "1.22.0", fecha: "2026-11-03", hito: true, texto: { es: "La estación se actualiza cada 10 segundos, como el mapa en vivo.", en: "The station updates every 10 seconds, like the live map.", pt: "A estação atualiza a cada 10 segundos, como o mapa ao vivo." } },
];

if (typeof module !== "undefined") module.exports = { VERSIONES };

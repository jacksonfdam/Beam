"use client";

/*
 * Lightweight client-side i18n for the Beam web app (landing + browser remote).
 *
 * No routing changes and no extra dependency: a React context holds the active
 * locale (persisted to localStorage, seeded from the browser language), and
 * `useI18n().t` returns the typed message bundle for that locale. The English
 * bundle defines the shape; the others must match it (TypeScript enforces this).
 */

import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";

export const LOCALES = ["en", "pt-BR", "es", "sv"] as const;
export type Locale = (typeof LOCALES)[number];

export const LOCALE_LABELS: Record<Locale, string> = {
  en: "EN",
  "pt-BR": "PT",
  es: "ES",
  sv: "SV",
};

export const LOCALE_FLAGS: Record<Locale, string> = {
  en: "🇬🇧",
  "pt-BR": "🇧🇷",
  es: "🇪🇸",
  sv: "🇸🇪",
};

export const LOCALE_NAMES: Record<Locale, string> = {
  en: "English",
  "pt-BR": "Português (BR)",
  es: "Español",
  sv: "Svenska",
};

const en = {
  nav: { features: "Features", howToConnect: "How to connect", openRemote: "Open remote" },
  hero: {
    badge: "Local-first presenter",
    title: "Turn any exported PDF into a flawless fullscreen presentation",
    subtitle:
      "Keynote, PowerPoint, Canva, Figma — export to PDF and Beam renders it exactly as designed. No broken fonts, no layout surprises. Control it all from your phone.",
    flow: "OPEN → PRESENT → DONE",
    download: "Download Beam",
    useBrowserRemote: "Use the browser remote",
  },
  features: {
    heading: "Everything a talk needs, nothing it doesn’t",
    sub: "Beam presents — it doesn’t author. That focus is why it stays fast, private, and dead simple.",
    items: [
      { title: "Exact-as-designed rendering", body: "Beam renders the PDF itself, so fonts, gradients, and layouts look precisely as you exported them — on any machine, on any projector." },
      { title: "Your phone is the remote", body: "Connect over the local network and drive the deck from your hand: next, previous, jump to a slide. No perceptible lag." },
      { title: "Speaker notes, privately", body: "Notes for the current slide show on your phone’s presenter view — pulled from a sidecar file you control, never embedded in the projected slide." },
      { title: "Live drawing on the slide", body: "Draw with your finger and the ink appears on the projected slide instantly, mapped to the right spot at any resolution. Clear with one tap." },
      { title: "Host-owned timer", body: "Start, pause, and reset a talk timer. The host owns the clock, so it keeps perfect time even if your phone drops and reconnects." },
      { title: "Multi-screen aware", body: "Present fullscreen on the external display while your laptop keeps the presenter view. Pick which screen beams the slides." },
      { title: "Fully local, fully private", body: "No accounts, no cloud storage, no telemetry. Devices talk directly over your Wi-Fi. Slides and notes never leave your network." },
      { title: "Browser remote, no install", body: "No app on hand? Open the remote in any browser and pair with a session code. It speaks the same protocol as the native remote." },
    ],
  },
  connect: {
    heading: "How to connect",
    sub: "Two devices, one Wi-Fi network, a short PIN. That’s the whole handshake — no sign-in, no pairing servers.",
    steps: [
      { title: "Open your deck on the desktop", body: "Launch Beam on your Mac, Windows, or Linux machine and open the exported PDF. Beam goes fullscreen on your chosen display." },
      { title: "Scan the QR or type the IP", body: "The presenter screen shows a QR code, the host IP, and a short PIN. Scan it with the Beam app, or enter the IP and PIN by hand." },
      { title: "Present from your phone", body: "Pick the deck, press Start, and navigate. Notes, timer, and drawing are right there in your hand — everything stays on your network." },
    ],
    preferPre: "Prefer no install? The ",
    browserLink: "browser remote",
    preferPost: " pairs with the same session code over a direct peer-to-peer connection.",
  },
  downloads: {
    heading: "Download Beam",
    sub: "The desktop app is the presenter and host. The mobile app is the remote. Builds are on the way.",
    soon: "Soon",
    notes: {
      macos: "Apple silicon & Intel",
      windows: "Windows 10 & 11",
      linux: "AppImage & .deb",
      mobile: "The remote app",
    },
  },
  footer: {
    tagline: "A local-first PDF presenter. Open → Present → Done.",
    whatHeading: "What leaves your device",
    whatBody:
      "Nothing of substance. Slides, speaker notes, navigation, timer, and ink stay on your local network — peer-to-peer between your devices. The browser remote uses a server only to exchange the initial WebRTC handshake (a session code and connection details); once paired, all data is direct. No accounts, no content storage, no telemetry.",
    built: "Built for presenters who just want it to work.",
    features: "Features",
    connect: "Connect",
    remote: "Remote",
  },
  pairing: {
    title: "Connect to a host",
    subtitle: "Enter the host IP and PIN shown on the presenter screen.",
    hostIp: "Host IP",
    portHint: "Port is optional — defaults to 53317.",
    pin: "PIN",
    yourName: "Your name",
    optional: "(optional)",
    connecting: "Connecting…",
    connect: "Connect",
    cancel: "Cancel",
  },
  controls: {
    chooseDeck: "Choose a deck",
    noDecks: "No decks available yet. Open one on the host.",
    slides: "slides",
    notes: "notes",
    connected: "Connected",
    disconnect: "Disconnect",
    slidesMode: "Slides",
    screenMode: "Screen",
    annotate: "Annotate",
    interact: "Interact",
    currentSlide: "Current slide",
    liveScreen: "Live screen",
    prev: "‹ Prev",
    next: "Next ›",
    first: "⤒ First",
    last: "Last ⤓",
    previousAria: "Previous",
    nextAria: "Next",
    firstAria: "First slide",
    lastAria: "Last slide",
    goTo: "Go to",
    goToAria: "Go to slide (1 to {total})",
    slideLabel: "Slide",
    speakerNotes: "Speaker notes",
    noNotesForSlide: "No notes for this slide.",
    noNotesSidecar: "This deck has no notes sidecar.",
    timer: "Timer",
    start: "Start",
    pause: "Pause",
    reset: "Reset",
    hideDrawing: "Hide drawing",
    drawOnSlide: "Draw on the slide",
    drawSpotlight: "Draw / spotlight",
    pen: "Pen",
    marker: "Marker",
    spotlight: "Spotlight",
    clearInk: "Clear ink",
    resetZoom: "Reset zoom",
    drawingAria: "Drawing surface — one finger draws, two fingers zoom",
  },
};

export type Dict = typeof en;

const ptBR: Dict = {
  nav: { features: "Recursos", howToConnect: "Como conectar", openRemote: "Abrir controle" },
  hero: {
    badge: "Apresentador local-first",
    title: "Transforme qualquer PDF exportado numa apresentação impecável em tela cheia",
    subtitle:
      "Keynote, PowerPoint, Canva, Figma — exporte para PDF e o Beam renderiza exatamente como foi desenhado. Sem fontes quebradas, sem surpresas de layout. Controle tudo pelo seu celular.",
    flow: "ABRIR → APRESENTAR → PRONTO",
    download: "Baixar o Beam",
    useBrowserRemote: "Usar o controle no navegador",
  },
  features: {
    heading: "Tudo o que uma palestra precisa, nada além disso",
    sub: "O Beam apresenta — não edita. É esse foco que o mantém rápido, privado e simples.",
    items: [
      { title: "Renderização fiel ao design", body: "O Beam renderiza o próprio PDF, então fontes, gradientes e layouts ficam exatamente como você exportou — em qualquer máquina, em qualquer projetor." },
      { title: "Seu celular é o controle", body: "Conecte-se pela rede local e conduza a apresentação na palma da mão: avançar, voltar, pular para um slide. Sem atraso perceptível." },
      { title: "Notas do apresentador, em privado", body: "As notas do slide atual aparecem na visão do apresentador no seu celular — vindas de um arquivo que você controla, nunca embutidas no slide projetado." },
      { title: "Desenho ao vivo no slide", body: "Desenhe com o dedo e a tinta aparece no slide projetado na hora, mapeada para o ponto certo em qualquer resolução. Limpe com um toque." },
      { title: "Cronômetro do host", body: "Inicie, pause e zere o cronômetro da palestra. O host controla o relógio, então ele mantém o tempo certo mesmo se o celular cair e reconectar." },
      { title: "Ciente de múltiplas telas", body: "Apresente em tela cheia no monitor externo enquanto o notebook mantém a visão do apresentador. Escolha qual tela projeta os slides." },
      { title: "Totalmente local, totalmente privado", body: "Sem contas, sem armazenamento na nuvem, sem telemetria. Os dispositivos conversam direto pelo Wi-Fi. Slides e notas nunca saem da sua rede." },
      { title: "Controle no navegador, sem instalar", body: "Sem app à mão? Abra o controle em qualquer navegador e pareie com um código de sessão. Ele fala o mesmo protocolo do controle nativo." },
    ],
  },
  connect: {
    heading: "Como conectar",
    sub: "Dois dispositivos, uma rede Wi-Fi, um PIN curto. É todo o handshake — sem login, sem servidores de pareamento.",
    steps: [
      { title: "Abra sua apresentação no desktop", body: "Inicie o Beam no seu Mac, Windows ou Linux e abra o PDF exportado. O Beam vai para tela cheia na tela escolhida." },
      { title: "Escaneie o QR ou digite o IP", body: "A tela do apresentador mostra um QR code, o IP do host e um PIN curto. Escaneie com o app Beam, ou digite o IP e o PIN à mão." },
      { title: "Apresente pelo celular", body: "Escolha a apresentação, toque em Iniciar e navegue. Notas, cronômetro e desenho ficam na sua mão — tudo permanece na sua rede." },
    ],
    preferPre: "Prefere não instalar? O ",
    browserLink: "controle no navegador",
    preferPost: " pareia com o mesmo código de sessão por uma conexão direta peer-to-peer.",
  },
  downloads: {
    heading: "Baixar o Beam",
    sub: "O app desktop é o apresentador e host. O app mobile é o controle. As builds estão a caminho.",
    soon: "Em breve",
    notes: {
      macos: "Apple silicon & Intel",
      windows: "Windows 10 e 11",
      linux: "AppImage e .deb",
      mobile: "O app de controle",
    },
  },
  footer: {
    tagline: "Um apresentador de PDF local-first. Abrir → Apresentar → Pronto.",
    whatHeading: "O que sai do seu dispositivo",
    whatBody:
      "Nada de substancial. Slides, notas, navegação, cronômetro e tinta ficam na sua rede local — peer-to-peer entre seus dispositivos. O controle no navegador usa um servidor apenas para trocar o handshake inicial do WebRTC (um código de sessão e detalhes de conexão); uma vez pareado, todos os dados são diretos. Sem contas, sem armazenamento de conteúdo, sem telemetria.",
    built: "Feito para apresentadores que só querem que funcione.",
    features: "Recursos",
    connect: "Conectar",
    remote: "Controle",
  },
  pairing: {
    title: "Conectar a um host",
    subtitle: "Digite o IP do host e o PIN mostrados na tela do apresentador.",
    hostIp: "IP do host",
    portHint: "A porta é opcional — padrão 53317.",
    pin: "PIN",
    yourName: "Seu nome",
    optional: "(opcional)",
    connecting: "Conectando…",
    connect: "Conectar",
    cancel: "Cancelar",
  },
  controls: {
    chooseDeck: "Escolha uma apresentação",
    noDecks: "Nenhuma apresentação disponível ainda. Abra uma no host.",
    slides: "slides",
    notes: "notas",
    connected: "Conectado",
    disconnect: "Desconectar",
    slidesMode: "Slides",
    screenMode: "Tela",
    annotate: "Anotar",
    interact: "Interagir",
    currentSlide: "Slide atual",
    liveScreen: "Tela ao vivo",
    prev: "‹ Anterior",
    next: "Próximo ›",
    first: "⤒ Primeiro",
    last: "Último ⤓",
    previousAria: "Anterior",
    nextAria: "Próximo",
    firstAria: "Primeiro slide",
    lastAria: "Último slide",
    goTo: "Ir para",
    goToAria: "Ir para o slide (1 a {total})",
    slideLabel: "Slide",
    speakerNotes: "Notas do apresentador",
    noNotesForSlide: "Sem notas para este slide.",
    noNotesSidecar: "Esta apresentação não tem arquivo de notas.",
    timer: "Cronômetro",
    start: "Iniciar",
    pause: "Pausar",
    reset: "Zerar",
    hideDrawing: "Ocultar desenho",
    drawOnSlide: "Desenhar no slide",
    drawSpotlight: "Desenhar / destaque",
    pen: "Caneta",
    marker: "Marcador",
    spotlight: "Destaque",
    clearInk: "Limpar tinta",
    resetZoom: "Redefinir zoom",
    drawingAria: "Área de desenho — um dedo desenha, dois dedos ampliam",
  },
};

const es: Dict = {
  nav: { features: "Funciones", howToConnect: "Cómo conectar", openRemote: "Abrir control" },
  hero: {
    badge: "Presentador local-first",
    title: "Convierte cualquier PDF exportado en una presentación impecable a pantalla completa",
    subtitle:
      "Keynote, PowerPoint, Canva, Figma — exporta a PDF y Beam lo renderiza exactamente como lo diseñaste. Sin fuentes rotas, sin sorpresas de diseño. Contrólalo todo desde tu teléfono.",
    flow: "ABRIR → PRESENTAR → LISTO",
    download: "Descargar Beam",
    useBrowserRemote: "Usar el control del navegador",
  },
  features: {
    heading: "Todo lo que necesita una charla, nada más",
    sub: "Beam presenta — no edita. Ese enfoque es lo que lo mantiene rápido, privado y simplísimo.",
    items: [
      { title: "Renderizado fiel al diseño", body: "Beam renderiza el propio PDF, así que las fuentes, los degradados y los diseños se ven exactamente como los exportaste — en cualquier equipo, en cualquier proyector." },
      { title: "Tu teléfono es el control", body: "Conéctate por la red local y maneja la presentación desde tu mano: siguiente, anterior, saltar a una diapositiva. Sin retraso perceptible." },
      { title: "Notas del orador, en privado", body: "Las notas de la diapositiva actual aparecen en la vista del orador de tu teléfono — desde un archivo que tú controlas, nunca incrustadas en la diapositiva proyectada." },
      { title: "Dibujo en vivo sobre la diapositiva", body: "Dibuja con el dedo y la tinta aparece en la diapositiva proyectada al instante, ubicada en el punto correcto a cualquier resolución. Borra con un toque." },
      { title: "Cronómetro del host", body: "Inicia, pausa y reinicia el cronómetro de la charla. El host controla el reloj, así que mantiene el tiempo exacto aunque tu teléfono se desconecte y vuelva." },
      { title: "Compatible con varias pantallas", body: "Presenta a pantalla completa en la pantalla externa mientras tu portátil mantiene la vista del orador. Elige qué pantalla proyecta las diapositivas." },
      { title: "Totalmente local, totalmente privado", body: "Sin cuentas, sin almacenamiento en la nube, sin telemetría. Los dispositivos hablan directo por tu Wi-Fi. Las diapositivas y notas nunca salen de tu red." },
      { title: "Control en el navegador, sin instalar", body: "¿Sin app a mano? Abre el control en cualquier navegador y emparéjalo con un código de sesión. Habla el mismo protocolo que el control nativo." },
    ],
  },
  connect: {
    heading: "Cómo conectar",
    sub: "Dos dispositivos, una red Wi-Fi, un PIN corto. Ese es todo el handshake — sin inicio de sesión, sin servidores de emparejamiento.",
    steps: [
      { title: "Abre tu presentación en el escritorio", body: "Inicia Beam en tu Mac, Windows o Linux y abre el PDF exportado. Beam pasa a pantalla completa en la pantalla que elijas." },
      { title: "Escanea el QR o escribe la IP", body: "La pantalla del presentador muestra un código QR, la IP del host y un PIN corto. Escanéalo con la app Beam, o ingresa la IP y el PIN a mano." },
      { title: "Presenta desde tu teléfono", body: "Elige la presentación, pulsa Iniciar y navega. Las notas, el cronómetro y el dibujo están en tu mano — todo se queda en tu red." },
    ],
    preferPre: "¿Prefieres no instalar? El ",
    browserLink: "control del navegador",
    preferPost: " se empareja con el mismo código de sesión por una conexión directa peer-to-peer.",
  },
  downloads: {
    heading: "Descargar Beam",
    sub: "La app de escritorio es el presentador y host. La app móvil es el control. Las versiones están en camino.",
    soon: "Pronto",
    notes: {
      macos: "Apple silicon e Intel",
      windows: "Windows 10 y 11",
      linux: "AppImage y .deb",
      mobile: "La app de control",
    },
  },
  footer: {
    tagline: "Un presentador de PDF local-first. Abrir → Presentar → Listo.",
    whatHeading: "Qué sale de tu dispositivo",
    whatBody:
      "Nada sustancial. Las diapositivas, notas, navegación, cronómetro y tinta se quedan en tu red local — peer-to-peer entre tus dispositivos. El control del navegador usa un servidor solo para intercambiar el handshake inicial de WebRTC (un código de sesión y detalles de conexión); una vez emparejado, todos los datos van directos. Sin cuentas, sin almacenamiento de contenido, sin telemetría.",
    built: "Hecho para presentadores que solo quieren que funcione.",
    features: "Funciones",
    connect: "Conectar",
    remote: "Control",
  },
  pairing: {
    title: "Conectar a un host",
    subtitle: "Ingresa la IP del host y el PIN que se muestran en la pantalla del presentador.",
    hostIp: "IP del host",
    portHint: "El puerto es opcional — por defecto 53317.",
    pin: "PIN",
    yourName: "Tu nombre",
    optional: "(opcional)",
    connecting: "Conectando…",
    connect: "Conectar",
    cancel: "Cancelar",
  },
  controls: {
    chooseDeck: "Elige una presentación",
    noDecks: "Aún no hay presentaciones. Abre una en el host.",
    slides: "diapositivas",
    notes: "notas",
    connected: "Conectado",
    disconnect: "Desconectar",
    slidesMode: "Diapositivas",
    screenMode: "Pantalla",
    annotate: "Anotar",
    interact: "Interactuar",
    currentSlide: "Diapositiva actual",
    liveScreen: "Pantalla en vivo",
    prev: "‹ Anterior",
    next: "Siguiente ›",
    first: "⤒ Primero",
    last: "Último ⤓",
    previousAria: "Anterior",
    nextAria: "Siguiente",
    firstAria: "Primera diapositiva",
    lastAria: "Última diapositiva",
    goTo: "Ir a",
    goToAria: "Ir a la diapositiva (1 a {total})",
    slideLabel: "Diapositiva",
    speakerNotes: "Notas del orador",
    noNotesForSlide: "Sin notas para esta diapositiva.",
    noNotesSidecar: "Esta presentación no tiene archivo de notas.",
    timer: "Cronómetro",
    start: "Iniciar",
    pause: "Pausar",
    reset: "Reiniciar",
    hideDrawing: "Ocultar dibujo",
    drawOnSlide: "Dibujar en la diapositiva",
    drawSpotlight: "Dibujar / foco",
    pen: "Lápiz",
    marker: "Marcador",
    spotlight: "Foco",
    clearInk: "Borrar tinta",
    resetZoom: "Restablecer zoom",
    drawingAria: "Área de dibujo — un dedo dibuja, dos dedos amplían",
  },
};

const sv: Dict = {
  nav: { features: "Funktioner", howToConnect: "Så ansluter du", openRemote: "Öppna fjärrkontroll" },
  hero: {
    badge: "Local-first-presentatör",
    title: "Förvandla vilken exporterad PDF som helst till en felfri helskärmspresentation",
    subtitle:
      "Keynote, PowerPoint, Canva, Figma — exportera till PDF och Beam återger den precis som den designats. Inga trasiga typsnitt, inga layoutöverraskningar. Styr allt från telefonen.",
    flow: "ÖPPNA → PRESENTERA → KLAR",
    download: "Ladda ner Beam",
    useBrowserRemote: "Använd webbfjärrkontrollen",
  },
  features: {
    heading: "Allt en presentation behöver, inget mer",
    sub: "Beam presenterar — det skapar inte. Det fokuset är varför det förblir snabbt, privat och dödssimpelt.",
    items: [
      { title: "Återgivning exakt som designad", body: "Beam återger PDF:en själv, så typsnitt, gradienter och layouter ser ut precis som du exporterade dem — på vilken dator och projektor som helst." },
      { title: "Telefonen är fjärrkontrollen", body: "Anslut via det lokala nätverket och styr presentationen från handen: nästa, föregående, hoppa till en bild. Ingen märkbar fördröjning." },
      { title: "Talaranteckningar, privat", body: "Anteckningar för aktuell bild visas i telefonens presentatörsvy — från en sidofil du styr, aldrig inbäddad i den projicerade bilden." },
      { title: "Live-ritning på bilden", body: "Rita med fingret och bläcket dyker upp på den projicerade bilden direkt, placerat på rätt ställe i alla upplösningar. Rensa med ett tryck." },
      { title: "Värdägd timer", body: "Starta, pausa och nollställ en presentationstimer. Värden äger klockan, så den håller tiden perfekt även om telefonen tappar och återansluter." },
      { title: "Medveten om flera skärmar", body: "Presentera i helskärm på den externa skärmen medan laptopen behåller presentatörsvyn. Välj vilken skärm som visar bilderna." },
      { title: "Helt lokalt, helt privat", body: "Inga konton, ingen molnlagring, ingen telemetri. Enheterna pratar direkt via ditt Wi-Fi. Bilder och anteckningar lämnar aldrig ditt nätverk." },
      { title: "Webbfjärrkontroll, ingen installation", body: "Ingen app till hands? Öppna fjärrkontrollen i valfri webbläsare och para med en sessionskod. Den talar samma protokoll som den inbyggda fjärrkontrollen." },
    ],
  },
  connect: {
    heading: "Så ansluter du",
    sub: "Två enheter, ett Wi-Fi-nätverk, en kort PIN. Det är hela handskakningen — ingen inloggning, inga parkopplingsservrar.",
    steps: [
      { title: "Öppna presentationen på datorn", body: "Starta Beam på din Mac, Windows eller Linux och öppna den exporterade PDF:en. Beam går till helskärm på den skärm du valt." },
      { title: "Skanna QR-koden eller skriv IP", body: "Presentatörsskärmen visar en QR-kod, värdens IP och en kort PIN. Skanna med Beam-appen, eller ange IP och PIN för hand." },
      { title: "Presentera från telefonen", body: "Välj presentationen, tryck Starta och navigera. Anteckningar, timer och ritning finns i din hand — allt stannar i ditt nätverk." },
    ],
    preferPre: "Föredrar du ingen installation? ",
    browserLink: "Webbfjärrkontrollen",
    preferPost: " parkopplas med samma sessionskod via en direkt peer-to-peer-anslutning.",
  },
  downloads: {
    heading: "Ladda ner Beam",
    sub: "Skrivbordsappen är presentatör och värd. Mobilappen är fjärrkontrollen. Versioner är på väg.",
    soon: "Snart",
    notes: {
      macos: "Apple silicon & Intel",
      windows: "Windows 10 & 11",
      linux: "AppImage & .deb",
      mobile: "Fjärrkontrollappen",
    },
  },
  footer: {
    tagline: "En local-first PDF-presentatör. Öppna → Presentera → Klar.",
    whatHeading: "Vad som lämnar din enhet",
    whatBody:
      "Inget väsentligt. Bilder, talaranteckningar, navigering, timer och bläck stannar i ditt lokala nätverk — peer-to-peer mellan dina enheter. Webbfjärrkontrollen använder en server endast för att utbyta den inledande WebRTC-handskakningen (en sessionskod och anslutningsdetaljer); väl parkopplad går all data direkt. Inga konton, ingen innehållslagring, ingen telemetri.",
    built: "Byggt för presentatörer som bara vill att det ska fungera.",
    features: "Funktioner",
    connect: "Anslut",
    remote: "Fjärrkontroll",
  },
  pairing: {
    title: "Anslut till en värd",
    subtitle: "Ange värdens IP och PIN som visas på presentatörsskärmen.",
    hostIp: "Värdens IP",
    portHint: "Porten är valfri — standard 53317.",
    pin: "PIN",
    yourName: "Ditt namn",
    optional: "(valfritt)",
    connecting: "Ansluter…",
    connect: "Anslut",
    cancel: "Avbryt",
  },
  controls: {
    chooseDeck: "Välj en presentation",
    noDecks: "Inga presentationer än. Öppna en på värden.",
    slides: "bilder",
    notes: "anteckningar",
    connected: "Ansluten",
    disconnect: "Koppla från",
    slidesMode: "Bilder",
    screenMode: "Skärm",
    annotate: "Anteckna",
    interact: "Interagera",
    currentSlide: "Aktuell bild",
    liveScreen: "Live-skärm",
    prev: "‹ Föreg.",
    next: "Nästa ›",
    first: "⤒ Första",
    last: "Sista ⤓",
    previousAria: "Föregående",
    nextAria: "Nästa",
    firstAria: "Första bilden",
    lastAria: "Sista bilden",
    goTo: "Gå till",
    goToAria: "Gå till bild (1 till {total})",
    slideLabel: "Bild",
    speakerNotes: "Talaranteckningar",
    noNotesForSlide: "Inga anteckningar för den här bilden.",
    noNotesSidecar: "Den här presentationen saknar anteckningsfil.",
    timer: "Timer",
    start: "Starta",
    pause: "Pausa",
    reset: "Nollställ",
    hideDrawing: "Dölj ritning",
    drawOnSlide: "Rita på bilden",
    drawSpotlight: "Rita / spotlight",
    pen: "Penna",
    marker: "Markör",
    spotlight: "Spotlight",
    clearInk: "Rensa bläck",
    resetZoom: "Återställ zoom",
    drawingAria: "Rityta — ett finger ritar, två fingrar zoomar",
  },
};

const messages: Record<Locale, Dict> = { en, "pt-BR": ptBR, es, sv };

const STORAGE_KEY = "beam.locale";

function isLocale(value: string | null): value is Locale {
  return value != null && (LOCALES as readonly string[]).includes(value);
}

function detectInitialLocale(): Locale {
  if (typeof window === "undefined") return "en";
  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (isLocale(stored)) return stored;
  const nav = window.navigator.language;
  if (nav.toLowerCase().startsWith("pt")) return "pt-BR";
  if (nav.toLowerCase().startsWith("es")) return "es";
  if (nav.toLowerCase().startsWith("sv")) return "sv";
  return "en";
}

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: Dict;
}

const I18nContext = createContext<I18nContextValue | null>(null);

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>("en");

  // Resolve the real locale on the client after mount (avoids hydration drift).
  useEffect(() => {
    setLocaleState(detectInitialLocale());
  }, []);

  const setLocale = (next: Locale) => {
    setLocaleState(next);
    if (typeof window !== "undefined") window.localStorage.setItem(STORAGE_KEY, next);
  };

  const value = useMemo<I18nContextValue>(
    () => ({ locale, setLocale, t: messages[locale] }),
    [locale],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used within <I18nProvider>");
  return ctx;
}

/** Dropdown language switcher: flag emoji + label, opens a flag + name menu. */
export function LanguageSwitcher({ className = "" }: { className?: string }) {
  const { locale, setLocale } = useI18n();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  return (
    <div ref={ref} className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label="Language"
        className="flex items-center gap-1.5 rounded-full border border-ink-line px-3 py-1.5 text-sm font-medium text-white/80 transition hover:text-white"
      >
        <span aria-hidden="true" className="text-base leading-none">{LOCALE_FLAGS[locale]}</span>
        <span>{LOCALE_LABELS[locale]}</span>
        <svg aria-hidden="true" width="12" height="12" viewBox="0 0 24 24" fill="none" className="opacity-70">
          <path d="M6 9l6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
      {open && (
        <ul
          role="listbox"
          className="absolute right-0 z-50 mt-2 min-w-[176px] overflow-hidden rounded-xl border border-ink-line bg-ink-soft p-1.5 shadow-2xl"
        >
          {LOCALES.map((l) => (
            <li key={l}>
              <button
                type="button"
                role="option"
                aria-selected={locale === l}
                onClick={() => { setLocale(l); setOpen(false); }}
                className={`flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm transition hover:bg-white/5 ${
                  locale === l ? "text-beam-glow" : "text-white/80"
                }`}
              >
                <span aria-hidden="true" className="text-base leading-none">{LOCALE_FLAGS[l]}</span>
                <span>{LOCALE_NAMES[l]}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

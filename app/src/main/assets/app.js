const $ = (s) => document.querySelector(s),
  esc = (s) =>
    String(s ?? "").replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[c],
    );
let data = { jobs: [], theme: {}, status: {} },
  page = "alarms",
  draft = null,
  month = new Date(),
  selectedDay = "";
const names = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"];
const presets = [
  ["12×36 · plantão", "hours", 12, 36],
  ["12×24", "hours", 12, 24],
  ["12×48", "hours", 12, 48],
  ["24×24", "hours", 24, 24],
  ["24×48", "hours", 24, 48],
  ["24×72", "hours", 24, 72],
  ["24×96", "hours", 24, 96],
  ["36×12", "hours", 36, 12],
  ["48×96", "hours", 48, 96],
  ["5×2 · ciclo de dias", "days", 5, 2],
  ["6×1 · ciclo de dias", "days", 6, 1],
  ["4×2 · ciclo de dias", "days", 4, 2],
  ["4×3 · ciclo de dias", "days", 4, 3],
  ["2×2 · ciclo de dias", "days", 2, 2],
  ["3×3 · ciclo de dias", "days", 3, 3],
  ["7×7 · embarque", "days", 7, 7],
  ["14×14 · embarque", "days", 14, 14],
  ["14×21 · embarque", "days", 14, 21],
  ["21×21 · embarque", "days", 21, 21],
  ["28×28 · embarque", "days", 28, 28],
  ["5×1 · ciclo de dias", "days", 5, 1],
  ["6×2 · ciclo de dias", "days", 6, 2],
  ["4×4 · ciclo de dias", "days", 4, 4],
  ["8×16 · diário", "hours", 8, 16],
  ["12×60", "hours", 12, 60],
  ["Dias fixos da semana", "weekly", 5, 2],
  ["Ciclo livre em horas", "hours", 12, 36],
  ["Ciclo livre em dias", "days", 5, 2],
  ["Sequência personalizada", "pattern", 1, 1],
];
function dateISO(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
function fmt(t, options = { hour: "2-digit", minute: "2-digit" }) {
  return new Date(t).toLocaleString("pt-BR", options);
}
function time(t) {
  return fmt(t);
}
function toast(s) {
  $("#toast").textContent = s;
  $("#toast").style.display = "block";
  setTimeout(() => ($("#toast").style.display = "none"), 4200);
}
function refresh() {
  if (typeof Native !== "undefined")
    try {
      data = JSON.parse(Native.state());
    } catch (e) {}
  data.jobs ??= [];
  data.theme ??= {};
  data.status ??= {};
  theme();
  if (!$("#editor").open) render();
}
function theme() {
  for (const k of ["bg", "panel", "accent", "text", "muted"])
    if (/^#[0-9a-f]{6}$/i.test(data.theme[k] || ""))
      document.documentElement.style.setProperty("--" + k, data.theme[k]);
}
function save() {
  const out = { jobs: data.jobs, theme: data.theme };
  let result = Native.save(JSON.stringify(out));
  if (result !== "ok") {
    toast(result);
    refresh();
    return false;
  }
  refresh();
  return true;
}
function tab(p) {
  page = p;
  render();
  window.scrollTo(0, 0);
}
function render() {
  document
    .querySelectorAll("nav button")
    .forEach((b) => b.classList.toggle("active", b.id === "nav-" + page));
  $("#main").innerHTML = {
    alarms: alarms,
    calendar: calendar,
    style: appearance,
    settings: settings,
  }[page]();
}
function describe(j) {
  return j.mode === "hours"
    ? `${j.work}h de trabalho / ${j.rest}h de folga`
    : j.mode === "days"
      ? `${j.work} dias de trabalho / ${j.rest} de folga`
      : j.mode === "weekly"
        ? "Dias fixos · " + j.weekdays.map((x) => names[x - 1]).join(", ")
        : "Ciclo personalizado · " + j.pattern.length + " dias";
}
function alarms() {
  let next = data.jobs
    .filter((j) => j.enabled && j.next > 0)
    .sort((a, b) => a.next - b.next)[0];
  let s = data.status,
    ready =
      s.exact && s.notifications && s.fullscreen && s.overlay && s.channel && s.volume > 0;
  return `<div class="eyebrow">CADA PLANTÃO, NO TEMPO CERTO</div><h1>Seu descanso.<br>Sua próxima jornada.</h1><p>Duas rotinas ou muitas. Um lugar para organizar todas.</p>${next ? `<section class="hero"><div class="row"><span class="eyebrow">PRÓXIMO ALARME</span><span class="badge">↻ RECORRENTE</span></div><div class="time">${time(next.next)}</div><div style="margin-top:10px">${esc(next.name)}</div><p style="margin-bottom:0">${fmt(next.next, { weekday: "long", day: "numeric", month: "long" })}</p></section>` : `<section class="hero"><span class="eyebrow">SEU TEMPO, ORGANIZADO</span><div class="time">--:--</div><p>Configure o primeiro plantão.<br>Os próximos se organizam sozinhos.</p></section>`}
${!ready ? `<button class="secondary warning" onclick="tab('settings')">⚠ Complete a proteção dos alarmes →</button>` : ""}<div class="row section"><h2 style="margin:0">Suas escalas <span class="sub">${data.jobs.length}</span></h2><span class="sub">Sem remarcar todo dia</span></div>${data.jobs.map((j) => `<article class="card job" style="--job:${esc(j.color)}"><div class="row"><h3><span class="dot"></span>${esc(j.name)}</h3><input type="checkbox" class="switch" aria-label="Ativar ${esc(j.name)}" ${j.enabled ? "checked" : ""} onchange="toggle('${j.id}',this.checked)"></div><div class="row"><div><div class="time">${j.next ? time(j.next) : esc(j.start)}</div><div class="sub">${esc(describe(j))}</div></div><button class="tiny" onclick="edit('${j.id}')">Editar ↗</button></div><div class="sub" style="margin-top:14px">${j.enabled ? (j.next ? "Próximo: " + fmt(j.next, { weekday: "short", day: "2-digit", month: "short" }) : "Sem ocorrência nos próximos 2 anos") : "Escala pausada por você"} · ${j.offsets.length} toque(s) por plantão</div></article>`).join("")}<button class="primary" onclick="edit()">＋ Adicionar escala</button><div class="hint">↻ Desligar o toque de hoje não desativa a escala. Ela só para quando você desliga a chave acima.</div>`;
}
function toggle(id, on) {
  data.jobs.find((j) => j.id === id).enabled = on;
  save();
}
function edit(id) {
  draft = structuredClone(
    data.jobs.find((j) => j.id === id) || {
      id: "j" + Date.now(),
      name: "",
      mode: "hours",
      work: 12,
      rest: 36,
      duration: 12,
      anchor: dateISO(new Date()),
      start: "07:00",
      offsets: [-60],
      weekdays: [1, 2, 3, 4, 5],
      dayTimes: {},
      pattern: "1100",
      patternTimes: {},
      skip: [],
      enabled: true,
      color: data.jobs.length ? "#55b8ff" : "#ff304f",
      snooze: 5,
      vibrate: true,
      audio: "",
      youtube: "",
      background: "",
      backgroundType: "",
      message: "",
    },
  );
  let j = draft;
  let pi = presets.findIndex(
    (p) => p[1] === j.mode && p[2] === j.work && p[3] === j.rest,
  );
  if (pi < 0)
    pi = presets.findIndex(
      (p) =>
        p[0] ===
        (j.mode === "pattern"
          ? "Sequência personalizada"
          : j.mode === "weekly"
            ? "Dias fixos da semana"
            : j.mode === "days"
              ? "Ciclo livre em dias"
              : "Ciclo livre em horas"),
    );
  $("#form").innerHTML =
    `<div class="row"><div><div class="eyebrow">SUA ROTINA, DO SEU JEITO</div><h1 style="font-size:28px">${id ? "Editar" : "Nova"} escala</h1></div><button class="tiny" onclick="closeEditor()">Fechar ✕</button></div><label for="name">Nome do trabalho</label><input id="name" maxlength="60" placeholder="Ex.: Hospital · plantão diurno" value="${esc(j.name)}"><div class="cols"><div><label for="color">Cor no calendário</label><input id="color" type="color" value="${esc(j.color)}"></div><div><label for="preset">Modelo de escala</label><select id="preset" onchange="preset()">${presets.map((p, i) => `<option value="${i}" ${i === pi ? "selected" : ""}>${p[0]}</option>`).join("")}</select></div></div><div id="cycleFields"></div><div class="cols"><div><label for="anchor">Primeiro dia de trabalho</label><input type="date" id="anchor" value="${j.anchor}"></div><div><label for="start">Início do plantão</label><input type="time" id="start" value="${j.start}"></div></div><label for="offsets">Toques: minutos antes do início</label><input id="offsets" value="${j.offsets.map((n) => -n).join(", ")}" placeholder="60, 30, 0"><div class="sub">Ex.: 60, 30 = uma hora e meia hora antes. Negativo toca depois. Cada plantão mantém esses horários.</div><details><summary>Som e YouTube</summary><button class="secondary" onclick="Native.media('audio')">♫ Escolher áudio do celular</button><div id="audioLabel" class="media-label">${esc(j.audioName || "Som padrão do alarme + reserva offline")}</div><button class="tiny" onclick="draft.audio='';draft.audioName='';$('#audioLabel').textContent='Som padrão'">Restaurar som padrão</button><button class="secondary" onclick="Native.youtube()">▶ Selecionar no YouTube</button><label for="youtube">Link ou ID do vídeo (opcional)</label><input id="youtube" value="${esc(j.youtube)}" placeholder="https://youtube.com/watch?v=?"><div class="hint">O YouTube precisa de internet e tela desbloqueada. Se não iniciar, o áudio local toca automaticamente. Para acordar sem depender da rede, escolha também um arquivo do celular.</div></details><details><summary>Tela do alarme e soneca</summary><div class="cols"><button class="secondary" onclick="Native.media('image')">▧ Foto</button><button class="secondary" onclick="Native.media('video')">▷ Vídeo</button></div><div class="media-label" id="bgLabel">${esc(j.backgroundName || "Fundo padrão")}</div><button class="tiny" onclick="draft.background='';draft.backgroundType='';$('#bgLabel').textContent='Fundo padrão'">Remover fundo</button><label for="message">Mensagem na tela</label><input id="message" maxlength="120" value="${esc(j.message)}" placeholder="Uma nova jornada começa agora."><label for="snooze">Soneca (minutos)</label><input id="snooze" type="number" min="1" max="60" value="${j.snooze}"><label><input type="checkbox" id="vibrate" ${j.vibrate ? "checked" : ""}> Vibrar junto com o áudio</label></details><details><summary>Exceções e horários por dia</summary><p>As exceções retiram plantões que começam nas datas escolhidas.</p><label for="skip">Folgas extras / férias (datas separadas por vírgula)</label><textarea id="skip" placeholder="2026-12-24, 2026-12-25">${j.skip.join(", ")}</textarea><div id="dayTimes"></div></details><div class="hint">Confira o primeiro dia e o início de cada emprego. O ciclo de cada trabalho ? independente do outro.</div><button class="primary" onclick="saveEditor()">Salvar escala recorrente</button>${id ? `<button class="danger" onclick="removeJob()">Excluir esta escala</button>` : ""}`;
  cycleFields();
  $("#editor").showModal();
}
function cycleFields() {
  let j = draft;
  $("#cycleFields").innerHTML =
    j.mode === "weekly"
      ? `<label>Dias da semana</label><div class="dayspick">${names.map((n, i) => `<label>${n}<input class="wd" type="checkbox" value="${i + 1}" ${j.weekdays.includes(i + 1) ? "checked" : ""}></label>`).join("")}</div>`
      : j.mode === "pattern"
        ? `<label for="pattern">Sequência de dias: 1 trabalha, 0 folga</label><input id="pattern" value="${esc(j.pattern)}"><div class="sub">Ex.: 111100 = quatro dias trabalhando e dois de folga. Repete a partir da data inicial.</div><label for="rotation">Revezamento de turnos (opcional)</label><input id="rotation" placeholder="07:00, 07:00, 19:00, 19:00, -, -" value="${Object.keys(j.patternTimes || {}).length ? [...j.pattern].map((v, i) => (v === "0" ? "-" : j.patternTimes[i] || j.start)).join(", ") : ""}"><div class="sub">Um horário para cada dia do ciclo; “-” significa folga. Quando preenchido, substitui a sequência 1/0 acima.</div>`
        : `<div class="cols"><div><label for="work">Trabalho (${j.mode === "hours" ? "horas" : "dias"})</label><input id="work" type="number" min="1" value="${j.work}"></div><div><label for="rest">Folga (${j.mode === "hours" ? "horas" : "dias"})</label><input id="rest" type="number" min="0" value="${j.rest}"></div></div>`;
  if (j.mode !== "hours")
    $("#cycleFields").innerHTML +=
      `<label for="duration">Duração de cada plantão (horas)</label><input id="duration" type="number" min="1" max="168" value="${j.duration}">`;
  $("#dayTimes").innerHTML =
    j.mode === "weekly"
      ? "<p>Opcional: início diferente em cada dia da semana.</p>" +
        names
          .map(
            (n, i) =>
              `<div class="row"><label>${n}</label><input style="width:150px" type="time" class="dt" data-day="${i + 1}" value="${j.dayTimes[i + 1] || ""}"></div>`,
          )
          .join("")
      : "<p>Para horários fixos diferentes por dia da semana, use o modelo “Dias fixos da semana”.</p>";
}
function preset() {
  let p = presets[+$("#preset").value];
  draft.mode = p[1];
  draft.work = p[2];
  draft.rest = p[3];
  cycleFields();
}
function closeEditor() {
  $("#editor").close();
  draft = null;
  refresh();
}
function saveEditor() {
  let j = draft;
  try {
    j.name = $("#name").value.trim();
    j.color = $("#color").value;
    j.anchor = $("#anchor").value;
    j.start = $("#start").value;
    j.offsets = $("#offsets")
      .value.split(",")
      .map((v) => {
        if (!/^-?\d+$/.test(v.trim()))
          throw Error("Digite os minutos separados por vírgula");
        return -Number(v.trim());
      });
    if (!j.name || !j.anchor || !j.start)
      throw Error("Preencha nome, data inicial e horário");
    if ($("#work")) {
      j.work = +$("#work").value;
      j.rest = +$("#rest").value;
    }
    if ($("#duration")) j.duration = +$("#duration").value;
    if ($("#pattern")) {
      j.pattern = $("#pattern").value.trim();
      j.patternTimes = {};
      let rot = $("#rotation").value.trim();
      if (rot) {
        let parts = rot.split(",").map((v) => v.trim());
        j.pattern = parts
          .map((v, i) => {
            if (v === "-") return "0";
            if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(v))
              throw Error("Use HH:mm ou - no revezamento");
            j.patternTimes[i] = v;
            return "1";
          })
          .join("");
      }
    }
    if (j.mode === "weekly") {
      j.weekdays = [...document.querySelectorAll(".wd:checked")].map(
        (e) => +e.value,
      );
      j.dayTimes = {};
      document.querySelectorAll(".dt").forEach((e) => {
        if (e.value) j.dayTimes[e.dataset.day] = e.value;
      });
    }
    j.skip = $("#skip")
      .value.split(",")
      .map((x) => x.trim())
      .filter(Boolean);
    j.snooze = +$("#snooze").value;
    if (!Number.isInteger(j.snooze) || j.snooze < 1 || j.snooze > 60)
      throw Error("Soneca deve ter entre 1 e 60 minutos");
    j.message = $("#message").value;
    j.vibrate = $("#vibrate").checked;
    let yt = $("#youtube").value.trim();
    if (yt && !/^[a-zA-Z0-9_-]{11}$/.test(yt)) {
      let u = new URL(yt);
      yt =
        u.hostname === "youtu.be"
          ? u.pathname.slice(1)
          : u.hostname === "youtube.com" || u.hostname.endsWith(".youtube.com")
            ? u.searchParams.get("v") || u.pathname.split("/").pop()
            : "";
    }
    if (yt && !/^[a-zA-Z0-9_-]{11}$/.test(yt))
      throw Error("Link de vídeo inválido");
    j.youtube = yt;
    let jobs = data.jobs.filter((x) => x.id !== j.id);
    jobs.push(j);
    let result = Native.save(JSON.stringify({ jobs, theme: data.theme }));
    if (result !== "ok") throw Error(result);
    closeEditor();
    toast("Escala salva. Os próximos plantões serão automáticos.");
  } catch (e) {
    toast(e.message);
  }
}
function removeJob() {
  if (confirm("Excluir esta escala e todos os seus próximos alarmes?")) {
    data.jobs = data.jobs.filter((j) => j.id !== draft.id);
    if (save()) closeEditor();
  }
}
function onMedia(o) {
  if (!draft) {
    toast("Arquivo importado. Abra uma escala para associá-lo.");
    return;
  }
  if (o.kind === "audio") {
    draft.audio = o.file;
    draft.audioName = o.name;
    $("#audioLabel").textContent = o.name;
  } else {
    draft.background = o.file;
    draft.backgroundType = o.kind;
    draft.backgroundName = o.name;
    $("#bgLabel").textContent = o.name;
  }
  toast("Mídia importada. Salve a escala para aplicar.");
}
function onYoutube(o) {
  if (draft) {
    draft.youtube = o.id;
    $("#youtube").value = o.id;
    toast("Vídeo selecionado. Salve a escala.");
  }
}
function calendar() {
  let m = dateISO(new Date(month.getFullYear(), month.getMonth(), 1)).slice(
      0,
      7,
    ),
    cells = JSON.parse(Native.calendar(m));
  let first =
    (new Date(month.getFullYear(), month.getMonth(), 1).getDay() + 6) % 7;
  return `<div class="eyebrow">UM OLHAR PARA O MÊS</div><h1>Seu calendário.</h1><p>Cada cor, um trabalho. Dias com dois pontos têm plantões dos dois empregos.</p><div class="row calendar-head"><button aria-label="Mês anterior" onclick="moveMonth(-1)">‹</button><h2 style="margin:0;text-transform:capitalize">${month.toLocaleDateString("pt-BR", { month: "long", year: "numeric" })}</h2><button aria-label="Próximo mês" onclick="moveMonth(1)">›</button></div><div class="grid">${names.map((n) => `<div class="weekday">${n}</div>`).join("")}${"<div></div>".repeat(first)}${cells.map((c) => `<button class="day ${c.date === dateISO(new Date()) ? "today" : ""}" onclick="day('${c.date}')">${+c.date.slice(-2)}<div class="marks">${c.jobs.map((id) => `<i class="mark" style="background:${esc(data.jobs.find((j) => j.id === id)?.color)}"></i>`).join("")}</div></button>`).join("")}</div><div class="legend">${data.jobs.map((j) => `<span style="--job:${esc(j.color)}"><i class="dot"></i>${esc(j.name)}${j.enabled ? "" : " (alarme pausado)"}</span>`).join("")}</div><div class="hint">O calendário mostra também a continuação de plantões que atravessam a meia-noite. Toque em um dia para ver os trabalhos.</div><div id="dayDetail"></div>${!data.jobs.length ? '<button class="primary" onclick="edit()">Adicionar primeiro trabalho</button>' : ""}`;
}
function moveMonth(n) {
  month = new Date(month.getFullYear(), month.getMonth() + n, 1);
  render();
}
function day(d) {
  let c = JSON.parse(Native.calendar(d.slice(0, 7))).find((c) => c.date === d);
  $("#dayDetail").innerHTML = `<h2>${d.split("-").reverse().join("/")}</h2>${
    c.jobs.length
      ? c.jobs
          .map((id) => {
            let j = data.jobs.find((j) => j.id === id);
            return `<div class="card job" style="--job:${esc(j.color)}"><h3>${esc(j.name)}</h3><p>${esc(describe(j))}</p><button class="tiny" onclick="edit('${j.id}')">Editar escala ?</button></div>`;
          })
          .join("")
      : '<div class="card">Dia livre de plantões cadastrados.</div>'
  }`;
}
function appearance() {
  return `<div class="eyebrow">COM A SUA IDENTIDADE</div><h1>Faça do seu jeito.</h1><p>Preto e vermelho por essência. Todas as cores podem mudar com você.</p><div class="preview"><span class="eyebrow" style="color:white">SEU PRÓXIMO COMEÇO</span><strong>06:00</strong><span>GATARME</span></div><h2>Escolha sua energia</h2><div class="swatches">${["#ff304f", "#b18aff", "#55b8ff", "#43d9a3", "#ffad42", "#e65ba6"].map((c) => `<button aria-label="Cor ${c}" class="swatch" style="background:${c}" onclick="setColor('accent','${c}')"></button>`).join("")}</div><div class="card">${[
    ["accent", "Cor de destaque", "#ff304f"],
    ["bg", "Fundo do aplicativo", "#09090b"],
    ["panel", "Cartões", "#17171c"],
    ["text", "Texto principal", "#fafafa"],
    ["muted", "Texto secundário", "#a1a1aa"],
  ]
    .map(
      ([k, n, c]) =>
        `<div class="row"><label for="theme-${k}">${n}</label><input id="theme-${k}" type="color" value="${data.theme[k] || c}" onchange="setColor('${k}',this.value)"></div>`,
    )
    .join(
      "",
    )}</div><div class="hint">Fotos, vídeos e mensagens da tela de despertar são individuais: Alarmes ? Editar ↗ Tela do alarme e soneca.</div><button class="secondary" onclick="data.theme={accent:'#ff304f',bg:'#09090b',panel:'#17171c',text:'#fafafa',muted:'#a1a1aa'};save()">Restaurar preto e vermelho</button>`;
}
function setColor(k, v) {
  data.theme[k] = v;
  save();
}
function settings() {
  let s = data.status;
  const rows = [
    ["exact", "Alarmes exatos", "Horário preciso, mesmo em repouso.", s.exact],
    [
      "notifications",
      "Notificações",
      "Controles para adiar e abrir o alarme.",
      s.notifications && s.channel,
    ],
    [
      "fullscreen",
      "Tela cheia",
      "Mostrar o alarme na tela bloqueada.",
      s.fullscreen,
    ],
    [
      "overlay",
      "Abrir sobre outros apps",
      "Abrir a tela do alarme também com o celular desbloqueado.",
      s.overlay,
    ],
    [
      "battery",
      "Bateria sem restrições",
      "Evitar suspensão do app pelo Android.",
      s.battery,
    ],
    [
      "volume",
      "Volume do alarme",
      "Volume atual: " + s.volume + ". Ajuste e teste.",
      s.volume > 0,
    ],
    [
      "dnd",
      "Não perturbe (opcional)",
      "Confira se alarmes são permitidos no seu modo.",
      s.dnd,
    ],
  ];
  return `<div class="eyebrow">PRONTO PARA DESPERTAR</div><h1>Proteja sua rotina.</h1><p>Autorize as funções abaixo e faça um teste com a tela bloqueada.</p><div class="card">${rows.map(([k, n, d, ok]) => `<div class="permission ${ok ? "ok" : ""}"><span class="indicator">${ok ? "✓" : "!"}</span><div class="info"><h3 style="font-size:14px">${n}</h3><div class="sub">${d}</div></div><button onclick="Native.permission('${k}')">${ok ? "Ajustar" : "Permitir"}</button></div>`).join("")}</div><button class="primary" onclick="Native.test()">◷ Testar alarme em 15 segundos</button><div class="hint warning">No Samsung: Ajustes → Assistência do aparelho → Bateria → Limites de uso em segundo plano → Aplicativos nunca suspensos. Adicione o GATARME.<br><br>Nenhum app toca com o celular desligado, sem bateria ou após “Forçar parada”. Se isso ocorrer, abra o GATARME novamente. Depois de reiniciar, os alarmes são restaurados automaticamente.</div><button class="secondary" onclick="Native.permission('app')">Abrir configurações do GATARME</button><details><summary>Última atividade do alarme</summary><p style="overflow-wrap:anywhere">${esc(s.event)}</p></details><div class="section"><h2>Privacidade, por padrão.</h2><p>Escalas e arquivos ficam neste aparelho. Sem conta, anúncios ou rastreamento próprio. Somente o YouTube usa a internet quando você abre a seleção ou reproduz um vídeo.</p><div class="sub">GATARME 1.0.1 · Feito para jornadas reais.</div></div>`;
}
function goBack() {
  if ($("#editor").open) closeEditor();
  else tab("alarms");
}
refresh();

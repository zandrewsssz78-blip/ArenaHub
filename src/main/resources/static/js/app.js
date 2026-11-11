/* ==========================================================
   ArenaHub — frontend (JavaScript sin frameworks)
   Consume la API REST de Spring Boot del mismo servidor:
     HU1  POST  /api/usuarios
     HU2  POST  /api/usuarios/login
     HU3  GET   /api/escenarios/disponibilidad?fecha=
     HU4  POST  /api/reservas
     HU5  PATCH /api/reservas/{id}/cancelar
     HU6  GET   /api/reservas?usuarioId=
     HU7  POST  /api/escenarios
     HU8  PATCH /api/escenarios/{id}/estado
   ========================================================== */
(() => {
  "use strict";

  const CLAVE_SESION = "arenahub.sesion";
  const DIAS_VISIBLES = 14;

  const TIPOS = {
    FUTBOL: "Fútbol", FUTBOL_5: "Fútbol 5", BALONCESTO: "Baloncesto",
    VOLEIBOL: "Voleibol", TENIS: "Tenis", OTRO: "Otro"
  };
  const ESTADOS_ESCENARIO = { ACTIVO: "Activo", EN_MANTENIMIENTO: "En mantenimiento", INACTIVO: "Inactivo" };
  const ESTADOS_RESERVA = { CONFIRMADA: "Confirmada", CANCELADA: "Cancelada", PENDIENTE: "Pendiente", RECHAZADA: "Rechazada" };

  /* Plano visto desde arriba de cada tipo de escenario (viewBox 0 0 120 72). */
  const PLANOS = {
    FUTBOL: `<rect x="3" y="3" width="114" height="66"/><line x1="60" y1="3" x2="60" y2="69"/><circle cx="60" cy="36" r="11"/><rect x="3" y="18" width="18" height="36"/><rect x="99" y="18" width="18" height="36"/><rect x="3" y="27" width="7" height="18"/><rect x="110" y="27" width="7" height="18"/>`,
    FUTBOL_5: `<rect x="3" y="3" width="114" height="66" rx="4"/><line x1="60" y1="3" x2="60" y2="69"/><circle cx="60" cy="36" r="9"/><path d="M3 18 a20 18 0 0 1 0 36"/><path d="M117 18 a20 18 0 0 0 0 36"/>`,
    BALONCESTO: `<rect x="3" y="3" width="114" height="66"/><line x1="60" y1="3" x2="60" y2="69"/><circle cx="60" cy="36" r="9"/><rect x="3" y="26" width="22" height="20"/><rect x="95" y="26" width="22" height="20"/><path d="M3 9 h8 a28 27 0 0 1 0 54 h-8"/><path d="M117 9 h-8 a28 27 0 0 0 0 54 h8"/>`,
    VOLEIBOL: `<rect x="3" y="3" width="114" height="66"/><line x1="60" y1="0" x2="60" y2="72" stroke-width="3"/><line x1="40" y1="3" x2="40" y2="69"/><line x1="80" y1="3" x2="80" y2="69"/>`,
    TENIS: `<rect x="3" y="3" width="114" height="66"/><line x1="3" y1="11" x2="117" y2="11"/><line x1="3" y1="61" x2="117" y2="61"/><line x1="60" y1="0" x2="60" y2="72" stroke-width="3"/><line x1="32" y1="11" x2="32" y2="61"/><line x1="88" y1="11" x2="88" y2="61"/><line x1="32" y1="36" x2="88" y2="36"/>`,
    OTRO: `<rect x="3" y="3" width="114" height="66" rx="8"/><rect x="14" y="14" width="92" height="44" rx="4" stroke-dasharray="4 4"/>`
  };
  const plano = (tipo) =>
    `<svg class="escenario__plano" viewBox="0 0 120 72" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-width="2">${PLANOS[tipo] || PLANOS.OTRO}</g></svg>`;

  /* ---------------- Utilidades ---------------- */
  const $ = (sel, raiz = document) => raiz.querySelector(sel);
  const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const pad = (n) => String(n).padStart(2, "0");
  const isoLocal = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const hoy = () => isoLocal(new Date());
  const hora = (t) => (t || "").slice(0, 5);                  // "18:00:00" -> "18:00"
  const deIso = (s) => { const [a, m, d] = s.split("-").map(Number); return new Date(a, m - 1, d); };
  const fechaLarga = (s) => deIso(s).toLocaleDateString("es-CO", { weekday: "long", day: "numeric", month: "long" });

  const estado = {
    sesion: null,
    fecha: hoy(),
    tipo: "TODOS",
    disponibilidad: [],
    misReservas: []
  };

  /* ---------------- API ---------------- */
  class ErrorApi extends Error {
    constructor(status, mensaje) { super(mensaje); this.status = status; }
  }

  async function api(metodo, ruta, cuerpo) {
    const opciones = { method: metodo, headers: { Accept: "application/json" } };
    if (cuerpo !== undefined) {
      opciones.headers["Content-Type"] = "application/json";
      opciones.body = JSON.stringify(cuerpo);
    }
    if (estado.sesion?.token) opciones.headers.Authorization = `Bearer ${estado.sesion.token}`;

    let respuesta;
    try {
      respuesta = await fetch(ruta, opciones);
    } catch {
      throw new ErrorApi(0, "No hay conexión con el servidor. Revisa que la aplicación esté corriendo.");
    }
    const texto = await respuesta.text();
    const datos = texto ? JSON.parse(texto) : null;
    if (!respuesta.ok) throw new ErrorApi(respuesta.status, datos?.mensaje || `Error ${respuesta.status}`);
    return datos;
  }

  /* ---------------- Aviso y diálogo ---------------- */
  let temporizadorAviso;
  function avisar(texto) {
    const aviso = $("#aviso");
    aviso.textContent = texto;
    aviso.hidden = false;
    clearTimeout(temporizadorAviso);
    temporizadorAviso = setTimeout(() => (aviso.hidden = true), 3500);
  }

  const dialogo = $("#dialogo");
  function abrirDialogo({ titulo, cuerpo, acciones, error = false }) {
    $("#dialogo-cuerpo").innerHTML = `
      <div class="dialogo__cabeza ${error ? "dialogo__cabeza--error" : ""}"><h2 id="dialogo-titulo">${esc(titulo)}</h2></div>
      <div class="dialogo__cuerpo">${cuerpo}</div>
      <div class="dialogo__acciones"></div>`;
    const caja = $(".dialogo__acciones", dialogo);
    acciones.forEach(({ texto, clase = "boton--secundario", alHacer }) => {
      const b = document.createElement("button");
      b.type = "button";
      b.className = `boton ${clase}`;
      b.textContent = texto;
      b.addEventListener("click", async () => {
        if (!alHacer) return dialogo.close();
        b.disabled = true;
        try { await alHacer(b); } finally { b.disabled = false; }
      });
      caja.appendChild(b);
    });
    dialogo.showModal();
    (caja.lastElementChild || caja).focus();
  }
  dialogo.addEventListener("click", (e) => { if (e.target === dialogo) dialogo.close(); });

  function mostrarError(form, mensaje) {
    const caja = $(".formulario__error", form);
    caja.textContent = mensaje;
    caja.hidden = !mensaje;
  }

  /* ---------------- Sesión ---------------- */
  function cargarSesion() {
    try { estado.sesion = JSON.parse(localStorage.getItem(CLAVE_SESION)); } catch { estado.sesion = null; }
  }
  function guardarSesion(sesion) {
    estado.sesion = sesion;
    try { localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion)); } catch { /* sin almacenamiento */ }
  }
  function cerrarSesion() {
    estado.sesion = null;
    try { localStorage.removeItem(CLAVE_SESION); } catch { /* nada */ }
    location.hash = "";
    pintar();
  }
  const esAdmin = () => estado.sesion?.usuario?.rol === "ADMINISTRADOR";

  /* ---------------- Acceso: login y registro ---------------- */
  function cambiarPestana(cual) {
    const login = cual === "login";
    $("#tab-login").setAttribute("aria-selected", login);
    $("#tab-registro").setAttribute("aria-selected", !login);
    $("#form-login").hidden = !login;
    $("#form-registro").hidden = login;
    $(login ? "#form-login input" : "#form-registro input").focus();
  }
  $("#tab-login").addEventListener("click", () => cambiarPestana("login"));
  $("#tab-registro").addEventListener("click", () => cambiarPestana("registro"));

  async function iniciarSesion(email, contrasena) {
    const r = await api("POST", "/api/usuarios/login", { email, contrasena });
    guardarSesion({ token: r.token, usuario: r.usuario });
    location.hash = "#/reservar";
    pintar();
  }

  $("#form-login").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    const datos = Object.fromEntries(new FormData(form));
    if (!datos.email || !datos.contrasena) return mostrarError(form, "Escribe tu correo y tu contraseña.");
    const boton = $("button[type=submit]", form);
    boton.disabled = true;
    try {
      await iniciarSesion(datos.email.trim(), datos.contrasena);
      form.reset();
      mostrarError(form, "");
    } catch (err) {
      mostrarError(form, err.status === 401 ? "El correo o la contraseña no coinciden." : err.message);
    } finally { boton.disabled = false; }
  });

  $("#form-registro").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    const datos = Object.fromEntries(new FormData(form));
    if (!datos.nombre?.trim() || !datos.email?.trim()) return mostrarError(form, "Completa tu nombre y tu correo.");
    if (!form.email.checkValidity()) return mostrarError(form, "Ese correo no parece válido.");
    if ((datos.contrasena || "").length < 8) return mostrarError(form, "La contraseña debe tener al menos 8 caracteres.");
    const boton = $("button[type=submit]", form);
    boton.disabled = true;
    try {
      await api("POST", "/api/usuarios", { nombre: datos.nombre.trim(), email: datos.email.trim(), contrasena: datos.contrasena });
      await iniciarSesion(datos.email.trim(), datos.contrasena);
      form.reset();
      mostrarError(form, "");
      avisar(`Cuenta creada. Bienvenido, ${datos.nombre.trim().split(" ")[0]}.`);
    } catch (err) {
      mostrarError(form, err.status === 409 ? "Ya existe una cuenta con ese correo. Inicia sesión." : err.message);
    } finally { boton.disabled = false; }
  });

  $("#btn-salir").addEventListener("click", cerrarSesion);

  /* ---------------- Ruteo ---------------- */
  const RUTAS = ["reservar", "mis-reservas", "admin"];
  function rutaActual() {
    const r = location.hash.replace(/^#\//, "");
    if (r === "admin" && !esAdmin()) return "reservar";
    return RUTAS.includes(r) ? r : "reservar";
  }

  function pintar() {
    const conSesion = !!estado.sesion;
    $("#vista-acceso").hidden = conSesion;
    $("#vista-app").hidden = !conSesion;
    if (!conSesion) return;

    const u = estado.sesion.usuario;
    $(".usuario__nombre").textContent = u.nombre;
    $(".usuario__inicial").textContent = (u.nombre || "?").trim().charAt(0).toUpperCase();
    $("#nav-admin").hidden = !esAdmin();

    const ruta = rutaActual();
    RUTAS.forEach((r) => ($(`#ruta-${r}`).hidden = r !== ruta));
    document.querySelectorAll(".nav a").forEach((a) =>
      a.dataset.ruta === ruta ? a.setAttribute("aria-current", "page") : a.removeAttribute("aria-current"));

    if (ruta === "reservar") vistaReservar();
    if (ruta === "mis-reservas") vistaMisReservas();
    if (ruta === "admin") vistaAdmin();
  }
  window.addEventListener("hashchange", pintar);

  /* ==========================================================
     HU3 + HU4 — Reservar
     ========================================================== */
  function pintarDias() {
    const caja = $("#dias");
    const base = new Date();
    let html = "";
    for (let i = 0; i < DIAS_VISIBLES; i++) {
      const d = new Date(base.getFullYear(), base.getMonth(), base.getDate() + i);
      const iso = isoLocal(d);
      const semana = i === 0 ? "Hoy" : d.toLocaleDateString("es-CO", { weekday: "short" }).replace(".", "");
      const mes = d.toLocaleDateString("es-CO", { month: "short" }).replace(".", "");
      html += `<button type="button" class="dia" role="radio" aria-checked="${iso === estado.fecha}" data-fecha="${iso}"
                 aria-label="${esc(fechaLarga(iso))}">
                 <span class="dia__semana">${esc(semana)}</span>
                 <span class="dia__numero">${d.getDate()}</span>
                 <span class="dia__mes">${esc(mes)}</span>
               </button>`;
    }
    caja.innerHTML = html;
  }
  $("#dias").addEventListener("click", (e) => {
    const b = e.target.closest(".dia");
    if (!b) return;
    estado.fecha = b.dataset.fecha;
    pintarDias();
    cargarDisponibilidad();
  });

  function pintarFiltros() {
    const tiposPresentes = [...new Set(estado.disponibilidad.map((e) => e.tipo))];
    const opciones = ["TODOS", ...Object.keys(TIPOS).filter((t) => tiposPresentes.includes(t))];
    if (!opciones.includes(estado.tipo)) estado.tipo = "TODOS";
    $("#filtros").innerHTML = opciones.map((t) =>
      `<button type="button" class="filtro" role="radio" aria-checked="${t === estado.tipo}" data-tipo="${t}">
         ${t === "TODOS" ? "Todos" : esc(TIPOS[t])}
       </button>`).join("");
  }
  $("#filtros").addEventListener("click", (e) => {
    const b = e.target.closest(".filtro");
    if (!b) return;
    estado.tipo = b.dataset.tipo;
    pintarFiltros();
    pintarEscenarios();
  });

  async function vistaReservar() {
    pintarDias();
    await cargarDisponibilidad();
  }

  async function cargarDisponibilidad() {
    const lista = $("#lista-escenarios");
    lista.innerHTML = `<p class="cargando">Buscando franjas libres…</p>`;
    try {
      const [disp, mias] = await Promise.all([
        api("GET", `/api/escenarios/disponibilidad?fecha=${estado.fecha}`),
        api("GET", `/api/reservas?usuarioId=${estado.sesion.usuario.id}`)
      ]);
      estado.disponibilidad = disp;
      estado.misReservas = mias;
      pintarFiltros();
      pintarEscenarios();
    } catch (err) {
      lista.innerHTML = `<div class="vacio"><strong>No se pudo cargar la disponibilidad</strong><p>${esc(err.message)}</p>
        <button class="boton boton--secundario" type="button" data-reintentar>Intentar de nuevo</button></div>`;
      $("[data-reintentar]", lista).addEventListener("click", cargarDisponibilidad);
    }
  }

  function franjaYaPaso(franja) {
    if (estado.fecha !== hoy()) return false;
    const ahora = new Date();
    const [h, m] = franja.horaInicio.split(":").map(Number);
    return h * 60 + m <= ahora.getHours() * 60 + ahora.getMinutes();
  }

  function pintarEscenarios() {
    const lista = $("#lista-escenarios");
    const visibles = estado.disponibilidad.filter((e) => estado.tipo === "TODOS" || e.tipo === estado.tipo);

    if (!visibles.length) {
      lista.innerHTML = `<div class="vacio"><strong>No hay escenarios activos</strong>
        <p>Cuando un administrador active un escenario, aparecerá aquí para reservar.</p></div>`;
      return;
    }

    const mias = new Set(estado.misReservas
      .filter((r) => r.estado === "CONFIRMADA" && r.fecha === estado.fecha)
      .map((r) => `${r.escenarioId}-${r.franjaId}`));

    lista.innerHTML = visibles.map((esc_) => {
      let libres = 0;
      const franjas = esc_.franjas.map((f) => {
        const tuya = mias.has(`${esc_.id}-${f.franjaId}`);
        const pasada = !tuya && franjaYaPaso(f);
        let clase = "", texto = "Libre", deshabilitada = false;
        if (tuya) { clase = "franja--tuya"; texto = "Tuya"; deshabilitada = true; }
        else if (f.ocupada) { clase = "franja--ocupada"; texto = "Ocupada"; deshabilitada = true; }
        else if (pasada) { clase = "franja--pasada"; texto = "Ya pasó"; deshabilitada = true; }
        else libres++;
        return `<button type="button" class="franja ${clase}" ${deshabilitada ? "disabled" : ""}
                  data-escenario="${esc_.id}" data-franja="${f.franjaId}"
                  aria-label="${hora(f.horaInicio)} a ${hora(f.horaFin)}, ${texto}">
                  <span class="franja__hora">${hora(f.horaInicio)}</span>
                  <span class="franja__estado">${texto}</span>
                </button>`;
      }).join("");

      return `<article class="escenario">
        <div class="escenario__ficha">
          ${plano(esc_.tipo)}
          <div>
            <h2 class="escenario__nombre">${esc(esc_.nombre)}</h2>
            <p class="escenario__datos">
              <span>${esc(esc_.ubicacion)}</span>
              ${esc_.capacidad ? `<span>Hasta ${esc_.capacidad} personas</span>` : ""}
            </p>
          </div>
          <span class="escenario__tipo">${esc(TIPOS[esc_.tipo] || esc_.tipo)}</span>
        </div>
        <div class="escenario__franjas">
          <p class="escenario__resumen">${libres
            ? `<strong>${libres}</strong> ${libres === 1 ? "franja libre" : "franjas libres"} el ${esc(fechaLarga(estado.fecha))}`
            : `Sin franjas libres el ${esc(fechaLarga(estado.fecha))}`}</p>
          <div class="franjas">${franjas}</div>
        </div>
      </article>`;
    }).join("");
  }

  $("#lista-escenarios").addEventListener("click", (e) => {
    const b = e.target.closest(".franja:not(:disabled)");
    if (!b) return;
    const escenario = estado.disponibilidad.find((x) => x.id === Number(b.dataset.escenario));
    const franja = escenario?.franjas.find((f) => f.franjaId === Number(b.dataset.franja));
    if (escenario && franja) confirmarReserva(escenario, franja);
  });

  function confirmarReserva(escenario, franja) {
    const rango = `${hora(franja.horaInicio)} – ${hora(franja.horaFin)}`;
    abrirDialogo({
      titulo: "Confirma tu reserva",
      cuerpo: `<dl class="resumen">
          <dt>Escenario</dt><dd>${esc(escenario.nombre)}</dd>
          <dt>Lugar</dt><dd>${esc(escenario.ubicacion)}</dd>
          <dt>Día</dt><dd class="capital">${esc(fechaLarga(estado.fecha))}</dd>
          <dt>Hora</dt><dd class="grande">${rango}</dd>
        </dl>`,
      acciones: [
        { texto: "Volver" },
        {
          texto: `Reservar ${hora(franja.horaInicio)}`, clase: "boton--principal",
          alHacer: async () => {
            try {
              const r = await api("POST", "/api/reservas", {
                usuarioId: estado.sesion.usuario.id,
                escenarioId: escenario.id,
                fecha: estado.fecha,
                franjaId: franja.franjaId
              });
              dialogo.close();
              reservaConfirmada(r);
            } catch (err) {
              dialogo.close();
              reservaFallida(err);
            }
            cargarDisponibilidad();
          }
        }
      ]
    });
  }

  function reservaConfirmada(r) {
    abrirDialogo({
      titulo: "Reserva confirmada",
      cuerpo: `<p>${esc(r.escenarioNombre)}, ${esc(fechaLarga(r.fecha))} de ${hora(r.horaInicio)} a ${hora(r.horaFin)}.</p>
               <p>Presenta este código en la entrada:</p>
               <p class="codigo-grande">${esc(r.codigoReserva)}</p>`,
      acciones: [
        { texto: "Ver mis reservas", alHacer: () => { dialogo.close(); location.hash = "#/mis-reservas"; } },
        { texto: "Listo", clase: "boton--principal" }
      ]
    });
  }

  function reservaFallida(err) {
    // 409: el índice único parcial de PostgreSQL rechazó la reserva (problema duro de concurrencia)
    const ganada = err.status === 409;
    abrirDialogo({
      titulo: ganada ? "Alguien reservó primero" : "No se pudo reservar",
      error: true,
      cuerpo: `<p>${ganada
        ? "Otra persona confirmó esa misma franja hace un momento. Ya actualizamos la disponibilidad: elige otra hora."
        : esc(err.message)}</p>`,
      acciones: [{ texto: "Elegir otra hora", clase: "boton--principal" }]
    });
  }

  /* ==========================================================
     HU5 + HU6 — Mis reservas
     ========================================================== */
  async function vistaMisReservas() {
    const prox = $("#reservas-proximas");
    const hist = $("#reservas-historial");
    prox.innerHTML = `<p class="cargando">Cargando tus reservas…</p>`;
    hist.innerHTML = "";
    try {
      estado.misReservas = await api("GET", `/api/reservas?usuarioId=${estado.sesion.usuario.id}`);
    } catch (err) {
      prox.innerHTML = `<div class="vacio"><strong>No se pudieron cargar tus reservas</strong><p>${esc(err.message)}</p></div>`;
      return;
    }
    const h = hoy();
    const orden = (a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio);
    const proximas = estado.misReservas.filter((r) => r.estado === "CONFIRMADA" && r.fecha >= h).sort(orden);
    const historial = estado.misReservas.filter((r) => !proximas.includes(r)).sort((a, b) => orden(b, a));

    prox.innerHTML = proximas.length
      ? proximas.map((r) => `
        <article class="tiquete">
          <div class="tiquete__cuerpo">
            <h3 class="tiquete__escenario">${esc(r.escenarioNombre)}</h3>
            <p class="tiquete__fecha">${esc(fechaLarga(r.fecha))}</p>
            <p class="tiquete__hora">${hora(r.horaInicio)} – ${hora(r.horaFin)}</p>
          </div>
          <div class="tiquete__talon">
            <div>
              <p class="tiquete__codigo-titulo">Código</p>
              <p class="tiquete__codigo">${esc(r.codigoReserva)}</p>
            </div>
            <button class="boton boton--peligro boton--chico" type="button" data-cancelar="${r.id}">Cancelar</button>
          </div>
        </article>`).join("")
      : `<div class="vacio"><strong>No tienes reservas próximas</strong>
           <p>Busca una franja libre y apártala en un clic.</p>
           <a class="boton boton--principal" href="#/reservar">Reservar una cancha</a></div>`;

    hist.innerHTML = historial.length
      ? historial.map((r) => `
        <div class="historial__fila">
          <strong>${esc(r.escenarioNombre)}</strong>
          <span>${esc(fechaLarga(r.fecha))}</span>
          <span>${hora(r.horaInicio)} – ${hora(r.horaFin)}</span>
          <span class="estado estado--${esc(r.estado)}">${esc(ESTADOS_RESERVA[r.estado] || r.estado)}</span>
        </div>`).join("")
      : `<p class="historial__fila"><span>Aquí verás tus reservas pasadas y canceladas.</span></p>`;
  }

  $("#reservas-proximas").addEventListener("click", (e) => {
    const b = e.target.closest("[data-cancelar]");
    if (!b) return;
    const r = estado.misReservas.find((x) => x.id === Number(b.dataset.cancelar));
    if (!r) return;
    abrirDialogo({
      titulo: "¿Cancelar esta reserva?",
      error: true,
      cuerpo: `<p>${esc(r.escenarioNombre)}, ${esc(fechaLarga(r.fecha))} de ${hora(r.horaInicio)} a ${hora(r.horaFin)}.</p>
               <p>La franja quedará libre para otras personas.</p>`,
      acciones: [
        { texto: "Conservarla" },
        {
          texto: "Cancelar reserva", clase: "boton--peligro",
          alHacer: async () => {
            try {
              await api("PATCH", `/api/reservas/${r.id}/cancelar`, { usuarioId: estado.sesion.usuario.id });
              dialogo.close();
              avisar("Reserva cancelada. La franja quedó libre.");
            } catch (err) {
              dialogo.close();
              avisar(err.message);
            }
            vistaMisReservas();
          }
        }
      ]
    });
  });

  /* ==========================================================
     HU7 + HU8 — Administración de escenarios
     ========================================================== */
  const selTipo = $("#form-escenario select[name=tipo]");
  selTipo.innerHTML = Object.entries(TIPOS).map(([v, t]) => `<option value="${v}">${esc(t)}</option>`).join("");

  async function vistaAdmin() {
    const cuerpo = $("#tabla-escenarios");
    cuerpo.innerHTML = `<tr><td colspan="4" class="cargando">Cargando escenarios…</td></tr>`;
    try {
      const escenarios = await api("GET", "/api/escenarios");
      cuerpo.innerHTML = escenarios.length ? escenarios.map((e) => `
        <tr>
          <td><strong>${esc(e.nombre)}</strong><small>${esc(e.ubicacion)}</small></td>
          <td>${esc(TIPOS[e.tipo] || e.tipo)}</td>
          <td>${e.capacidad ?? "—"}</td>
          <td>
            <label class="oculto" for="estado-${e.id}">Estado de ${esc(e.nombre)}</label>
            <select id="estado-${e.id}" data-escenario="${e.id}" data-estado="${e.estado}">
              ${Object.entries(ESTADOS_ESCENARIO).map(([v, t]) =>
                `<option value="${v}" ${v === e.estado ? "selected" : ""}>${esc(t)}</option>`).join("")}
            </select>
          </td>
        </tr>`).join("")
        : `<tr><td colspan="4">Aún no hay escenarios. Registra el primero con el formulario.</td></tr>`;
    } catch (err) {
      cuerpo.innerHTML = `<tr><td colspan="4">${esc(err.message)}</td></tr>`;
    }
  }

  $("#tabla-escenarios").addEventListener("change", async (e) => {
    const sel = e.target.closest("select[data-escenario]");
    if (!sel) return;
    const anterior = sel.dataset.estado;
    sel.disabled = true;
    try {
      const r = await api("PATCH", `/api/escenarios/${sel.dataset.escenario}/estado`, { estado: sel.value });
      sel.dataset.estado = r.estado;
      avisar(`${r.nombre}: ${ESTADOS_ESCENARIO[r.estado].toLowerCase()}.`);
    } catch (err) {
      sel.value = anterior;
      avisar(err.message);
    } finally { sel.disabled = false; }
  });

  $("#form-escenario").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    const d = Object.fromEntries(new FormData(form));
    if (!d.nombre.trim() || !d.ubicacion.trim()) return mostrarError(form, "El nombre y la ubicación son obligatorios.");
    const capacidad = d.capacidad ? Number(d.capacidad) : null;
    if (capacidad !== null && !(capacidad > 0)) return mostrarError(form, "La capacidad debe ser mayor que cero.");
    const boton = $("button[type=submit]", form);
    boton.disabled = true;
    try {
      const r = await api("POST", "/api/escenarios", {
        nombre: d.nombre.trim(), tipo: d.tipo, ubicacion: d.ubicacion.trim(),
        capacidad, descripcion: d.descripcion.trim() || null
      });
      form.reset();
      mostrarError(form, "");
      avisar(`${r.nombre} registrado.`);
      vistaAdmin();
    } catch (err) {
      mostrarError(form, err.message);
    } finally { boton.disabled = false; }
  });

  /* ---------------- Inicio ---------------- */
  cargarSesion();
  pintar();
})();

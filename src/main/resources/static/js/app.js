/**
 * ArenaHub — Frontend Controller Modular
 * Arquitectura UI/UX Deportiva Profesional
 */

import { api } from './api.js';
import { auth } from './auth.js';

// Imágenes deportivas de alta resolución según el tipo de cancha
const SPORT_IMAGES = {
  FUTBOL_5: 'https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=600&auto=format&fit=crop&q=80',
  FUTBOL: 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=600&auto=format&fit=crop&q=80',
  BALONCESTO: 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=600&auto=format&fit=crop&q=80',
  TENIS: 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=600&auto=format&fit=crop&q=80',
  VOLEIBOL: 'https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=600&auto=format&fit=crop&q=80',
  DEFAULT: 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=600&auto=format&fit=crop&q=80'
};

// Formato legible de tipos de deporte
const SPORT_LABELS = {
  FUTBOL_5: 'Fútbol 5',
  FUTBOL: 'Fútbol 11',
  BALONCESTO: 'Baloncesto',
  TENIS: 'Tenis',
  VOLEIBOL: 'Voleibol',
  OTRO: 'Polideportivo'
};

// Estado centralizado de la aplicación
const AppState = {
  courts: [],
  selectedCourt: null,
  selectedSlot: null,
  pendingBookingPayload: null,
  filterDate: new Date().toISOString().split('T')[0]
};

// ==========================================================
// 1. SISTEMA DE NOTIFICACIONES TOAST
// ==========================================================
export function showNotification(message, type = 'success') {
  const container = document.getElementById('toast-stack-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast-item ${type === 'error' ? 'error' : ''}`;
  toast.textContent = message;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 350);
  }, 3500);
}

// ==========================================================
// 2. NAVEGACIÓN ENTRE VISTAS
// ==========================================================
export function navigateToView(viewTarget) {
  // Ocultar todas las secciones
  document.querySelectorAll('.view-section').forEach(sec => sec.classList.remove('active'));
  document.querySelectorAll('.nav-link').forEach(link => link.classList.remove('active'));

  // Cerrar menú móvil si está abierto
  document.getElementById('nav-main-menu')?.classList.remove('open');

  const heroSection = document.getElementById('hero-banner-section');

  if (viewTarget === 'view-inicio' || viewTarget === 'view-escenarios') {
    document.getElementById('view-escenarios-section')?.classList.add('active');
    document.querySelector('.nav-link[data-nav="view-inicio"]')?.classList.add('active');
    if (heroSection) heroSection.style.display = 'block';

    if (viewTarget === 'view-escenarios') {
      document.querySelector('.nav-link[data-nav="view-escenarios"]')?.classList.add('active');
      document.querySelector('.nav-link[data-nav="view-inicio"]')?.classList.remove('active');
      // Scroll suave hacia los filtros
      document.getElementById('view-escenarios-section')?.scrollIntoView({ behavior: 'smooth' });
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  } else if (viewTarget === 'view-reservas') {
    if (heroSection) heroSection.style.display = 'none';
    document.getElementById('view-reservas-section')?.classList.add('active');
    document.querySelector('.nav-link[data-nav="view-reservas"]')?.classList.add('active');
    loadUserReservations();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } else if (viewTarget === 'view-admin') {
    if (heroSection) heroSection.style.display = 'none';
    document.getElementById('view-admin-section')?.classList.add('active');
    document.querySelector('.nav-link[data-nav="view-admin"]')?.classList.add('active');
    loadAdminCourts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

// ==========================================================
// 3. INICIALIZACIÓN DE LA APLICACIÓN
// ==========================================================
document.addEventListener('DOMContentLoaded', () => {
  setupNavigationEvents();
  setupFilterControls();
  setupAuthEvents();
  setupModalEvents();
  setupAdminEvents();
  
  // Cargar disponibilidad inicial
  initDateFilter();
  loadCourtsWithAvailability();
});

// Configuración de la fecha por defecto (Hoy)
function initDateFilter() {
  const dateInput = document.getElementById('filter-booking-date');
  if (dateInput) {
    dateInput.value = AppState.filterDate;
    dateInput.min = AppState.filterDate;
  }
}

// Configuración de enlaces y botones de navegación
function setupNavigationEvents() {
  // Enlaces de navegación con atributo data-nav
  document.querySelectorAll('[data-nav]').forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      navigateToView(item.dataset.nav);
    });
  });

  // Botones del Hero
  document.getElementById('btn-hero-explore')?.addEventListener('click', () => {
    navigateToView('view-escenarios');
  });

  document.getElementById('btn-hero-bookings')?.addEventListener('click', () => {
    navigateToView('view-reservas');
  });

  // Menú móvil hamburguesa
  document.getElementById('mobile-menu-toggle')?.addEventListener('click', () => {
    document.getElementById('nav-main-menu')?.classList.toggle('open');
  });

  // Botón refrescar reservas
  document.getElementById('btn-refresh-reservas')?.addEventListener('click', () => {
    loadUserReservations();
  });
}

// ==========================================================
// 4. CARGA DE CANCHAS Y DISPONIBILIDAD (API REAL)
// ==========================================================
async function loadCourtsWithAvailability() {
  const container = document.getElementById('courts-cards-container');
  if (!container) return;

  container.innerHTML = `
    <div style="grid-column: 1 / -1; text-align: center; padding: 3rem 1rem;">
      <div style="color: var(--accent-lime); font-size: 1.5rem; margin-bottom: 0.5rem;">⚡</div>
      <p style="color: var(--text-secondary);">Consultando escenarios deportivos en tiempo real...</p>
    </div>
  `;

  try {
    const selectedDate = document.getElementById('filter-booking-date')?.value || AppState.filterDate;
    
    // Consultar disponibilidad real por fecha en Spring Boot
    let courtsData;
    try {
      courtsData = await api.getDisponibilidad(selectedDate);
    } catch {
      // Si la consulta de fecha falla por estar fuera de rango, consultar catálogo general
      courtsData = await api.getEscenarios();
    }

    if (!courtsData || courtsData.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 3rem 1rem;">
          <p style="color: var(--text-secondary); font-size: 1.05rem;">No se encontraron escenarios disponibles para esta fecha.</p>
        </div>
      `;
      return;
    }

    AppState.courts = courtsData;
    renderCourtsList(courtsData);
  } catch (err) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 3rem 1rem;">
        <p style="color: var(--text-secondary);">Inicia el servidor Spring Boot (<code>.\\mvnw.cmd spring-boot:run</code>) para cargar los escenarios en vivo.</p>
      </div>
    `;
  }
}

// Renderizado de las tarjetas en la cuadrícula
function renderCourtsList(courts) {
  const container = document.getElementById('courts-cards-container');
  if (!container) return;

  const searchKeyword = document.getElementById('search-court-name')?.value.trim().toLowerCase() || '';
  const selectedSport = document.getElementById('filter-sport-type')?.value || '';
  const selectedLocation = document.getElementById('filter-location-type')?.value.toLowerCase() || '';
  const selectedStatus = document.getElementById('filter-court-status')?.value || '';

  // Aplicar filtros en memoria
  const filtered = courts.filter(c => {
    const matchesName = !searchKeyword || (c.nombre && c.nombre.toLowerCase().includes(searchKeyword));
    const matchesSport = !selectedSport || c.tipo === selectedSport;
    const matchesLocation = !selectedLocation || (c.ubicacion && c.ubicacion.toLowerCase().includes(selectedLocation));
    const courtStatus = c.estado || 'ACTIVO';
    const matchesStatus = !selectedStatus || courtStatus === selectedStatus;

    return matchesName && matchesSport && matchesLocation && matchesStatus;
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 3rem 1rem;">
        <p style="color: var(--text-secondary); font-size: 1.05rem;">No hay canchas que coincidan con los filtros seleccionados.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(court => {
    const imgSrc = SPORT_IMAGES[court.tipo] || SPORT_IMAGES.DEFAULT;
    const sportName = SPORT_LABELS[court.tipo] || court.tipo;
    const isAvailable = !court.estado || court.estado === 'ACTIVO';
    
    // Estado y Pill Badge
    let statusPillHtml = '';
    if (court.estado === 'EN_MANTENIMIENTO') {
      statusPillHtml = '<span class="court-status-pill status-mantenimiento">En Mantenimiento</span>';
    } else if (court.estado === 'INACTIVO') {
      statusPillHtml = '<span class="court-status-pill status-inactivo">Inactivo</span>';
    } else {
      statusPillHtml = '<span class="court-status-pill status-disponible">Disponible</span>';
    }

    // Franjas de horarios
    const franjas = court.franjas && court.franjas.length > 0
      ? court.franjas.slice(0, 8).map(f => ({
          id: f.franjaId,
          hora: f.horaInicio.substring(0, 5),
          horaFin: f.horaFin.substring(0, 5),
          ocupada: f.ocupada
        }))
      : [
          { id: 13, hora: '18:00', horaFin: '19:00', ocupada: false },
          { id: 14, hora: '19:00', horaFin: '20:00', ocupada: false },
          { id: 15, hora: '20:00', horaFin: '21:00', ocupada: false },
          { id: 16, hora: '21:00', horaFin: '22:00', ocupada: false }
        ];

    return `
      <article class="court-card" data-court-id="${court.id}">
        <div class="court-media-wrapper">
          <img src="${imgSrc}" alt="${court.nombre}" class="court-photo" loading="lazy">
          ${statusPillHtml}
        </div>

        <div class="court-body">
          <div class="court-header">
            <h3 class="court-name">${court.nombre}</h3>
            <span class="court-sport-chip">${sportName}</span>
          </div>

          <div class="court-info-list">
            <div class="court-info-item">
              <span>📍 ${court.ubicacion || 'Sede Principal'}</span>
            </div>
            <div class="court-info-item">
              <span>👥 Capacidad: ${court.capacidad || 10} personas</span>
            </div>
            ${court.descripcion ? `
              <div class="court-info-item" style="color: var(--text-muted); font-size: 0.8rem;">
                <span>${court.descripcion}</span>
              </div>
            ` : ''}
          </div>

          <div class="time-slots-section">
            <span class="time-slots-label">Horarios Disponibles</span>
            <div class="time-slots-row">
              ${franjas.map(f => `
                <button 
                  type="button" 
                  class="slot-pill ${f.ocupada ? 'occupied' : ''}" 
                  data-court-id="${court.id}"
                  data-court-name="${court.nombre}"
                  data-sport="${sportName}"
                  data-franja-id="${f.id}"
                  data-hora="${f.hora}"
                  data-hora-fin="${f.horaFin}"
                  ${f.ocupada || !isAvailable ? 'disabled' : ''}
                  title="${f.ocupada ? 'Horario Ocupado' : 'Horario Disponible'}">
                  ${f.hora}
                </button>
              `).join('')}
            </div>
          </div>

          <button 
            type="button" 
            class="btn-book-action"
            data-court-id="${court.id}"
            data-court-name="${court.nombre}"
            data-sport="${sportName}"
            ${!isAvailable ? 'disabled' : ''}>
            ${isAvailable ? 'Reservar Turno' : 'No Disponible'}
          </button>
        </div>
      </article>
    `;
  }).join('');

  // Eventos para seleccionar píldoras de horario
  container.querySelectorAll('.slot-pill:not(.occupied)').forEach(pill => {
    pill.addEventListener('click', (e) => {
      e.stopPropagation();
      const card = pill.closest('.court-card');
      card.querySelectorAll('.slot-pill').forEach(p => p.classList.remove('active'));
      pill.classList.add('active');

      AppState.selectedCourt = {
        id: pill.dataset.courtId,
        nombre: pill.dataset.courtName,
        deporte: pill.dataset.sport
      };
      AppState.selectedSlot = {
        id: pill.dataset.franjaId,
        hora: pill.dataset.hora,
        horaFin: pill.dataset.horaFin
      };
    });
  });

  // Eventos para el botón "Reservar Turno"
  container.querySelectorAll('.btn-book-action:not(:disabled)').forEach(btn => {
    btn.addEventListener('click', () => {
      const card = btn.closest('.court-card');
      const activePill = card.querySelector('.slot-pill.active');

      if (!activePill) {
        showNotification('Por favor selecciona primero un horario (ej: 18:00)', 'error');
        return;
      }

      openBookingModal(
        btn.dataset.courtId,
        btn.dataset.courtName,
        btn.dataset.sport,
        activePill.dataset.franjaId,
        activePill.dataset.hora,
        activePill.dataset.horaFin
      );
    });
  });
}

// ==========================================================
// 5. MODAL DE CONFIRMACIÓN DE RESERVA (HU4)
// ==========================================================
function openBookingModal(courtId, courtName, sportName, franjaId, hora, horaFin) {
  const currentUser = auth.getUsuario();
  if (!currentUser) {
    showNotification('Debes iniciar sesión para realizar una reserva', 'error');
    openAuthModal('login');
    return;
  }

  const selectedDate = document.getElementById('filter-booking-date')?.value || AppState.filterDate;

  document.getElementById('summary-court-name').textContent = courtName;
  document.getElementById('summary-court-sport').textContent = sportName;
  document.getElementById('summary-court-date').textContent = selectedDate;
  document.getElementById('summary-court-time').textContent = `${hora} - ${horaFin || (parseInt(hora)+1)+':00'}`;
  document.getElementById('summary-court-user').textContent = `${currentUser.nombre} (${currentUser.email})`;

  AppState.pendingBookingPayload = {
    usuarioId: currentUser.id,
    escenarioId: courtId,
    fecha: selectedDate,
    franjaId: franjaId
  };

  document.getElementById('modal-booking-confirm')?.classList.add('active');
}

// Eventos de los Modales
function setupModalEvents() {
  // Cerrar Modal Reserva
  document.getElementById('btn-close-booking-modal')?.addEventListener('click', () => {
    document.getElementById('modal-booking-confirm')?.classList.remove('active');
  });
  document.getElementById('btn-cancel-booking-modal')?.addEventListener('click', () => {
    document.getElementById('modal-booking-confirm')?.classList.remove('active');
  });

  // Ejecutar Reserva Real (POST /api/reservas)
  document.getElementById('btn-execute-booking')?.addEventListener('click', async () => {
    if (!AppState.pendingBookingPayload) return;
    const btn = document.getElementById('btn-execute-booking');
    btn.disabled = true;
    btn.textContent = 'Procesando reserva...';

    try {
      const p = AppState.pendingBookingPayload;
      const response = await api.crearReserva(p.usuarioId, p.escenarioId, p.fecha, p.franjaId);
      
      document.getElementById('modal-booking-confirm')?.classList.remove('active');
      showNotification(`¡Reserva confirmada con código ${response.codigoReserva || 'OK'}!`);
      
      // Recargar disponibilidad en vivo
      loadCourtsWithAvailability();
    } catch (err) {
      showNotification(err.message || 'El horario ya fue reservado o no está disponible', 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Confirmar y Reservar';
    }
  });

  // Cerrar Modal Auth
  document.getElementById('btn-close-auth-modal')?.addEventListener('click', () => {
    document.getElementById('modal-auth-dialog')?.classList.remove('active');
  });

  // Alternar Login / Registro
  document.getElementById('switch-to-register')?.addEventListener('click', () => {
    document.getElementById('auth-modal-heading').textContent = 'Crear Cuenta';
    document.getElementById('form-user-login').style.display = 'none';
    document.getElementById('form-user-register').style.display = 'block';
  });

  document.getElementById('switch-to-login')?.addEventListener('click', () => {
    document.getElementById('auth-modal-heading').textContent = 'Iniciar Sesión';
    document.getElementById('form-user-login').style.display = 'block';
    document.getElementById('form-user-register').style.display = 'none';
  });
}

function openAuthModal(mode = 'login') {
  const modal = document.getElementById('modal-auth-dialog');
  if (mode === 'register') {
    document.getElementById('auth-modal-heading').textContent = 'Crear Cuenta';
    document.getElementById('form-user-login').style.display = 'none';
    document.getElementById('form-user-register').style.display = 'block';
  } else {
    document.getElementById('auth-modal-heading').textContent = 'Iniciar Sesión';
    document.getElementById('form-user-login').style.display = 'block';
    document.getElementById('form-user-register').style.display = 'none';
  }
  modal?.classList.add('active');
}

// ==========================================================
// 6. GESTIÓN DE AUTENTICACIÓN (LOGIN & REGISTRO)
// ==========================================================
function setupAuthEvents() {
  document.getElementById('btn-header-login')?.addEventListener('click', () => openAuthModal('login'));
  document.getElementById('btn-header-register')?.addEventListener('click', () => openAuthModal('register'));

  // Submit Login
  document.getElementById('form-user-login')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-login');
    btn.disabled = true;
    btn.textContent = 'Entrando...';

    const email = document.getElementById('login-user-email').value;
    const password = document.getElementById('login-user-password').value;

    try {
      const res = await api.login(email, password);
      auth.guardarSesion(res.token, res.usuario);
      document.getElementById('modal-auth-dialog')?.classList.remove('active');
      showNotification(`¡Bienvenido de nuevo, ${res.usuario.nombre}!`);
    } catch (err) {
      showNotification(err.message || 'Credenciales inválidas', 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Entrar a mi Cuenta';
    }
  });

  // Submit Registro
  document.getElementById('form-user-register')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-register');
    btn.disabled = true;
    btn.textContent = 'Registrando...';

    const name = document.getElementById('reg-user-name').value;
    const email = document.getElementById('reg-user-email').value;
    const password = document.getElementById('reg-user-password').value;

    try {
      await api.registrarUsuario(name, email, password);
      // Login automático
      const sesion = await api.login(email, password);
      auth.guardarSesion(sesion.token, sesion.usuario);
      document.getElementById('modal-auth-dialog')?.classList.remove('active');
      showNotification(`¡Cuenta creada con éxito! Bienvenido, ${name}`);
    } catch (err) {
      showNotification(err.message || 'Error al crear la cuenta', 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Registrarme';
    }
  });

  // Escuchar cambios de sesión
  auth.onAuthStateChanged((user) => {
    const container = document.getElementById('nav-user-actions');
    if (!container) return;

    if (user) {
      container.innerHTML = `
        <div style="display: flex; align-items: center; gap: 0.65rem;">
          <div style="text-align: right;">
            <div style="font-size: 0.88rem; font-weight: 600; color: var(--text-main);">${user.nombre}</div>
            <div style="font-size: 0.72rem; color: var(--accent-lime); font-weight: 700;">${user.rol || 'JUGADOR'}</div>
          </div>
          <button id="btn-user-logout" class="btn btn-secondary btn-sm" title="Cerrar sesión">
            Cerrar Sesión
          </button>
        </div>
      `;
      document.getElementById('btn-user-logout')?.addEventListener('click', () => {
        auth.cerrarSesion();
        showNotification('Has cerrado tu sesión');
        navigateToView('view-inicio');
      });
    } else {
      container.innerHTML = `
        <button class="btn btn-secondary btn-sm" id="btn-header-login">Iniciar Sesión</button>
        <button class="btn btn-primary btn-sm" id="btn-header-register">Crear Cuenta</button>
      `;
      document.getElementById('btn-header-login')?.addEventListener('click', () => openAuthModal('login'));
      document.getElementById('btn-header-register')?.addEventListener('click', () => openAuthModal('register'));
    }
  });
}

// ==========================================================
// 7. VISTA: MIS RESERVAS (HISTORIAL Y CANCELACIÓN)
// ==========================================================
async function loadUserReservations() {
  const container = document.getElementById('user-reservations-container');
  if (!container) return;

  const user = auth.getUsuario();
  if (!user) {
    container.innerHTML = `
      <div class="reservation-item-card">
        <div>
          <p style="color: var(--text-secondary); margin-bottom: 0.8rem;">Debes iniciar sesión para consultar tus reservas.</p>
          <button class="btn btn-primary btn-sm" id="btn-login-from-reservas">Iniciar Sesión Ahora</button>
        </div>
      </div>
    `;
    document.getElementById('btn-login-from-reservas')?.addEventListener('click', () => openAuthModal('login'));
    return;
  }

  container.innerHTML = `
    <div style="text-align: center; padding: 2rem;">
      <p style="color: var(--text-secondary);">Cargando tu historial de reservas...</p>
    </div>
  `;

  try {
    const list = await api.getMisReservas(user.id);
    if (!list || list.length === 0) {
      container.innerHTML = `
        <div class="reservation-item-card">
          <p style="color: var(--text-secondary);">Aún no tienes reservas registradas. ¡Explora las canchas y reserva tu primer turno!</p>
        </div>
      `;
      return;
    }

    container.innerHTML = list.map(res => {
      const isConfirmed = res.estado === 'CONFIRMADA';
      const statusClass = res.estado === 'CONFIRMADA' ? 'status-confirmada' :
                          res.estado === 'CANCELADA' ? 'status-cancelada' : 'status-rechazada';

      return `
        <div class="reservation-item-card">
          <div>
            <h4 style="font-size: 1.05rem; font-weight: 700; color: var(--text-main); margin-bottom: 0.3rem;">
              ${res.escenario ? res.escenario.nombre : 'Escenario Deportivo'}
            </h4>
            <div class="reservation-meta">
              <span>📅 Fecha: <strong>${res.fecha}</strong></span>
              <span>⏰ Horario: <strong>${res.franja ? res.franja.horaInicio : '18:00'} - ${res.franja ? res.franja.horaFin : '19:00'}</strong></span>
              <span>Código: <code>${res.codigoReserva}</code></span>
            </div>
            <div style="margin-top: 0.5rem;">
              <span class="badge-status ${statusClass}">${res.estado}</span>
            </div>
          </div>

          ${isConfirmed ? `
            <div>
              <button 
                type="button" 
                class="btn btn-danger btn-sm" 
                onclick="window.executeCancelBooking(${res.id}, ${user.id})">
                Cancelar Reserva
              </button>
            </div>
          ` : ''}
        </div>
      `;
    }).join('');
  } catch (err) {
    container.innerHTML = `
      <div class="reservation-item-card">
        <p style="color: var(--accent-red);">Error al consultar tus reservas. Verifica la conexión con el servidor.</p>
      </div>
    `;
  }
}

// Función global para cancelar reserva con confirmación
window.executeCancelBooking = async (reservaId, usuarioId) => {
  if (!confirm('¿Estás seguro de que deseas cancelar esta reserva? El horario quedará libre para otros usuarios.')) {
    return;
  }

  try {
    await api.cancelarReserva(reservaId, usuarioId);
    showNotification('Reserva cancelada exitosamente y horario liberado');
    loadUserReservations();
    loadCourtsWithAvailability();
  } catch (err) {
    showNotification(err.message || 'Error al cancelar la reserva', 'error');
  }
};

// ==========================================================
// 8. VISTA: ADMINISTRACIÓN (ADMIN DASHBOARD)
// ==========================================================
function setupAdminEvents() {
  document.getElementById('form-register-court')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-court');
    btn.disabled = true;
    btn.textContent = 'Guardando...';

    const payload = {
      nombre: document.getElementById('admin-court-name').value.trim(),
      tipo: document.getElementById('admin-court-sport').value,
      ubicacion: document.getElementById('admin-court-location').value.trim(),
      capacidad: parseInt(document.getElementById('admin-court-capacity').value, 10) || 10,
      descripcion: document.getElementById('admin-court-desc').value.trim()
    };

    try {
      await api.crearEscenario(payload);
      showNotification('¡Escenario deportivo registrado exitosamente!');
      e.target.reset();
      loadAdminCourts();
      loadCourtsWithAvailability();
    } catch (err) {
      showNotification(err.message || 'Error al registrar el escenario', 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Registrar Escenario';
    }
  });
}

async function loadAdminCourts() {
  const container = document.getElementById('admin-courts-list');
  if (!container) return;

  try {
    const list = await api.getEscenarios();
    if (!list || list.length === 0) {
      container.innerHTML = '<p style="color: var(--text-secondary);">No hay canchas registradas en el sistema.</p>';
      return;
    }

    container.innerHTML = list.map(c => {
      const isAvailable = c.estado === 'ACTIVO';
      const statusPill = isAvailable 
        ? '<span class="court-status-pill status-disponible" style="position: static;">Activo</span>'
        : '<span class="court-status-pill status-mantenimiento" style="position: static;">Mantenimiento / Inactivo</span>';

      return `
        <div style="background-color: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 0.9rem 1.1rem; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <strong style="color: var(--text-main); font-size: 0.95rem;">${c.nombre}</strong>
            <div style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 0.2rem;">
              <span>📍 ${c.ubicacion}</span> · 
              <span>⚽ ${SPORT_LABELS[c.tipo] || c.tipo}</span> · 
              <span>👥 Capacidad: ${c.capacidad}</span>
            </div>
          </div>
          <div style="display: flex; align-items: center; gap: 0.8rem;">
            ${statusPill}
            ${isAvailable ? `
              <button class="btn btn-secondary btn-sm" onclick="window.deactivateCourtClick(${c.id})">
                Poner en Mantenimiento
              </button>
            ` : ''}
          </div>
        </div>
      `;
    }).join('');
  } catch (err) {
    container.innerHTML = '<p style="color: var(--text-secondary);">Error al consultar el inventario de canchas.</p>';
  }
}

// Función global para desactivar cancha
window.deactivateCourtClick = async (courtId) => {
  if (!confirm('¿Deseas marcar este escenario en mantenimiento? Dejará de aparecer en la disponibilidad y se bloquearán nuevas reservas.')) {
    return;
  }

  try {
    await api.desactivarEscenario(courtId);
    showNotification('Escenario marcado en mantenimiento exitosamente');
    loadAdminCourts();
    loadCourtsWithAvailability();
  } catch (err) {
    showNotification(err.message || 'Error al desactivar el escenario', 'error');
  }
};

// ==========================================================
// 9. CONTROLES DE FILTROS EN TIEMPO REAL
// ==========================================================
function setupFilterControls() {
  document.getElementById('search-court-name')?.addEventListener('input', () => {
    renderCourtsList(AppState.courts);
  });

  document.getElementById('filter-sport-type')?.addEventListener('change', () => {
    renderCourtsList(AppState.courts);
  });

  document.getElementById('filter-location-type')?.addEventListener('change', () => {
    renderCourtsList(AppState.courts);
  });

  document.getElementById('filter-court-status')?.addEventListener('change', () => {
    renderCourtsList(AppState.courts);
  });

  document.getElementById('filter-booking-date')?.addEventListener('change', () => {
    loadCourtsWithAvailability();
  });
}

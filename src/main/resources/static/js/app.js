/**
 * ArenaHub — Lógica de la Aplicación y Dashboard Minimalista
 * Módulo 0 + Integración con API de Spring Boot
 */

import { api } from './api.js';
import { auth } from './auth.js';

// Imágenes curadas por tipo de cancha deportiva
const COURT_IMAGES = {
  FUTBOL_5: 'https://images.unsplash.com/photo-1529900748604-07564a03e7a6?w=600&auto=format&fit=crop&q=80',
  FUTBOL: 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=600&auto=format&fit=crop&q=80',
  BALONCESTO: 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=600&auto=format&fit=crop&q=80',
  TENIS: 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=600&auto=format&fit=crop&q=80',
  VOLEIBOL: 'https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=600&auto=format&fit=crop&q=80',
  DEFAULT: 'https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?w=600&auto=format&fit=crop&q=80'
};

// Estado global de la aplicación
const state = {
  escenarios: [],
  selectedCourtId: null,
  selectedFranjaId: null,
  selectedTimeText: null,
  fechaFiltro: new Date().toISOString().split('T')[0]
};

// Notificación Toast flotante
export function showToast(message, type = 'info') {
  const container = document.getElementById('toast-stack');
  if (!container) return;

  const item = document.createElement('div');
  item.className = `toast-item ${type === 'error' ? 'error' : ''}`;
  item.textContent = message;

  container.appendChild(item);
  setTimeout(() => {
    item.style.opacity = '0';
    item.style.transition = 'opacity 0.3s ease';
    setTimeout(() => item.remove(), 300);
  }, 3500);
}

// Navegación entre vistas principales (Sidebar)
export function switchView(viewId) {
  document.querySelectorAll('.view-panel').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));

  const target = document.getElementById(viewId);
  if (target) target.classList.add('active');

  const menuLink = document.querySelector(`.menu-item[data-view="${viewId}"]`);
  if (menuLink) menuLink.classList.add('active');

  if (viewId === 'view-reservas') {
    cargarMisReservas();
  } else if (viewId === 'view-explorar') {
    cargarCanchas();
  }
}

// Inicialización de la aplicación
document.addEventListener('DOMContentLoaded', () => {
  initDateFilter();
  setupNavigation();
  setupAuth();
  setupFilters();
  setupModals();
  cargarCanchas();
});

// Configurar filtro de fecha inicial (Hoy)
function initDateFilter() {
  const dateInput = document.getElementById('filter-fecha');
  if (dateInput) {
    dateInput.value = state.fechaFiltro;
    dateInput.min = state.fechaFiltro;
  }
}

// Configurar eventos de navegación
function setupNavigation() {
  document.querySelectorAll('[data-view]').forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      switchView(item.dataset.view);
    });
  });
}

// Carga las canchas y disponibilidad desde el Backend
async function cargarCanchas() {
  const container = document.getElementById('courts-grid-container');
  if (!container) return;

  container.innerHTML = '<p style="color: var(--text-secondary); grid-column: 1 / -1;">Consultando canchas disponibles...</p>';

  try {
    const fecha = document.getElementById('filter-fecha')?.value || state.fechaFiltro;
    
    // Intentar consultar disponibilidad con franjas
    let data;
    try {
      data = await api.getDisponibilidad(fecha);
    } catch {
      // Si falla, cargar catálogo básico
      data = await api.getEscenarios();
    }

    if (!data || data.length === 0) {
      container.innerHTML = '<p style="color: var(--text-secondary); grid-column: 1 / -1;">No hay canchas disponibles para los filtros seleccionados.</p>';
      return;
    }

    state.escenarios = data;
    renderCourts(data);
  } catch (err) {
    container.innerHTML = '<p style="color: var(--text-secondary); grid-column: 1 / -1;">Conecta el backend Spring Boot (<code>.\\mvnw.cmd spring-boot:run</code>) para ver las canchas en vivo.</p>';
  }
}

// Renderiza las tarjetas de canchas idénticas a la imagen de referencia
function renderCourts(courts) {
  const container = document.getElementById('courts-grid-container');
  if (!container) return;

  const ubicacionFiltro = document.getElementById('filter-ubicacion')?.value;
  const deporteFiltro = document.getElementById('filter-deporte')?.value;

  const filtered = courts.filter(c => {
    const uMatch = !ubicacionFiltro || (c.ubicacion && c.ubicacion.toLowerCase().includes(ubicacionFiltro.toLowerCase()));
    const dMatch = !deporteFiltro || c.tipo === deporteFiltro;
    return uMatch && dMatch;
  });

  if (filtered.length === 0) {
    container.innerHTML = '<p style="color: var(--text-secondary); grid-column: 1 / -1;">No se encontraron canchas con esos filtros.</p>';
    return;
  }

  container.innerHTML = filtered.map(c => {
    const imgSrc = COURT_IMAGES[c.tipo] || COURT_IMAGES.DEFAULT;
    const isAvailable = c.estado ? c.estado === 'ACTIVO' : true;
    const statusText = isAvailable ? 'Disponible' : 'En Mantenimiento';
    const statusClass = isAvailable ? 'pill-available' : 'pill-maintenance';

    // Franjas de horarios rápidos (18:00, 19:00, 20:00, 21:00)
    const franjasDefault = [
      { id: 13, hora: '18:00', ocupada: false },
      { id: 14, hora: '19:00', ocupada: false },
      { id: 15, hora: '20:00', ocupada: false },
      { id: 16, hora: '21:00', ocupada: false }
    ];

    const franjas = c.franjas && c.franjas.length > 0
      ? c.franjas.filter(f => {
          const h = parseInt(f.horaInicio.split(':')[0], 10);
          return h >= 18 && h <= 21;
        }).map(f => ({
          id: f.franjaId,
          hora: f.horaInicio.substring(0, 5),
          ocupada: f.ocupada
        }))
      : franjasDefault;

    return `
      <div class="court-card" data-court-id="${c.id}">
        <div class="court-image-wrapper">
          <img src="${imgSrc}" alt="${c.nombre}" class="court-image">
          <span class="court-status-pill ${statusClass}">${statusText}</span>
        </div>

        <div class="court-body">
          <h3 class="court-name">${c.nombre}</h3>
          
          <div class="court-meta">
            <span>📍 ${c.ubicacion || 'Sede Principal'}</span>
          </div>
          
          <div class="court-type">
            <span>${c.tipo} · Capacidad ${c.capacidad || 10} pers.</span>
          </div>

          <div class="time-slots-container">
            ${franjas.map(f => `
              <button 
                type="button"
                class="time-slot-pill ${f.ocupada ? 'occupied' : ''}" 
                data-court-id="${c.id}"
                data-court-name="${c.nombre}"
                data-franja-id="${f.id}" 
                data-hora="${f.hora}"
                ${f.ocupada || !isAvailable ? 'disabled' : ''}>
                ${f.hora}
              </button>
            `).join('')}
          </div>

          <button 
            type="button" 
            class="btn-book" 
            data-court-id="${c.id}"
            data-court-name="${c.nombre}"
            ${!isAvailable ? 'disabled' : ''}>
            ${isAvailable ? 'Reservar Ahora' : 'No Disponible'}
          </button>
        </div>
      </div>
    `;
  }).join('');

  // Configurar eventos en las píldoras de horario
  container.querySelectorAll('.time-slot-pill:not(.occupied)').forEach(pill => {
    pill.addEventListener('click', (e) => {
      e.stopPropagation();
      const courtId = pill.dataset.courtId;
      
      // Deseleccionar otras píldoras de la misma tarjeta
      const card = pill.closest('.court-card');
      card.querySelectorAll('.time-slot-pill').forEach(p => p.classList.remove('selected'));
      pill.classList.add('selected');

      state.selectedCourtId = courtId;
      state.selectedFranjaId = pill.dataset.franjaId;
      state.selectedTimeText = pill.dataset.hora;
    });
  });

  // Configurar eventos en el botón "Reservar Ahora"
  container.querySelectorAll('.btn-book:not(:disabled)').forEach(btn => {
    btn.addEventListener('click', () => {
      const courtId = btn.dataset.courtId;
      const courtName = btn.dataset.courtName;
      
      const card = btn.closest('.court-card');
      const selectedPill = card.querySelector('.time-slot-pill.selected');

      if (!selectedPill) {
        showToast('Selecciona primero un horario (ej: 18:00)', 'error');
        return;
      }

      abrirModalReserva(courtId, courtName, selectedPill.dataset.franjaId, selectedPill.dataset.hora);
    });
  });
}

// Abrir modal de confirmación de reserva
function abrirModalReserva(courtId, courtName, franjaId, hora) {
  const user = auth.getUsuario();
  if (!user) {
    showToast('Debes iniciar sesión para reservar', 'error');
    document.getElementById('auth-modal')?.classList.add('active');
    return;
  }

  const modal = document.getElementById('booking-modal');
  const fecha = document.getElementById('filter-fecha')?.value || state.fechaFiltro;

  document.getElementById('modal-court-name').textContent = courtName;
  document.getElementById('modal-court-date').textContent = fecha;
  document.getElementById('modal-court-time').textContent = `${hora} - ${parseInt(hora)+1}:00`;
  document.getElementById('modal-court-user').textContent = `${user.nombre} (${user.email})`;

  state.bookingPayload = {
    usuarioId: user.id,
    escenarioId: courtId,
    fecha: fecha,
    franjaId: franjaId
  };

  modal.classList.add('active');
}

// Configurar modales y filtros
function setupModals() {
  // Modal Reserva
  document.getElementById('modal-btn-cancel')?.addEventListener('click', () => {
    document.getElementById('booking-modal')?.classList.remove('active');
  });

  document.getElementById('modal-btn-confirm')?.addEventListener('click', async () => {
    if (!state.bookingPayload) return;
    const btn = document.getElementById('modal-btn-confirm');
    btn.disabled = true;
    btn.textContent = 'Confirmando...';

    try {
      const p = state.bookingPayload;
      const res = await api.crearReserva(p.usuarioId, p.escenarioId, p.fecha, p.franjaId);
      document.getElementById('booking-modal')?.classList.remove('active');
      showToast(`¡Reserva confirmada con código ${res.codigoReserva || 'OK'}!`, 'info');
      cargarCanchas();
    } catch (err) {
      showToast(err.message || 'Error al confirmar la reserva', 'error');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Confirmar Reserva';
    }
  });

  // Modal Auth
  document.getElementById('auth-modal-close')?.addEventListener('click', () => {
    document.getElementById('auth-modal')?.classList.remove('active');
  });

  document.getElementById('link-show-register')?.addEventListener('click', () => {
    document.getElementById('auth-modal-title').textContent = 'Crear Cuenta';
    document.getElementById('auth-form-login').style.display = 'none';
    document.getElementById('auth-form-register').style.display = 'block';
  });

  document.getElementById('link-show-login')?.addEventListener('click', () => {
    document.getElementById('auth-modal-title').textContent = 'Iniciar Sesión';
    document.getElementById('auth-form-login').style.display = 'block';
    document.getElementById('auth-form-register').style.display = 'none';
  });

  document.getElementById('btn-open-login')?.addEventListener('click', () => {
    document.getElementById('auth-modal')?.classList.add('active');
  });

  // Enviar formulario Login
  document.getElementById('auth-form-login')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('auth-email').value;
    const password = document.getElementById('auth-password').value;

    try {
      const res = await api.login(email, password);
      auth.guardarSesion(res.token, res.usuario);
      document.getElementById('auth-modal')?.classList.remove('active');
      showToast(`¡Bienvenido de nuevo, ${res.usuario.nombre}!`);
    } catch (err) {
      showToast(err.message || 'Credenciales inválidas', 'error');
    }
  });

  // Enviar formulario Registro
  document.getElementById('auth-form-register')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const nombre = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;

    try {
      const nuevo = await api.registrarUsuario(nombre, email, password);
      // Auto-login
      const sesion = await api.login(email, password);
      auth.guardarSesion(sesion.token, sesion.usuario);
      document.getElementById('auth-modal')?.classList.remove('active');
      showToast(`¡Cuenta creada con éxito! Bienvenido, ${nuevo.nombre}`);
    } catch (err) {
      showToast(err.message || 'Error al registrar usuario', 'error');
    }
  });

  // Formulario Admin Crear Cancha
  document.getElementById('form-admin-crear')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const datos = {
      nombre: document.getElementById('new-nombre').value,
      tipo: document.getElementById('new-tipo').value,
      ubicacion: document.getElementById('new-ubicacion').value,
      capacidad: parseInt(document.getElementById('new-capacidad').value, 10) || 10,
      descripcion: document.getElementById('new-descripcion').value
    };

    try {
      await api.crearEscenario(datos);
      showToast('Cancha registrada exitosamente');
      e.target.reset();
      switchView('view-explorar');
    } catch (err) {
      showToast(err.message || 'Error al registrar escenario', 'error');
    }
  });
}

// Configurar filtros dinámicos
function setupFilters() {
  document.getElementById('filter-ubicacion')?.addEventListener('change', () => renderCourts(state.escenarios));
  document.getElementById('filter-deporte')?.addEventListener('change', () => renderCourts(state.escenarios));
  document.getElementById('filter-fecha')?.addEventListener('change', () => cargarCanchas());
}

// Configurar estado de autenticación en la UI
function setupAuth() {
  auth.onAuthStateChanged((user) => {
    const section = document.getElementById('sidebar-user-section');
    if (!section) return;

    if (user) {
      section.innerHTML = `
        <div class="user-profile-badge">
          <div class="user-info">
            <span class="user-name">${user.nombre}</span>
            <span class="user-role">${user.rol || 'JUGADOR'}</span>
          </div>
          <button id="btn-sidebar-logout" style="background: none; border: none; color: var(--text-muted); cursor: pointer;" title="Cerrar sesión">
            <svg style="width: 16px; height: 16px;" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
            </svg>
          </button>
        </div>
      `;

      document.getElementById('btn-sidebar-logout')?.addEventListener('click', () => {
        auth.cerrarSesion();
        showToast('Sesión cerrada');
      });
    } else {
      section.innerHTML = `
        <button class="btn-secondary" style="width: 100%;" id="btn-open-login">
          Iniciar Sesión
        </button>
      `;
      document.getElementById('btn-open-login')?.addEventListener('click', () => {
        document.getElementById('auth-modal')?.classList.add('active');
      });
    }
  });
}

// Cargar "Mis Reservas"
async function cargarMisReservas() {
  const container = document.getElementById('user-reservations-list');
  if (!container) return;

  const user = auth.getUsuario();
  if (!user) {
    container.innerHTML = '<p style="color: var(--text-secondary);">Inicia sesión para consultar tus reservas activas.</p>';
    return;
  }

  container.innerHTML = '<p style="color: var(--text-secondary);">Cargando tus reservas...</p>';

  try {
    const reservas = await api.getMisReservas(user.id);
    if (!reservas || reservas.length === 0) {
      container.innerHTML = '<p style="color: var(--text-secondary);">No tienes ninguna reserva registrada actualmente.</p>';
      return;
    }

    container.innerHTML = reservas.map(r => {
      const isConfirmed = r.estado === 'CONFIRMADA';
      return `
        <div class="reservation-card">
          <div>
            <h4 style="font-size: 1rem; margin-bottom: 0.3rem;">${r.escenario ? r.escenario.nombre : 'Cancha Deportiva'}</h4>
            <div style="font-size: 0.85rem; color: var(--text-secondary);">
              <span>📅 Fecha: ${r.fecha}</span> · 
              <span>⏰ Horario: ${r.franja ? r.franja.horaInicio : '18:00'} - ${r.franja ? r.franja.horaFin : '19:00'}</span> ·
              <span>Código: <code>${r.codigoReserva}</code></span>
            </div>
            <div style="margin-top: 0.4rem;">
              <span class="court-status-pill ${isConfirmed ? 'pill-available' : 'pill-inactive'}" style="position: static;">
                ${r.estado}
              </span>
            </div>
          </div>

          ${isConfirmed ? `
            <button class="btn-secondary" style="color: var(--red); border-color: rgba(239, 68, 68, 0.4);" onclick="window.cancelarReservaClick(${r.id}, ${user.id})">
              Cancelar
            </button>
          ` : ''}
        </div>
      `;
    }).join('');
  } catch (err) {
    container.innerHTML = '<p style="color: var(--text-secondary);">Error al obtener tus reservas.</p>';
  }
}

// Función global para cancelar reserva
window.cancelarReservaClick = async (reservaId, usuarioId) => {
  if (!confirm('¿Seguro que deseas cancelar esta reserva? El horario quedará libre para otros deportistas.')) {
    return;
  }

  try {
    await api.cancelarReserva(reservaId, usuarioId);
    showToast('Reserva cancelada exitosamente');
    cargarMisReservas();
  } catch (err) {
    showToast(err.message || 'Error al cancelar la reserva', 'error');
  }
};

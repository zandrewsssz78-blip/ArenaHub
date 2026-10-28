/**
 * ArenaHub — Lógica de Interfaz y Navegación
 * Proyecto de Ingeniería de Software — UTADEO
 */

import { api } from './api.js';
import { auth } from './auth.js';

// Muestra una notificación temporal en la esquina inferior
export function mostrarAlerta(mensaje, tipo = 'info') {
  const contenedor = document.getElementById('contenedor-alertas');
  if (!contenedor) return;

  const alerta = document.createElement('div');
  alerta.className = `alerta alerta-${tipo}`;
  alerta.textContent = mensaje;

  contenedor.appendChild(alerta);

  setTimeout(() => {
    alerta.style.opacity = '0';
    alerta.style.transition = 'opacity 0.3s ease';
    setTimeout(() => alerta.remove(), 300);
  }, 3200);
}

// Cambia la pestaña o vista visible en pantalla
export function cambiarVista(idVista) {
  const vistas = document.querySelectorAll('.vista');
  vistas.forEach(v => v.classList.remove('activa'));

  const objetivo = document.getElementById(idVista);
  if (objetivo) {
    objetivo.classList.add('activa');
  }

  // Actualizar el enlace activo en el menú
  document.querySelectorAll('.nav-link').forEach(link => {
    if (link.dataset.vista === idVista) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Inicialización de la interfaz
document.addEventListener('DOMContentLoaded', async () => {
  // Manejo de clics en el menú de navegación
  document.querySelectorAll('[data-vista]').forEach(elem => {
    elem.addEventListener('click', (e) => {
      e.preventDefault();
      cambiarVista(elem.dataset.vista);
    });
  });

  // Reaccionar al estado de sesión del usuario
  auth.onAuthStateChanged((usuario) => {
    const contenedorUsuario = document.getElementById('nav-seccion-usuario');
    if (!contenedorUsuario) return;

    if (usuario) {
      contenedorUsuario.innerHTML = `
        <span style="font-size: 0.9rem; color: var(--color-texto); font-weight: 500;">
          ${usuario.nombre}
        </span>
        <button id="btn-cerrar-sesion" class="btn btn-outline btn-sm">
          Salir
        </button>
      `;
      document.getElementById('btn-cerrar-sesion')?.addEventListener('click', () => {
        auth.cerrarSesion();
        mostrarAlerta('Has cerrado sesión', 'info');
        cambiarVista('vista-inicio');
      });
    } else {
      contenedorUsuario.innerHTML = `
        <button class="btn btn-outline btn-sm" data-vista="vista-login">Iniciar Sesión</button>
        <button class="btn btn-verde btn-sm" data-vista="vista-registro">Registrarse</button>
      `;
      contenedorUsuario.querySelectorAll('[data-vista]').forEach(btn => {
        btn.addEventListener('click', () => cambiarVista(btn.dataset.vista));
      });
    }
  });

  // Cargar las canchas iniciales en la vista de inicio
  cargarCanchasInicio();
});

// Carga las canchas registradas para mostrarlas de forma natural
async function cargarCanchasInicio() {
  const contenedor = document.getElementById('lista-canchas-inicio');
  if (!contenedor) return;

  try {
    const escenarios = await api.getEscenarios();
    if (!escenarios || escenarios.length === 0) {
      contenedor.innerHTML = '<p style="color: var(--color-texto-secundario);">No hay canchas registradas en este momento.</p>';
      return;
    }

    contenedor.innerHTML = escenarios.map(cancha => {
      const estadoClase = cancha.estado === 'ACTIVO' ? 'etiqueta-activo' :
                          cancha.estado === 'EN_MANTENIMIENTO' ? 'etiqueta-mantenimiento' : 'etiqueta-inactivo';
      const estadoTexto = cancha.estado === 'ACTIVO' ? 'Disponible' :
                          cancha.estado === 'EN_MANTENIMIENTO' ? 'En Mantenimiento' : 'Inactivo';

      return `
        <div class="tarjeta">
          <div>
            <div class="tarjeta-header">
              <h3 class="tarjeta-titulo">${cancha.nombre}</h3>
              <span class="etiqueta ${estadoClase}">${estadoTexto}</span>
            </div>
            <p class="tarjeta-descripcion">${cancha.descripcion || 'Sin descripción disponible.'}</p>
            <div class="tarjeta-detalles">
              <span>📍 Ubicación: ${cancha.ubicacion}</span>
              <span>👥 Capacidad: ${cancha.capacidad || 10} personas</span>
              <span>⚽ Tipo: ${cancha.tipo}</span>
            </div>
          </div>
          <div class="tarjeta-footer">
            <span class="etiqueta etiqueta-deporte">${cancha.tipo}</span>
            <button class="btn btn-verde btn-sm" onclick="window.seleccionarCancha(${cancha.id})">
              Ver Horarios
            </button>
          </div>
        </div>
      `;
    }).join('');
  } catch (err) {
    contenedor.innerHTML = '<p style="color: var(--color-texto-secundario);">Inicia el servidor Spring Boot para cargar los escenarios en vivo.</p>';
  }
}

// Función global para cuando el usuario hace clic en "Ver Horarios"
window.seleccionarCancha = (id) => {
  cambiarVista('vista-canchas');
};

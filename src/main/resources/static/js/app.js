/**
 * ArenaHub — Aplicación Principal (Módulo 0)
 * Orquestador de vistas, navegación, notificaciones y estado base
 */

import { api } from './api.js';
import { auth } from './auth.js';

// Sistema de Notificaciones Toast
export function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
  toast.innerHTML = `
    <span style="font-weight: bold; font-size: 1.1rem;">${icon}</span>
    <span>${message}</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 400);
  }, 3500);
}

// Navegación entre vistas (SPA)
export function navigateTo(viewId) {
  const views = document.querySelectorAll('.view-section');
  views.forEach(v => v.classList.remove('active'));

  const target = document.getElementById(viewId);
  if (target) {
    target.classList.add('active');
  }

  // Actualizar enlaces de navegación activos
  const navLinks = document.querySelectorAll('.nav-link');
  navLinks.forEach(link => {
    if (link.dataset.view === viewId) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Inicialización de la aplicación
document.addEventListener('DOMContentLoaded', async () => {
  console.log('🚀 ArenaHub Frontend v0.1 inicializado');

  // Enlaces de navegación
  document.querySelectorAll('[data-view]').forEach(elem => {
    elem.addEventListener('click', (e) => {
      e.preventDefault();
      const targetView = elem.dataset.view;
      navigateTo(targetView);
    });
  });

  // Suscripción a cambios de autenticación
  auth.onAuthStateChanged((user) => {
    const authContainer = document.getElementById('navbar-auth-section');
    if (!authContainer) return;

    if (user) {
      authContainer.innerHTML = `
        <div style="display: flex; align-items: center; gap: 0.6rem;">
          <div style="text-align: right;">
            <div style="font-size: 0.88rem; font-weight: 600; color: var(--text-main);">${user.nombre}</div>
            <div style="font-size: 0.72rem; color: var(--primary); text-transform: uppercase;">${user.rol || 'JUGADOR'}</div>
          </div>
          <button id="btn-logout" class="btn btn-secondary btn-sm" title="Cerrar sesión">
            Cerrar Sesión
          </button>
        </div>
      `;
      document.getElementById('btn-logout')?.addEventListener('click', () => {
        auth.cerrarSesion();
        showToast('Has cerrado sesión correctamente', 'info');
        navigateTo('view-home');
      });
    } else {
      authContainer.innerHTML = `
        <button class="btn btn-secondary btn-sm" data-view="view-login">Iniciar Sesión</button>
        <button class="btn btn-primary btn-sm" data-view="view-registro">Registrarse</button>
      `;
      // Reasignar eventos a los botones recién inyectados
      authContainer.querySelectorAll('[data-view]').forEach(btn => {
        btn.addEventListener('click', () => navigateTo(btn.dataset.view));
      });
    }
  });

  // Comprobar estado de conexión con el backend
  const statusIndicator = document.getElementById('backend-status');
  if (statusIndicator) {
    const isOnline = await api.pingBackend();
    if (isOnline) {
      statusIndicator.className = 'badge badge-green';
      statusIndicator.innerHTML = '<span class="status-dot"></span> Backend Spring Boot Conectado';
    } else {
      statusIndicator.className = 'badge badge-amber';
      statusIndicator.innerHTML = '<span class="status-dot"></span> Backend Desconectado (Verifica docker y mvnw)';
    }
  }
});

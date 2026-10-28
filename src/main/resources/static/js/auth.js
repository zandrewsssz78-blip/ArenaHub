/**
 * ArenaHub — Gestión de Sesión y Autenticación (Módulo 0)
 * Almacenamiento local seguro en el navegador (localStorage)
 */

const STORAGE_KEY_USER = 'arenahub_user';
const STORAGE_KEY_TOKEN = 'arenahub_token';

class AuthManager {
  constructor() {
    this.listeners = [];
  }

  getUsuario() {
    const raw = localStorage.getItem(STORAGE_KEY_USER);
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  getToken() {
    return localStorage.getItem(STORAGE_KEY_TOKEN);
  }

  isAutenticado() {
    return this.getUsuario() !== null;
  }

  guardarSesion(token, usuario) {
    localStorage.setItem(STORAGE_KEY_TOKEN, token);
    localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(usuario));
    this.notify();
  }

  cerrarSesion() {
    localStorage.removeItem(STORAGE_KEY_TOKEN);
    localStorage.removeItem(STORAGE_KEY_USER);
    this.notify();
  }

  onAuthStateChanged(callback) {
    this.listeners.push(callback);
    // Ejecutar de inmediato con el estado actual
    callback(this.getUsuario());
  }

  notify() {
    const user = this.getUsuario();
    this.listeners.forEach(cb => cb(user));
  }
}

export const auth = new AuthManager();

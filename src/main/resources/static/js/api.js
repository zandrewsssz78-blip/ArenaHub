/**
 * ArenaHub — Cliente de API (Módulo 0)
 * Comunicación HTTP asíncrona con el Backend Spring Boot 3
 */

const API_BASE = '/api';

export const api = {
  /**
   * Método base para realizar peticiones HTTP y capturar errores de forma uniforme
   */
  async request(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    const defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options.headers,
      },
    };

    try {
      const response = await fetch(url, config);
      const isJson = response.headers.get('content-type')?.includes('application/json');
      const data = isJson ? await response.json() : await response.text();

      if (!response.ok) {
        // Manejo del objeto ErrorResponse de Spring Boot (ManejadorErrores.java)
        const errorMsg = data.mensaje || data.error || (typeof data === 'string' ? data : `Error HTTP ${response.status}`);
        const error = new Error(errorMsg);
        error.status = response.status;
        error.data = data;
        throw error;
      }

      return data;
    } catch (err) {
      console.error(`[API Error] ${options.method || 'GET'} ${url}:`, err);
      throw err;
    }
  },

  // ==========================================
  // AUTENTICACIÓN Y USUARIOS (HU1 & HU2)
  // ==========================================
  async registrarUsuario(nombre, email, contrasena) {
    return this.request('/usuarios', {
      method: 'POST',
      body: JSON.stringify({ nombre, email, contrasena }),
    });
  },

  async login(email, contrasena) {
    return this.request('/usuarios/login', {
      method: 'POST',
      body: JSON.stringify({ email, contrasena }),
    });
  },

  // ==========================================
  // ESCENARIOS Y DISPONIBILIDAD (HU3, HU7 & HU8)
  // ==========================================
  async getDisponibilidad(fechaIso) {
    return this.request(`/escenarios/disponibilidad?fecha=${fechaIso}`);
  },

  async getEscenarios() {
    return this.request('/escenarios');
  },

  async getEscenarioPorId(id) {
    return this.request(`/escenarios/${id}`);
  },

  async crearEscenario(datos) {
    return this.request('/escenarios', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  },

  async desactivarEscenario(id) {
    return this.request(`/escenarios/${id}/desactivar`, {
      method: 'PATCH',
    });
  },

  async cambiarEstadoEscenario(id, estado) {
    return this.request(`/escenarios/${id}/estado`, {
      method: 'PATCH',
      body: JSON.stringify({ estado }),
    });
  },

  // ==========================================
  // RESERVAS (HU4, HU5 & HU6)
  // ==========================================
  async crearReserva(usuarioId, escenarioId, fecha, franjaId) {
    return this.request('/reservas', {
      method: 'POST',
      body: JSON.stringify({ usuarioId, escenarioId, fecha, franjaId }),
    });
  },

  async getMisReservas(usuarioId) {
    return this.request(`/reservas?usuarioId=${usuarioId}`);
  },

  async cancelarReserva(reservaId, usuarioId) {
    return this.request(`/reservas/${reservaId}/cancelar`, {
      method: 'PATCH',
      body: JSON.stringify({ usuarioId }),
    });
  },

  // ==========================================
  // VERIFICACIÓN DE SALUD / CONEXIÓN
  // ==========================================
  async pingBackend() {
    try {
      await fetch('/api/escenarios', { method: 'GET' });
      return true;
    } catch {
      return false;
    }
  }
};

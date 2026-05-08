// Configuración global del frontend
// Usa localhost en desarrollo y la URL de producción cuando se sirva desde otro host.
const isLocalhost = ["localhost", "127.0.0.1"].includes(window.location.hostname);
const API_BASE_URL = isLocalhost
    ? "http://localhost:8080/api"
    : "https://logitrack-omv3.onrender.com/api";

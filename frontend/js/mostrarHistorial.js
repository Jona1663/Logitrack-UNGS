// API_BASE_URL se define en frontend/js/config.js
const API_URL = `${API_BASE_URL}/envios/historial-completo`;
const tokenAuth = sessionStorage.getItem("token");

const authHeaders = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${tokenAuth}`
};

const tbody = document.getElementById("tbodyHistorial");
const emptyState = document.getElementById("emptyState");
const resultsCard = document.getElementById("resultsTable");

// Función para formatear el texto del Enum (Ej: "EN_TRANSITO" -> "En tránsito")
function formatearEstado(textoEnum) {
    if (!textoEnum || textoEnum === "INICIAL") return "Creación";
    let texto = textoEnum.replace('_', ' ').toLowerCase();
    texto = texto.charAt(0).toUpperCase() + texto.slice(1);
    if (texto === "En transito") return "En tránsito";
    return texto;
}

async function cargarHistorialDeCambios() {
    // Estado de carga inicial
    tbody.innerHTML = `
        <tr>
            <td colspan="6" class="text-center text-muted small py-4">
                <span class="spinner-border spinner-border-sm me-2 text-success"></span> Procesando registros de auditoría...
            </td>
        </tr>`;

    try {
        const response = await fetch(API_URL, { headers: authHeaders });

        if (!response.ok) {
            if (response.status === 401) window.location.href = "../index.html";
            throw new Error("Error al obtener el historial del servidor");
        }

        const registros = await response.json();

        if (!registros || registros.length === 0) {
            resultsCard.classList.add("d-none");
            emptyState.classList.remove("d-none");
            return;
        }

        resultsCard.classList.remove("d-none");
        emptyState.classList.add("d-none");

        tbody.innerHTML = registros.map(reg => {
            // ─── SOLUCIÓN: Atrapamos los datos sin importar cómo los formatee Java ───
            const idRegistro = reg.idReg || reg.id_reg;
            const idRastreo = reg.idRastreo || reg.id_rastreo;
            const estAnterior = reg.estadoAnterior || reg.estado_anterior;
            const estNuevo = reg.estadoNuevo || reg.estado_nuevo;
            const fechaRaw = reg.fechaHora || reg.fecha_hora;
            const resp = reg.responsable;

            // Parseo de fechas seguro
            const fecha = fechaRaw ? new Date(fechaRaw) : null;
            const fechaStr = fecha ? fecha.toLocaleDateString("es-AR") : "—";
            const horaStr = fecha ? fecha.toLocaleTimeString("es-AR", { hour: '2-digit', minute: '2-digit' }) : "—";

            // Construimos la descripción
            const descModificacion = `Cambio: <b>${formatearEstado(estAnterior)}</b> <i class="bi bi-arrow-right mx-1 text-muted"></i> <b>${formatearEstado(estNuevo)}</b>`;

            return `
                <tr>
                    <td class="ps-4 small fw-bold text-muted">#${idRegistro}</td>
                    <td class="small">
                        <a href="./detalleEnvio.html?id=${idRastreo}" class="text-decoration-none fw-bold text-success">
                            ${idRastreo}
                        </a>
                    </td>
                    <td class="small">${descModificacion}</td>
                    <td class="small">${fechaStr}</td>
                    <td class="small">${horaStr}</td>
                    <td class="small pe-4">
                        <span class="badge bg-light text-dark border"><i class="bi bi-person-fill me-1"></i>${resp}</span>
                    </td>
                </tr>`;
        }).join("");

    } catch (error) {
        console.error("Error al cargar auditoría:", error);
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-danger py-4">
                    <i class="bi bi-exclamation-octagon-fill me-2"></i> Error al conectar con el servidor de auditoría.
                </td>
            </tr>`;
    }
}

// Iniciar la carga al abrir la página
document.addEventListener("DOMContentLoaded", cargarHistorialDeCambios);
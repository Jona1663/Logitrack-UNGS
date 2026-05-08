// Apuntamos al backend centralizado en frontend/js/config.js
const API_URL = `${API_BASE_URL}/envios`;

// Extraemos el token JWT de la sesión
const tokenAuth = sessionStorage.getItem("token");
const authHeaders = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${tokenAuth}`
};

// Referencias al DOM
const searchInput = document.getElementById("searchInput");
const stateSelect = document.getElementById("stateSelect");
const resultsTable = document.getElementById("resultsTable");
const tbody = resultsTable.querySelector("tbody");
const emptyTable = document.getElementById("emptyTable");
const noResultsTable = document.getElementById("noResultsTable");
const btnBuscar = document.querySelector("form button[type='submit']");
const btnLimpiar = document.querySelector("form button[type='reset']");

// Función principal de búsqueda
async function buscar() {
    const query = searchInput.value.trim().toLowerCase();
    const estadoFiltro = stateSelect.value; // Ej: "En tránsito"

    // Estado visual de carga
    btnBuscar.disabled = true;
    btnBuscar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Buscando...`;

    tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span> Obteniendo datos...</td></tr>`;
    emptyTable.classList.add("d-none");
    resultsTable.classList.remove("d-none");
    noResultsTable.classList.add("d-none");

    try {
        // Hacemos el GET al backend enviando el Token
        const res = await fetch(API_URL, { headers: authHeaders });
        
        if (!res.ok) {
            // Si el token expiró, pateamos al usuario al login
            if (res.status === 401) window.location.href = "../index.html";
            throw new Error("Error al obtener los envíos");
        }
        
        const envios = await res.json();

        // Filtrado lógico en el cliente
        const filtrados = envios.filter(e => {
            // Extraemos los datos previniendo que vengan nulos (Optional Chaining '?')
            const idEnvio = e.id_envio || "";
            const ctg = e.tracking_ctg || "";
            const cliente = e.origen?.empresa?.razon_social || "";
            const destino = e.destino?.empresa?.razon_social || "";
            const grano = e.tipo_grano || "";

            const coincideQuery =
                idEnvio.toLowerCase().includes(query) ||
                ctg.toLowerCase().includes(query) ||
                cliente.toLowerCase().includes(query) ||
                destino.toLowerCase().includes(query) ||
                grano.toLowerCase().includes(query);

            // Normalizamos el Enum del backend (ej. "EN_TRANSITO") vs el Select ("En tránsito")
            let estadoNormalizado = estadoFiltro.toUpperCase().replace(' ', '_');
            if (estadoNormalizado === "EN_TRÁNSITO") estadoNormalizado = "EN_TRANSITO"; // Parche para el tilde
            
            const coincideEstado = estadoFiltro === "Cualquier Estado" || e.estado_actual === estadoNormalizado;

            return coincideQuery && coincideEstado;
        });

        // Manejo de sin resultados
        if (filtrados.length === 0) {
            resultsTable.classList.add("d-none");
            noResultsTable.classList.remove("d-none");
            return;
        }

        // Dibujar filas mapeando la clase Envio.java
        tbody.innerHTML = filtrados.map(e => {
            // Conversiones para la vista
            const nombreCliente = e.origen?.empresa?.razon_social || "Sin cliente";
            const pesoTn = e.kg_origen ? (e.kg_origen / 1000).toFixed(1) : "0";
            const estadoFormateado = e.estado_actual ? e.estado_actual.replace('_', ' ') : "DESCONOCIDO";

            return `
            <tr>
                <td class="ps-4">
                    <span class="fw-bold text-success d-block">${e.id_envio}</span>
                    <small class="text-muted" style="font-size: 0.7rem;">CTG: ${e.tracking_ctg}</small>
                </td>
                <td>
                    <span class="d-block fw-medium text-dark">${nombreCliente}</span>
                    <small class="text-muted"><i class="bi bi-geo-alt"></i> ${e.destino?.nombre_lugar || "Destino pendiente"}</small>
                </td>
                <td>
                    <span class="d-block fw-medium text-dark">${e.tipo_grano}</span>
                    <small class="text-muted">${pesoTn} Tn</small>
                </td>
                <td>
                    <span class="badge ${getEstadoClass(e.estado_actual)} rounded-pill px-3">
                        ${estadoFormateado}
                    </span>
                </td>
                <td class="text-end pe-4">
                    <a href="./detalleEnvio.html?id=${e.id_envio}" class="btn btn-sm btn-outline-success shadow-sm rounded-3 fw-medium">
                        <i class="bi bi-eye-fill me-1"></i> Ficha
                    </a>
                </td>
            </tr>
        `}).join("");

    } catch (error) {
        console.error("Error en la búsqueda:", error);
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4"><i class="bi bi-exclamation-triangle-fill me-2"></i> Error al conectar con el servidor.</td></tr>`;
    } finally {
        // Restaurar botón
        btnBuscar.disabled = false;
        btnBuscar.innerHTML = "Buscar";
    }
}

// Función auxiliar para colorear los badges, usando los nombres exactos de tu Enum Java
function getEstadoClass(estadoEnum) {
    switch (estadoEnum) {
        case 'PENDIENTE': return 'bg-secondary bg-opacity-10 text-secondary border border-secondary-subtle';
        case 'EN_TRANSITO': return 'bg-primary bg-opacity-10 text-primary border border-primary-subtle';
        case 'EN_SUCURSAL': return 'bg-warning bg-opacity-10 text-warning-emphasis border border-warning-subtle';
        case 'ENTREGADO': return 'bg-success bg-opacity-10 text-success border border-success-subtle';
        default: return 'bg-light text-dark border';
    }
}

// Evento: Enviar el formulario (Enter o Click en buscar)
document.querySelector("form").addEventListener("submit", (e) => {
    e.preventDefault();
    buscar();
});

// Evento: Botón limpiar restaura la vista al estado inicial
if (btnLimpiar) {
    btnLimpiar.addEventListener("click", function () {
        emptyTable.classList.remove("d-none");
        resultsTable.classList.add("d-none");
        noResultsTable.classList.add("d-none");
    });
}
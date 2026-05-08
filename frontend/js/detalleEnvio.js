// API_BASE_URL se define en frontend/js/config.js
const API_URL = `${API_BASE_URL}/envios`;
const tokenAuth = sessionStorage.getItem("token");

// Configuración de cabeceras para Spring Security
const authHeaders = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${tokenAuth}`
};

// Obtener ID de la URL
const params = new URLSearchParams(window.location.search);
const id = params.get("id");

if (!id) {
    window.location.href = "./busqueda.html";
}

// Referencias del DOM
const form = document.getElementById("formDetalle");
const btnEditar = form.querySelector("button[type='submit']");
const selectEstado = document.getElementById("selectEstado");
const selectPrioridad = document.getElementById("selectPrioridad");
const divBotones = document.getElementById("botonesEdicion");

let estadoOriginal = null;
let prioridadOriginal = null;

// ─── LÓGICA DE ROLES Y PERMISOS ───
function aplicarPermisosSegunRol() {
    const usuarioLogueado = JSON.parse(sessionStorage.getItem("usuarioLogueado"));
    const rol = usuarioLogueado.rol.toLowerCase().replace('role_', '');

    if (rol === "operador") {
        // Operador: Puede cambiar estado, pero NO prioridad
        selectEstado.disabled = false;
        selectPrioridad.disabled = true;
        divBotones.classList.remove("d-none");
    } else if (rol === "supervisor") {
        // Supervisor: Control total
        selectEstado.disabled = false;
        selectPrioridad.disabled = false;
        divBotones.classList.remove("d-none");
    } else {
        // Otros roles (Ej. Chofer): Modo solo lectura
        selectEstado.disabled = true;
        selectPrioridad.disabled = true;
        divBotones.classList.add("d-none");
    }
}

// ─── AYUDANTES PARA FORMATEAR ENUMS DE JAVA ───
function normalizarEnum(valorEnum) {
    if(!valorEnum) return "Sin determinar";
    // De "EN_TRANSITO" a "En tránsito"
    let texto = valorEnum.replace('_', ' ').toLowerCase();
    texto = texto.charAt(0).toUpperCase() + texto.slice(1);
    if(texto === "En transito") return "En tránsito";
    return texto;
}

function enumParaJava(textoSelect) {
    // De "En tránsito" a "EN_TRANSITO"
    let texto = textoSelect.toUpperCase().replace(' ', '_');
    if(texto === "EN_TRÁNSITO") return "EN_TRANSITO";
    return texto;
}

function setSelectValue(selectEl, valueText) {
    const option = Array.from(selectEl.options).find(o => o.text === valueText || o.value === valueText);
    if (option) option.selected = true;
}

// ─── CARGAR DATOS DEL ENVÍO ───
async function cargarDetalle() {
    try {
        const response = await fetch(`${API_URL}/${id}`, { headers: authHeaders });
        if (!response.ok) throw new Error("Envío no encontrado");

        const envio = await response.json();

        // Mapeo contra las propiedades reales de tu clase Envio.java
        document.getElementById("trackingId").textContent = envio.id_envio;
        document.getElementById("clienteNombre").value = envio.origen?.empresa?.razon_social || "No especificado";
        
        document.getElementById("origenNombre").value = envio.origen?.nombre_lugar || "No especificado";
        document.getElementById("destinoNombre").value = envio.destino?.nombre_lugar || "No especificado";

        // Distancia no viene en el backend, se asume 0 por ahora o se elimina
        document.getElementById("distanciaKm").textContent = "N/A"; 
        
        // Convertir de KG a Toneladas
        const pesoTn = envio.kg_origen ? (envio.kg_origen / 1000).toFixed(1) : 0;
        document.getElementById("peso").value = pesoTn;
        document.getElementById("granoNombre").value = envio.tipo_grano || "General";

        // Mapeo de campos editables
        const estadoFormateado = normalizarEnum(envio.estado_actual);
        const prioridadFormateada = normalizarEnum(envio.prioridad_ia);

        setSelectValue(selectEstado, estadoFormateado);
        setSelectValue(selectPrioridad, prioridadFormateada);

        estadoOriginal = estadoFormateado;
        prioridadOriginal = prioridadFormateada;

        aplicarPermisosSegunRol();
        await cargarHistorial();

    } catch (error) {
        console.error(error);
        await Swal.fire({ icon: "error", title: "Error al visualizar el envío", showConfirmButton: false, timer: 1500 });
        window.location.href = "./busqueda.html";
    }
}

// ─── CARGAR HISTORIAL DE AUDITORÍA ───
async function cargarHistorial() {
    const tbody = document.getElementById("tbodyHistorial");
    
    try {
        const response = await fetch(`${API_URL}/${id}/historial`, { headers: authHeaders });
        if (!response.ok) throw new Error("Error al obtener registros");

        const registros = await response.json();

        if (!registros || registros.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted small py-3">Sin registros de cambios en la ruta</td></tr>`;
            return;
        }

        tbody.innerHTML = registros.map(reg => {
            // Parseo de la fecha (LocalDateTime) que devuelve Java
            const fecha = reg.fecha_hora ? new Date(reg.fecha_hora) : null;
            const fechaStr = fecha ? fecha.toLocaleDateString("es-AR") : "—";
            const horaStr = fecha ? fecha.toLocaleTimeString("es-AR", {hour: '2-digit', minute:'2-digit'}) : "—";
            
            // Construir la descripción del evento
            let eventoTexto = `Cambio a ${normalizarEnum(reg.estado_nuevo)}`;
            if(reg.estado_anterior) {
                eventoTexto = `De ${normalizarEnum(reg.estado_anterior)} a ${normalizarEnum(reg.estado_nuevo)}`;
            }

            return `
                <tr>
                    <td class="ps-4 small fw-medium text-dark"><i class="bi bi-arrow-right-short text-success"></i> ${eventoTexto}</td>
                    <td class="small text-muted">${fechaStr}</td>
                    <td class="small text-muted">${horaStr}</td>
                    <td class="small text-muted"><i class="bi bi-person-fill"></i> ${reg.usuario?.username || "Sistema"}</td>
                </tr>`;
        }).join("");

    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-danger small py-3">No se pudo cargar la auditoría</td></tr>`;
    }
}

// ─── GUARDAR CAMBIOS (PUT) ───
async function editarEnvio() {
    const nuevoEstado = selectEstado.value;
    const nuevaPrioridad = selectPrioridad.value;

    if (nuevoEstado === estadoOriginal && nuevaPrioridad === prioridadOriginal) {
        Swal.fire({ icon: "info", title: "No hay cambios para guardar", showConfirmButton: false, timer: 1500 });
        return;
    }

    try {
        btnEditar.disabled = true;
        btnEditar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Actualizando...`;

        // Preparamos los datos normalizándolos para Java (Enums en mayúsculas)
        const dto = {
            estado: enumParaJava(nuevoEstado),
            prioridad: enumParaJava(nuevaPrioridad)
        };

        const response = await fetch(`${API_URL}/${id}`, {
            method: "PUT",
            headers: authHeaders,
            body: JSON.stringify(dto)
        });

        if (!response.ok) throw new Error(await response.text());

        await Swal.fire({ icon: "success", title: "Operación actualizada", showConfirmButton: false, timer: 1500 });
        window.location.href = "./busqueda.html";

    } catch (error) {
        console.error(error);
        Swal.fire({ icon: "error", title: "No se pudo actualizar", text: error.message });
    } finally {
        btnEditar.disabled = false;
        btnEditar.innerHTML = "Guardar Cambios";
    }
}

form.addEventListener("submit", function (e) {
    e.preventDefault();
    editarEnvio();
});

// Inicializar
cargarDetalle();
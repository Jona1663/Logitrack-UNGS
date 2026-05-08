// viajeChofer.js
const API_ENVIO = `${API_BASE_URL}/envios`;
const tokenAuth = sessionStorage.getItem("token");

const headers = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${tokenAuth}`
};

let envioActual = null;

// Mapa de transiciones de estado
const FLUJO_LOGISTICO = {
    "PENDIENTE": { label: "En Tránsito", siguiente: "EN_TRANSITO", btnText: "Iniciar Viaje" },
    "EN_TRANSITO": { label: "En Punto de Recolección", siguiente: "EN_PUNTO_DE_RECOLECCION", btnText: "Llegué a Carga" },
    "EN_PUNTO_DE_RECOLECCION": { label: "En Reparto", siguiente: "EN_REPARTO", btnText: "Iniciar Reparto" },
    "EN_REPARTO": { label: "Entregado", siguiente: "ENTREGADO", btnText: "Confirmar Entrega" },
    "ENTREGADO": { label: "Finalizado", siguiente: null, btnText: "Viaje Completado" }
};

document.addEventListener("DOMContentLoaded", cargarViajeActivo);

async function cargarViajeActivo() {
    try {
        const res = await fetch(`${API_ENVIO}/mis-asignaciones`, { headers });
        const viajes = await res.json();

        if (viajes.length === 0) {
            document.getElementById("pantallaVacia").classList.remove("d-none");
            document.getElementById("pantallaViaje").classList.add("d-none");
            return;
        }

        envioActual = viajes[0]; // Tomamos el primer viaje activo
        renderizarUI();
        
    } catch (error) {
        console.error("Error cargando viaje:", error);
    }
}

function renderizarUI() {
    document.getElementById("pantallaVacia").classList.add("d-none");
    document.getElementById("pantallaViaje").classList.remove("d-none");

    document.getElementById("txtIdEnvio").textContent = `#${envioActual.id_envio}`;
    document.getElementById("txtOrigen").textContent = envioActual.origen.nombre_lugar;
    document.getElementById("txtDireccionOrigen").textContent = envioActual.origen.direccion;
    document.getElementById("txtDestino").textContent = envioActual.destino.nombre_lugar;
    document.getElementById("txtDireccionDestino").textContent = envioActual.destino.direccion;

    const estadoInfo = FLUJO_LOGISTICO[envioActual.estado_actual];
    const btnPrincipal = document.getElementById("btnAccionPrincipal");
    const badge = document.getElementById("badgeEstado");

    badge.textContent = envioActual.estado_actual.replace(/_/g, ' ');

    if (estadoInfo && estadoInfo.siguiente) {
        btnPrincipal.textContent = estadoInfo.btnText;
        btnPrincipal.disabled = false;
        btnPrincipal.onclick = () => actualizarEstado(estadoInfo.siguiente);
    } else {
        btnPrincipal.textContent = "Viaje Finalizado";
        btnPrincipal.disabled = true;
        btnPrincipal.classList.replace("btn-success", "btn-secondary");
    }
}

async function actualizarEstado(nuevoEstado) {
    const { isConfirmed } = await Swal.fire({
        title: '¿Confirmar cambio?',
        text: `Pasarás al estado: ${nuevoEstado.replace(/_/g, ' ')}`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#1b4332',
        confirmButtonText: 'Sí, confirmar'
    });

    if (!isConfirmed) return;

    try {
        const res = await fetch(`${API_ENVIO}/${envioActual.id_envio}/estado`, {
            method: "PATCH",
            headers,
            body: JSON.stringify({ estado_nuevo: nuevoEstado })
        });

        if (res.ok) {
            envioActual.estado_actual = nuevoEstado;
            renderizarUI();
            Swal.fire("¡Éxito!", "Estado actualizado", "success");
        }
    } catch (error) {
        Swal.fire("Error", "No se pudo actualizar el estado", "error");
    }
}

// Lógica de Incidencias
document.getElementById("btnEnviarIncidencia").addEventListener("click", async () => {
    const descripcion = document.getElementById("txtIncidencia").value.trim();
    if (!descripcion) return;

    try {
        const res = await fetch(`${API_ENVIO}/${envioActual.id_envio}/incidencias`, {
            method: "POST",
            headers,
            body: JSON.stringify({ descripcion })
        });

        if (res.ok) {
            Swal.fire("Enviado", "La incidencia fue registrada", "success");
            document.getElementById("txtIncidencia").value = "";
            bootstrap.Offcanvas.getInstance(document.getElementById("panelIncidencias")).hide();
        }
    } catch (error) {
        Swal.fire("Error", "No se pudo enviar el reporte", "error");
    }
});
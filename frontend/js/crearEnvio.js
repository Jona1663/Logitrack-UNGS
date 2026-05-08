// API_BASE_URL se define en frontend/js/config.js

// Le cambiamos el nombre a 'tokenAuth' para que no choque con sesion.js
const tokenAuth = sessionStorage.getItem("token");

const authHeaders = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${tokenAuth}` // Usamos el nuevo nombre aquí
};

// ─── VARIABLES GLOBALES PARA ALMACENAR CATÁLOGOS REALES ───
let dbClientes = [];
let dbEstablecimientos = [];
let dbGranos = [];

// ─── REFERENCIAS AL DOM ───
const form = document.querySelector("form");
const btnCrear = document.getElementById("btnCrearEnvio");
const checkAcepto = document.getElementById("flexCheckChecked");

const inputBuscarCliente = document.getElementById("inputBuscarCliente");
const listaResultadosCliente = document.getElementById("listaResultadosCliente");
const clienteCuitSeleccionado = document.getElementById("clienteCuitSeleccionado");

const selectOrigen = document.getElementById("selectOrigen");
const selectDestino = document.getElementById("selectDestino");

const inputCtg = document.getElementById("inputCtg");
const inputCpe = document.getElementById("inputCpe");
const selectChofer = document.getElementById("selectChofer");
const selectCamion = document.getElementById("selectCamion");

const pesoInput = document.getElementById("peso");
const inputBuscarGrano = document.getElementById("inputBuscarGrano");
const listaResultadosGrano = document.getElementById("listaResultadosGrano");
const granoIdSeleccionado = document.getElementById("granoIdSeleccionado"); // Guardará el STRING del Enum (ej. "SOJA")

// ─── CARGA INICIAL DE CATÁLOGOS DESDE EL BACKEND ───
document.addEventListener("DOMContentLoaded", async () => {
    try {
        // 1. Cargar Metadatos (Tipos de Grano)
        const resMeta = await fetch(`${API_BASE_URL}/catalogos/metadatos`, { headers: authHeaders });
        const metadatos = await resMeta.json();
        // Es un array de String. Buscamos ambas opciones y si falla, devolvemos un array vacío para que no explote
        dbGranos = metadatos.tiposGrano || metadatos.tipos_grano || [];

        // 2. Cargar Choferes
        const resChoferes = await fetch(`${API_BASE_URL}/catalogos/choferes`, { headers: authHeaders });
        const choferes = await resChoferes.json();
        selectChofer.innerHTML = '<option value="" selected>Seleccione un chofer...</option>';
        choferes.forEach(c => {
            // Asumimos que persona_asociada trae el nombre. Si no, mostramos la licencia.
            const nombre = c.persona_asociada ? `${c.persona_asociada.nombre} ${c.persona_asociada.apellido}` : `Chofer ID: ${c.id_chofer}`;
            selectChofer.innerHTML += `<option value="${c.id_chofer}">${nombre} (Lic: ${c.nro_licencia})</option>`;
        });

        // 3. Cargar Camiones
        const resCamiones = await fetch(`${API_BASE_URL}/catalogos/camiones`, { headers: authHeaders });
        const camiones = await resCamiones.json();
        selectCamion.innerHTML = '<option value="" selected>Seleccione un camión...</option>';
        camiones.forEach(c => {
            selectCamion.innerHTML += `<option value="${c.patente}">Patente: ${c.patente} (Tara: ${c.tara_vacio_kg}kg)</option>`;
        });

        // 4. Cargar Clientes y Establecimientos para el buscador
        const resEmp = await fetch(`${API_BASE_URL}/catalogos/empresas`, { headers: authHeaders });
        dbClientes = await resEmp.json();

        const resEst = await fetch(`${API_BASE_URL}/catalogos/establecimientos`, { headers: authHeaders });
        dbEstablecimientos = await resEst.json();

    } catch (error) {
        console.error("Error al cargar catálogos:", error);
        Swal.fire("Error", "No se pudieron cargar los catálogos del servidor.", "error");
    }
});

// ─── LÓGICA BUSCADOR DE CLIENTE ───
inputBuscarCliente.addEventListener("input", (e) => {
    const texto = e.target.value.toLowerCase();
    listaResultadosCliente.innerHTML = "";
    clienteCuitSeleccionado.value = "";
    resetearEstablecimientos();

    if (texto.length < 2) {
        listaResultadosCliente.classList.add("d-none");
        return;
    }

    const resultados = dbClientes.filter(c =>
        c.razon_social.toLowerCase().includes(texto) || c.cuit.includes(texto)
    );

    if (resultados.length > 0) {
        listaResultadosCliente.classList.remove("d-none");
        resultados.forEach(cliente => {
            const li = document.createElement("li");
            li.className = "list-group-item cliente-item border-0 border-bottom py-2";
            li.innerHTML = `
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <span class="fw-bold text-dark d-block">${cliente.razon_social}</span>
                        <small class="text-muted">CUIT: ${cliente.cuit} | ${cliente.tipo_empresa}</small>
                    </div>
                </div>`;

            // Usamos "mousedown" en lugar de "click"
            li.addEventListener("mousedown", () => {
                inputBuscarCliente.value = cliente.razon_social;
                clienteCuitSeleccionado.value = cliente.cuit;
                listaResultadosCliente.classList.add("d-none");
                cargarEstablecimientos(cliente.cuit);

                // Forzamos a Pristine a validar ahora que sí hay datos
                if (typeof pristine !== 'undefined') {
                    pristine.validate(inputBuscarCliente);
                }
            });
            listaResultadosCliente.appendChild(li);
        });
    } else {
        listaResultadosCliente.classList.add("d-none");
    }
});

// ─── LÓGICA ESTABLECIMIENTOS ───
function cargarEstablecimientos(cuit) {
    // Filtramos los establecimientos donde el cuit del objeto empresa coincida
    const estFiltrados = dbEstablecimientos.filter(e => e.empresa.cuit === cuit);

    selectOrigen.innerHTML = '<option value="" selected>Seleccione el punto de carga...</option>';
    selectDestino.innerHTML = '<option value="" selected>Seleccione el punto de descarga...</option>';

    estFiltrados.forEach(est => {
        selectOrigen.innerHTML += `<option value="${est.id_establecimiento}">${est.nombre_lugar} (${est.direccion})</option>`;
        selectDestino.innerHTML += `<option value="${est.id_establecimiento}">${est.nombre_lugar} (${est.direccion})</option>`;
    });

    selectOrigen.disabled = false;
    selectDestino.disabled = false;
}

function resetearEstablecimientos() {
    selectOrigen.innerHTML = '<option value="" selected>Seleccione un cliente primero...</option>';
    selectDestino.innerHTML = '<option value="" selected>Seleccione un cliente primero...</option>';
    selectOrigen.disabled = true;
    selectDestino.disabled = true;
}

// ─── LÓGICA BUSCADOR DE GRANOS (DESDE ENUM) ───
inputBuscarGrano.addEventListener("input", (e) => {
    const texto = e.target.value.toLowerCase();
    listaResultadosGrano.innerHTML = "";
    granoIdSeleccionado.value = "";

    if (texto.length < 1) {
        listaResultadosGrano.classList.add("d-none");
        return;
    }

    // dbGranos es un array simple: ["SOJA", "TRIGO", ...]
    const resultados = dbGranos.filter(g => g.toLowerCase().includes(texto));

    if (resultados.length > 0) {
        listaResultadosGrano.classList.remove("d-none");
        resultados.forEach(grano => {
            const li = document.createElement("li");
            li.className = "list-group-item cliente-item border-0 border-bottom py-2 fw-medium";
            li.textContent = grano;

            // Usamos "mousedown" en lugar de "click"
            li.addEventListener("mousedown", () => {
                inputBuscarGrano.value = grano;
                granoIdSeleccionado.value = grano;
                listaResultadosGrano.classList.add("d-none");

                // Forzamos a Pristine a validar ahora que sí hay datos
                if (typeof pristine !== 'undefined') {
                    pristine.validate(inputBuscarGrano);
                }
            });
            listaResultadosGrano.appendChild(li);
        });
    } else {
        listaResultadosGrano.classList.add("d-none");
    }
});

// Ocultar listas flotantes
document.addEventListener("click", (e) => {
    if (!inputBuscarCliente.contains(e.target)) listaResultadosCliente.classList.add("d-none");
    if (!inputBuscarGrano.contains(e.target)) listaResultadosGrano.classList.add("d-none");
});

// Checkbox
checkAcepto.addEventListener('change', function () {
    btnCrear.disabled = !this.checked;
});

// ─── ENVÍO DEL FORMULARIO AL BACKEND ───
form.addEventListener("submit", async function (e) {
    e.preventDefault();

    // Validar manualmente si los campos nuevos están completos
    if (!inputCtg.value || !inputCpe.value || !selectChofer.value || !selectCamion.value || !selectOrigen.value || !granoIdSeleccionado.value) {
        Swal.fire({
            icon: "warning",
            title: "Faltan datos",
            text: "Por favor, complete todos los campos obligatorios.",
            confirmButtonText: "Entendido"
        });
        return;
    }

    // Conversión de Toneladas (Frontend) a Kilogramos (Backend)
    const toneladas = parseFloat(pesoInput.value) || 0;
    const kilos = Math.round(toneladas * 1000);

    // Armamos el objeto exactamente como lo pide EnvioRequestDTO.java
    const envioDTO = {
        tracking_ctg: inputCtg.value.trim(),
        cpe: inputCpe.value.trim(),
        id_origen: parseInt(selectOrigen.value),
        id_destino: parseInt(selectDestino.value),
        id_chofer: parseInt(selectChofer.value),
        patente_camion: selectCamion.value,
        tipo_grano: granoIdSeleccionado.value,
        prioridad_ia: "ALTA", // Aquí puedes mantener tu lógica de Python si la necesitas a futuro
        kg_origen: kilos
        // No enviamos id_usuario_creador porque el backend lo saca del Token
    };

    try {
        btnCrear.disabled = true;
        btnCrear.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Registrando...`;

        const response = await fetch(`${API_BASE_URL}/envios`, {
            method: "POST",
            headers: authHeaders,
            body: JSON.stringify(envioDTO)
        });

        if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(errorMsg || "Error al registrar el viaje");
        }

        form.reset();
        await Swal.fire({
            position: "center",
            icon: "success",
            title: "Viaje registrado con éxito",
            showConfirmButton: false,
            timer: 1500
        });
        window.location.href = "./menu.html";

    } catch (error) {
        console.error(error);
        Swal.fire({
            position: "center",
            icon: "error",
            title: "No se pudo registrar el viaje",
            text: error.message,
            showConfirmButton: true
        });
    } finally {
        btnCrear.disabled = !checkAcepto.checked;
        btnCrear.innerHTML = "Registrar Viaje";
    }
});
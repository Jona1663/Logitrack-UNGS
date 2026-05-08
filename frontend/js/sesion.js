// 1. Verificación de Seguridad (Auth Guard)
const usuarioJSON = sessionStorage.getItem("usuarioLogueado");
const token = sessionStorage.getItem("token");

// Si no hay usuario o no hay token, lo expulsamos al index
if (!usuarioJSON || !token) {
    window.location.href = "../index.html";
}

const usuario = JSON.parse(usuarioJSON);

// 2. Cargar datos en la Interfaz
// Usamos usuario.username porque así lo devuelve tu LoginResponseDTO
const nombreUI = document.getElementById("nombreUsuario");
if (nombreUI) nombreUI.textContent = usuario.username;

const elRol = document.getElementById("rolUsuario");
if (elRol) {
    // Limpiamos el rol por si Spring Boot envía "ROLE_SUPERVISOR" y lo ponemos bonito
    const rolMostrar = usuario.rol.replace('ROLE_', '').toLowerCase();
    elRol.textContent = rolMostrar;
}

// 3. Lógica de Cierre de Sesión
const btnCerrar = document.getElementById("btnCerrarSesion");
if (btnCerrar) {
    btnCerrar.addEventListener("click", () => {
        // Es vital eliminar el token JWT además de los datos del usuario
        sessionStorage.removeItem("usuarioLogueado");
        sessionStorage.removeItem("token");
        window.location.href = "../index.html";
    });
}

// 4. Mostrar panel de historial en el Menú (solo si es la pantalla menu.html)
// Spring Boot envía "SUPERVISOR", lo pasamos a minúsculas para comparar fácilmente
const rolNormalizado = usuario.rol.toLowerCase().replace('role_', '');
const cardHistorial = document.getElementById("cardHistorial");
if (cardHistorial) {
    if (rolNormalizado === "supervisor") {
        cardHistorial.classList.remove("d-none");
    } else {
        cardHistorial.classList.add("d-none");
    }
}
// Asumo que API_BASE_URL está definido globalmente en js/config.js
const API_URL = `${API_BASE_URL}/auth/login`;

async function login() {
    // 1. Capturamos los elementos del DOM
    // const inputUsername = document.getElementById("exampleInputEmail1").value.trim();

    const inputUsernameElement = document.getElementById("inputUsername");
    const inputPasswordElement = document.getElementById("inputPassword"); // Actualizado según el nuevo HTML
    const mensajeError = document.getElementById("mensajeError");
    const btnIngresar = document.getElementById("btnIngresar");

    const usernameValue = inputUsernameElement.value.trim();
    const passwordValue = inputPasswordElement.value.trim();

    // Ocultar error previo y restaurar texto
    mensajeError.classList.add("d-none");

    // 2. Validación básica en Frontend
    if (!usernameValue || !passwordValue) {
        mostrarError("Por favor, complete ambos campos.");
        return; // Detenemos la ejecución si faltan datos
    }

    // 3. Activar estado de "Cargando" en el botón
    const textoOriginalBoton = btnIngresar.innerHTML;
    btnIngresar.disabled = true;
    btnIngresar.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Autenticando...`;

    try {
        // 4. Armamos la petición POST para Spring Boot
        const res = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                username: usernameValue, // Mapea exactamente con tu LoginRequestDTO
                password: passwordValue // Mapea exactamente con tu LoginRequestDTO
            })
        });

        // 5. Manejo de respuestas y manejo de errores de autenticación (Ej: Status 401 Unauthorized)
        if (!res.ok) {
            if (res.status === 401 || res.status === 403) {
                throw new Error("Credenciales incorrectas o usuario inactivo.");
            }
            throw new Error(`Error en el servidor: código ${res.status}`);
        }

        // Si el login es exitoso, procesamos la respuesta (LoginResponseDTO)
        const data = await res.json();

        // 6. Proceso exitoso
        if (data.token) {
            // Guardamos el JWT y los datos del usuario en sessionStorage
            sessionStorage.setItem("token", data.token);
            sessionStorage.setItem("usuarioLogueado", JSON.stringify({
                username: data.username,
                rol: data.rol
            }));

            // NORMALIZAMOS EL ROL PARA EVALUAR LA REDIRECCIÓN
            const rolUser = data.rol.toLowerCase().replace('role_', '');

            // Redirigimos según el rol
            if (rolUser === 'chofer') {
                window.location.href = "./views/viajeChofer.html";
            } else {
                window.location.href = "./views/menu.html"; // Operadores, Supervisores y Admins van al panel
            }
        } else {
            throw new Error("Respuesta inválida del servidor.");
        }
    } catch (error) {
        console.error("Error en el login:", error);
        // Mostrar mensaje de error en la interfaz
        mostrarError(error.message || "Error al conectar con el servidor. Intente nuevamente.");

        // Limpiamos el campo de contraseña por seguridad y UX tras un fallo. Tambien se limpia el campo de usuario.
        inputPasswordElement.value = "";
        inputUsernameElement.value = "";
        // inputPasswordElement.focus();
        inputUsernameElement.focus();


    } finally {
        // 7. Restaurar estado del botón, pase lo que pase
        btnIngresar.disabled = false;
        btnIngresar.innerHTML = textoOriginalBoton;
    }
}

// Función auxiliar para mostrar el error de forma limpia
function mostrarError(mensaje) {
    const mensajeError = document.getElementById("mensajeError");
    // Asumiendo que agregaste el id="errorText" al <span> dentro de la alerta en el nuevo HTML
    const spanError = document.getElementById("errorText") || mensajeError.querySelector("span");
    spanError.textContent = mensaje;
    mensajeError.classList.remove("d-none");
}

// Event Listener
document.getElementById("loginForm").addEventListener("submit", function (event) {
    // Es CRÍTICO prevenir el comportamiento por defecto (que recarga la página)
    event.preventDefault();
    login();
});

//Script para el toggle de contraseña
document.getElementById('togglePassword').addEventListener('click', function (e) {
    const passwordInput = document.getElementById('inputPassword');
    const icon = this.querySelector('i');
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        icon.classList.remove('bi-eye');
        icon.classList.add('bi-eye-slash');
    } else {
        passwordInput.type = 'password';
        icon.classList.remove('bi-eye-slash');
        icon.classList.add('bi-eye');
    }
});
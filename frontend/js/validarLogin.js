// API_BASE_URL se define en frontend/js/config.js
const API_URL = `${API_BASE_URL}/auth/login`;

async function login() {
    // 1. Capturamos los valores del HTML
    // Nota: Aunque el HTML dice "Email", el backend espera que la variable se llame "username"
    const inputUsername = document.getElementById("exampleInputEmail1").value.trim();
    const inputPassword = document.getElementById("exampleInputPassword1").value.trim();
    const mensajeError = document.getElementById("mensajeError");

    // Ocultar error previo
    mensajeError.classList.add("d-none");

    try {
        // 2. Armamos la petición POST para Spring Boot
        const res = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                username: inputUsername, // Mapea exactamente con tu LoginRequestDTO
                password: inputPassword  // Mapea exactamente con tu LoginRequestDTO
            })
        });

        // 3. Manejo de errores de autenticación (Ej: Status 401 Unauthorized)
        if (!res.ok) {
            if (res.status === 401) {
                throw new Error("Credenciales incorrectas o usuario inactivo.");
            }
            throw new Error(`Error en el servidor: ${res.status}`);
        }

        // 4. Si el login es exitoso, procesamos la respuesta (LoginResponseDTO)
        const data = await res.json();

        if (data.token) {
            // Guardamos el JWT y los datos del usuario en sessionStorage
            sessionStorage.setItem("token", data.token);
            sessionStorage.setItem("usuarioLogueado", JSON.stringify({
                username: data.username,
                rol: data.rol
            }));
            
            // Redirigimos al menú principal
            window.location.href = "./views/menu.html";
        }

    } catch (error) {
        console.error("Error en el login:", error);
        // Mostrar mensaje de error en la interfaz
        mensajeError.querySelector("span").textContent = error.message || "Error al conectar con el servidor. Intente nuevamente.";
        mensajeError.classList.remove("d-none");
    }
}

document.getElementById("btnIngresar").addEventListener("click", login);
// ============================================================
// CONFIGURACIÓN DE LA API
// ============================================================
const API_URL = 'http://localhost:8080/api/personal';

// ============================================================
// ELEMENTOS DEL DOM
// ============================================================
const form = document.getElementById('adminLoginForm');
const alertMessage = document.getElementById('alertMessage');
const usuarioInput = document.getElementById('usuario');
const claveInput = document.getElementById('clave');

// ============================================================
// FUNCIONES AUXILIARES
// ============================================================

/**
 * Muestra un mensaje toast en la parte superior de la pantalla
 * @param {string} mensaje - El mensaje a mostrar
 * @param {string} tipo - 'success' o 'error'
 */
function mostrarMensaje(mensaje, tipo) {
    console.log(`[${tipo.toUpperCase()}] ${mensaje}`);

    // Agregar icono según el tipo
    const icono = tipo === 'success' ? '✅' : '❌';

    alertMessage.innerHTML = `<strong>${icono} ${mensaje}</strong>`;
    alertMessage.className = `alert ${tipo}`;
    alertMessage.style.display = 'block';

    // Auto-ocultar después de 8 segundos
    setTimeout(() => {
        ocultarMensaje();
    }, 8000);
}

/**
 * Oculta el mensaje con animación
 */
function ocultarMensaje() {
    alertMessage.style.opacity = '0';
    alertMessage.style.transform = 'translateX(-50%) translateY(-50px)';

    setTimeout(() => {
        alertMessage.style.display = 'none';
        alertMessage.style.opacity = '1';
        alertMessage.style.transform = 'translateX(-50%) translateY(0)';
    }, 300);
}

/**
 * Cambia el estado del botón de submit
 * @param {string} texto - Texto del botón
 * @param {boolean} deshabilitado - Si está deshabilitado o no
 */
function cambiarEstadoBoton(texto, deshabilitado) {
    const submitBtn = form.querySelector('.submit-btn');
    submitBtn.textContent = texto;
    submitBtn.disabled = deshabilitado;
}

/**
 * Valida el formato del usuario
 * @param {string} usuario - Usuario a validar
 * @returns {boolean} - true si es válido
 */
function validarUsuario(usuario) {
    // Usuario debe tener al menos 3 caracteres y solo letras/números
    const regex = /^[a-zA-Z0-9]{3,50}$/;
    return regex.test(usuario);
}

// ============================================================
// MANEJO DEL FORMULARIO DE LOGIN
// ============================================================

form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const usuario = usuarioInput.value.trim();
    const clave = claveInput.value.trim();

    // Validación básica en frontend
    if (!usuario || !clave) {
        mostrarMensaje('Por favor complete todos los campos', 'error');
        return;
    }

    // Validar formato de usuario
    if (!validarUsuario(usuario)) {
        mostrarMensaje('El usuario solo puede contener letras y números (mínimo 3 caracteres)', 'error');
        return;
    }

    console.log('========== INICIANDO LOGIN ==========');
    console.log('Usuario:', usuario);
    console.log('Clave:', '[OCULTA POR SEGURIDAD]');

    try {
        // Cambiar botón a estado de carga
        cambiarEstadoBoton('VALIDANDO...', true);

        console.log('Enviando petición a:', `${API_URL}/login`);

        // Llamada a la API
        const response = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                usuario: usuario,
                clave: clave
            })
        });

        console.log('Response status:', response.status);
        console.log('Response OK:', response.ok);

        const data = await response.json();
        console.log('Response data:', data);

        // Restaurar botón
        cambiarEstadoBoton('INICIAR SESIÓN', false);

        if (data.success) {
            // ✅ LOGIN EXITOSO
            mostrarMensaje(data.mensaje, 'success');
            console.log('✓ LOGIN EXITOSO');
            console.log('Datos del empleado:', data.data);

            // Guardar datos en sessionStorage
            sessionStorage.setItem('empleado', JSON.stringify(data.data));
            sessionStorage.setItem('empleadoID', data.data.empleadoID);
            sessionStorage.setItem('nombreCompleto', data.data.nombreCompleto);
            sessionStorage.setItem('cargo', data.data.cargo);

            // Limpiar formulario
            form.reset();

            // Redirigir al dashboard después de 2 segundos
            setTimeout(() => {
                console.log('Redirigiendo al dashboard...');
                window.location.href = 'main.html';
            }, 2000);

        } else {
            // ❌ LOGIN FALLIDO
            mostrarMensaje(data.mensaje, 'error');
            console.log('✗ LOGIN FALLIDO');

            // Limpiar contraseña y enfocar
            claveInput.value = '';
            claveInput.focus();
        }

    } catch (error) {
        // ⚠️ ERROR DE RED O SERVIDOR
        console.error('========== ERROR DE RED ==========');
        console.error('Error completo:', error);

        let mensajeError = 'Error de conexión. ';

        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            mensajeError += 'Verifica que el backend esté corriendo en el puerto 8080.';
        } else {
            mensajeError += 'Por favor intente nuevamente.';
        }

        mostrarMensaje(mensajeError, 'error');

        // Restaurar botón
        cambiarEstadoBoton('INICIAR SESIÓN', false);
    }
});

// ============================================================
// VALIDACIONES EN TIEMPO REAL
// ============================================================

// Prevenir espacios en el campo de usuario
usuarioInput.addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\s/g, '');
});

// Convertir usuario a minúsculas automáticamente
usuarioInput.addEventListener('blur', (e) => {
    e.target.value = e.target.value.toLowerCase();
});

// Detectar Enter para submit
usuarioInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        claveInput.focus();
    }
});

// ============================================================
// VERIFICAR SI YA HAY UNA SESIÓN ACTIVA
// ============================================================

window.addEventListener('load', () => {
    console.log('✓ admin-login.js cargado correctamente');
    console.log('API URL configurada:', API_URL);

    // Verificar si ya hay una sesión activa
    const empleado = sessionStorage.getItem('empleado');
    if (empleado) {
        console.log('⚠️ Ya hay una sesión activa');
        const empleadoData = JSON.parse(empleado);
        mostrarMensaje(`Ya tienes una sesión activa como ${empleadoData.nombreCompleto}`, 'success');

        // Preguntar si quiere continuar con esa sesión
        setTimeout(() => {
            if (confirm('Ya tienes una sesión activa. ¿Deseas continuar al dashboard?')) {
                window.location.href = 'main.html';
            } else {
                // Limpiar sesión
                sessionStorage.clear();
                mostrarMensaje('Sesión anterior cerrada. Puedes iniciar sesión nuevamente.', 'success');
            }
        }, 1000);
    }

    // Enfocar el campo de usuario al cargar
    usuarioInput.focus();
});

// ============================================================
// MANEJO DE ERRORES GLOBALES
// ============================================================

window.addEventListener('error', (e) => {
    console.error('Error global capturado:', e);
});

window.addEventListener('unhandledrejection', (e) => {
    console.error('Promise rechazada:', e.reason);
});
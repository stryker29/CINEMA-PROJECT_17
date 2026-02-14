const API_URL = 'http://localhost:8080/cineestar/api/procesos';
const empleado = JSON.parse(sessionStorage.getItem('empleado'));

if (!empleado) {
    alert('No has iniciado sesión');
    window.location.href = 'admin-login.html';
} else {
    document.getElementById('nombreEmpleado').textContent = empleado.nombreCompleto;
}

// ========== FUNCIONES GENERALES ==========

function mostrarMensaje(mensaje, tipo) {
    const alertMessage = document.getElementById('alertMessage');
    alertMessage.textContent = mensaje;
    alertMessage.className = `alert ${tipo}`;
    alertMessage.style.display = 'block';

    setTimeout(() => {
        alertMessage.style.display = 'none';
    }, 8000);
}

function cambiarTab(tabName) {
    // Ocultar todos los contenidos
    const contents = document.querySelectorAll('.tab-content');
    contents.forEach(content => content.classList.remove('active'));

    // Desactivar todos los botones
    const buttons = document.querySelectorAll('.tab-btn');
    buttons.forEach(btn => btn.classList.remove('active'));

    // Activar la pestaña seleccionada
    document.getElementById(`tab-${tabName}`).classList.add('active');
    event.target.classList.add('active');

    // Cargar datos si es necesario
    if (tabName === 'funcion') {
        cargarPeliculas();
        cargarSalas();
    } else if (tabName === 'cartelera') {
        cargarCartelera();
    }
}

// ========== TAB 1: REGISTRAR PELÍCULA ==========

document.addEventListener('DOMContentLoaded', () => {
    // Contador de caracteres para sinopsis
    const sinopsis = document.getElementById('sinopsis');
    const contador = document.getElementById('contadorSinopsis');

    sinopsis.addEventListener('input', () => {
        contador.textContent = sinopsis.value.length;
    });

    // Establecer fecha mínima y máxima
    const fechaEstreno = document.getElementById('fechaEstreno');
    const hoy = new Date();
    const minFecha = new Date('1900-01-01');
    const maxFecha = new Date();
    maxFecha.setFullYear(hoy.getFullYear() + 1);

    fechaEstreno.min = minFecha.toISOString().split('T')[0];
    fechaEstreno.max = maxFecha.toISOString().split('T')[0];
    fechaEstreno.value = hoy.toISOString().split('T')[0];

    // Establecer fecha/hora mínima para funciones (2 horas futuro)
    const fechaHoraInicio = document.getElementById('fechaHoraInicio');
    const dosHorasFuturo = new Date(hoy.getTime() + 2 * 60 * 60 * 1000);
    fechaHoraInicio.min = dosHorasFuturo.toISOString().slice(0, 16);
    fechaHoraInicio.value = dosHorasFuturo.toISOString().slice(0, 16);
});

// Manejar envío del formulario de película
document.getElementById('formPelicula').addEventListener('submit', async (e) => {
    e.preventDefault();

    const titulo = document.getElementById('titulo').value.trim();
    const genero = document.getElementById('genero').value;
    const duracion = parseInt(document.getElementById('duracion').value);
    const clasificacion = document.getElementById('clasificacion').value;
    const director = document.getElementById('director').value.trim();
    const sinopsis = document.getElementById('sinopsis').value.trim();
    const fechaEstreno = document.getElementById('fechaEstreno').value;

    // Validaciones
    if (titulo.length < 2) {
        mostrarMensaje('El título debe tener al menos 2 caracteres', 'error');
        return;
    }

    if (duracion < 60 || duracion > 240) {
        mostrarMensaje('La duración debe estar entre 60 y 240 minutos', 'error');
        return;
    }

    if (director.length < 3) {
        mostrarMensaje('El nombre del director debe tener al menos 3 caracteres', 'error');
        return;
    }

    if (sinopsis.length < 10) {
        mostrarMensaje('La sinopsis debe tener al menos 10 caracteres', 'error');
        return;
    }

    const datosPelicula = {
        titulo: titulo,
        genero: genero,
        duracion: duracion,
        clasificacion: clasificacion,
        director: director,
        sinopsis: sinopsis,
        fechaEstreno: fechaEstreno,
        creadoPor: empleado.empleadoID
    };

    try {
        const response = await fetch(`${API_URL}/registro/pelicula`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(datosPelicula)
        });

        const data = await response.json();

        if (response.ok && data.estado === 1) {
            mostrarMensaje(`✅ ${data.mensaje}. ID: ${data.peliculaId}`, 'success');

            setTimeout(() => {
                document.getElementById('formPelicula').reset();
                document.getElementById('contadorSinopsis').textContent = '0';
                const hoy = new Date();
                document.getElementById('fechaEstreno').value = hoy.toISOString().split('T')[0];
            }, 2000);
        } else {
            mostrarMensaje(data.mensaje || 'Error al registrar la película', 'error');
        }
    } catch (error) {
        console.error('Error completo:', error);
        mostrarMensaje('Error al conectar con el servidor. Verifique la conexión.', 'error');
    }
});

// ========== TAB 2: REGISTRAR FUNCIÓN ==========

async function cargarPeliculas() {
    try {
        const response = await fetch(`${API_URL}/peliculas/activas`);
        const peliculas = await response.json();

        const select = document.getElementById('peliculaId');
        select.innerHTML = '<option value="">-- Seleccione una película --</option>';

        peliculas.forEach(pelicula => {
            const option = document.createElement('option');
            option.value = pelicula.PeliculaID;
            option.textContent = `${pelicula.Titulo} (${pelicula.Genero})`;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Error al cargar películas:', error);
        mostrarMensaje('Error al cargar la lista de películas', 'error');
    }
}

async function cargarSalas() {
    try {
        const response = await fetch(`${API_URL}/salas/disponibles`);
        const salas = await response.json();

        const select = document.getElementById('salaId');
        select.innerHTML = '<option value="">-- Seleccione una sala --</option>';

        salas.forEach(sala => {
            const option = document.createElement('option');
            option.value = sala.SalaID;
            option.textContent = `${sala.Nombre} - ${sala.TipoSala} (Cap: ${sala.CapacidadTotal})`;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Error al cargar salas:', error);
        mostrarMensaje('Error al cargar la lista de salas', 'error');
    }
}

// Manejar envío del formulario de función
document.getElementById('formFuncion').addEventListener('submit', async (e) => {
    e.preventDefault();

    const peliculaId = parseInt(document.getElementById('peliculaId').value);
    const salaId = parseInt(document.getElementById('salaId').value);
    const fechaHoraInicio = document.getElementById('fechaHoraInicio').value;
    const precioBase = parseFloat(document.getElementById('precioBase').value);

    // Validaciones
    if (!peliculaId) {
        mostrarMensaje('Debe seleccionar una película', 'error');
        return;
    }

    if (!salaId) {
        mostrarMensaje('Debe seleccionar una sala', 'error');
        return;
    }

    if (!fechaHoraInicio) {
        mostrarMensaje('Debe seleccionar fecha y hora', 'error');
        return;
    }

    if (precioBase < 10 || precioBase > 100) {
        mostrarMensaje('El precio debe estar entre S/ 10.00 y S/ 100.00', 'error');
        return;
    }

    // Validar que la fecha sea al menos 2 horas en el futuro
    const fechaSeleccionada = new Date(fechaHoraInicio);
    const ahora = new Date();
    const dosHorasFuturo = new Date(ahora.getTime() + 2 * 60 * 60 * 1000);

    if (fechaSeleccionada < dosHorasFuturo) {
        mostrarMensaje('La función debe programarse con al menos 2 horas de anticipación', 'error');
        return;
    }

    // Convertir fecha a formato SQL Server (YYYY-MM-DD HH:mm:ss)
    const fechaFormateada = fechaHoraInicio.replace('T', ' ') + ':00';

    const datosFuncion = {
        peliculaId: peliculaId,
        salaId: salaId,
        fechaHoraInicio: fechaFormateada,
        precioBase: precioBase,
        creadoPor: empleado.empleadoID
    };

    try {
        const response = await fetch(`${API_URL}/registro/funcion`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(datosFuncion)
        });

        const data = await response.json();

        if (response.ok && data.codigoEstado === 1) {
            mostrarMensaje(`✅ ${data.mensaje}. ID: ${data.funcionId}`, 'success');

            setTimeout(() => {
                document.getElementById('formFuncion').reset();
                const hoy = new Date();
                const dosHorasFuturo = new Date(hoy.getTime() + 2 * 60 * 60 * 1000);
                document.getElementById('fechaHoraInicio').value = dosHorasFuturo.toISOString().slice(0, 16);
            }, 2000);
        } else {
            mostrarMensaje(data.mensaje || 'Error al registrar la función', 'error');
        }
    } catch (error) {
        console.error('Error completo:', error);
        mostrarMensaje('Error al conectar con el servidor. Verifique la conexión.', 'error');
    }
});

// ========== TAB 3: VER CARTELERA ==========

async function cargarCartelera() {
    const container = document.getElementById('carteleraContainer');
    container.innerHTML = '<div class="loading">Cargando cartelera...</div>';

    try {
        const response = await fetch(`${API_URL}/cartelera`);
        const funciones = await response.json();

        if (!funciones || funciones.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>📭 No hay funciones programadas</h3>
                    <p>Comienza registrando una nueva función en la pestaña "Registrar Función"</p>
                </div>
            `;
            return;
        }

        let html = '<div class="cartelera-grid">';

        funciones.forEach(funcion => {
            let fechaInicio = 'Fecha no disponible';
            try {
                if (funcion.FechaHoraInicio) {
                    const fecha = new Date(funcion.FechaHoraInicio);
                    if (!isNaN(fecha.getTime())) {
                        fechaInicio = fecha.toLocaleString('es-PE', {
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit'
                        });
                    }
                }
            } catch (e) {
                console.error('Error al formatear fecha:', e);
            }

            html += `
                <div class="funcion-card">
                    <h4>🎬 ${funcion.Pelicula || 'Sin título'}</h4>
                    <div class="funcion-info">
                        <p><strong>🎞️ Función ID:</strong> ${funcion.FuncionID}</p>
                        <p><strong>🏛️ Sala:</strong> ${funcion.Sala || 'N/A'} (${funcion.TipoSala || 'Estándar'})</p>
                        <p><strong>📅 Fecha y Hora:</strong> ${fechaInicio}</p>
                        <p><strong>💺 Asientos:</strong> ${funcion.AsientosDisponibles || 0} disponibles</p>
                        <p><strong>💰 Precio:</strong> S/ ${(funcion.PrecioBase || 0).toFixed(2)}</p>
                        <p><strong>📊 Estado:</strong> ${funcion.Estado || 'Desconocido'}</p>
                        <p><strong>👤 Creado por:</strong> ${funcion.CreadoPor || 'N/A'}</p>
                    </div>
                </div>
            `;
        });

        html += '</div>';
        container.innerHTML = html;

    } catch (error) {
        console.error('Error completo:', error);
        container.innerHTML = `
            <div class="empty-state">
                <h3>❌ Error al cargar la cartelera</h3>
                <p>No se pudo conectar con el servidor. Verifique su conexión.</p>
            </div>
        `;
    }
}
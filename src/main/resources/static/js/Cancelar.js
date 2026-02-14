const API_URL = 'http://localhost:8080/api/reservas';
const empleado = JSON.parse(sessionStorage.getItem('empleado'));

if (!empleado) {
    alert('No has iniciado sesión');
    window.location.href = 'admin-login.html';
} else {
    document.getElementById('nombreEmpleado').textContent = empleado.nombreCompleto;
}

// ✅ FUNCIÓN PARA CAMBIAR EL TIPO DE BÚSQUEDA
function cambiarTipoBusqueda() {
    const tipoBusqueda = document.getElementById('tipoBusqueda').value;
    const campoCodigo = document.getElementById('campoCodigo');
    const campoNombre = document.getElementById('campoNombre');

    // Ocultar ambos campos
    campoCodigo.classList.remove('active');
    campoNombre.classList.remove('active');

    // Limpiar campos
    document.getElementById('codigoReserva').value = '';
    document.getElementById('nombreCliente').value = '';

    // Ocultar resultados previos
    document.getElementById('resultsSection').style.display = 'none';

    // Mostrar el campo seleccionado
    if (tipoBusqueda === 'codigo') {
        campoCodigo.classList.add('active');
        setTimeout(() => document.getElementById('codigoReserva').focus(), 100);
    } else if (tipoBusqueda === 'nombre') {
        campoNombre.classList.add('active');
        setTimeout(() => document.getElementById('nombreCliente').focus(), 100);
    }
}

function mostrarMensaje(mensaje, tipo) {
    const alertMessage = document.getElementById('alertMessage');
    alertMessage.textContent = mensaje;
    alertMessage.className = `alert ${tipo}`;
    alertMessage.style.display = 'block';

    setTimeout(() => {
        alertMessage.style.display = 'none';
    }, 8000);
}

// ✅ BÚSQUEDA POR CÓDIGO
async function buscarReserva() {
    const codigo = document.getElementById('codigoReserva').value.trim();

    if (!codigo) {
        mostrarMensaje('Por favor ingrese un código de reserva', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_URL}/buscar/codigo/${codigo}`);

        if (!response.ok) {
            if (response.status === 404) {
                mostrarMensaje(`No se encontró ninguna reserva con el código: ${codigo}`, 'error');
                document.getElementById('resultsSection').style.display = 'none';
                return;
            }
            throw new Error(`Error: ${response.status}`);
        }

        const data = await response.json();

        if (data.success && data.data) {
            mostrarReserva(data.data);
        } else {
            mostrarMensaje(data.mensaje || 'No se encontró la reserva', 'error');
            document.getElementById('resultsSection').style.display = 'none';
        }
    } catch (error) {
        console.error('Error completo:', error);
        mostrarMensaje('Error al buscar la reserva. Verifique la conexión con el servidor.', 'error');
        document.getElementById('resultsSection').style.display = 'none';
    }
}

// ✅ BÚSQUEDA POR NOMBRE
async function buscarPorNombre() {
    const nombre = document.getElementById('nombreCliente').value.trim();

    if (!nombre || nombre.length < 3) {
        mostrarMensaje('Por favor ingrese al menos 3 caracteres del nombre', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_URL}/buscar/cliente/${nombre}`);

        if (!response.ok) {
            if (response.status === 404) {
                mostrarMensaje(`No se encontraron reservas para: ${nombre}`, 'error');
                document.getElementById('resultsSection').style.display = 'none';
                return;
            }
            throw new Error(`Error: ${response.status}`);
        }

        const data = await response.json();

        if (data.success && data.data && data.data.length > 0) {
            mostrarMultiplesReservas(data.data);
        } else {
            mostrarMensaje(data.mensaje || 'No se encontraron reservas', 'error');
            document.getElementById('resultsSection').style.display = 'none';
        }
    } catch (error) {
        console.error('Error completo:', error);
        mostrarMensaje('Error al buscar reservas. Verifique la conexión con el servidor.', 'error');
        document.getElementById('resultsSection').style.display = 'none';
    }
}

// ✅ MOSTRAR MÚLTIPLES RESERVAS
function mostrarMultiplesReservas(reservas) {
    const resultsSection = document.getElementById('resultsSection');
    const reservaCard = document.getElementById('reservaCard');

    let html = `
        <div style="background: rgba(255, 0, 0, 0.1); padding: 15px; border-radius: 10px; margin-bottom: 20px; border: 2px solid #ff0000;">
            <h4 style="color: #ff0000; margin: 0;">✅ Se encontraron ${reservas.length} reserva(s)</h4>
        </div>
    `;

    reservas.forEach((reserva, index) => {
        const estadoBadge = reserva.estado === 'Cancelada'
            ? '<span style="color: #ff0000;">❌ CANCELADA</span>'
            : reserva.estado === 'Confirmada'
            ? '<span style="color: #00ff00;">✅ CONFIRMADA</span>'
            : '<span style="color: #ffff00;">⏳ PENDIENTE</span>';

        let fechaInicio = 'Fecha no disponible';
        try {
            if (reserva.fechaHoraInicio) {
                const fecha = new Date(reserva.fechaHoraInicio);
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
            <div class="reserva-card">
                <h4>Código: ${reserva.codigoReserva} - ${estadoBadge}</h4>
                <div class="reserva-info">
                    <div class="info-item">
                        <strong>Cliente:</strong><br>${reserva.cliente || 'No disponible'}
                    </div>
                    <div class="info-item">
                        <strong>Película:</strong><br>${reserva.pelicula || 'No disponible'}
                    </div>
                    <div class="info-item">
                        <strong>Fecha Función:</strong><br>${fechaInicio}
                    </div>
                    <div class="info-item">
                        <strong>Estado:</strong><br>${reserva.estado}
                    </div>
                </div>
                ${reserva.estado !== 'Cancelada' ? `
                    <button class="btn" onclick="seleccionarReserva(${reserva.reservaID}, '${reserva.codigoReserva}')">
                        📋 Seleccionar para cancelar
                    </button>
                ` : `
                    <p style="color: #ff6666; margin-top: 15px; font-weight: bold; text-align: center;">
                        ⚠️ Esta reserva ya fue cancelada
                    </p>
                `}
            </div>
        `;
    });

    reservaCard.innerHTML = html;
    resultsSection.style.display = 'block';
}

// ✅ SELECCIONAR RESERVA PARA CANCELAR
function seleccionarReserva(reservaID, codigoReserva) {
    // Cambiar a búsqueda por código
    document.getElementById('tipoBusqueda').value = 'codigo';
    cambiarTipoBusqueda();

    // Llenar el campo con el código
    document.getElementById('codigoReserva').value = codigoReserva;

    // Buscar automáticamente
    buscarReserva();
}

function mostrarReserva(reserva) {
    const resultsSection = document.getElementById('resultsSection');
    const reservaCard = document.getElementById('reservaCard');

    const estadoBadge = reserva.estado === 'Cancelada'
        ? '<span style="color: #ff0000;">❌ CANCELADA</span>'
        : reserva.estado === 'Confirmada'
        ? '<span style="color: #00ff00;">✅ CONFIRMADA</span>'
        : '<span style="color: #ffff00;">⏳ PENDIENTE</span>';

    let fechaInicio = 'Fecha no disponible';
    try {
        if (reserva.fechaHoraInicio) {
            const fecha = new Date(reserva.fechaHoraInicio);
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
        console.error('Error al formatear fecha función:', e);
    }

    let infoCancelacion = '';
    if (reserva.estado === 'Cancelada') {
        let fechaCancelacion = 'No disponible';
        if (reserva.fechaCancelacion) {
            try {
                const fecha = new Date(reserva.fechaCancelacion);
                if (!isNaN(fecha.getTime())) {
                    fechaCancelacion = fecha.toLocaleString('es-PE', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit'
                    });
                }
            } catch (e) {
                console.error('Error al formatear fecha cancelación:', e);
            }
        }

        infoCancelacion = `
            <div style="background: rgba(255, 0, 0, 0.1); padding: 20px; border-radius: 10px; border: 2px solid #ff0000; margin-top: 20px;">
                <h4 style="color: #ff0000; margin-bottom: 15px;">📋 INFORMACIÓN DE CANCELACIÓN</h4>
                <div class="reserva-info">
                    <div class="info-item">
                        <strong>Fecha Cancelación:</strong><br>${fechaCancelacion}
                    </div>
                    <div class="info-item">
                        <strong>Cancelado Por:</strong><br>${reserva.empleadoNombre || 'No disponible'}
                    </div>
                    <div class="info-item" style="grid-column: 1 / -1;">
                        <strong>Motivo:</strong><br>${reserva.motivo || 'Sin motivo registrado'}
                    </div>
                </div>
                <p style="color: #ff6666; margin-top: 20px; font-weight: bold; text-align: center;">
                    ⚠️ Esta reserva ya fue cancelada y no puede ser procesada nuevamente.
                </p>
            </div>
        `;
    }

    reservaCard.innerHTML = `
        <div class="reserva-card">
            <h4>Código: ${reserva.codigoReserva} - ${estadoBadge}</h4>

            <div class="reserva-info">
                <div class="info-item">
                    <strong>Cliente:</strong><br>${reserva.cliente || 'No disponible'}
                </div>
                <div class="info-item">
                    <strong>Película:</strong><br>${reserva.pelicula || 'No disponible'}
                </div>
                <div class="info-item">
                    <strong>Fecha Función:</strong><br>${fechaInicio}
                </div>
                <div class="info-item">
                    <strong>Estado:</strong><br>${reserva.estado}
                </div>
            </div>

            ${reserva.estado === 'Cancelada' ? infoCancelacion : `
                <div class="cancel-form">
                    <label style="color: #ff6666; font-weight: bold; margin-bottom: 10px; display: block;">
                        Motivo de Cancelación (10-200 caracteres):
                    </label>
                    <textarea id="motivoCancelacion" placeholder="Ingrese el motivo de la cancelación..." maxlength="200"></textarea>
                    <div style="text-align: right; margin-top: 5px; font-size: 12px; color: #666;">
                        <span id="contadorCaracteres">0</span>/200 caracteres
                    </div>
                    <button class="btn" style="margin-top: 15px;" onclick="confirmarCancelacion(${reserva.reservaID})">
                        ❌ Cancelar Reserva
                    </button>
                </div>
            `}
        </div>
    `;

    const textarea = document.getElementById('motivoCancelacion');
    if (textarea) {
        textarea.addEventListener('input', function() {
            const contador = document.getElementById('contadorCaracteres');
            if (contador) {
                contador.textContent = this.value.length;
            }
        });
    }

    resultsSection.style.display = 'block';
}

async function confirmarCancelacion(reservaID) {
    const motivo = document.getElementById('motivoCancelacion').value.trim();

    if (!motivo || motivo.length < 10) {
        mostrarMensaje('El motivo debe tener al menos 10 caracteres', 'error');
        return;
    }

    if (motivo.length > 200) {
        mostrarMensaje('El motivo no puede exceder 200 caracteres', 'error');
        return;
    }

    if (!confirm('¿Está seguro de cancelar esta reserva? Esta acción no se puede deshacer.')) {
        return;
    }

    try {
        const response = await fetch(`${API_URL}/cancelar`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                reservaID: reservaID,
                empleadoID: empleado.empleadoID,
                motivo: motivo
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            mostrarMensaje(errorData.mensaje || 'Error al cancelar la reserva', 'error');
            return;
        }

        const data = await response.json();

        if (data.success) {
            mostrarMensaje(data.mensaje, 'success');
            setTimeout(() => {
                // Limpiar todo
                document.getElementById('tipoBusqueda').value = '';
                document.getElementById('codigoReserva').value = '';
                document.getElementById('nombreCliente').value = '';
                document.getElementById('campoCodigo').classList.remove('active');
                document.getElementById('campoNombre').classList.remove('active');
                document.getElementById('resultsSection').style.display = 'none';
            }, 3000);
        } else {
            mostrarMensaje(data.mensaje, 'error');
        }
    } catch (error) {
        console.error('Error completo:', error);
        mostrarMensaje('Error al cancelar la reserva. Verifique la conexión con el servidor.', 'error');
    }
}

// ✅ Event Listeners para búsqueda con Enter
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('codigoReserva').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            buscarReserva();
        }
    });

    document.getElementById('nombreCliente').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            buscarPorNombre();
        }
    });
});
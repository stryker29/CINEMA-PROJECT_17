const API_BASE_URL = 'http://localhost:8080/api/reservas';

// Formatear fecha
function formatearFecha(fecha) {
    if (!fecha) return 'N/A';
    const date = new Date(fecha);
    return date.toLocaleString('es-PE', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Mostrar alerta
function mostrarAlerta(mensaje, tipo = 'error') {
    const container = document.getElementById('alertContainer');
    container.innerHTML = `<div class="alert ${tipo}">${mensaje}</div>`;
    container.querySelector('.alert').style.display = 'block';
    
    setTimeout(() => {
        container.innerHTML = '';
    }, 5000);
}

// Cambiar entre tabs
function cambiarTab(tab) {
    // Actualizar botones
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');

    // Actualizar secciones
    document.querySelectorAll('.historial-section').forEach(section => {
        section.classList.remove('active');
    });

    if (tab === 'canceladas') {
        document.getElementById('seccion-canceladas').classList.add('active');
    } else if (tab === 'activas') {
        document.getElementById('seccion-activas').classList.add('active');

        // Cargar activas si no se han cargado
        if (!document.getElementById('bodyActivas').hasChildNodes()) {
            cargarReservasActivas();
        }
    }
}

// Cargar cancelaciones
async function cargarCancelaciones() {
    const loading = document.getElementById('loadingCancelaciones');
    const tabla = document.getElementById('tablaCancelaciones');
    const empty = document.getElementById('emptyCancelaciones');
    const tbody = document.getElementById('bodyCancelaciones');

    console.log('🎬 Iniciando carga de cancelaciones...');

    try {
        loading.style.display = 'block';
        tabla.style.display = 'none';
        empty.style.display = 'none';

        console.log('📡 Haciendo fetch a:', `${API_BASE_URL}/auditoria/cancelaciones`);

        const response = await fetch(`${API_BASE_URL}/auditoria/cancelaciones`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        console.log('✅ Status:', response.status);
        console.log('✅ OK?:', response.ok);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        console.log('📦 Respuesta completa:', result);
        console.log('📦 Success?:', result.success);
        console.log('📦 Data:', result.data);
        console.log('📦 Cantidad:', result.cantidad);

        if (result.success && result.data && result.data.length > 0) {
            console.log('✅ Hay', result.data.length, 'cancelaciones');

            // Actualizar estadísticas
            document.getElementById('totalCancelaciones').textContent = result.cantidad || result.data.length;

            // Llenar tabla
            tbody.innerHTML = result.data.map((item, index) => {
                console.log(`📋 Item ${index}:`, item);
                return `
                    <tr>
                        <td><strong>${item.CodigoReserva || item.codigoReserva || 'N/A'}</strong></td>
                        <td>${item.Cliente || item.cliente || 'N/A'}</td>
                        <td>${item.Pelicula || item.pelicula || 'N/A'}</td>
                        <td>${formatearFecha(item.FechaHoraInicio || item.fechaHoraInicio)}</td>
                        <td>${formatearFecha(item.FechaCancelacion || item.fechaCancelacion)}</td>
                        <td>${item.CanceladoPor || item.canceladoPor || 'N/A'}</td>
                        <td><span class="badge badge-cancelada">${item.Rol || item.rol || 'N/A'}</span></td>
                        <td style="max-width: 300px; word-wrap: break-word;">${item.Motivo || item.motivo || 'Sin motivo'}</td>
                    </tr>
                `;
            }).join('');

            console.log('✅ Tabla creada con', result.data.length, 'filas');
            tabla.style.display = 'block';
        } else {
            console.warn('⚠️ No hay datos o success es false');
            console.warn('   - success:', result.success);
            console.warn('   - data existe?:', !!result.data);
            console.warn('   - data.length:', result.data?.length);
            empty.style.display = 'block';
        }
    } catch (error) {
        console.error('❌ Error completo:', error);
        console.error('❌ Error message:', error.message);
        console.error('❌ Error stack:', error.stack);
        mostrarAlerta('❌ Error al cargar el historial: ' + error.message);
        empty.style.display = 'block';
    } finally {
        loading.style.display = 'none';
        console.log('🏁 Carga finalizada');
    }
}

// Cargar reservas activas
async function cargarReservasActivas() {
    const loading = document.getElementById('loadingActivas');
    const tabla = document.getElementById('tablaActivas');
    const empty = document.getElementById('emptyActivas');
    const tbody = document.getElementById('bodyActivas');

    console.log('🎬 Iniciando carga de reservas activas...');

    try {
        loading.style.display = 'block';
        tabla.style.display = 'none';
        empty.style.display = 'none';

        console.log('📡 Haciendo fetch a:', `${API_BASE_URL}/auditoria/activas`);

        const response = await fetch(`${API_BASE_URL}/auditoria/activas`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        console.log('✅ Status:', response.status);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        console.log('📦 Reservas activas:', result);

        if (result.success && result.data && result.data.length > 0) {
            console.log('✅ Hay', result.data.length, 'reservas activas');

            // Actualizar estadísticas
            document.getElementById('totalActivas').textContent = result.cantidad || result.data.length;

            // Llenar tabla
            tbody.innerHTML = result.data.map((item, index) => {
                console.log(`📋 Reserva ${index}:`, item);
                const estadoBadge = item.Estado === 'Confirmada' ? 'badge-confirmada' : 'badge-pendiente';
                return `
                    <tr>
                        <td><strong>${item.CodigoReserva || item.codigoReserva || 'N/A'}</strong></td>
                        <td><span class="badge ${estadoBadge}">${item.Estado || item.estado || 'N/A'}</span></td>
                        <td>${item.Cliente || item.cliente || 'N/A'}</td>
                        <td>${item.Pelicula || item.pelicula || 'N/A'}</td>
                        <td>${item.Sala || item.sala || 'N/A'}</td>
                        <td>${formatearFecha(item.FechaHoraInicio || item.fechaHoraInicio)}</td>
                        <td>${item.CantidadAsientos || item.cantidadAsientos || 0}</td>
                        <td>S/ ${(item.PrecioTotal || item.precioTotal || 0).toFixed(2)}</td>
                        <td>${item.RegistradoPor || item.registradoPor || 'N/A'}</td>
                    </tr>
                `;
            }).join('');

            console.log('✅ Tabla creada con', result.data.length, 'filas');
            tabla.style.display = 'block';
        } else {
            console.warn('⚠️ No hay reservas activas');
            empty.style.display = 'block';
        }
    } catch (error) {
        console.error('❌ Error:', error);
        mostrarAlerta('❌ Error al cargar reservas activas: ' + error.message);
        empty.style.display = 'block';
    } finally {
        loading.style.display = 'none';
        console.log('🏁 Carga finalizada');
    }
}

// Cargar cancelaciones al iniciar
document.addEventListener('DOMContentLoaded', () => {
    cargarCancelaciones();
});
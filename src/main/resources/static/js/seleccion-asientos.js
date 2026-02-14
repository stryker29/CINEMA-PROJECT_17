// ============================================================
// 🎬 SISTEMA DE SELECCIÓN DE ASIENTOS - CINESTAR (CORREGIDO)
// ============================================================

// ✅ URLs corregidas
const API_ROOT_URL = 'http://localhost:8080/api';
const API_RESERVAS_URL = `${API_ROOT_URL}/reservas`;
const API_FUNCIONES_URL = `${API_ROOT_URL}/funciones`;
const API_ASIENTOS_URL = `${API_RESERVAS_URL}/asientos`;

// ==================== VARIABLES GLOBALES ====================
let funcionSeleccionada = null;
let salaSeleccionada = null;
let asientosDisponibles = [];
let asientosSeleccionados = [];
let precioTotal = 0.0;
let protegerSalida = false;

// Tipos de entrada (deben coincidir con la BD)
const TIPOS_ENTRADA = {
    1: { nombre: 'Adulto', precio: 29.00 },
    2: { nombre: 'Niño', precio: 25.00 },
    3: { nombre: 'Persona con discapacidad', precio: 22.00 },
    4: { nombre: 'Acompañante', precio: 22.00 }
};

// ==================== INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('✓ Sistema de selección cargado');

    const urlParams = new URLSearchParams(window.location.search);
    funcionSeleccionada = parseInt(urlParams.get('funcionId')) || 1;
    salaSeleccionada = parseInt(urlParams.get('salaId')) || 1;

    if (!funcionSeleccionada || !salaSeleccionada) {
        mostrarMensaje('❌ Parámetros inválidos', 'error');
        return;
    }

    inicializarPagina();

    document.getElementById('btnContinuar')?.addEventListener('click', procesarReserva);
    document.getElementById('btnCancelar')?.addEventListener('click', limpiarSeleccion);
});

// ==================== INICIALIZACIÓN DE PÁGINA ====================
async function inicializarPagina() {
    mostrarCargando(true);
    await cargarInformacionFuncion();
    await cargarAsientosDesdeDB();
    mostrarCargando(false);
}

// ==================== CARGAR INFORMACIÓN DE LA FUNCIÓN ====================
async function cargarInformacionFuncion() {
    const tituloElem = document.getElementById('tituloPelicula');
    const generoElem = document.getElementById('genero');
    const duracionElem = document.getElementById('duracion');
    const clasificacionElem = document.getElementById('clasificacion');
    const horarioElem = document.getElementById('horario');
    const salaElem = document.getElementById('sala');
    const disponiblesElem = document.getElementById('asientosDisponibles');

    try {
        const resp = await fetch(`${API_FUNCIONES_URL}/${funcionSeleccionada}`);

        if (resp.ok) {
            const json = await resp.json();
            const dto = json.data;

            if (!dto) throw new Error('Respuesta sin data');

            tituloElem.textContent = dto.titulo || 'Función sin título';
            generoElem.textContent = dto.genero || '-';
            duracionElem.textContent = dto.duracion ? `${dto.duracion} min` : '-';
            clasificacionElem.textContent = dto.clasificacion || '-';
            horarioElem.textContent = dto.fechaHoraInicio || '-';
            salaElem.textContent = dto.salaNombre || `Sala ${dto.salaId || salaSeleccionada}`;

            if (dto.asientosDisponibles != null) {
                disponiblesElem.textContent = dto.asientosDisponibles;
            }

            console.log('✓ Datos de función cargados desde BD');
            return;
        }
    } catch (e) {
        console.warn('⚠️ No se pudo obtener la función desde la API, usando datos mock.', e);
    }

    // Mock si falla
    console.warn('Usando datos mock para la función');
    const funcionesMock = {
        1: {
            titulo: 'FIVE NIGHTS AT FREDDY\'S 2',
            genero: 'Terror',
            duracion: 110,
            clasificacion: 'XD + 2D',
            horario: 'jueves, 04/12 - 00:05',
            sala: 'Sala 10',
            disponibles: '-'
        },
        2: {
            titulo: 'SOTO TEADOR',
            genero: 'Suspenso',
            duracion: 110,
            clasificacion: 'PG-13',
            horario: 'viernes, 05/12 - 18:00',
            sala: 'Sala 5',
            disponibles: '-'
        }
    };

    const data = funcionesMock[funcionSeleccionada] || funcionesMock[1];

    tituloElem.textContent = data.titulo;
    generoElem.textContent = data.genero;
    duracionElem.textContent = `${data.duracion} min`;
    clasificacionElem.textContent = data.clasificacion;
    horarioElem.textContent = data.horario;
    salaElem.textContent = data.sala;
    disponiblesElem.textContent = data.disponibles;
}

// ==================== CARGAR ASIENTOS DESDE DB ====================
async function cargarAsientosDesdeDB() {
    try {
        console.log('Generando asientos para Sala', salaSeleccionada, 'Función', funcionSeleccionada);
        const asientos = generarAsientosCompletos();

        await aplicarAsientosDesdeBackend(asientos);

        asientosDisponibles = asientos;
        const disponibles = asientos.filter(a => a.estado === 'Disponible').length;
        document.getElementById('asientosDisponibles').textContent = disponibles;

        console.log(`✓ ${disponibles} asientos disponibles de ${asientos.length} totales`);

        renderizarAsientos(asientos);

    } catch (error) {
        console.error('Error cargando asientos:', error);
        mostrarMensaje('⚠️ Error al cargar asientos', 'warning');
    }
}

// ==================== APLICAR ESTADOS REALES DESDE BACKEND ====================
async function aplicarAsientosDesdeBackend(asientos) {
    try {
        const resp = await fetch(`${API_ASIENTOS_URL}?funcionId=${funcionSeleccionada}`);

        if (!resp.ok) {
            console.warn('⚠️ No se pudieron cargar estados desde BD (HTTP ' + resp.status + '), usando solo locales');
            return;
        }

        const json = await resp.json();
        const lista = json.asientos || [];

        lista.forEach(asientoBD => {
            const asiento = asientos.find(
                a => a.fila === asientoBD.fila && a.numero === asientoBD.numero
            );

            if (asiento) {
                asiento.estado = asientoBD.estado || 'Disponible';
                console.log(`Estado BD → ${asiento.fila}${asiento.numero}: ${asiento.estado}`);
            }
        });
    } catch (e) {
        console.warn('⚠️ Error al obtener estados reales de asientos, usando solo estados locales', e);
    }
}

// ==================== GENERAR ASIENTOS COMPLETOS ====================
function generarAsientosCompletos() {
    const asientos = [];
    const filas = ['A', 'B', 'C', 'D', 'E'];
    let asientoId = (salaSeleccionada - 1) * 70 + 1;

    filas.forEach(fila => {
        if (fila !== 'E') {
            for (let numero = 1; numero <= 14; numero++) {
                asientos.push({
                    asientoId: asientoId++,
                    salaId: salaSeleccionada,
                    fila: fila,
                    numero: numero,
                    estado: 'Disponible',
                    tipo: 'Normal',
                    categoriaAcceso: 'Normal',
                    asientoRelacionadoId: null
                });
            }
        } else {
            // Fila E: Pares especiales (1-8) + Normales (9-14)
            const pares = [
                [1, 2], [3, 4], [5, 6], [7, 8]
            ];

            pares.forEach(([disc, acomp]) => {
                const id1 = asientoId++;
                const id2 = asientoId++;
                asientos.push({
                    asientoId: id1,
                    salaId: salaSeleccionada,
                    fila: 'E',
                    numero: disc,
                    estado: 'Disponible',
                    tipo: 'Discapacitado',
                    categoriaAcceso: 'Accesibilidad',
                    asientoRelacionadoId: id2
                });
                asientos.push({
                    asientoId: id2,
                    salaId: salaSeleccionada,
                    fila: 'E',
                    numero: acomp,
                    estado: 'Disponible',
                    tipo: 'Acompañante',
                    categoriaAcceso: 'Accesibilidad',
                    asientoRelacionadoId: id1
                });
            });

            // E9-E14: Normales
            for (let numero = 9; numero <= 14; numero++) {
                asientos.push({
                    asientoId: asientoId++,
                    salaId: salaSeleccionada,
                    fila: 'E',
                    numero: numero,
                    estado: 'Disponible',
                    tipo: 'Normal',
                    categoriaAcceso: 'Normal',
                    asientoRelacionadoId: null
                });
            }
        }
    });

    return asientos;
}

// ==================== RENDERIZADO ====================
function renderizarAsientos(asientos) {
    const contenedor = document.getElementById('asientosGrid');
    contenedor.innerHTML = '';

    const asientosPorFila = agruparPorFila(asientos);

    Object.keys(asientosPorFila).sort().forEach(fila => {
        const filaDiv = document.createElement('div');
        filaDiv.className = 'fila-asientos';

        const etiqueta = document.createElement('span');
        etiqueta.className = 'etiqueta-fila';
        etiqueta.textContent = fila;
        filaDiv.appendChild(etiqueta);

        const asientosFila = asientosPorFila[fila].sort((a, b) => a.numero - b.numero);

        asientosFila.forEach(asiento => {
            const btn = crearBotonAsiento(asiento);
            filaDiv.appendChild(btn);
        });

        contenedor.appendChild(filaDiv);
    });

    agregarLeyenda();
}

function agruparPorFila(asientos) {
    return asientos.reduce((acc, asiento) => {
        if (!acc[asiento.fila]) acc[asiento.fila] = [];
        acc[asiento.fila].push(asiento);
        return acc;
    }, {});
}

function crearBotonAsiento(asiento) {
    const btn = document.createElement('button');
    btn.className = 'asiento';
    btn.dataset.asientoId = asiento.asientoId;
    btn.dataset.fila = asiento.fila;
    btn.dataset.numero = asiento.numero;
    btn.dataset.tipo = asiento.tipo;
    btn.dataset.estado = asiento.estado;

    btn.classList.add(`asiento-${asiento.tipo.toLowerCase()}`);

    const estadoCss = `estado-${asiento.estado.toLowerCase().replace(/\s+/g, '-')}`;
    btn.classList.add(estadoCss);

    let icono = '';
    if (asiento.tipo === 'Discapacitado') icono = '♿';
    else if (asiento.tipo === 'Acompañante') icono = '🤝';

    btn.innerHTML = `${icono}${asiento.numero}`;
    btn.title = `${asiento.fila}${asiento.numero} - ${asiento.tipo} (${asiento.estado})`;

    if (asiento.estado !== 'Disponible') {
        btn.disabled = true;
    } else {
        btn.addEventListener('click', () => toggleAsiento(asiento, btn));
    }

    return btn;
}

function agregarLeyenda() {
    const leyenda = document.getElementById('leyenda');
    leyenda.innerHTML = `
        <div class="leyenda-item">
            <span class="asiento estado-disponible"></span> Disponible
        </div>
        <div class="leyenda-item">
            <span class="asiento asiento-seleccionado"></span> Seleccionado
        </div>
        <div class="leyenda-item">
            <span class="asiento estado-reservado"></span> Reservado
        </div>
        <div class="leyenda-item">
            <span class="asiento estado-ocupado"></span> Ocupado
        </div>
        <div class="leyenda-item">
            <span class="asiento estado-no-disponible"></span> No disponible
        </div>
        <div class="leyenda-item">
            <span class="asiento asiento-discapacitado">♿</span> Discapacidad
        </div>
        <div class="leyenda-item">
            <span class="asiento asiento-acompañante">🤝</span> Acompañante
        </div>
    `;
}

// ==================== SELECCIÓN ====================
function toggleAsiento(asiento, btnElement) {
    const index = asientosSeleccionados.findIndex(
        a => a.fila === asiento.fila && a.numero === asiento.numero
    );

    if (index >= 0) {
        asientosSeleccionados.splice(index, 1);
        btnElement.classList.remove('asiento-seleccionado');
    } else {
        if (asientosSeleccionados.length >= 3) {
            mostrarMensaje('❌ Máximo 3 asientos por reserva', 'warning');
            return;
        }
        mostrarModalTipoEntrada(asiento, btnElement);
    }

    actualizarResumen();
}

function mostrarModalTipoEntrada(asiento, btnElement) {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';

    let opcionesTipos = '';
    if (asiento.tipo === 'Discapacitado') {
        opcionesTipos = `<option value="3">Persona con discapacidad - S/ 22.00</option>`;
    } else if (asiento.tipo === 'Acompañante') {
        opcionesTipos = `<option value="4">Acompañante - S/ 22.00</option>`;
    } else {
        opcionesTipos = `
            <option value="1">Adulto - S/ 29.00</option>
            <option value="2">Niño - S/ 25.00</option>
        `;
    }

    modal.innerHTML = `
        <div class="modal-content">
            <h3>🎫 Tipo de Entrada</h3>
            <p><strong>Asiento:</strong> ${asiento.fila}${asiento.numero}</p>
            <p><strong>Tipo:</strong> ${asiento.tipo}</p>
            <select id="selectTipoEntrada" class="form-control">
                <option value="">-- Seleccione --</option>
                ${opcionesTipos}
            </select>
            <div class="modal-buttons">
                <button id="btnConfirmarTipo" class="btn btn-primary">Confirmar</button>
                <button id="btnCancelarTipo" class="btn btn-secondary">Cancelar</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);

    document.getElementById('btnConfirmarTipo').addEventListener('click', () => {
        const tipoEntradaId = parseInt(document.getElementById('selectTipoEntrada').value);

        if (!tipoEntradaId) {
            mostrarMensaje('❌ Seleccione un tipo de entrada', 'error');
            return;
        }

        asientosSeleccionados.push({
            fila: asiento.fila,
            numero: asiento.numero,
            tipoEntradaId: tipoEntradaId,
            asientoId: asiento.asientoId,
            tipo: asiento.tipo,
            precio: TIPOS_ENTRADA[tipoEntradaId].precio
        });

        btnElement.classList.add('asiento-seleccionado');
        actualizarResumen();
        document.body.removeChild(modal);
    });

    document.getElementById('btnCancelarTipo').addEventListener('click', () => {
        document.body.removeChild(modal);
    });
}

// ==================== RESUMEN ====================
function actualizarResumen() {
    const resumenDiv = document.getElementById('resumen');
    precioTotal = asientosSeleccionados.reduce((sum, a) => sum + a.precio, 0);

    protegerSalida = asientosSeleccionados.length > 0;

    if (asientosSeleccionados.length === 0) {
        resumenDiv.innerHTML = '<p class="text-muted">No hay asientos seleccionados</p>';
        document.getElementById('btnContinuar').disabled = true;
        return;
    }

    let html = '<div class="lista-asientos">';
    asientosSeleccionados.forEach(asiento => {
        const tipoEntrada = TIPOS_ENTRADA[asiento.tipoEntradaId];
        html += `
            <div class="asiento-resumen">
                <span class="ubicacion">${asiento.fila}${asiento.numero}</span>
                <span class="tipo">${tipoEntrada.nombre}</span>
                <span class="precio">S/ ${asiento.precio.toFixed(2)}</span>
            </div>
        `;
    });
    html += '</div>';
    html += `
        <div class="total-resumen">
            <strong>Total:</strong>
            <strong>S/ ${precioTotal.toFixed(2)}</strong>
        </div>
    `;

    resumenDiv.innerHTML = html;
    document.getElementById('btnContinuar').disabled = false;
}

// ==================== LIMPIAR SELECCIÓN ====================
function limpiarSeleccion() {
    if (asientosSeleccionados.length === 0) {
        mostrarMensaje('⚠️ No hay asientos seleccionados para limpiar', 'warning');
        return;
    }

    const disponibles = asientosDisponibles.filter(a => a.estado === 'Disponible').length;
    if (disponibles === 0) {
        mostrarMensaje('❌ La sala ya no tiene asientos disponibles, no se puede limpiar la selección.', 'error');
        return;
    }

    asientosSeleccionados = [];
    precioTotal = 0.0;

    document.querySelectorAll('.asiento-seleccionado').forEach(btn => {
        btn.classList.remove('asiento-seleccionado');
    });

    actualizarResumen();
    mostrarMensaje('🧹 Selección limpiada', 'success');
}

// ==================== PROCESAR RESERVA ====================
async function procesarReserva() {
    if (asientosSeleccionados.length === 0) {
        mostrarMensaje('❌ Seleccione al menos un asiento', 'warning');
        return;
    }

    const usuarioId = obtenerUsuarioId();
    if (!usuarioId) {
        mostrarMensaje('❌ Inicie sesión para continuar', 'error');
        setTimeout(() => window.location.href = 'crearReserva.html', 2000);
        return;
    }

    // ✅ Formato correcto para inscribirDto
    const requestBody = {
        idCliente: usuarioId,
        idFuncion: funcionSeleccionada,
        asientosSeleccionados: asientosSeleccionados.map(a => ({
            fila: a.fila,
            numero: a.numero,
            tipoEntradaId: a.tipoEntradaId
        }))
    };

    console.log('========== ENVIANDO RESERVA ==========');
    console.log('Request:', JSON.stringify(requestBody, null, 2));

    try {
        mostrarCargando(true);

        // ✅ Endpoint correcto
        const response = await fetch('http://localhost:8080/api/reservas/crear', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody)
        });

        console.log('Response status:', response.status);
        const data = await response.json();
        console.log('Response data:', data);

        if (response.ok && data.success) {
            mostrarMensaje(data.mensaje || '✅ Reserva exitosa', 'success');
            console.log('✓ Código de reserva:', data.codigoReserva);

            protegerSalida = false;
            asientosSeleccionados = [];

            setTimeout(() => {
                window.location.href = `confirmacion.html?codigo=${data.codigoReserva}`;
            }, 2000);
        } else {
            mostrarMensaje(data.mensaje || '❌ Error al procesar reserva', 'error');
        }

    } catch (error) {
        console.error('========== ERROR DE RED ==========');
        console.error(error);

        let mensaje = '❌ Error de conexión. ';
        if (error.message && error.message.includes('fetch')) {
            mensaje += 'Verifica que el backend esté en http://localhost:8080';
        }
        mostrarMensaje(mensaje, 'error');

    } finally {
        mostrarCargando(false);
    }
}

// ==================== UTILIDADES ====================
function obtenerUsuarioId() {
    const id = sessionStorage.getItem('usuarioID');

    if (!id) {
        mostrarMensaje('❌ Error: No hay cliente registrado. Volviendo al inicio...', 'error');
        setTimeout(() => {
            window.location.href = 'crearReserva.html';
        }, 2000);
        return null;
    }
    return parseInt(id);
}

function mostrarCargando(mostrar) {
    const loader = document.getElementById('loader');
    if (loader) loader.style.display = mostrar ? 'flex' : 'none';
}

function mostrarMensaje(mensaje, tipo = 'info') {
    console.log(`[${tipo.toUpperCase()}] ${mensaje}`);

    const alertDiv = document.getElementById('alertMessage');
    if (!alertDiv) return;

    const icono = tipo === 'success' ? '✅' :
        tipo === 'error' ? '❌' :
            tipo === 'warning' ? '⚠️' : 'ℹ️';

    alertDiv.innerHTML = `<strong>${icono} ${mensaje}</strong>`;
    alertDiv.className = `alert ${tipo}`;
    alertDiv.style.display = 'block';

    setTimeout(() => {
        alertDiv.style.opacity = '0';
        setTimeout(() => {
            alertDiv.style.display = 'none';
            alertDiv.style.opacity = '1';
        }, 300);
    }, 5000);
}

// ==================== AVISO AL SALIR ====================
window.addEventListener('beforeunload', (e) => {
    if (protegerSalida && asientosSeleccionados.length > 0) {
        e.preventDefault();
        e.returnValue = '¿Salir? Perderá su selección de asientos.';
    }
});

console.log('✅ Sistema cargado correctamente');
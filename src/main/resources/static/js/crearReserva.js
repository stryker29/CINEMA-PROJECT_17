// ============================================================
// 🎫 SISTEMA DE RESERVAS - CINESTAR
// ============================================================

const API_URL = 'http://localhost:8080/api';
const empleado = JSON.parse(sessionStorage.getItem('empleado')) || { nombreCompleto: 'Admin', empleadoID: 1 };

// Variables globales
let clienteSeleccionado = null;
let funcionSeleccionada = null;
let asientosDisponibles = [];
let asientosSeleccionados = [];
let reservaActual = null;

// Tipos de entrada
const TIPOS_ENTRADA = {
    1: { nombre: 'Adulto', precio: 29.00 },
    2: { nombre: 'Niño', precio: 25.00 },
    3: { nombre: 'Persona con discapacidad', precio: 22.00 },
    4: { nombre: 'Acompañante', precio: 22.00 }
};

// Inicialización
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('nombreEmpleado').textContent = empleado.nombreCompleto;

    document.getElementById('buscarCliente').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') buscarCliente();
    });
});

// ==================== MENSAJES ====================

function mostrarMensaje(mensaje, tipo) {
    const alertDiv = document.getElementById('alertMessage');
    const iconos = {
        success: '✅',
        error: '❌',
        warning: '⚠️',
        info: 'ℹ️'
    };

    alertDiv.innerHTML = `<strong>${iconos[tipo] || ''} ${mensaje}</strong>`;
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

// ==================== PASO 1: CLIENTE ====================

async function buscarCliente() {
    const busqueda = document.getElementById('buscarCliente').value.trim();

    if (!busqueda || busqueda.length < 3) {
        mostrarMensaje('Ingrese al menos 3 caracteres', 'warning');
        return;
    }

    try {
        let response = await fetch(`${API_URL}/usuarios/buscar/email/${encodeURIComponent(busqueda)}`);

        if (!response.ok) {
            response = await fetch(`${API_URL}/usuarios/buscar/nombre/${encodeURIComponent(busqueda)}`);
        }

        if (!response.ok) {
            mostrarMensaje('No se encontró ningún cliente', 'info');
            mostrarFormNuevoCliente();
            document.getElementById('email').value = busqueda.includes('@') ? busqueda : '';
            return;
        }

        const data = await response.json();
        const clientes = Array.isArray(data.data) ? data.data : [data.data];

        if (clientes.length === 1) {
            mostrarClienteEncontrado(clientes[0]);
        } else {
            mostrarListaClientes(clientes);
        }

    } catch (error) {
        console.error('Error:', error);
        mostrarMensaje('Error al buscar cliente', 'error');
    }
}

function mostrarClienteEncontrado(cliente) {
    clienteSeleccionado = cliente;

    const div = document.getElementById('clienteEncontrado');
    div.innerHTML = `
        <div class="cliente-info">
            <h4>✅ Cliente Encontrado</h4>
            <p><strong>ID:</strong> ${cliente.usuarioID || cliente.UsuarioID}</p>
            <p><strong>Nombre:</strong> ${cliente.nombre} ${cliente.apellido}</p>
            <p><strong>Email:</strong> ${cliente.email}</p>
            <p><strong>Teléfono:</strong> ${cliente.telefono || 'No registrado'}</p>
            <button class="btn btn-primary btn-full" onclick="continuarAFunciones()">
                Continuar con este cliente →
            </button>
        </div>
    `;
    div.style.display = 'block';
    document.getElementById('formNuevoCliente').style.display = 'none';
    document.getElementById('btnNuevoCliente').style.display = 'none';
}

function mostrarListaClientes(clientes) {
    const div = document.getElementById('clienteEncontrado');
    let html = '<h4 style="color: #ff6666;">📋 Varios clientes encontrados:</h4>';

    clientes.forEach(cliente => {
        html += `
            <div class="funcion-card" onclick='seleccionarClienteDeLista(${JSON.stringify(cliente).replace(/'/g, "&apos;")})'>
                <h4>${cliente.nombre} ${cliente.apellido}</h4>
                <p style="color: #ccc;"><strong>Email:</strong> ${cliente.email}</p>
            </div>
        `;
    });

    div.innerHTML = html;
    div.style.display = 'block';
}

function seleccionarClienteDeLista(cliente) {
    mostrarClienteEncontrado(cliente);
}

function mostrarFormNuevoCliente() {
    document.getElementById('formNuevoCliente').style.display = 'block';
    document.getElementById('btnNuevoCliente').style.display = 'none';
    document.getElementById('clienteEncontrado').style.display = 'none';
}

async function registrarCliente() {
    const nombre = document.getElementById('nombre').value.trim();
    const apellido = document.getElementById('apellido').value.trim();
    const email = document.getElementById('email').value.trim();
    const telefono = document.getElementById('telefono').value.trim();

    // Validaciones
    if (!nombre || nombre.length < 2) {
        mostrarMensaje('El nombre debe tener al menos 2 caracteres', 'error');
        return;
    }

    if (!apellido || apellido.length < 2) {
        mostrarMensaje('El apellido debe tener al menos 2 caracteres', 'error');
        return;
    }

    if (!email || !email.includes('@')) {
        mostrarMensaje('Ingrese un email válido', 'error');
        return;
    }

    const datosCliente = {
        nombre,
        apellido,
        email,
        telefono: telefono || null
    };

    try {
        const response = await fetch(`${API_URL}/usuarios/registro`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datosCliente)
        });

        const data = await response.json();

        if (response.ok && data.success) {
            mostrarMensaje('Cliente registrado exitosamente', 'success');

            const usuarioID = data.usuarioID || data.data?.usuarioID;

            if (!usuarioID) {
                mostrarMensaje('Cliente registrado pero sin ID. Búsquelo por email.', 'warning');
                document.getElementById('buscarCliente').value = email;
                setTimeout(() => buscarCliente(), 2000);
                return;
            }

            clienteSeleccionado = {
                usuarioID,
                UsuarioID: usuarioID,
                nombre,
                apellido,
                email,
                telefono
            };

            setTimeout(() => continuarAFunciones(), 1500);
        } else {
            mostrarMensaje(data.mensaje || 'Error al registrar cliente', 'error');
        }

    } catch (error) {
        console.error('Error:', error);
        mostrarMensaje('Error de conexión', 'error');
    }
}

function continuarAFunciones() {
    if (!clienteSeleccionado) {
        mostrarMensaje('Debe seleccionar un cliente primero', 'error');
        return;
    }

    document.getElementById('step2').classList.add('active');
    document.getElementById('seccionCliente').style.display = 'none';
    document.getElementById('seccionFuncion').style.display = 'block';

    cargarFunciones();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ==================== PASO 2: FUNCIONES ====================

async function cargarFunciones() {
    const contenedor = document.getElementById('listaFunciones');
    contenedor.innerHTML = '<div class="loading">⏳ Cargando funciones...</div>';

    try {
        const response = await fetch(`${API_URL}/procesos/cartelera`);

        if (!response.ok) throw new Error('Error al cargar cartelera');

        const funciones = await response.json();

        if (!funciones || funciones.length === 0) {
            contenedor.innerHTML = '<div style="text-align: center; padding: 40px; color: #ff6666;"><h3>❌ No hay funciones disponibles</h3></div>';
            return;
        }

        let html = '';
        funciones.forEach(funcion => {
            let fechaInicio = 'Fecha no disponible';
            try {
                if (funcion.FechaHoraInicio) {
                    const fecha = new Date(funcion.FechaHoraInicio);
                    if (!isNaN(fecha.getTime())) {
                        fechaInicio = fecha.toLocaleString('es-PE', {
                            weekday: 'long',
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
                <div class="funcion-card" onclick="seleccionarFuncion(${funcion.FuncionID}, ${funcion.SalaID || 1})">
                    <h4>🎬 ${funcion.Pelicula || 'Sin título'}</h4>
                    <div class="funcion-info">
                        <div><strong>Sala:</strong> ${funcion.Sala || 'N/A'}</div>
                        <div><strong>Tipo:</strong> ${funcion.TipoSala || 'Estándar'}</div>
                        <div><strong>Fecha:</strong> ${fechaInicio}</div>
                        <div><strong>Disponibles:</strong> ${funcion.AsientosDisponibles || 0} asientos</div>
                        <div><strong>Precio:</strong> S/ ${(funcion.PrecioBase || 0).toFixed(2)}</div>
                    </div>
                </div>
            `;
        });

        contenedor.innerHTML = html;

    } catch (error) {
        console.error('Error:', error);
        contenedor.innerHTML = '<div style="text-align: center; padding: 40px; color: #ff6666;"><h3>❌ Error al cargar funciones</h3></div>';
    }
}

function seleccionarFuncion(funcionId, salaId) {
    if (!clienteSeleccionado) {
        mostrarMensaje('Error: No hay cliente seleccionado', 'error');
        return;
    }

    funcionSeleccionada = { id: funcionId, salaId: salaId };

    document.getElementById('step3').classList.add('active');
    document.getElementById('seccionFuncion').style.display = 'none';
    document.getElementById('seccionAsientos').style.display = 'block';

    cargarAsientos(funcionId, salaId);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ==================== PASO 3: ASIENTOS ====================

async function cargarAsientos(funcionId, salaId) {
    try {
        const asientos = generarAsientosCompletos(salaId);
        await aplicarEstadosReales(funcionId, asientos);

        asientosDisponibles = asientos;
        renderizarAsientos(asientos);

    } catch (error) {
        console.error('Error:', error);
        mostrarMensaje('Error al cargar asientos', 'error');
    }
}

function generarAsientosCompletos(salaId) {
    const asientos = [];
    const filas = ['A', 'B', 'C', 'D', 'E'];
    let asientoId = (salaId - 1) * 70 + 1;

    filas.forEach(fila => {
        if (fila !== 'E') {
            for (let numero = 1; numero <= 14; numero++) {
                asientos.push({
                    asientoId: asientoId++,
                    salaId,
                    fila,
                    numero,
                    estado: 'Disponible',
                    tipo: 'Normal'
                });
            }
        } else {
            // Fila E: Pares especiales
            const pares = [[1,2], [3,4], [5,6], [7,8]];
            pares.forEach(([disc, acomp]) => {
                const id1 = asientoId++;
                const id2 = asientoId++;
                asientos.push({
                    asientoId: id1,
                    salaId,
                    fila: 'E',
                    numero: disc,
                    estado: 'Disponible',
                    tipo: 'Discapacitado'
                });
                asientos.push({
                    asientoId: id2,
                    salaId,
                    fila: 'E',
                    numero: acomp,
                    estado: 'Disponible',
                    tipo: 'Acompañante'
                });
            });

            for (let numero = 9; numero <= 14; numero++) {
                asientos.push({
                    asientoId: asientoId++,
                    salaId,
                    fila: 'E',
                    numero,
                    estado: 'Disponible',
                    tipo: 'Normal'
                });
            }
        }
    });

    return asientos;
}

async function aplicarEstadosReales(funcionId, asientos) {
    try {
        const response = await fetch(`${API_URL}/reservas/asientos?funcionId=${funcionId}`);

        if (!response.ok) {
            console.warn('No se pudieron cargar estados desde BD');
            return;
        }

        const data = await response.json();
        const lista = data.asientos || [];

        lista.forEach(asientoBD => {
            const asiento = asientos.find(
                a => a.fila === asientoBD.fila && a.numero === asientoBD.numero
            );
            if (asiento) {
                asiento.estado = asientoBD.estado || 'Disponible';
            }
        });

    } catch (e) {
        console.warn('Error al obtener estados:', e);
    }
}

function renderizarAsientos(asientos) {
    const contenedor = document.getElementById('asientosGrid');
    contenedor.innerHTML = '';

    const porFila = {};
    asientos.forEach(a => {
        if (!porFila[a.fila]) porFila[a.fila] = [];
        porFila[a.fila].push(a);
    });

    Object.keys(porFila).sort().forEach(fila => {
        const filaDiv = document.createElement('div');
        filaDiv.className = 'fila-asientos';

        const etiqueta = document.createElement('span');
        etiqueta.className = 'etiqueta-fila';
        etiqueta.textContent = fila;
        filaDiv.appendChild(etiqueta);

        porFila[fila].sort((a, b) => a.numero - b.numero).forEach(asiento => {
            const btn = crearBotonAsiento(asiento);
            filaDiv.appendChild(btn);
        });

        contenedor.appendChild(filaDiv);
    });
}

function crearBotonAsiento(asiento) {
    const btn = document.createElement('button');
    btn.className = 'asiento';
    btn.dataset.fila = asiento.fila;
    btn.dataset.numero = asiento.numero;
    btn.dataset.asientoId = asiento.asientoId;

    btn.classList.add(`estado-${asiento.estado.toLowerCase().replace(/\s+/g, '-')}`);

    let icono = '';
    if (asiento.tipo === 'Discapacitado') icono = '♿';
    else if (asiento.tipo === 'Acompañante') icono = '🤝';

    btn.innerHTML = `${icono}${asiento.numero}`;
    btn.title = `${asiento.fila}${asiento.numero} - ${asiento.tipo}`;

    if (asiento.estado !== 'Disponible') {
        btn.disabled = true;
    } else {
        btn.addEventListener('click', () => toggleAsiento(asiento, btn));
    }

    return btn;
}

function toggleAsiento(asiento, btnElement) {
    const index = asientosSeleccionados.findIndex(
        a => a.fila === asiento.fila && a.numero === asiento.numero
    );

    if (index >= 0) {
        asientosSeleccionados.splice(index, 1);
        btnElement.classList.remove('asiento-seleccionado');
    } else {
        if (asientosSeleccionados.length >= 3) {
            mostrarMensaje('Máximo 3 asientos por reserva', 'warning');
            return;
        }
        mostrarModalTipoEntrada(asiento, btnElement);
    }

    actualizarResumen();
}

function mostrarModalTipoEntrada(asiento, btnElement) {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';

    let opciones = '';
    if (asiento.tipo === 'Discapacitado') {
        opciones = `<option value="3">Persona con discapacidad - S/ 22.00</option>`;
    } else if (asiento.tipo === 'Acompañante') {
        opciones = `<option value="4">Acompañante - S/ 22.00</option>`;
    } else {
        opciones = `
            <option value="1">Adulto - S/ 29.00</option>
            <option value="2">Niño - S/ 25.00</option>
        `;
    }

    modal.innerHTML = `
        <div class="modal-content">
            <h3>🎫 Tipo de Entrada</h3>
            <p><strong>Asiento:</strong> ${asiento.fila}${asiento.numero}</p>
            <p><strong>Tipo:</strong> ${asiento.tipo}</p>
            <select id="selectTipoEntrada">
                <option value="">-- Seleccione --</option>
                ${opciones}
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
            mostrarMensaje('Seleccione un tipo de entrada', 'error');
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

function actualizarResumen() {
    const resumenDiv = document.getElementById('resumen');
    const precioTotal = asientosSeleccionados.reduce((sum, a) => sum + a.precio, 0);

    if (asientosSeleccionados.length === 0) {
        resumenDiv.innerHTML = '<p style="color: #888; text-align: center;">No hay asientos seleccionados</p>';
        document.getElementById('btnConfirmar').disabled = true;
        return;
    }

    let html = '<div>';
    asientosSeleccionados.forEach(asiento => {
        const tipoEntrada = TIPOS_ENTRADA[asiento.tipoEntradaId];
        html += `
            <div class="asiento-resumen">
                <span style="font-weight: bold; color: #ff6666;">${asiento.fila}${asiento.numero}</span>
                <span style="color: #ccc;">${tipoEntrada.nombre}</span>
                <span style="color: #00ff00; font-weight: bold;">S/ ${asiento.precio.toFixed(2)}</span>
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

    // Solo habilitar si no hay reserva confirmada
    if (reservaActual) {
        document.getElementById('btnConfirmar').disabled = true;
    } else {
        document.getElementById('btnConfirmar').disabled = false;
    }
}

// ==================== CONFIRMAR RESERVA ====================

async function confirmarReserva() {
    if (asientosSeleccionados.length === 0) {
        mostrarMensaje('Seleccione al menos un asiento', 'warning');
        return;
    }

    const requestBody = {
        idCliente: clienteSeleccionado.usuarioID || clienteSeleccionado.UsuarioID,
        idFuncion: funcionSeleccionada.id,
        idEmpleado: empleado.empleadoID,
        asientosSeleccionados: asientosSeleccionados.map(a => ({
            fila: a.fila,
            numero: a.numero,
            tipoEntradaId: a.tipoEntradaId
        }))
    };

    console.log('========== ENVIANDO RESERVA ==========');
    console.log(JSON.stringify(requestBody, null, 2));

    try {
        const response = await fetch(`${API_URL}/reservas/crear`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestBody)
        });

        const data = await response.json();
        console.log('Response:', data);

        if (response.ok && data.success) {
            reservaActual = {
                codigoReserva: data.codigoReserva,
                fechaExpiracion: data.data?.fechaExpiracion,
                precioTotal: data.data?.precioTotal
            };

            mostrarMensaje(
                `✅ Reserva confirmada | Código: ${data.codigoReserva} | ⏰ Tiene 15 minutos para pagar`,
                'success'
            );

            // Deshabilitar asientos
            document.querySelectorAll('.asiento').forEach(btn => {
                btn.disabled = true;
            });

            document.getElementById('btnConfirmar').disabled = true;

            // Redirigir al menú principal en 3 segundos
            setTimeout(() => {
                 window.location.href = 'main.html';
            }, 3000);

        } else {
            mostrarMensaje(data.mensaje || '❌ Error al procesar reserva', 'error');
        }

    } catch (error) {
        console.error('Error:', error);
        mostrarMensaje('❌ Error de conexión con el servidor', 'error');
    }
}
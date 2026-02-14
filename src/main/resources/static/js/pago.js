const API_URL = 'http://localhost:8080/api';
const API_PROCESOS = 'http://localhost:8080/bdcinestar/api/procesos';
let reservaActual = null;
const empleado = JSON.parse(sessionStorage.getItem('empleado'));

if (!empleado) { alert("Inicie sesión"); window.location.href = 'admin-login.html'; }

function cambiarTipoBusqueda() {
    const t = document.getElementById('tipoBusqueda').value;
    document.querySelectorAll('.input-container').forEach(el => el.style.display = 'none');
    document.getElementById('listaResultados').style.display = 'none';
    document.getElementById('paymentSection').style.display = 'none';
    if(t === 'codigo') document.getElementById('campoCodigo').style.display = 'block';
    if(t === 'nombre') document.getElementById('campoNombre').style.display = 'block';
}

function mostrarAlerta(m) { const b = document.getElementById('alertBox'); b.textContent = m; b.style.display = 'block'; setTimeout(() => b.style.display = 'none', 4000); }

async function buscarPorCodigo() {
    const c = document.getElementById('codigoInput').value.trim();
    if(!c) return mostrarAlerta("Código inválido");
    try {
        let res = await fetch(`${API_URL}/reservas/buscar/codigo/${c}`);
        let data = await res.json();
        if(data.success && data.data) {
            if(data.data.estado !== 'Pendiente') return mostrarAlerta(`Reserva ya ${data.data.estado}`);
            buscarEnActivas(c);
        } else mostrarAlerta("No encontrada");
    } catch(e) { mostrarAlerta("Error búsqueda"); }
}

async function buscarPorNombre() {
    const n = document.getElementById('nombreInput').value.trim();
    if(n.length < 3) return mostrarAlerta("Mínimo 3 letras");
    const lista = document.getElementById('listaResultados');
    lista.style.display = 'block'; lista.innerHTML = '<div style="text-align:center">Buscando...</div>';
    document.getElementById('paymentSection').style.display = 'none';

    try {
        let res = await fetch(`${API_URL}/reservas/buscar/cliente/${n}`);
        let data = await res.json();
        lista.innerHTML = '';
        if(data.success && data.data.length > 0) {
            const pen = data.data.filter(r => r.estado === 'Pendiente');
            if(pen.length === 0) { lista.innerHTML = '<div style="text-align:center">Sin pendientes</div>'; return; }
            pen.forEach(r => {
                const d = document.createElement('div'); d.className = 'reserva-item';
                d.innerHTML = `<div><strong style="color:#ff0000">${r.codigoReserva}</strong><br><small>${r.pelicula}</small></div><span style="color:yellow">COBRAR</span>`;
                d.onclick = () => buscarEnActivas(r.codigoReserva);
                lista.appendChild(d);
            });
        } else lista.innerHTML = '<div style="text-align:center">No encontrada</div>';
    } catch(e) { mostrarAlerta("Error búsqueda"); }
}

async function buscarEnActivas(c) {
    try {
        let res = await fetch(`${API_URL}/reservas/auditoria/activas`);
        let data = await res.json();
        if(data.success) {
            const r = data.data.find(x => x.CodigoReserva === c || x.codigoReserva === c);
            if(r) {
                reservaActual = r;
                document.getElementById('lblCodigo').textContent = r.CodigoReserva || r.codigoReserva;
                document.getElementById('lblCliente').textContent = r.Cliente || r.cliente;
                document.getElementById('lblPelicula').textContent = r.Pelicula || r.pelicula;
                document.getElementById('lblFecha').textContent = new Date(r.FechaHoraInicio || r.fechaHoraInicio).toLocaleString();
                document.getElementById('lblPrecio').textContent = (r.PrecioTotal || r.precioTotal).toFixed(2);
                document.getElementById('paymentSection').style.display = 'block';
                document.getElementById('listaResultados').style.display = 'none';
            } else mostrarAlerta("Detalles no encontrados");
        }
    } catch(e) { mostrarAlerta("Error detalles"); }
}

async function procesarPago() {
    if(!confirm("¿Cobrar?")) return;
    try {
        let res = await fetch(`${API_PROCESOS}/confirmar-reserva`, {
            method: 'POST', headers: {'Content-Type':'application/json'},
            body: JSON.stringify({ reservaId: reservaActual.ReservaID || reservaActual.reservaID, empleadoId: empleado.empleadoID })
        });
        let b = await res.json();
        if(res.ok) {
            document.getElementById('tktPelicula').textContent = b.peliculaTitulo;
            document.getElementById('tktSala').textContent = b.salaNombre;
            document.getElementById('tktFecha').textContent = b.fechaHoraFuncion;
            document.getElementById('tktCodigo').textContent = b.codigoBoleto;
            document.getElementById('tktCliente').textContent = b.clienteNombreCompleto;
            document.getElementById('tktTotal').textContent = b.precioTotal.toFixed(2);
            document.getElementById('ticketModal').style.display = 'flex';
            document.getElementById('paymentSection').style.display = 'none';
        } else mostrarAlerta(b.mensaje);
    } catch(e) { mostrarAlerta("Error cobro"); }
}

function cerrarTicket() { window.location.href = 'main.html'; }
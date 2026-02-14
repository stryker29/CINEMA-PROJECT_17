const API_BASE = 'http://localhost:8080';
 const API_VENTAS = `${API_BASE}/bdcinestar/api/procesos`;
 const API_CONSULTAS = `${API_BASE}/api`;

 const PRECIOS = {
     1: { nombre: 'Adulto', precio: 29.00 },
     2: { nombre: 'Niño', precio: 25.00 },
     3: { nombre: 'Discapacitado', precio: 22.00 },
     4: { nombre: 'Acompañante', precio: 22.00 }
 };

 let empleado = JSON.parse(sessionStorage.getItem('empleado'));
 let asientosSeleccionados = [];
 let asientoPendiente = null;

 document.addEventListener('DOMContentLoaded', () => {
     if (!empleado) { alert("No hay sesión activa."); window.location.href = 'admin-login.html'; return; }
     document.getElementById('nombreEmpleado').textContent = empleado.nombreCompleto;
     cargarFunciones();
 });

 function mostrarAlerta(msg, tipo) {
     const box = document.getElementById('alertBox');
     box.textContent = msg; box.className = tipo === 'success' ? 'alert alert-success' : 'alert alert-error';
     box.style.display = 'block'; setTimeout(() => box.style.display = 'none', 4000);
 }

 async function cargarFunciones() {
     try {
         const res = await fetch(`${API_CONSULTAS}/procesos/cartelera`);
         const funciones = await res.json();
         const select = document.getElementById('selectFuncion');
         select.innerHTML = '<option value="">-- Seleccione Función --</option>';
         funciones.forEach(f => {
             const fecha = new Date(f.FechaHoraInicio).toLocaleString('es-PE', { weekday: 'short', day: '2-digit', month: '2-digit', hour: '2-digit', minute:'2-digit' });
             const opt = document.createElement('option');
             opt.value = f.FuncionID; opt.dataset.sala = f.Sala; opt.dataset.precioBase = f.PrecioBase;
             opt.textContent = `${f.Pelicula} | ${fecha} | ${f.Sala}`;
             select.appendChild(opt);
         });
     } catch (e) { mostrarAlerta("Error al cargar cartelera", "error"); }
 }

 async function cargarAsientos() {
     const select = document.getElementById('selectFuncion');
     const funcionId = select.value;
     asientosSeleccionados = []; actualizarResumen();

     if (!funcionId) {
         document.getElementById('asientosMap').innerHTML = '';
         return;
     }
     if (select.selectedOptions[0].dataset.precioBase) {
         const pb = parseFloat(select.selectedOptions[0].dataset.precioBase);
         PRECIOS[1].precio = pb;
         PRECIOS[2].precio = pb - 4.00; // Niño siempre 4 soles menos
     }
     document.getElementById('infoSala').textContent = `Sala: ${select.selectedOptions[0].dataset.sala}`;

     try {
         const res = await fetch(`${API_CONSULTAS}/reservas/asientos?funcionId=${funcionId}`);
         const data = await res.json();
         if(data.success) renderizarMapa(data.asientos);
     } catch (e) { mostrarAlerta("Error cargando asientos", "error"); }
 }

 function renderizarMapa(lista) {
     const mapa = document.getElementById('asientosMap'); mapa.innerHTML = '';
     const filas = {};
     lista.forEach(a => { if(!filas[a.fila]) filas[a.fila] = []; filas[a.fila].push(a); });

     Object.keys(filas).sort().forEach(filaKey => {
         const filaDiv = document.createElement('div'); filaDiv.className = 'fila';
         const label = document.createElement('div'); label.className = 'etiqueta-fila'; label.textContent = filaKey;
         filaDiv.appendChild(label);
         filas[filaKey].sort((a,b) => a.numero - b.numero).forEach(asiento => {
             const btn = document.createElement('button');
             btn.className = `asiento ${asiento.estado.toLowerCase()}`;
             btn.textContent = asiento.numero; btn.id = `asiento-${asiento.asientoId}`;
             if (asiento.tipo === 'Discapacitado') btn.textContent = '♿';
             if (asiento.tipo === 'Acompañante') btn.textContent = '🤝';
             if (asiento.estado !== 'Disponible') btn.disabled = true;
             else btn.onclick = () => manejarClickAsiento(btn, asiento);
             filaDiv.appendChild(btn);
         });
         mapa.appendChild(filaDiv);
     });
 }

 function manejarClickAsiento(btn, asiento) {
     const idx = asientosSeleccionados.findIndex(a => a.asientoId === asiento.asientoId);
     if (idx > -1) {
         asientosSeleccionados.splice(idx, 1);
         btn.classList.remove('seleccionado');
         actualizarResumen();
     } else {
         asientoPendiente = { btn, asiento };
         mostrarModalTipos(asiento);
     }
 }

 function mostrarModalTipos(asiento) {
     const modal = document.getElementById('tipoEntradaModal');
     const lista = document.getElementById('opcionesEntrada');
     document.getElementById('asientoModalLabel').textContent = `Asiento: ${asiento.fila}${asiento.numero} (${asiento.tipo})`;
     lista.innerHTML = '';
     const tipos = [];
     if (asiento.tipo === 'Discapacitado') tipos.push({id:3, ...PRECIOS[3]});
     else if (asiento.tipo === 'Acompañante') tipos.push({id:4, ...PRECIOS[4]});
     else { tipos.push({id:1, ...PRECIOS[1]}); tipos.push({id:2, ...PRECIOS[2]}); }

     tipos.forEach(t => {
         const btn = document.createElement('button'); btn.className = 'tipo-btn';
         btn.innerHTML = `${t.nombre} <span>S/ ${t.precio.toFixed(2)}</span>`;
         btn.onclick = () => {
             asientosSeleccionados.push({ asientoId: asiento.asientoId, fila: asiento.fila, numero: asiento.numero, tipoEntradaId: t.id, nombreTipo: t.nombre, precio: t.precio });
             asientoPendiente.btn.classList.add('seleccionado');
             document.getElementById('tipoEntradaModal').style.display = 'none';
             actualizarResumen();
         };
         lista.appendChild(btn);
     });
     modal.style.display = 'flex';
 }
 function cerrarModal() { document.getElementById('tipoEntradaModal').style.display = 'none'; asientoPendiente = null; }

 function actualizarResumen() {
     const container = document.getElementById('resumenVenta');
     let total = 0; let html = '';
     asientosSeleccionados.forEach((a, i) => {
         total += a.precio;
         html += `<div class="resumen-item"><span>${a.fila}${a.numero} - ${a.nombreTipo}</span><span>S/ ${a.precio.toFixed(2)} <b style="color:red;cursor:pointer" onclick="remover(${i})">x</b></span></div>`;
     });
     container.innerHTML = html || '<p style="color:#888;text-align:center">Sin asientos</p>';
     document.getElementById('precioTotal').textContent = total.toFixed(2);
     document.getElementById('btnVender').disabled = asientosSeleccionados.length === 0;
 }

 function remover(idx) {
     const id = asientosSeleccionados[idx].asientoId;
     document.getElementById(`asiento-${id}`).classList.remove('seleccionado');
     asientosSeleccionados.splice(idx, 1);
     actualizarResumen();
 }

 async function procesarVentaCompleta() {
     const btn = document.getElementById('btnVender');
     const nombre = document.getElementById('clienteNombre').value.trim();
     const apellido = document.getElementById('clienteApellido').value.trim();
     const telefono = document.getElementById('clienteTelefono').value.trim();
     const funcionId = parseInt(document.getElementById('selectFuncion').value);

     if (!nombre || !apellido || !telefono) return mostrarAlerta("Datos incompletos", "error");
     btn.disabled = true; btn.textContent = "PROCESANDO...";

     try {
         const emailDummy = `${telefono}@fast.client`;
         let uId = null;
         // 1. Cliente
         let resC = await fetch(`${API_BASE}/api/usuarios/registro`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({nombre, apellido, email:emailDummy, telefono}) });
         let dataC = await resC.json();
         if(resC.ok && dataC.success) uId = dataC.usuarioID;
         else {
             let resB = await fetch(`${API_BASE}/api/usuarios/buscar/email/${emailDummy}`);
             if(resB.ok) uId = (await resB.json()).data.usuarioID;
             else throw new Error("Error cliente");
         }

         // 2. Reserva
         let resR = await fetch(`${API_BASE}/api/reservas/crear`, {
             method: 'POST', headers: {'Content-Type':'application/json'},
             body: JSON.stringify({ idCliente: uId, idFuncion: funcionId, idEmpleado: empleado.empleadoID, asientosSeleccionados: asientosSeleccionados.map(a => ({fila:a.fila, numero:a.numero, tipoEntradaId:a.tipoEntradaId})) })
         });
         let dataR = await resR.json();
         if(!dataR.success) throw new Error(dataR.mensaje);

         // 3. Pago
         let resId = (await (await fetch(`${API_BASE}/api/reservas/buscar/codigo/${dataR.codigoReserva}`)).json()).data.reservaID;
         let resP = await fetch(`${API_VENTAS}/confirmar-reserva`, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({reservaId: resId, empleadoId: empleado.empleadoID}) });
         let dataP = await resP.json();

         if(resP.ok) {
             document.getElementById('tktPelicula').textContent = dataP.peliculaTitulo;
             document.getElementById('tktSala').textContent = dataP.salaNombre;
             document.getElementById('tktFecha').textContent = dataP.fechaHoraFuncion;
             document.getElementById('tktCodigo').textContent = dataP.codigoBoleto;
             document.getElementById('tktCliente').textContent = dataP.clienteNombreCompleto;
             document.getElementById('tktTotal').textContent = dataP.precioTotal.toFixed(2);
             document.getElementById('ticketModal').style.display = 'flex';
         } else throw new Error(dataP.mensaje);

     } catch (e) {
         mostrarAlerta(e.message, "error"); btn.disabled = false; btn.textContent = "💳 CONFIRMAR VENTA";
     }
 }
 function cerrarTicket() { location.reload(); }
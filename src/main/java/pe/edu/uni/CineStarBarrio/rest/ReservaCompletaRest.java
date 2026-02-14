package pe.edu.uni.CineStarBarrio.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.uni.CineStarBarrio.dto.ClienteDto;
import pe.edu.uni.CineStarBarrio.dto.ReservaCompletaDto;
import pe.edu.uni.CineStarBarrio.service.ReservaCompletaService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller unificado para el proceso completo de reservas
 * Incluye: Registro/búsqueda de clientes, crear reserva y listar asientos
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReservaCompletaRest {

    @Autowired
    private ReservaCompletaService reservaService;

    // ==================== GESTIÓN DE CLIENTES ====================

    /**
     * Registrar nuevo cliente (SIN contraseña)
     * POST /api/usuarios/registro
     *
     * Request body ejemplo:
     * {
     *   "nombre": "Juan",
     *   "apellido": "Pérez",
     *   "email": "juan@gmail.com",
     *   "telefono": "987654321"
     * }
     */
    @PostMapping("/usuarios/registro")
    public ResponseEntity<?> registrarCliente(@RequestBody ClienteDto bean) {
        try {
            ClienteDto resultado = reservaService.registrarCliente(bean);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", resultado.getMensaje());
            response.put("usuarioID", resultado.getUsuarioID());
            response.put("data", resultado);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(crearRespuestaError(e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(crearRespuestaError("❌ Error interno del servidor: " + e.getMessage()));
        }
    }

    /**
     * Buscar cliente por email
     * GET /api/usuarios/buscar/email/{email}
     */
    @GetMapping("/usuarios/buscar/email/{email}")
    public ResponseEntity<?> buscarClientePorEmail(@PathVariable String email) {
        try {
            ClienteDto cliente = reservaService.buscarClientePorEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Cliente encontrado");
            response.put("data", cliente);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(crearRespuestaError(e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(crearRespuestaError("❌ Error al buscar cliente: " + e.getMessage()));
        }
    }

    /**
     * Buscar clientes por nombre
     * GET /api/usuarios/buscar/nombre/{nombre}
     */
    @GetMapping("/usuarios/buscar/nombre/{nombre}")
    public ResponseEntity<?> buscarClientesPorNombre(@PathVariable String nombre) {
        try {
            List<ClienteDto> clientes = reservaService.buscarClientesPorNombre(nombre);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cantidad", clientes.size());
            response.put("data", clientes);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(crearRespuestaError(e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(crearRespuestaError("❌ Error al buscar clientes: " + e.getMessage()));
        }
    }

    // ==================== RESERVAS ====================

    /**
     * Crear una nueva reserva completa
     * POST /api/reservas/crear
     *
     * Request body ejemplo:
     * {
     *   "idCliente": 1,
     *   "idFuncion": 2,
     *   "idEmpleado": 1,
     *   "asientosSeleccionados": [
     *     {
     *       "fila": "A",
     *       "numero": 5,
     *       "tipoEntradaId": 1
     *     },
     *     {
     *       "fila": "A",
     *       "numero": 6,
     *       "tipoEntradaId": 2
     *     }
     *   ]
     * }
     */
    @PostMapping("/reservas/crear")
    public ResponseEntity<?> crearReserva(@RequestBody ReservaCompletaDto bean) {
        try {
            // Validación básica en el controlador
            if (bean.getIdCliente() == null || bean.getIdFuncion() == null) {
                return ResponseEntity.badRequest()
                        .body(crearRespuestaError("❌ Debe proporcionar idCliente e idFuncion."));
            }

            if (bean.getAsientosSeleccionados() == null || bean.getAsientosSeleccionados().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(crearRespuestaError("❌ Debe seleccionar al menos un asiento."));
            }

            // Procesar la reserva
            ReservaCompletaDto resultado = reservaService.crearReserva(bean);

            // Retornar respuesta exitosa
            return ResponseEntity.ok(crearRespuestaExito(resultado));

        } catch (RuntimeException e) {
            // Errores de negocio
            return ResponseEntity.badRequest()
                    .body(crearRespuestaError(e.getMessage()));

        } catch (Exception e) {
            // Errores inesperados
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(crearRespuestaError("❌ Error interno del servidor: " + e.getMessage()));
        }
    }

    /**
     * Listar asientos de una función con su estado real
     * GET /api/reservas/asientos?funcionId=1
     */
    @GetMapping("/reservas/asientos")
    public ResponseEntity<?> listarAsientosPorFuncion(@RequestParam("funcionId") Integer funcionId) {
        try {
            if (funcionId == null || funcionId <= 0) {
                return ResponseEntity.badRequest()
                        .body(crearRespuestaError("❌ Debe proporcionar un funcionId válido."));
            }

            List<Map<String, Object>> asientos = reservaService.listarAsientosPorFuncion(funcionId);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("success", true);
            respuesta.put("cantidad", asientos.size());
            respuesta.put("asientos", asientos);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(crearRespuestaError("❌ Error al obtener asientos: " + e.getMessage()));
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Crea una respuesta exitosa estandarizada
     */
    private Map<String, Object> crearRespuestaExito(ReservaCompletaDto resultado) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("mensaje", resultado.getMensaje());
        response.put("codigoReserva", resultado.getCodigoReserva());
        response.put("data", resultado);
        return response;
    }


    @GetMapping("/procesos/cartelera")
    public ResponseEntity<?> obtenerCartelera() {
        try {
            List<Map<String, Object>> funciones = reservaService.obtenerCartelera();

            return ResponseEntity.ok(funciones);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(crearRespuestaError("❌ Error al cargar cartelera: " + e.getMessage()));
        }
    }
    private Map<String, Object> crearRespuestaError(String mensaje) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("mensaje", mensaje);
        response.put("data", null);
        return response;
    }
}
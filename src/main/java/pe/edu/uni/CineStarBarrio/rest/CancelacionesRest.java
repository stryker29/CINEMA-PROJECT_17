package pe.edu.uni.CineStarBarrio.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.edu.uni.CineStarBarrio.service.CancelacionesService;
import pe.edu.uni.CineStarBarrio.dto.CancelacionesDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "*")
public class CancelacionesRest {

    @Autowired
    private CancelacionesService reservaService;

    @GetMapping("/buscar/codigo/{codigo}")
    public ResponseEntity<?> buscarPorCodigo(@PathVariable String codigo) {
        try {
            CancelacionesDto reserva = reservaService.buscarReservaPorCodigo(codigo);

            if (reserva != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", reserva);
                response.put("mensaje", "Reserva encontrada exitosamente");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "No se encontró ninguna reserva con el código: " + codigo);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error al buscar la reserva: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/buscar/cliente/{nombre}")
    public ResponseEntity<?> buscarPorCliente(@PathVariable String nombre) {
        try {
            List<CancelacionesDto> reservas = reservaService.buscarReservasPorCliente(nombre);

            if (!reservas.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", reservas);
                response.put("cantidad", reservas.size());
                response.put("mensaje", "Se encontraron " + reservas.size() + " reserva(s)");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("data", reservas);
                response.put("mensaje", "No se encontraron reservas para el cliente: " + nombre);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error al buscar reservas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/cancelar")
    public ResponseEntity<?> cancelarReserva(@RequestBody CancelacionesDto request) {
        try {
            // Validación básica de entrada
            if (request.getReservaID() == null || request.getReservaID() <= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "El ID de reserva es inválido o no fue proporcionado");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getEmpleadoID() == null || request.getEmpleadoID() <= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "El ID de empleado es inválido o no fue proporcionado");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getMotivo() == null || request.getMotivo().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "Debe especificar un motivo de cancelación");
                return ResponseEntity.badRequest().body(response);
            }

            // Ejecutar cancelación con validaciones completas
            CancelacionesDto result = reservaService.cancelarReserva(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.getEstadoCancelar() == 1);
            response.put("mensaje", result.getMensaje());
            response.put("reservaID", request.getReservaID());

            if (result.getEstadoCancelar() == 1) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error inesperado al cancelar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/auditoria/activas")
    public ResponseEntity<?> obtenerReservasActivas() {
        try {
            List<Map<String, Object>> reservas = reservaService.obtenerReservasActivas();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reservas);
            response.put("cantidad", reservas.size());
            response.put("mensaje", "Reservas activas obtenidas exitosamente");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error al obtener reservas activas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/auditoria/cancelaciones")
    public ResponseEntity<?> obtenerHistorialCancelaciones() {
        try {
            List<Map<String, Object>> historial = reservaService.obtenerHistorialCancelaciones();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", historial);
            response.put("cantidad", historial.size());
            response.put("mensaje", "Historial de cancelaciones obtenido exitosamente");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error al obtener historial: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }




    @GetMapping("/comparar-liberacion/{reservaId}")
    public ResponseEntity<?> compararLiberacionAsientos(@PathVariable int reservaId) {
        try {
            Map<String, Object> resultado = reservaService.compararLiberacionAsientos(reservaId);

            if (resultado.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "No se encontró información para la reserva ID: " + reservaId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(resultado);
            response.put("mensaje", "Comparación realizada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error al comparar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

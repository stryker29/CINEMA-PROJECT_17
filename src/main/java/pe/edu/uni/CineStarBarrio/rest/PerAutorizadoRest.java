package pe.edu.uni.CineStarBarrio.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.uni.CineStarBarrio.dto.PerAutorizadoDto;
import pe.edu.uni.CineStarBarrio.service.PerAutorizadoService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/personal")
@CrossOrigin(origins = "*")
public class PerAutorizadoRest {

    @Autowired
    private PerAutorizadoService personalService;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody PerAutorizadoDto request) {
        try {
            // Validación básica de entrada
            if (request.getUsuario() == null || request.getUsuario().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "El usuario es obligatorio");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getClave() == null || request.getClave().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "La contraseña es obligatoria");
                return ResponseEntity.badRequest().body(response);
            }

            // Ejecutar validación de login
            PerAutorizadoDto result = personalService.validarLogin(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.getEstadoLogin() == 1);
            response.put("mensaje", result.getMensaje());

            if (result.getEstadoLogin() == 1) {
                // Login exitoso - devolver datos del empleado (sin la contraseña)
                Map<String, Object> empleadoData = new HashMap<>();
                empleadoData.put("empleadoID", result.getEmpleadoID());
                empleadoData.put("nombre", result.getNombre());
                empleadoData.put("apellido", result.getApellido());
                empleadoData.put("nombreCompleto", result.getNombreCompleto());
                empleadoData.put("cargo", result.getCargo());
                empleadoData.put("fechaContratacion", result.getFechaContratacion());

                response.put("data", empleadoData);
                return ResponseEntity.ok(response);
            } else {
                // Login fallido
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error inesperado en el servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/verificar/{empleadoID}")
    public ResponseEntity<?> verificarSesion(@PathVariable Integer empleadoID) {
        try {
            PerAutorizadoDto empleado = personalService.obtenerEmpleadoPorID(empleadoID);

            if (empleado != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", empleado);
                response.put("mensaje", "Sesión válida");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "Empleado no encontrado o inactivo");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error al verificar sesión: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
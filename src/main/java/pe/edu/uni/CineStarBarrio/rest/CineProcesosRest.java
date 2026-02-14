package pe.edu.uni.CineStarBarrio.rest;

// 1. IMPORTACIONES NECESARIAS PARA EL LOG
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Asegúrate que estos imports coincidan con tus carpetas
import pe.edu.uni.CineStarBarrio.dto.VentaDto;
import pe.edu.uni.CineStarBarrio.dto.ConfirmacionDto;
import pe.edu.uni.CineStarBarrio.service.CineProcesosService;

import java.sql.SQLException;

@RestController
@RequestMapping("/bdcinestar/api/procesos")
public class CineProcesosRest {

    // 2. DEFINICIÓN DE LA VARIABLE 'log' (Esto es lo que te faltaba)
    private static final Logger log = LoggerFactory.getLogger(CineProcesosRest.class);

    private final CineProcesosService cineProcesosService;

    // 3. INYECCIÓN POR CONSTRUCTOR (Elimina el warning "field is never assigned")
    public CineProcesosRest(CineProcesosService cineProcesosService) {
        this.cineProcesosService = cineProcesosService;
    }

    @PostMapping("/registrar-venta")
    public ResponseEntity<?> registrarVenta(@RequestBody VentaDto bean) {
        try {
            return ResponseEntity.ok(cineProcesosService.registrarVenta(bean));

        } catch (SQLException e) {
            log.warn("Error SQL en venta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERROR SQL: " + e.getMessage());

        } catch (DataAccessException e) {
            log.error("Error de datos en venta", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERROR DE DATOS: " + e.getMessage());

        } catch (Exception e) {
            log.error("Error interno en venta", e); // Reemplaza e.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("ERROR INTERNO: " + e.getMessage());
        }
    }

    @PostMapping("/confirmar-reserva")
    public ResponseEntity<?> confirmarReserva(@RequestBody ConfirmacionDto bean) {
        try {
            return ResponseEntity.ok(cineProcesosService.confirmarReserva(bean));

        } catch (SQLException e) {
            // Ahora sí funciona 'log' aquí
            log.warn("Error al confirmar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERROR: " + e.getMessage());

        } catch (Exception e) {
            // Ahora sí funciona 'log' aquí
            log.error("Error interno al confirmar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR SISTEMA: " + e.getMessage());
        }
    }
}
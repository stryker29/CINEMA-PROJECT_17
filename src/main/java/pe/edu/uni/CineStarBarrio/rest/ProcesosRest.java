package pe.edu.uni.CineStarBarrio.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.uni.CineStarBarrio.dto.PeliculaDto;
import pe.edu.uni.CineStarBarrio.dto.FuncionDto;
import pe.edu.uni.CineStarBarrio.service.ProcesosService;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cineestar/api/procesos")
public class ProcesosRest {

    @Autowired
    private ProcesosService procesosService;

    // RF1 - PARTE 1: Registrar nueva película
    @PostMapping(value = "/registro/pelicula", headers = "Accept=application/json")
    public ResponseEntity<?> registrarPelicula(@RequestBody PeliculaDto bean) {
        try {
            PeliculaDto resultado = procesosService.registrarPelicula(bean);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERROR: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hubo un error al intentar registrar la película. Contacte a soporte.");
        }
    }

    // RF1 - PARTE 2: Registrar función para película existente
    @PostMapping(value = "/registro/funcion", headers = "Accept=application/json")
    public ResponseEntity<?> registrarFuncion(@RequestBody FuncionDto bean) {
        try {
            FuncionDto resultado = procesosService.registrarFuncion(bean);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERROR: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hubo un error al intentar registrar la función. Contacte a soporte.");
        }
    }

    // CONSULTAS: Obtener cartelera
    @GetMapping(value = "/cartelera")
    public ResponseEntity<?> obtenerCartelera() {
        try {
            List<Map<String, Object>> cartelera = procesosService.obtenerCartelera();
            return ResponseEntity.ok(cartelera);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hubo un error al obtener la cartelera. Contacte a soporte.");
        }
    }

    // CONSULTAS: Obtener películas activas
    @GetMapping(value = "/peliculas/activas")
    public ResponseEntity<?> obtenerPeliculasActivas() {
        try {
            List<Map<String, Object>> peliculas = procesosService.obtenerPeliculasActivas();
            return ResponseEntity.ok(peliculas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hubo un error al obtener las películas. Contacte a soporte.");
        }
    }

    // CONSULTAS: Obtener salas disponibles
    @GetMapping(value = "/salas/disponibles")
    public ResponseEntity<?> obtenerSalasDisponibles() {
        try {
            List<Map<String, Object>> salas = procesosService.obtenerSalasDisponibles();
            return ResponseEntity.ok(salas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hubo un error al obtener las salas. Contacte a soporte.");
        }
    }

    // CONSULTAS: Obtener empleados autorizados
    @GetMapping(value = "/empleados/autorizados")
    public ResponseEntity<?> obtenerEmpleadosAutorizados() {
        try {
            List<Map<String, Object>> empleados = procesosService.obtenerEmpleadosAutorizados();
            return ResponseEntity.ok(empleados);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hubo un error al obtener los empleados. Contacte a soporte.");
        }
    }

    // CONSULTAS: Obtener funciones por película
    @GetMapping(value = "/funciones/pelicula/{peliculaId}")
    public ResponseEntity<?> obtenerFuncionesPorPelicula(@PathVariable int peliculaId) {
        try {
            List<Map<String, Object>> funciones = procesosService.obtenerFuncionesPorPelicula(peliculaId);
            return ResponseEntity.ok(funciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hubo un error al obtener las funciones. Contacte a soporte.");
        }
    }
}
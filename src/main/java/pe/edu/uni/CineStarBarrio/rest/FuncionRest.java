package pe.edu.uni.CineStarBarrio.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.uni.CineStarBarrio.dto.FuncionDto;
import pe.edu.uni.CineStarBarrio.service.FuncionService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/funciones")
@CrossOrigin(origins = "*")
public class FuncionRest {

    @Autowired
    private FuncionService funcionService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getFuncion(@PathVariable("id") Integer id) {
        Map<String, Object> resp = new HashMap<>();

        try {
            FuncionDto dto = funcionService.obtenerPorId(id);
            resp.put("success", true);
            resp.put("data", dto);
            return ResponseEntity.ok(resp);

        } catch (EmptyResultDataAccessException e) {
            resp.put("success", false);
            resp.put("mensaje", "Función no encontrada.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);

        } catch (Exception e) {
            resp.put("success", false);
            resp.put("mensaje", "Error al obtener la función: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }
}

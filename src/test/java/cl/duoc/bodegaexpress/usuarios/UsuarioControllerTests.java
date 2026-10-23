package cl.duoc.bodegaexpress.usuarios;

import cl.duoc.bodegaexpress.usuarios.controller.UsuarioController;
import cl.duoc.bodegaexpress.usuarios.exception.ManejadorErrores;
import cl.duoc.bodegaexpress.usuarios.model.Usuario;
import cl.duoc.bodegaexpress.usuarios.repository.UsuarioRepository;
import cl.duoc.bodegaexpress.usuarios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuarioControllerTests {
    private UsuarioRepository repository;
    private MockMvc mvc;
    private Usuario usuario;

    private static final String BODY = """
            {"nombre":"Carlos","apellido":"Perez","email":"carlos.perez@example.com"}
            """;

    @BeforeEach
    void configurar() {
        repository = mock(UsuarioRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new UsuarioController(new UsuarioService(repository)))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        usuario = new Usuario(1L, "Carlos", "Perez", "carlos.perez@example.com");
    }

    @Test
    void listarYBuscarPorId() throws Exception {
        when(repository.findAll()).thenReturn(List.of(usuario));
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));

        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Carlos"))
                .andExpect(jsonPath("$[0].email").value("carlos.perez@example.com"));
        mvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.apellido").value("Perez"));
    }

    @Test
    void crearDevuelve201LocationYGeneraIdEnLaPersistencia() throws Exception {
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario nuevo = invocation.getArgument(0);
            org.junit.jupiter.api.Assertions.assertNull(nuevo.getId());
            nuevo.setId(1L);
            return nuevo;
        });

        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("{", "{\"id\":99,")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/usuarios/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("carlos.perez@example.com"));
    }

    @Test
    void actualizarModificaLosCamposYConservaElId() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(put("/api/usuarios/1").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("Carlos", "Camila").replace("Perez", "Rojas")
                                .replace("carlos.perez@example.com", "camila.rojas@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Camila"))
                .andExpect(jsonPath("$.apellido").value("Rojas"))
                .andExpect(jsonPath("$.email").value("camila.rojas@example.com"));
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));

        mvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent());

        verify(repository).delete(usuario);
    }

    @Test
    void operacionesSobreUsuarioInexistenteDevuelven404() throws Exception {
        mvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/usuarios/99").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/usuarios/99"))
                .andExpect(status().isNotFound());

        verify(repository, never()).save(any());
        verify(repository, never()).delete(any(Usuario.class));
    }

    @Test
    void rechazarDatosInvalidosEnCreacionYActualizacion() throws Exception {
        List<String> cuerposInvalidos = List.of(
                "{}",
                BODY.replace("Carlos", " "),
                BODY.replace("Perez", ""),
                BODY.replace("carlos.perez@example.com", ""),
                BODY.replace("carlos.perez@example.com", "correo-invalido")
        );

        for (String body : cuerposInvalidos) {
            mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/api/usuarios/1").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(repository);
    }

    @Test
    void emailDuplicadoEnCreacionDevuelve409() throws Exception {
        when(repository.save(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("email duplicado"));

        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("El email ya está registrado"));
    }

    @Test
    void emailDuplicadoEnActualizacionDevuelve409() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.save(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("email duplicado"));

        mvc.perform(put("/api/usuarios/1").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("carlos.perez@example.com", "otro@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

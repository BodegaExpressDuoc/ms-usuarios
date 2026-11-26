package cl.duoc.bodegaexpress.usuarios.service;

import cl.duoc.bodegaexpress.usuarios.model.Usuario;
import cl.duoc.bodegaexpress.usuarios.repository.UsuarioRepository;
import cl.duoc.bodegaexpress.usuarios.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con id: " + id));
    }

    @Transactional
    public Usuario crearUsuario(Usuario usuario) {
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(usuario.getNombre());
        nuevoUsuario.setApellido(usuario.getApellido());
        nuevoUsuario.setEmail(usuario.getEmail());
        nuevoUsuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(nuevoUsuario);
    }

    @Transactional
    public Usuario actualizarUsuario(Long id, Usuario usuario) {
        Usuario usuarioExistente = buscarPorId(id);
        usuarioExistente.setNombre(usuario.getNombre());
        usuarioExistente.setApellido(usuario.getApellido());
        usuarioExistente.setEmail(usuario.getEmail());
        usuarioExistente.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuarioExistente);
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = buscarPorId(id);
        usuarioRepository.delete(usuario);
    }

    public String iniciarSesion(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(encontrado -> passwordEncoder.matches(password, encontrado.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        return jwtService.generarToken(usuario.getEmail());
    }
}

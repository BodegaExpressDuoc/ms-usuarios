package cl.duoc.bodegaexpress.usuarios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Solo acepta peticiones que llegan a través del AWS API Gateway.
 * <p>
 * El API Gateway agrega el header {@value #HEADER} con un secreto compartido
 * ({@code GATEWAY_SECRET}) en cada integración. Si el header falta o no coincide,
 * se responde 403 sin llegar al controlador. Si {@code GATEWAY_SECRET} está vacío
 * (desarrollo local y tests), el filtro no restringe nada.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroGateway extends OncePerRequestFilter {

    public static final String HEADER = "X-Gateway-Secret";

    private final byte[] secreto;

    public FiltroGateway(@Value("${gateway.secret:}") String secreto) {
        this.secreto = secreto == null || secreto.isBlank() ? null : secreto.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (secreto == null || coincide(request.getHeader(HEADER))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"Acceso permitido solo a través del API Gateway\"}");
    }

    private boolean coincide(String recibido) {
        return recibido != null && MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8));
    }
}

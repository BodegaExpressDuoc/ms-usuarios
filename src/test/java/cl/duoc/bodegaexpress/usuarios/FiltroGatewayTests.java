package cl.duoc.bodegaexpress.usuarios;

import cl.duoc.bodegaexpress.usuarios.security.FiltroGateway;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class FiltroGatewayTests {

    private static MockHttpServletResponse ejecutar(FiltroGateway filtro, String header, MockFilterChain chain)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/usuarios");
        if (header != null) {
            request.addHeader(FiltroGateway.HEADER, header);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, chain);
        return response;
    }

    @Test
    void conSecretoCorrectoDejaPasar() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = ejecutar(new FiltroGateway("s3creto"), "s3creto", chain);
        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void sinHeaderOConSecretoIncorrectoResponde403() throws Exception {
        for (String header : new String[]{null, "", "otro"}) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = ejecutar(new FiltroGateway("s3creto"), header, chain);
            assertEquals(403, response.getStatus());
            assertNull(chain.getRequest(), "no debe llegar al controlador");
        }
    }

    @Test
    void sinSecretoConfiguradoNoRestringe() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        assertEquals(200, ejecutar(new FiltroGateway(""), null, chain).getStatus());
        assertNotNull(chain.getRequest());
    }
}

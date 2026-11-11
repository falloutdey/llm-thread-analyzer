package com.llm_thread_analyzer.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Rotas antigas /api/tutor/**: token obrigatório, e desligáveis no servidor de produção. */
class FiltroRotasAntigasTutorTest {

    private MockHttpServletResponse chamar(FiltroRotasAntigasTutor filtro, String uri, String token, MockFilterChain cadeia)
            throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        if (token != null) req.addHeader("X-Relatorio-Token", token);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filtro.doFilter(req, resp, cadeia);
        return resp;
    }

    @Test
    void comOTokenCertoPassa() throws Exception {
        MockFilterChain cadeia = new MockFilterChain();
        chamar(new FiltroRotasAntigasTutor(true, "segredo"), "/api/tutor/submissoes", "segredo", cadeia);
        assertNotNull(cadeia.getRequest(), "a requisição seguiu para o controlador");
    }

    @Test
    void semTokenOuComTokenErradoResponde401() throws Exception {
        FiltroRotasAntigasTutor filtro = new FiltroRotasAntigasTutor(true, "segredo");
        MockFilterChain c1 = new MockFilterChain();
        assertEquals(401, chamar(filtro, "/api/tutor/submissoes", null, c1).getStatus());
        assertNull(c1.getRequest());
        MockFilterChain c2 = new MockFilterChain();
        assertEquals(401, chamar(filtro, "/api/tutor/conversas", "outro", c2).getStatus());
        assertNull(c2.getRequest());
    }

    @Test
    void semRelatorioTokenConfiguradoNinguemUsa() throws Exception {
        MockFilterChain cadeia = new MockFilterChain();
        assertEquals(403, chamar(new FiltroRotasAntigasTutor(true, ""), "/api/tutor/submissoes", "", cadeia).getStatus());
        assertNull(cadeia.getRequest());
    }

    @Test
    void desligadasRespondem404MesmoComToken() throws Exception {
        MockFilterChain cadeia = new MockFilterChain();
        assertEquals(404, chamar(new FiltroRotasAntigasTutor(false, "segredo"), "/api/tutor/submissoes", "segredo", cadeia).getStatus());
        assertNull(cadeia.getRequest());
    }

    @Test
    void outrasRotasNaoSaoAfetadas() throws Exception {
        MockFilterChain cadeia = new MockFilterChain();
        chamar(new FiltroRotasAntigasTutor(false, ""), "/api/java/projetos", null, cadeia);
        assertNotNull(cadeia.getRequest());
    }
}

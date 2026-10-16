package com.prueba.polizas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "x-api-key";

    private static final List<String> RUTAS_PUBLICAS =
            List.of("/swagger-ui", "/v3/api-docs", "/actuator/health");

    private final byte[] apiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${seguridad.api-key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return RUTAS_PUBLICAS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(HEADER);

        // MessageDigest.isEqual compara en tiempo constante (evita ataques de timing)
        if (recibida == null || !MessageDigest.isEqual(apiKey, recibida.getBytes(StandardCharsets.UTF_8))) {
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                    "Falta el header " + HEADER + " o su valor no es válido");
            problema.setTitle("No autorizado");

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), problema);
            return;
        }

        chain.doFilter(request, response);
    }
}

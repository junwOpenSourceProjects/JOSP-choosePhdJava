package josp.choosphd.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import josp.choosphd.common.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwt;
    private final ObjectMapper json = new ObjectMapper();

    public AuthInterceptor(JwtService jwt) {
        this.jwt = jwt;
    }

    private static final Set<String> WHITELIST = Set.of(
            "/api/v1/auth/login",
            "/api/v1/health",
            "/api/v1/dict/regions",
            "/api/v1/dict/subjects",
            "/api/v1/dict/sources",
            "/api/v1/filter/regions",
            "/api/v1/filter/subjects",
            "/api/v1/filter/sources",
            "/api/v1/filter/countries",
            "/api/v1/filter/years",
            "/api/v1/overview/summary"
    );

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String path = req.getRequestURI();
        if (WHITELIST.contains(path)) return true;

        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            unauthorized(resp, "missing token");
            return false;
        }
        try {
            Claims c = jwt.parse(h.substring(7));
            req.setAttribute("user", c.getSubject());
            req.setAttribute("role", c.get("role", String.class));
            return true;
        } catch (JwtException e) {
            unauthorized(resp, "invalid token: " + e.getMessage());
            return false;
        }
    }

    private void unauthorized(HttpServletResponse resp, String msg) throws Exception {
        resp.setStatus(401);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try (PrintWriter w = resp.getWriter()) {
            w.write(json.writeValueAsString(ApiResult.error(401, msg)));
        }
    }
}

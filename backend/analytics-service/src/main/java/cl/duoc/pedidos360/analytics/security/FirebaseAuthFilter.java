package cl.duoc.pedidos360.analytics.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autorizacion: intercepta cada peticion a /api/**, extrae el
 * header "Authorization: Bearer <JWT>", y usa el Firebase Admin SDK para
 * verificar la firma, el issuer y la vigencia del token contra el IDaaS
 * (Firebase Authentication). Si el token no es valido, corta la cadena
 * con 401. Si es valido, deja el uid del usuario en el request y continua.
 */
@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // Las rutas fuera de /api quedan libres (health check, consola h2, etc.)
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Preflight CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Falta el token de autorizacion (Bearer)");
            return;
        }

        String idToken = authHeader.substring(7);
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            // uid disponible para el controlador (ej: para registrar quien creo el pedido)
            request.setAttribute("uid", decodedToken.getUid());
            request.setAttribute("email", decodedToken.getEmail());
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalido o expirado: " + e.getMessage());
        }
    }
}

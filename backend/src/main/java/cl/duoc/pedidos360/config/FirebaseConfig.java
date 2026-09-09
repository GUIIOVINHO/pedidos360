package cl.duoc.pedidos360.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Inicializa el SDK de administracion de Firebase (el IDaaS usado en este
 * proyecto). Esto permite al backend verificar, en cada request, la firma
 * y vigencia del JWT (ID Token) emitido por Firebase Authentication.
 */
@Component
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                Resource resource = resourceLoader.getResource("classpath:" + credentialsPath);
                try (InputStream serviceAccount = resource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase Admin SDK inicializado correctamente.");
                }
            }
        } catch (Exception e) {
            log.error("No se pudo inicializar Firebase Admin SDK. Revisa que exista " +
                    "src/main/resources/{} (ver README). Error: {}", credentialsPath, e.getMessage());
        }
    }
}

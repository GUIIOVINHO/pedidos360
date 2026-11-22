package cl.duoc.pedidos360.kafkaadmin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Inicializa el Firebase Admin SDK (IDaaS) para poder verificar el JWT en cada request.
 *
 * El archivo de credenciales se busca en este orden:
 *  1. la ruta configurada en firebase.credentials.path (por defecto ../firebase-service-account.json,
 *     es decir, la carpeta backend/ compartida por todos los microservicios)
 *  2. backend/firebase-service-account.json (si se ejecuta desde la raiz del repo)
 *  3. src/main/resources/firebase-service-account.json (ubicacion usada en la EP1)
 */
@Component
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }
            Resource resource = buscarCredenciales();
            if (resource == null) {
                log.error("No se encontro firebase-service-account.json. Copialo en la carpeta backend/ (ver README).");
                return;
            }
            try (InputStream serviceAccount = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK inicializado correctamente.");
            }
        } catch (Exception e) {
            log.error("No se pudo inicializar Firebase Admin SDK: {}", e.getMessage());
        }
    }

    private Resource buscarCredenciales() {
        List<Resource> candidatos = List.of(
                new FileSystemResource(credentialsPath),
                new FileSystemResource("backend/firebase-service-account.json"),
                new ClassPathResource("firebase-service-account.json"));
        for (Resource candidato : candidatos) {
            if (candidato.exists()) {
                return candidato;
            }
        }
        return null;
    }
}

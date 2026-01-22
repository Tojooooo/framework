package mg.tojooooo.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SecurityConfig {
    private static final Properties properties = new Properties();
    
    static {
        try (InputStream input = SecurityConfig.class.getClassLoader().getResourceAsStream("/WEB-INF/application.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                // Valeurs par défaut
                properties.setProperty("session.user.key", "CURRENT_USER");
                properties.setProperty("session.roles.key", "USER_ROLES");
            }
        } catch (IOException e) {
            // En cas d'erreur, on utilise les valeurs par défaut
            properties.setProperty("session.user.key", "CURRENT_USER");
            properties.setProperty("session.roles.key", "USER_ROLES");
        }
    }
    
    public static String getSessionUserKey() {
        return properties.getProperty("session.user.key", "CURRENT_USER");
    }
    
    public static String getSessionRolesKey() {
        return properties.getProperty("session.roles.key", "USER_ROLES");
    }
}
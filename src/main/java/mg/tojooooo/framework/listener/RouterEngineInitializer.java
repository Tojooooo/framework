package mg.tojooooo.framework.listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import mg.tojooooo.framework.RouterEngine;

@WebListener
public class RouterEngineInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Initialisation du RouterEngine...");
        
        RouterEngine routerEngine = new RouterEngine();
        try {
            routerEngine.loadClasspathControllers();
            routerEngine.loadControllerUrlMappings();
            
            ServletContext context = sce.getServletContext();
            context.setAttribute("routerEngine", routerEngine);
            
            routerEngine.printUrlMappedMethods();
            
            System.out.println("RouterEngine initialisé avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du RouterEngine: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Nettoyage du RouterEngine...");
        // Nettoyage si nécessaire
    }
}
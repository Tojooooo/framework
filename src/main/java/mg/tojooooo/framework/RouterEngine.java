package mg.tojooooo.framework;

import java.lang.reflect.Method;

import mg.tojooooo.framework.annotation.Route;

public class RouterEngine {
    private Object controller;
    
    public RouterEngine(Object controller) {
        this.controller = controller;
    }
    
    public void handleRequest(String url) {
        Method[] methods = controller.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(Route.class)) {
                Route annotation = method.getAnnotation(Route.class);
                String routeUrl = annotation.value();
                
                if (routeUrl.equals(url)) {
                    try {
                        method.invoke(controller);
                        return;
                    } catch (Exception e) {
                        System.out.println("Erreur pour l'URL " + url + ": " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println("404 - Aucune route trouvée pour: " + url);
    }
}
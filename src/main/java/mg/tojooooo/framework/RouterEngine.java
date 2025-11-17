package mg.tojooooo.framework;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mg.tojooooo.framework.annotation.Route;
import mg.tojooooo.framework.util.JavaControllerScanner;
import mg.tojooooo.framework.util.RouteMapping;

public class RouterEngine {
    private Set<Class<?>> controllers;
    private Map<String, RouteMapping> routeMappings;

    public void setControllers(Set<Class<?>> controllers) { this.controllers = controllers; }
    public Set<Class<?>> getControllers() { return controllers; }
    public void setRouteMappings(Map<String, RouteMapping> routeMappings) { this.routeMappings = routeMappings; }
    public Map<String, RouteMapping> getRouteMappings() { return routeMappings; }

    public void loadClasspathControllers() throws Exception {
        setControllers(JavaControllerScanner.findControllers());
    }

    public void loadControllerUrlMappings() throws Exception {
        setRouteMappings(findAllRoutes());
    }

    public RouteMapping findRouteMapping(String url) {
        if (routeMappings == null || routeMappings.isEmpty()) {
            return null;
        }
        
        return routeMappings.get(url);
    }

    public String getUrlReturnValue(String url) throws Exception {
        RouteMapping routeMapping = findRouteMapping(url);
        if (routeMapping == null) return null;

        Object controllerInstance = routeMapping.getControllerClass().getDeclaredConstructor().newInstance();
        return (String) routeMapping.getMethod().invoke(controllerInstance);
    }

    private boolean matchesRoute(String routePattern, String actualUrl) {
        return routePattern.equals(actualUrl);
    }

    // Méthode pour obtenir toutes les routes disponibles
    public Map<String, RouteMapping> getAllRoutes() {
        return new HashMap<>(routeMappings); // Retourne une copie
    }

    public Map<String, RouteMapping> findAllRoutes() throws Exception {
        Map<String, RouteMapping> allRoutes = new HashMap<>();
        
        if (controllers == null || controllers.isEmpty()) {
            return allRoutes;
        }
        
        for (Class<?> controller : controllers) {
            Method[] methods = controller.getDeclaredMethods();
            
            for (Method method : methods) {
                if (method.isAnnotationPresent(Route.class)) {
                    Route annotation = method.getAnnotation(Route.class);
                    String routeUrl = annotation.value();
                    
                    allRoutes.put(routeUrl, new RouteMapping(controller, method, annotation));
                }
            }
        }
        
        return allRoutes;
    }

    // Cette méthode n'est plus nécessaire dans la nouvelle logique
    public Map<Class<?>, Set<Method>> findUrlMappedMethods() throws Exception {
        Map<Class<?>, Set<Method>> controllerMethods = new HashMap<>();
        
        for (Class<?> controller : controllers) {
            Set<Method> urlMappedMethods = findUrlMappedMethodsInController(controller);
            if (!urlMappedMethods.isEmpty()) {
                controllerMethods.put(controller, urlMappedMethods);
            }
        }
        
        return controllerMethods;
    }

    private Set<Method> findUrlMappedMethodsInController(Class<?> controller) {
        Set<Method> urlMappedMethods = new HashSet<>();
        
        Method[] methods = controller.getDeclaredMethods();
        for (Method method : methods) {
            if (isUrlMappedMethod(method)) {
                urlMappedMethods.add(method);
            }
        }
        
        return urlMappedMethods;
    }

    private boolean isUrlMappedMethod(Method method) {
        if (method.isAnnotationPresent(Route.class)) {
            return true;
        }
        
        return Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> 
                    annotation.annotationType().getSimpleName().equals("UrlMapping"));
    }

    public void printUrlMappedMethods() {
        if (routeMappings == null || routeMappings.isEmpty()) {
            System.out.println("Aucune route trouvée.");
            return;
        }
        
        System.out.println("\n=== ROUTES DISPONIBLES (" + routeMappings.size() + ") ===");
        routeMappings.forEach((url, routeMapping) -> {
            System.out.println("- URL: " + url);
            System.out.println("  Controller: " + routeMapping.getControllerClass().getSimpleName());
            System.out.println("  Méthode: " + routeMapping.getMethod().getName() + "()");
        });
    }

    public Set<Method> findAllUrlMappedMethods() {
        Set<Method> allMethods = new HashSet<>();
        
        for (Class<?> controller : controllers) {
            allMethods.addAll(findUrlMappedMethodsInController(controller));
        }
        
        return allMethods;
    }
}
package mg.tojooooo.framework;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import mg.tojooooo.framework.annotation.Route;
import mg.tojooooo.framework.annotation.RequestParam;
import mg.tojooooo.framework.annotation.PathParam;
import mg.tojooooo.framework.util.JavaControllerScanner;
import mg.tojooooo.framework.util.RouteMapping;

import org.modelmapper.ModelMapper;


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
        
        return getRouteMapping(url);
    }

    
    public Object getUrlReturnValue(HttpServletRequest request, String url) throws Exception {
        RouteMapping routeMapping = findRouteMapping(url);
        if (routeMapping == null) return null;

        Object[] paramValues = processRequestData(request, routeMapping);

        Object controllerInstance = routeMapping.getControllerClass().getDeclaredConstructor().newInstance();
        return routeMapping.getMethod().invoke(controllerInstance, paramValues);
    }

    private Object[] processRequestData(HttpServletRequest request, RouteMapping routeMapping) {
        Method mth = routeMapping.getMethod();
        Parameter[] params = mth.getParameters();
        Object[] paramValues = new Object[params.length];
        ModelMapper modelMapper = new ModelMapper();
        for (int i = 0; i < params.length; i++) {
            Class<?> paramType = params[i].getType();
            // Object requestParamValue = request.getParameter(params[i].getName());
            if (params[i].isAnnotationPresent(PathParam.class)) {
                PathParam pp = params[i].getAnnotation(PathParam.class);
                String[] urlChunks = routeMapping.getUrl().split("/");
                String[] requestUrlChunks = request.getRequestURI().substring(request.getContextPath().length()).split("/");
                for (int j = 0; j < urlChunks.length; j++) {
                    if (urlChunks[j].startsWith("{") && urlChunks[j].endsWith("}")) {
                        urlChunks[j] = urlChunks[j].replace("{", "");
                        urlChunks[j] = urlChunks[j].replace("}", "");
                        if (urlChunks[j].startsWith(pp.value())) {
                            paramValues[i] = modelMapper.map(requestUrlChunks[j], paramType);
                            break;
                        }
                    }
                }
            } else if (params[i].isAnnotationPresent(RequestParam.class)) {
                RequestParam a = params[i].getAnnotation(RequestParam.class);
                paramValues[i] = modelMapper.map(request.getParameter(a.value()), paramType);
            } else {
                paramValues[i] = modelMapper.map(request.getParameter(params[i].getName()), paramType);
            }
        }
        return paramValues;
    }


    private RouteMapping getRouteMapping(String url) {
        if (routeMappings.get(url) != null) return routeMappings.get(url);

        String[] urlChunks = url.split("/");
        for (Map.Entry<String, RouteMapping> entry: routeMappings.entrySet()) {
            String[] entryUrlChunks = entry.getValue().getUrl().split("/");
            boolean matches = true;
            if (urlChunks.length == entryUrlChunks.length) {
                for (int i = 0; i < urlChunks.length; i++) {
                    if (!urlChunksMatch(urlChunks[i], entryUrlChunks[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) return entry.getValue();
            }
        }

        return null;
    }

    private boolean urlChunksMatch(String chunk1, String chunk2) {
        if (chunk1.equals(chunk2)) return true;
        if (
            (chunk1.startsWith("{") && chunk1.endsWith("}")) || 
            (chunk2.startsWith("{") && chunk2.endsWith("}"))
        ) return true;
        return false;
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
package mg.tojooooo.framework;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import mg.tojooooo.framework.annotation.Route;
import mg.tojooooo.framework.annotation.RequestParam;
import mg.tojooooo.framework.annotation.Get;
import mg.tojooooo.framework.annotation.PathParam;
import mg.tojooooo.framework.annotation.Post;
import mg.tojooooo.framework.util.JavaControllerScanner;
import mg.tojooooo.framework.util.RouteMapping;
import mg.tojooooo.framework.util.UrlMappedMethod;

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

    public RouteMapping findRouteMapping(String url, HttpServletRequest request) {
        if (routeMappings == null || routeMappings.isEmpty()) {
            return null;
        }
        
        return getRouteMapping(url, request);
    }

    public Object getUrlReturnValue(HttpServletRequest request, String url) throws Exception {
        RouteMapping routeMapping = findRouteMapping(url, request);
        if (routeMapping == null) return null;

        Object[] paramValues = processRequestData(request, routeMapping);

        Object controllerInstance = routeMapping.getControllerClass().getDeclaredConstructor().newInstance();
        if (!routeMapping.getUrlMappedMethods().isEmpty()) {
            Method method = routeMapping.getUrlMappedMethods().get(0).getMethod();
            return method.invoke(controllerInstance, paramValues);
        }
        
        return null;
    }

    private Object[] processRequestData(HttpServletRequest request, RouteMapping routeMapping) throws Exception {
        Method mth = !routeMapping.getUrlMappedMethods().isEmpty() 
            ? routeMapping.getUrlMappedMethods().get(0).getMethod() 
            : null;
        
        if (mth == null) return new Object[0];
        
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
                            if (paramType == Boolean.class) {
                                paramValues[i] = Boolean.parseBoolean(requestUrlChunks[j]);
                            } else {
                                paramValues[i] = modelMapper.map(requestUrlChunks[j], paramType);
                            }
                            break;
                        }
                    }
                }
            } else if (params[i].isAnnotationPresent(RequestParam.class)) {
                RequestParam a = params[i].getAnnotation(RequestParam.class);
                if (paramType == Boolean.class) {
                    paramValues[i] = Boolean.parseBoolean(request.getParameter(a.value()));
                } else {
                    paramValues[i] = modelMapper.map(request.getParameter(a.value()), paramType);
                }
            } else if (params[i].getType() == Map.class) {
                ParameterizedType genericType = (ParameterizedType) params[i].getParameterizedType();
                Type[] arrTypes = genericType.getActualTypeArguments();
                if (arrTypes.length == 2 && arrTypes[0] == String.class && arrTypes[1] == Object.class) {
                    paramValues[i] = processMapParam(request, modelMapper);
                } else {
                    throw new Exception("Type de map inattendu. doit etre Map<String, Object>");
                }
            } else {
                if (paramType == Boolean.class) {
                    paramValues[i] = Boolean.parseBoolean(request.getParameter(params[i].getName()));
                } else {
                    paramValues[i] = modelMapper.map(request.getParameter(params[i].getName()), paramType);
                }
            }
        }
        return paramValues;
    }

    private Map<String, Object> processMapParam(HttpServletRequest request, ModelMapper modelMapper) {
        Map<String, Object> m = new HashMap<String, Object>();
        for(String paramName: Collections.list(request.getParameterNames())) {
            m.put(paramName, modelMapper.map(request.getParameter(paramName), Object.class));
        }
        return m;
    }

    private RouteMapping getRouteMapping(String url, HttpServletRequest request) {
        if (routeMappings.get(url) != null) {
            RouteMapping mapping = routeMappings.get(url);
            return getMatchingMethod(mapping, request);
        }

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
                if (matches) {
                    return getMatchingMethod(entry.getValue(), request);
                }
            }
        }

        return null;
    }

    private RouteMapping getMatchingMethod(RouteMapping routeMapping, HttpServletRequest request) {
        String requestMethod = request.getMethod().toUpperCase();
        List<UrlMappedMethod> urlMappedMethods = routeMapping.getUrlMappedMethods();
        
        for (UrlMappedMethod urlMappedMethod : urlMappedMethods) {
            if (requestMethod.equalsIgnoreCase(urlMappedMethod.getRequestMethod())) {
                return new RouteMapping(
                    routeMapping.getControllerClass(), 
                    Arrays.asList(urlMappedMethod), 
                    routeMapping.getUrl()
                );
            }
        }
        
        // zay hita ihany raha tsisy
        if (!urlMappedMethods.isEmpty()) {
            return new RouteMapping(
                routeMapping.getControllerClass(), 
                Arrays.asList(urlMappedMethods.get(0)), 
                routeMapping.getUrl()
            );
        }
        
        return routeMapping;
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

    public Map<String, RouteMapping> getAllRoutes() {
        return new HashMap<>(routeMappings);
    }

    public Map<String, RouteMapping> findAllRoutes() throws Exception {
        Map<String, RouteMapping> allRoutes = new HashMap<>();
        
        if (controllers == null || controllers.isEmpty()) {
            return allRoutes;
        }
        
        for (Class<?> controller : controllers) {
            Method[] methods = controller.getDeclaredMethods();
            Map<String, List<UrlMappedMethod>> routeMethodsMap = new HashMap<>();
            
            for (Method method : methods) {
                if (method.isAnnotationPresent(Route.class)) {
                    Route annotation = method.getAnnotation(Route.class);
                    String routeUrl = annotation.value();
                    String requestMethod = determineRequestMethod(method);
                    
                    UrlMappedMethod urlMappedMethod = new UrlMappedMethod(method, requestMethod);
                    
                    routeMethodsMap.computeIfAbsent(routeUrl, k -> new ArrayList<>()).add(urlMappedMethod);
                }
            }
            
            for (Map.Entry<String, List<UrlMappedMethod>> entry : routeMethodsMap.entrySet()) {
                String routeUrl = entry.getKey();
                List<UrlMappedMethod> urlMappedMethods = entry.getValue();
                
                allRoutes.put(routeUrl, new RouteMapping(controller, urlMappedMethods, routeUrl));
            }
        }
        
        return allRoutes;
    }

    private String determineRequestMethod(Method method) {
        if (method.isAnnotationPresent(Get.class)) {
            return "GET";
        } else if (method.isAnnotationPresent(Post.class)) {
            return "POST";
        }
        return null;
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
            // System.out.println("- URL: " + url);
            // System.out.println("  Controller: " + routeMapping.getControllerClass().getSimpleName());
            // System.out.println("  Méthode: " + routeMapping.getMethod().getName() + "()");
            System.out.println(routeMapping.toString());
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
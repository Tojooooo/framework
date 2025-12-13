package mg.tojooooo.framework;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
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
                Object value = handlePathParam(request, routeMapping, params[i], paramType, modelMapper);
                paramValues[i] = convertValue(value.toString(), paramType);
            } else if (params[i].isAnnotationPresent(RequestParam.class)) {
                RequestParam a = params[i].getAnnotation(RequestParam.class);
                paramValues[i] = convertValue(request.getParameter(a.value()), paramType);
            } else if (params[i].getType() == Map.class) {
                ParameterizedType genericType = (ParameterizedType) params[i].getParameterizedType();
                Type[] arrTypes = genericType.getActualTypeArguments();
                if (arrTypes.length == 2 && arrTypes[0] == String.class && arrTypes[1] == Object.class) {
                    paramValues[i] = processMapParam(request, modelMapper);
                } else {
                    throw new Exception("Type de map inattendu. doit etre Map<String, Object>");
                }
            } else if (isCustomObject(paramType)) {
                paramValues[i] = createCustomObject(request, paramType, "");
            } else {
                paramValues[i] = modelMapper.map(request.getParameter(params[i].getName()), paramType);
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

    private Object handlePathParam(HttpServletRequest request, RouteMapping routeMapping, 
                                Parameter param, Class<?> paramType, ModelMapper modelMapper) {
        PathParam pp = param.getAnnotation(PathParam.class);
        String[] urlChunks = routeMapping.getUrl().split("/");
        String[] requestUrlChunks = request.getRequestURI().substring(request.getContextPath().length()).split("/");
        for (int i = 0; i < urlChunks.length; i++) {
            if (urlChunks[i].startsWith("{") && urlChunks[i].endsWith("}")) {
                urlChunks[i] = urlChunks[i].replace("{", "");
                urlChunks[i] = urlChunks[i].replace("}", "");
                if (urlChunks[i].startsWith(pp.value())) {
                    return convertValue(requestUrlChunks[i], paramType);
                }
            }
        }
        return null;
    }

    private Map<String, Object> handleMapParam(HttpServletRequest request, Parameter param, 
                                            ModelMapper modelMapper) {
        Map<String, Object> map = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] values = request.getParameterValues(paramName);
            
            if (values != null) {
                map.put(paramName, values.length == 1 ? values[0] : values);
            }
        }
        
        return map;
    }

    // Vérifie si c'est un objet personnalisé
    private boolean isCustomObject(Class<?> clazz) {
        // System.out.println("custom: "+ clazz.getName());
        // Simplifié : tout ce qui n'est pas type Java standard est considéré comme custom
        String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";

        return !clazz.isPrimitive() 
            && !clazz.isArray() 
            && !Collection.class.isAssignableFrom(clazz)
            && !Map.class.isAssignableFrom(clazz)
            && !packageName.startsWith("java.")
            && !packageName.startsWith("javax.");
    }

    private Object createCustomObject(HttpServletRequest request, Class<?> clazz, String prefix) {
        try {
            Object instance = null;
            try {
                // classe interne statique
                instance = clazz.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException e1) {
                System.err.println("Impossible de créer une instance de " + clazz.getName());
                return null;
            }
            
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                String fieldName = field.getName();
                
                // nom du paramètre
                String paramName = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
                
                // Vérifie si c'est une liste
                if (List.class.isAssignableFrom(fieldType)) {
                    // type générique de la liste
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) genericType;
                        Type[] typeArgs = pt.getActualTypeArguments();
                        if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                            Class<?> listItemType = (Class<?>) typeArgs[0];
                            
                            List<Object> list = createObjectList(request, listItemType, paramName);
                            field.set(instance, list);
                        }
                    }
                }
                // recursion si field objet personnalisé
                else if (isCustomObject(fieldType)) {
                    // Appel récursif
                    Object nestedObject = createCustomObject(request, fieldType, paramName);
                    field.set(instance, nestedObject);
                }
                // Vérifie si un tableau pour checkboxes)
                else if (fieldType.isArray()) {
                    Object array = createArrayFromRequest(request, fieldType, paramName);
                    if (array != null) {
                        field.set(instance, array);
                    }
                }
                // Type simple (String, Integer, etc.)
                else {
                    String paramValue = request.getParameter(paramName);
                    if (paramValue != null) {
                        field.set(instance, convertValue(paramValue, fieldType));
                    }
                    
                    // Gestion checkboxes booléennes
                    if ((fieldType == Boolean.class || fieldType == boolean.class) 
                        && request.getParameter(paramName + ".checkbox") != null) {
                        String checkboxValue = request.getParameter(paramName);
                        boolean isChecked = "on".equals(checkboxValue) || "true".equals(checkboxValue);
                        field.set(instance, isChecked);
                    }
                }
            }
            
            return instance;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Crée liste d'objets à partir des paramètres de la requête
    private List<Object> createObjectList(HttpServletRequest request, Class<?> itemType, String prefix) {
        List<Object> list = new ArrayList<>();
        
        // Cherche tous les indices dans les paramètres
        Set<Integer> indices = new HashSet<>();
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            
            // dept[0] ohatra
            if (paramName.startsWith(prefix + "[") && paramName.contains("]")) {
                try {
                    // Extraire l'index
                    int startBracket = paramName.indexOf("[");
                    int endBracket = paramName.indexOf("]", startBracket);
                    String indexStr = paramName.substring(startBracket + 1, endBracket);
                    int index = Integer.parseInt(indexStr);
                    indices.add(index);
                } catch (Exception e) {
                    // Ignorer les paramètres mal formés
                }
            }
        }
        
        // crée un objet pour chaque indice
        for (int index : indices) {
            String itemPrefix = prefix + "[" + index + "]";
            
            if (isCustomObject(itemType)) {
                // Objet personnalisé
                Object item = createCustomObject(request, itemType, itemPrefix);
                if (item != null) {
                    while (list.size() <= index) {
                        list.add(null);
                    }
                    list.set(index, item);
                }
            } else {
                // Type simple
                String paramName = prefix + "[" + index + "]";
                String value = request.getParameter(paramName);
                if (value != null) {
                    while (list.size() <= index) {
                        list.add(null);
                    }
                    list.set(index, convertValue(value, itemType));
                }
            }
        }
        
        return list;
    }

    // Crée un tableau à partir des valeurs d'un paramètre (pour les checkboxes multiples)
    private Object createArrayFromRequest(HttpServletRequest request, Class<?> arrayType, String paramName) {
        String[] values = request.getParameterValues(paramName);
        if (values == null || values.length == 0) {
            return null;
        }
        
        Class<?> componentType = arrayType.getComponentType();
        
        if (componentType == String.class) {
            return values;
        } else if (componentType == int.class) {
            int[] array = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Integer.parseInt(values[i]);
            }
            return array;
        } else if (componentType == Integer.class) {
            Integer[] array = new Integer[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Integer.parseInt(values[i]);
            }
            return array;
        } else if (componentType == boolean.class) {
            boolean[] array = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Boolean.parseBoolean(values[i]);
            }
            return array;
        } else if (componentType == Boolean.class) {
            Boolean[] array = new Boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Boolean.parseBoolean(values[i]);
            }
            return array;
        }
        
        return null;
    }

    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(value);

        try {
            ModelMapper mm = new ModelMapper();
            return mm.map(value, targetType);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
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
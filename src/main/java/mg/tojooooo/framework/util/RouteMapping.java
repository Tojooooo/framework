package mg.tojooooo.framework.util;

import java.lang.reflect.Method;

import mg.tojooooo.framework.annotation.Route;

public class RouteMapping {
    private final Class<?> controllerClass;
    private final Method method;
    private final String url;
    
    public RouteMapping(Class<?> controllerClass, Method method, String url) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.url = url;
    }
    
    public RouteMapping(Class<?> controllerClass, Method method, Route routeAnnotation) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.url = routeAnnotation != null ? routeAnnotation.value() : "";
    }
    
    public Class<?> getControllerClass() { return controllerClass; }
    public Method getMethod() { return method; }
    public String getUrl() { return url; }
    
    @Override
    public String toString() {
        return controllerClass.getSimpleName() + "." + method.getName() + "() -> " + url;
    }
}
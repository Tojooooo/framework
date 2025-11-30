package mg.tojooooo.framework.util;

import java.lang.reflect.Method;
import java.util.List;

import mg.tojooooo.framework.annotation.Route;

public class RouteMapping {
    private final Class<?> controllerClass;
    private final List<UrlMappedMethod> urlMappedMethods;
    private final String url;
    
    public RouteMapping(Class<?> controllerClass, List<UrlMappedMethod> urlMappedMethods, String url) {
        this.controllerClass = controllerClass;
        this.urlMappedMethods = urlMappedMethods;
        this.url = url;
    }
    
    public RouteMapping(Class<?> controllerClass, List<UrlMappedMethod> urlMappedMethods, Route routeAnnotation) {
        this.controllerClass = controllerClass;
        this.urlMappedMethods = urlMappedMethods;
        this.url = routeAnnotation != null ? routeAnnotation.value() : "";
    }
    
    public Class<?> getControllerClass() { return controllerClass; }
    public List<UrlMappedMethod> getUrlMappedMethods() { return urlMappedMethods; }
    public String getUrl() { return url; }
    
    @Override
    public String toString() {
        String returnString = controllerClass.getSimpleName() +"\n";
        for (UrlMappedMethod urlMappedMethod : urlMappedMethods) {
            returnString += "\t"+ urlMappedMethod.getMethod().getName() + " [" + urlMappedMethod.getRequestMethod() + "]";
        }
        returnString += "\t() -> "+ url;
        return returnString;
    }
}
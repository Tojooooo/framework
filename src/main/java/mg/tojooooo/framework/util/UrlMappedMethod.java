package mg.tojooooo.framework.util;

import java.lang.reflect.Method;

public class UrlMappedMethod {
    private Method method;
    private String requestMethod;

    public UrlMappedMethod() {}
    public UrlMappedMethod(Method method, String requestMethod) {
        setMethod(method);
        setRequestMethod(requestMethod);
    }

    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
}

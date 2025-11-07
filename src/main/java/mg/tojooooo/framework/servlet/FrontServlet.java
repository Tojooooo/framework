package mg.tojooooo.framework.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.tojooooo.framework.RouterEngine;
import mg.tojooooo.framework.util.RouteMapping;
import jakarta.servlet.RequestDispatcher;
import java.io.PrintWriter;

public class FrontServlet extends HttpServlet {

    private RequestDispatcher defaultDispatcher;
    private RouterEngine routerEngine;

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        
        ServletContext context = getServletContext();
        routerEngine = (RouterEngine) context.getAttribute("routerEngine");
        
        // fallback routerEngine
        if (routerEngine == null) {
            routerEngine = new RouterEngine();
            try {
                routerEngine.loadClasspathControllers();
                routerEngine.loadControllerUrlMappings();
            } catch (Exception e) {
                System.out.println("Errour loading controllers and urls : "+ e.getMessage());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        handleRequest(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        handleRequest(request, response);
    }
    
    private void handleRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String url = request.getRequestURI().substring(request.getContextPath().length());
        boolean ressourceExists = getServletContext().getResource(url) != null;
        
        if (ressourceExists) {
            defaultDispatcher.forward(request, response);
        } else {
            PrintWriter out = response.getWriter();
            RouteMapping routeMapping = routerEngine.findRouteMapping(url);
            printUrl(out, url);
            printRouteMap(out, routeMapping);
        }
    }

    private void printUrl(PrintWriter out, String url) {
        out.println("URL demandee : " + url);
    }

    private void printRouteMap(PrintWriter out, RouteMapping routeMap) {
        if (routeMap == null) out.println("Route introuvable");
        else out.println(routeMap.toString());
    }
    
}
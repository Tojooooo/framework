package mg.tojooooo.framework.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.tojooooo.framework.RouterEngine;
import mg.tojooooo.framework.util.ModelView;
import mg.tojooooo.framework.util.RouteMapping;
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
            processUrlReturnValue(request, response, url);
        }
    }

    private void processUrlReturnValue(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        PrintWriter out = response.getWriter();
        RouteMapping routeMapping = routerEngine.findRouteMapping(url);
        try {
            Object returnValue = routerEngine.getUrlReturnValue(url);
            if (returnValue == null) {
                printUrl(out, url);
            } else if (returnValue instanceof String) {
                out.println(returnValue);
            } else if (returnValue instanceof ModelView) {
                RequestDispatcher disp = request.getRequestDispatcher(((ModelView)returnValue).getView());
                disp.forward(request, response);
            }
        } catch (Exception e) {
            printError(out, e.getMessage());
        }
    }

    private void printUrl(PrintWriter out, String url) {
        out.println("URL demandee : " + url);
    }

    private void printRouteMap(PrintWriter out, RouteMapping routeMap) {
        if (routeMap == null) out.println("Route introuvable");
        else out.println(routeMap.toString());
    }

    private void printError(PrintWriter out, String errorMessage) {
        out.println("error : "+ errorMessage);
    }
    
}
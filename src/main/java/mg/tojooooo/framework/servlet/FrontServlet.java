package mg.tojooooo.framework.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.RequestDispatcher;
import java.io.PrintWriter;

public class FrontServlet extends HttpServlet {

    private RequestDispatcher defaultDispatcher;

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
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
            printUrl(response.getWriter(), url);
        }
    }

    private void printUrl(PrintWriter out, String url) {
        out.println("URL demandee : " + url);
    }
    
}
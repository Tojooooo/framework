package mg.tojooooo.framework;

import mg.tojooooo.framework.annotation.Route;

public class Router {
    
    // Routes de test
    @Route("/")
    public void home() {
        System.out.println("URL : /");
    }
    
    @Route("/users")
    public void users() {
        System.out.println("URL : /users");
    }
    
    @Route("/products")
    public void products() {
        System.out.println("URL : /products");
    }
    
    @Route("/contact")
    public void contact() {
        System.out.println("URL : /contact");
    }
}
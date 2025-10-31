package mg.tojooooo.framework.util;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.lang.reflect.Modifier;

import mg.tojooooo.framework.annotation.FrameworkController;

public class JavaControllerScanner {
    
    public static Set<Class<?>> findControllers() throws Exception {
        Set<Class<?>> controllers = new HashSet<>();
        
        // ClassLoader an'ny classpath
        Enumeration<URL> roots = Thread.currentThread()
            .getContextClassLoader()
            .getResources("");
        
        while (roots.hasMoreElements()) {
            URL root = roots.nextElement();
            if (root.getProtocol().equals("file")) {
                scanDirectory(new File(root.getFile()), "", controllers);
            }
        }
        
        return controllers;
    }
    
    public static Set<Class<?>> findControllers(String... basePackages) throws Exception {
        Set<Class<?>> controllers = new HashSet<>();
        
        for (String basePackage : basePackages) {
            String path = basePackage.replace('.', '/');
            Enumeration<URL> resources = Thread.currentThread()
                .getContextClassLoader()
                .getResources(path);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    scanDirectory(new File(resource.getFile()), basePackage, controllers);
                }
            }
        }
        
        return controllers;
    }
    
    private static void scanDirectory(File directory, String packageName, Set<Class<?>> controllers) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                String newPackage = packageName.isEmpty() ? 
                    file.getName() : packageName + "." + file.getName();
                scanDirectory(file, newPackage, controllers);
            } else if (file.getName().endsWith(".java")) {
                processJavaFile(file, packageName, controllers);
            } else if (file.getName().endsWith(".class")) {
                processClassFile(file, packageName, controllers);
            }
        }
    }
    
    private static void processJavaFile(File javaFile, String packageName, Set<Class<?>> controllers) {
        try {
            String className = javaFile.getName().replace(".java", "");
            String fullClassName = packageName + "." + className;
            
            checkIfClassIsController(fullClassName, controllers);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du fichier " + javaFile + ": " + e.getMessage());
        }
    }
    
    private static void processClassFile(File classFile, String packageName, Set<Class<?>> controllers) {
        try {
            String className = classFile.getName().replace(".class", "");
            String fullClassName = packageName + "." + className;
            
            checkIfClassIsController(fullClassName, controllers);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de la classe " + classFile + ": " + e.getMessage());
        }
    }
    
    private static void checkIfClassIsController(String fullClassName, Set<Class<?>> controllers) {
        try {
            String cleanClassName = fullClassName.contains("$") ? 
                fullClassName.substring(0, fullClassName.indexOf('$')) : fullClassName;
            
            Class<?> clazz = Class.forName(cleanClassName);
            
            if (isControllerClass(clazz)) {
                controllers.add(clazz);
            }
            
        } catch (ClassNotFoundException e) {
            System.out.println("Classe non compilée (ignorée): " + fullClassName);
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            System.err.println("Classe non chargable: " + fullClassName + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur avec la classe " + fullClassName + ": " + e.getMessage());
        }
    }
    
    private static boolean isControllerClass(Class<?> clazz) {
        if (clazz.isAnnotationPresent(FrameworkController.class)) {
            return true;
        }
        
        return Arrays.stream(clazz.getAnnotations())
                .anyMatch(annotation -> 
                    annotation.annotationType().getSimpleName().equals("FrameworkController"));
    }
    
    public static void printControllers(Set<Class<?>> controllers) {
        if (controllers.isEmpty()) {
            System.out.println("Aucun controller trouvé.");
            return;
        }
        
        System.out.println("\n=== CONTROLLERS TROUVÉS (" + controllers.size() + ") ===");
        controllers.forEach(controller -> {
            System.out.println("- " + controller.getName());
            
            System.out.println("  Méthodes publiques:");
            Arrays.stream(controller.getDeclaredMethods())
                  .filter(method -> Modifier.isPublic(method.getModifiers()))
                  .forEach(method -> System.out.println("  - " + method.getName() + "()"));
        });
    }
}
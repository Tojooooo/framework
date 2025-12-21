package mg.tojooooo.framework.util;

import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class UploadedFile {
    private String originalFilename;
    private String contentType;
    private long size;
    private byte[] content;
    
    public UploadedFile(Part part) throws IOException {
        this.originalFilename = part.getSubmittedFileName();
        this.contentType = part.getContentType();
        this.size = part.getSize();
        this.content = part.getInputStream().readAllBytes();
    }
    
    // Getters
    public String getOriginalFilename() { 
        return originalFilename; 
    }
    
    public String getContentType() { 
        return contentType; 
    }
    
    public long getSize() { 
        return size; 
    }
    
    public String getExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }
    
    public byte[] getBytes() {
        return content;
    }
    
    /**
     * Sauvegarde le fichier à l'emplacement spécifié
     * @param path Chemin complet du fichier (avec nom et extension)
     */
    public void saveTo(String path) throws IOException {
        Path filePath = Paths.get(path);
        // Créer les répertoires parents si nécessaire
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath, content);
    }
    
    /**
     * Sauvegarde le fichier dans un répertoire avec son nom original
     * @param directory Répertoire de destination
     */
    public void saveToDirectory(String directory) throws IOException {
        Path dirPath = Paths.get(directory);
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve(originalFilename);
        Files.write(filePath, content);
    }
    
    @Override
    public String toString() {
        return "UploadedFile{" +
                "originalFilename='" + originalFilename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                '}';
    }
}
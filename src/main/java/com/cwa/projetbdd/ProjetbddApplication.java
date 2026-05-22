package com.cwa.projetbdd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree de l'application.
 * Lance le serveur Spring Boot embarque (Tomcat) sur le port 8080.
 *
 * Acces :
 *   - Frontend : http://localhost:8080/
 *   - API REST : http://localhost:8080/api/...
 */
@SpringBootApplication
public class ProjetbddApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjetbddApplication.class, args);
    }
}

package com.userrolemgmt.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            // Cargar driver H2
            Class.forName("org.h2.Driver");
            LOGGER.log(Level.INFO, "Driver H2 cargado correctamente");
            
            // URL para H2 en memoria
            String url = "jdbc:h2:mem:userrolemgmt;DB_CLOSE_DELAY=-1";
            String user = "sa";
            String password = "";
            
            LOGGER.log(Level.INFO, "Conectando a H2 en memoria con URL: " + url);
            connection = DriverManager.getConnection(url, user, password);
            LOGGER.log(Level.INFO, "Conexión a H2 establecida correctamente");
            
            initializeDatabase();
            
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver H2 no encontrado", e);
            throw new RuntimeException("Driver H2 no encontrado", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al conectar a la base de datos H2", e);
            throw new RuntimeException("Error de conexión a H2", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error general", e);
            throw new RuntimeException("Error general: " + e.getMessage(), e);
        }
    }
    
    private void initializeDatabase() {
        try {
            LOGGER.log(Level.INFO, "Inicializando esquema de base de datos");
            
            try (java.sql.Statement stmt = connection.createStatement()) {
                // Crear tabla de usuarios
                stmt.execute("CREATE TABLE users (" +
                        "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "username VARCHAR(100) NOT NULL, " +
                        "email VARCHAR(255) NOT NULL, " +
                        "password_hash VARCHAR(255) NOT NULL, " +
                        "first_name VARCHAR(100), " +
                        "last_name VARCHAR(100), " +
                        "active BOOLEAN DEFAULT TRUE, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                
                // Crear tabla de roles
                stmt.execute("CREATE TABLE roles (" +
                        "role_id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "role_name VARCHAR(100) NOT NULL, " +
                        "description VARCHAR(255), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                
                // Crear tabla de relación usuario-rol
                stmt.execute("CREATE TABLE user_roles (" +
                        "user_id INT NOT NULL, " +
                        "role_id INT NOT NULL, " +
                        "assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (user_id, role_id), " +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id), " +
                        "FOREIGN KEY (role_id) REFERENCES roles(role_id))");
                
                // Insertar datos de ejemplo
                LOGGER.log(Level.INFO, "Insertando datos de ejemplo");
                
                // Usuarios de ejemplo
                stmt.execute("INSERT INTO users (username, email, password_hash, first_name, last_name) " +
                        "VALUES ('admin', 'admin@example.com', 'hashed_password', 'Admin', 'User')");
                
                stmt.execute("INSERT INTO users (username, email, password_hash, first_name, last_name) " +
                        "VALUES ('user1', 'user1@example.com', 'hashed_password', 'Regular', 'User')");
                
                stmt.execute("INSERT INTO users (username, email, password_hash, first_name, last_name) " +
                        "VALUES ('manager', 'manager@example.com', 'hashed_password', 'Manager', 'User')");
                
                // Roles de ejemplo
                stmt.execute("INSERT INTO roles (role_name, description) " +
                        "VALUES ('ADMIN', 'Administrator role with full access')");
                
                stmt.execute("INSERT INTO roles (role_name, description) " +
                        "VALUES ('USER', 'Regular user with limited access')");
                
                stmt.execute("INSERT INTO roles (role_name, description) " +
                        "VALUES ('MANAGER', 'Manager with department access')");
                
                // Asignaciones usuario-rol
                stmt.execute("INSERT INTO user_roles (user_id, role_id) VALUES (1, 1)");
                stmt.execute("INSERT INTO user_roles (user_id, role_id) VALUES (2, 2)");
                stmt.execute("INSERT INTO user_roles (user_id, role_id) VALUES (3, 3)");
            }
            
            LOGGER.log(Level.INFO, "Base de datos inicializada correctamente");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar la base de datos", e);
            throw new RuntimeException("Error al inicializar la base de datos", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                instance = new DatabaseConnection();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al verificar el estado de la conexión", e);
            throw new RuntimeException("Error al verificar la conexión", e);
        }
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.log(Level.INFO, "Conexión cerrada exitosamente");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error al cerrar la conexión", e);
            }
        }
    }

    
}
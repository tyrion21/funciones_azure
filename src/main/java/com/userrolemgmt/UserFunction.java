package com.userrolemgmt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.userrolemgmt.dao.UserDAO;
import com.userrolemgmt.model.User;
import com.userrolemgmt.util.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Función Azure para gestionar operaciones CRUD de usuarios
 */
public class UserFunction {
    private static final Logger LOGGER = Logger.getLogger(UserFunction.class.getName());
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Obtener todos los usuarios
     */
    @FunctionName("getAllUsers")
    public HttpResponseMessage getAllUsers(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.GET}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "users") 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para obtener todos los usuarios");
        
        try {
            List<User> users = userDAO.getAllUsers();
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(users))
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuarios", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener usuarios: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Obtener un usuario por ID
     */
    @FunctionName("getUserById")
    public HttpResponseMessage getUserById(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.GET}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "users/{userId}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("userId") String userIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para obtener usuario con ID: " + userIdStr);
        
        try {
            long userId = Long.parseLong(userIdStr);
            User user = userDAO.getUserById(userId);
            
            if (user != null) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(gson.toJson(user))
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Usuario no encontrado con ID: " + userId)
                        .build();
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de usuario inválido: " + userIdStr, e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de usuario inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuario con ID: " + userIdStr, e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener usuario: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Crear un nuevo usuario
     */
    @FunctionName("createUser")
    public HttpResponseMessage createUser(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.POST}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "users") 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para crear un nuevo usuario");
        
        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporcione datos de usuario en el cuerpo de la solicitud")
                    .build();
        }
        
        try {
            User user = gson.fromJson(requestBody, User.class);
            User createdUser = userDAO.createUser(user);
            
            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(createdUser))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear usuario", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear usuario: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Actualizar un usuario existente
     */
    @FunctionName("updateUser")
    public HttpResponseMessage updateUser(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.PUT}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "users/{userId}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("userId") String userIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para actualizar usuario con ID: " + userIdStr);
        
        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporcione datos de usuario en el cuerpo de la solicitud")
                    .build();
        }
        
        try {
            long userId = Long.parseLong(userIdStr);
            User user = gson.fromJson(requestBody, User.class);
            user.setUserId(userId);
            
            // Verificar que el usuario existe
            User existingUser = userDAO.getUserById(userId);
            if (existingUser == null) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Usuario no encontrado con ID: " + userId)
                        .build();
            }
            
            boolean updated = userDAO.updateUser(user);
            
            if (updated) {
                // Obtener el usuario actualizado para devolverlo
                User updatedUser = userDAO.getUserById(userId);
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(gson.toJson(updatedUser))
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo actualizar el usuario")
                        .build();
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de usuario inválido: " + userIdStr, e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de usuario inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar usuario con ID: " + userIdStr, e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar usuario: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Eliminar un usuario
     */
    @FunctionName("deleteUser")
    public HttpResponseMessage deleteUser(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.DELETE}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "users/{userId}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("userId") String userIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para eliminar usuario con ID: " + userIdStr);
        
        try {
            long userId = Long.parseLong(userIdStr);
            
            // Verificar que el usuario existe
            User existingUser = userDAO.getUserById(userId);
            if (existingUser == null) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Usuario no encontrado con ID: " + userId)
                        .build();
            }
            
            boolean deleted = userDAO.deleteUser(userId);
            
            if (deleted) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .body("Usuario eliminado correctamente")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo eliminar el usuario")
                        .build();
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de usuario inválido: " + userIdStr, e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de usuario inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar usuario con ID: " + userIdStr, e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar usuario: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Asignar un rol a un usuario
     */
    @FunctionName("assignRoleToUser")
    public HttpResponseMessage assignRoleToUser(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.POST}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "users/{userId}/roles/{roleId}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("userId") String userIdStr,
            @BindingName("roleId") String roleIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para asignar rol " + roleIdStr + " a usuario " + userIdStr);
        
        try {
            long userId = Long.parseLong(userIdStr);
            long roleId = Long.parseLong(roleIdStr);
            
            boolean assigned = userDAO.assignRoleToUser(userId, roleId);
            
            if (assigned) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .body("Rol asignado correctamente al usuario")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo asignar el rol al usuario")
                        .build();
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de usuario o rol inválido", e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de usuario o rol inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al asignar rol a usuario", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al asignar rol a usuario: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Eliminar un rol de un usuario
     */
    @FunctionName("removeRoleFromUser")
    public HttpResponseMessage removeRoleFromUser(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.DELETE}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "users/{userId}/roles/{roleId}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("userId") String userIdStr,
            @BindingName("roleId") String roleIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para eliminar rol " + roleIdStr + " de usuario " + userIdStr);
        
        try {
            long userId = Long.parseLong(userIdStr);
            long roleId = Long.parseLong(roleIdStr);
            
            boolean removed = userDAO.removeRoleFromUser(userId, roleId);
            
            if (removed) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .body("Rol eliminado correctamente del usuario")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("No se encontró asignación de rol para eliminar")
                        .build();
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de usuario o rol inválido", e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de usuario o rol inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar rol de usuario", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar rol de usuario: " + e.getMessage())
                    .build();
        }
    }
}
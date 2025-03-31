package com.usuarioroles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.usuarioroles.dao.RoleDAO;
import com.usuarioroles.model.Role;
import com.usuarioroles.model.User;
import com.usuarioroles.util.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Función Azure para gestionar operaciones CRUD de roles
 */
public class RoleFunction {
    private static final Logger LOGGER = Logger.getLogger(RoleFunction.class.getName());
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    private final RoleDAO roleDAO = new RoleDAO();

    /**
     * Obtener todos los roles
     */
    @FunctionName("getAllRoles")
    public HttpResponseMessage getAllRoles(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.GET}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "roles") 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para obtener todos los roles");
        
        try {
            List<Role> roles = roleDAO.getAllRoles();
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(roles))
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener roles", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener roles: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Obtener un rol por ID
     */
    @FunctionName("getRoleById")
    public HttpResponseMessage getRoleById(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.GET}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "roles/{roleId}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("roleId") String roleIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para obtener rol con ID: " + roleIdStr);
        
        try {
            long roleId = Long.parseLong(roleIdStr);
            Role role = roleDAO.getRoleById(roleId);
            
            if (role != null) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(gson.toJson(role))
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Rol no encontrado con ID: " + roleId)
                        .build();
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de rol inválido: " + roleIdStr, e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de rol inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener rol con ID: " + roleIdStr, e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener rol: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Crear un nuevo rol
     */
    @FunctionName("createRole")
    public HttpResponseMessage createRole(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.POST}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "roles") 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para crear un nuevo rol");
        
        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporcione datos del rol en el cuerpo de la solicitud")
                    .build();
        }
        
        try {
            Role role = gson.fromJson(requestBody, Role.class);
            Role createdRole = roleDAO.createRole(role);
            
            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(createdRole))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear rol", e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear rol: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Actualizar un rol existente
     */
    @FunctionName("updateRole")
    public HttpResponseMessage updateRole(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.PUT}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "roles/{roleId}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("roleId") String roleIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para actualizar rol con ID: " + roleIdStr);
        
        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporcione datos del rol en el cuerpo de la solicitud")
                    .build();
        }
        
        try {
            long roleId = Long.parseLong(roleIdStr);
            Role role = gson.fromJson(requestBody, Role.class);
            role.setRoleId(roleId);
            
            // Verificar que el rol existe
            Role existingRole = roleDAO.getRoleById(roleId);
            if (existingRole == null) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Rol no encontrado con ID: " + roleId)
                        .build();
            }
            
            boolean updated = roleDAO.updateRole(role);
            
            if (updated) {
                // Obtener el rol actualizado para devolverlo
                Role updatedRole = roleDAO.getRoleById(roleId);
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(gson.toJson(updatedRole))
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo actualizar el rol")
                        .build();
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de rol inválido: " + roleIdStr, e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de rol inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar rol con ID: " + roleIdStr, e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar rol: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Eliminar un rol
     */
    @FunctionName("deleteRole")
    public HttpResponseMessage deleteRole(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.DELETE}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "roles/{roleId}") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("roleId") String roleIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para eliminar rol con ID: " + roleIdStr);
        
        try {
            long roleId = Long.parseLong(roleIdStr);
            
            // Verificar que el rol existe
            Role existingRole = roleDAO.getRoleById(roleId);
            if (existingRole == null) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Rol no encontrado con ID: " + roleId)
                        .build();
            }
            
            boolean deleted = roleDAO.deleteRole(roleId);
            
            if (deleted) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .body("Rol eliminado correctamente")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo eliminar el rol")
                        .build();
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de rol inválido: " + roleIdStr, e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de rol inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar rol con ID: " + roleIdStr, e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar rol: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Obtener todos los usuarios asignados a un rol
     */
    @FunctionName("getUsersByRoleId")
    public HttpResponseMessage getUsersByRoleId(
            @HttpTrigger(name = "req", 
                        methods = {HttpMethod.GET}, 
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "roles/{roleId}/users") 
            HttpRequestMessage<Optional<String>> request,
            @BindingName("roleId") String roleIdStr,
            final ExecutionContext context) {
        
        context.getLogger().info("Solicitud recibida para obtener usuarios con rol ID: " + roleIdStr);
        
        try {
            long roleId = Long.parseLong(roleIdStr);
            
            // Verificar que el rol existe
            Role existingRole = roleDAO.getRoleById(roleId);
            if (existingRole == null) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Rol no encontrado con ID: " + roleId)
                        .build();
            }
            
            List<User> users = roleDAO.getUsersByRoleId(roleId);
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(users))
                    .build();
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de rol inválido: " + roleIdStr, e);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de rol inválido")
                    .build();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuarios para rol ID: " + roleIdStr, e);
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener usuarios para rol: " + e.getMessage())
                    .build();
        }
    }
}
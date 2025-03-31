package com.usuarioroles.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.usuarioroles.model.Role;
import com.usuarioroles.model.User;
import com.usuarioroles.util.DatabaseConnection;

public class RoleDAO {
    private static final Logger LOGGER = Logger.getLogger(RoleDAO.class.getName());
    
    // Consultas SQL como constantes
    private static final String SQL_GET_ALL_ROLES = "SELECT * FROM roles ORDER BY role_id";
    private static final String SQL_GET_ROLE_BY_ID = "SELECT * FROM roles WHERE role_id = ?";
    private static final String SQL_GET_ROLE_BY_NAME = "SELECT * FROM roles WHERE role_name = ?";
    private static final String SQL_CREATE_ROLE = "INSERT INTO roles (role_name, description) VALUES (?, ?)";
    private static final String SQL_UPDATE_ROLE = "UPDATE roles SET role_name = ?, description = ? WHERE role_id = ?";
    private static final String SQL_DELETE_USER_ROLES = "DELETE FROM user_roles WHERE role_id = ?";
    private static final String SQL_DELETE_ROLE = "DELETE FROM roles WHERE role_id = ?";
    private static final String SQL_GET_USERS_BY_ROLE = "SELECT u.* FROM users u JOIN user_roles ur ON u.user_id = ur.user_id WHERE ur.role_id = ?";
    
    public List<Role> getAllRoles() throws SQLException {
        List<Role> roles = new ArrayList<>();
        
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_GET_ALL_ROLES)) {
            
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todos los roles", e);
            throw e;
        }
        
        return roles;
    }

    public Role getRoleById(long roleId) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SQL_GET_ROLE_BY_ID)) {
            pstmt.setLong(1, roleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRole(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener rol por ID: " + roleId, e);
            throw e;
        }
        
        return null;
    }

    public Role getRoleByName(String roleName) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SQL_GET_ROLE_BY_NAME)) {
            pstmt.setString(1, roleName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRole(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener rol por nombre: " + roleName, e);
            throw e;
        }
        
        return null;
    }

    public Role createRole(Role role) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SQL_CREATE_ROLE, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, role.getRoleName());
            pstmt.setString(2, role.getDescription());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        long roleId = rs.getLong(1);
                        role.setRoleId(roleId);
                        
                        // Recuperar el rol completo con timestamps
                        Role completeRole = getRoleById(roleId);
                        if (completeRole != null) {
                            role.setCreatedAt(completeRole.getCreatedAt());
                            role.setUpdatedAt(completeRole.getUpdatedAt());
                        }
                        
                        return role;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear un rol", e);
            throw e;
        }
        
        return null;
    }

    public boolean updateRole(Role role) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SQL_UPDATE_ROLE)) {
            
            pstmt.setString(1, role.getRoleName());
            pstmt.setString(2, role.getDescription());
            pstmt.setLong(3, role.getRoleId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar rol ID: " + role.getRoleId(), e);
            throw e;
        }
    }

    public boolean deleteRole(long roleId) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            
            try {
                // Eliminar referencias en user_roles
                try (PreparedStatement pstmt = connection.prepareStatement(SQL_DELETE_USER_ROLES)) {
                    pstmt.setLong(1, roleId);
                    pstmt.executeUpdate();
                }
                
                // Eliminar el rol
                try (PreparedStatement pstmt = connection.prepareStatement(SQL_DELETE_ROLE)) {
                    pstmt.setLong(1, roleId);
                    int rowsAffected = pstmt.executeUpdate();
                    
                    connection.commit();
                    return rowsAffected > 0;
                }
            } catch (SQLException e) {
                connection.rollback();
                LOGGER.log(Level.SEVERE, "Error al eliminar rol ID: " + roleId, e);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<User> getUsersByRoleId(long roleId) throws SQLException {
        List<User> users = new ArrayList<>();
        
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SQL_GET_USERS_BY_ROLE)) {
            
            pstmt.setLong(1, roleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuarios para rol ID: " + roleId, e);
            throw e;
        }
        
        return users;
    }

    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        return new Role(
                rs.getLong("role_id"),
                rs.getString("role_name"),
                rs.getString("description"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at"));
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getInt("active") == 1,
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at"));
    }
}

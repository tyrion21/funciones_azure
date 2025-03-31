package com.usuarioroles.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.usuarioroles.model.Role;
import com.usuarioroles.model.User;
import com.usuarioroles.util.DatabaseConnection;

public class UserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());
    private final Connection connection;

    public UserDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // Obtener todos los usuarios
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users ORDER BY user_id";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                // Cargar roles para este usuario
                user.setRoles(getUserRoles(user.getUserId()));
                users.add(user);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todos los usuarios", e);
            throw e;
        }

        return users;
    }

    // Obtener un usuario por ID
    public User getUserById(long userId) throws SQLException {
        String query = "SELECT * FROM users WHERE user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    // Cargar roles para este usuario
                    user.setRoles(getUserRoles(userId));
                    return user;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuario por ID: " + userId, e);
            throw e;
        }

        return null;
    }

    // Obtener un usuario por nombre de usuario
    public User getUserByUsername(String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    // Cargar roles para este usuario
                    user.setRoles(getUserRoles(user.getUserId()));
                    return user;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener usuario por username: " + username, e);
            throw e;
        }

        return null;
    }

    // Crear un nuevo usuario
    public User createUser(User user) throws SQLException {
        // Intenta el enfoque de Oracle primero
        try {
            String oracleQuery = "INSERT INTO users (username, email, password_hash, first_name, last_name, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING user_id, created_at, updated_at";

            try (PreparedStatement pstmt = connection.prepareStatement(oracleQuery)) {
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, user.getPasswordHash());
                pstmt.setString(4, user.getFirstName());
                pstmt.setString(5, user.getLastName());
                pstmt.setInt(6, user.isActive() ? 1 : 0);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        user.setUserId(rs.getLong("user_id"));
                        user.setCreatedAt(rs.getTimestamp("created_at"));
                        user.setUpdatedAt(rs.getTimestamp("updated_at"));

                        // Si hay roles asignados, guardarlos
                        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                            for (Role role : user.getRoles()) {
                                assignRoleToUser(user.getUserId(), role.getRoleId());
                            }
                            // Recargar roles
                            user.setRoles(getUserRoles(user.getUserId()));
                        }

                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            // Si falla con la sintaxis de Oracle, intenta con H2
            LOGGER.log(Level.INFO, "Usando enfoque H2 para crear usuario después de error Oracle: " + e.getMessage());

            String h2Query = "INSERT INTO users (username, email, password_hash, first_name, last_name, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(h2Query, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, user.getPasswordHash());
                pstmt.setString(4, user.getFirstName());
                pstmt.setString(5, user.getLastName());
                pstmt.setInt(6, user.isActive() ? 1 : 0);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            long userId = rs.getLong(1);
                            user.setUserId(userId);

                            // Obtener datos adicionales con una consulta separada
                            try (PreparedStatement stmt = connection
                                    .prepareStatement("SELECT created_at, updated_at FROM users WHERE user_id = ?")) {
                                stmt.setLong(1, userId);
                                try (ResultSet timeRs = stmt.executeQuery()) {
                                    if (timeRs.next()) {
                                        user.setCreatedAt(timeRs.getTimestamp("created_at"));
                                        user.setUpdatedAt(timeRs.getTimestamp("updated_at"));
                                    }
                                }
                            }

                            // Si hay roles asignados, guardarlos
                            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                                for (Role role : user.getRoles()) {
                                    assignRoleToUser(user.getUserId(), role.getRoleId());
                                }
                                // Recargar roles
                                user.setRoles(getUserRoles(user.getUserId()));
                            }

                            return user;
                        }
                    }
                }
            }
        }

        return null;
    }

    // Actualizar un usuario existente
    public boolean updateUser(User user) throws SQLException {
        String query = "UPDATE users SET username = ?, email = ?, password_hash = ?, " +
                "first_name = ?, last_name = ?, active = ? " +
                "WHERE user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getFirstName());
            pstmt.setString(5, user.getLastName());
            pstmt.setInt(6, user.isActive() ? 1 : 0);
            pstmt.setLong(7, user.getUserId());

            int rowsAffected = pstmt.executeUpdate();

            // Actualizar roles si es necesario
            if (rowsAffected > 0 && user.getRoles() != null) {
                // Eliminar todos los roles actuales
                removeAllRolesFromUser(user.getUserId());

                // Asignar nuevos roles
                for (Role role : user.getRoles()) {
                    assignRoleToUser(user.getUserId(), role.getRoleId());
                }
            }

            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al actualizar usuario ID: " + user.getUserId(), e);
            throw e;
        }
    }

    // Eliminar un usuario
    public boolean deleteUser(long userId) throws SQLException {
        // Primero eliminar relaciones en user_roles
        removeAllRolesFromUser(userId);

        // Luego eliminar el usuario
        String query = "DELETE FROM users WHERE user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setLong(1, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar usuario ID: " + userId, e);
            throw e;
        }
    }

    // Asignar un rol a un usuario
    public boolean assignRoleToUser(long userId, long roleId) throws SQLException {
        String query = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, roleId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al asignar rol ID: " + roleId + " a usuario ID: " + userId, e);
            throw e;
        }
    }

    // Eliminar un rol de un usuario
    public boolean removeRoleFromUser(long userId, long roleId) throws SQLException {
        String query = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, roleId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar rol ID: " + roleId + " de usuario ID: " + userId, e);
            throw e;
        }
    }

    // Eliminar todos los roles de un usuario
    private boolean removeAllRolesFromUser(long userId) throws SQLException {
        String query = "DELETE FROM user_roles WHERE user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setLong(1, userId);

            int rowsAffected = pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar todos los roles del usuario ID: " + userId, e);
            throw e;
        }
    }

    // Obtener roles de un usuario
    private List<Role> getUserRoles(long userId) throws SQLException {
        List<Role> roles = new ArrayList<>();
        String query = "SELECT r.* FROM roles r " +
                "JOIN user_roles ur ON r.role_id = ur.role_id " +
                "WHERE ur.user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Role role = new Role(
                            rs.getLong("role_id"),
                            rs.getString("role_name"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at"),
                            rs.getTimestamp("updated_at"));
                    roles.add(role);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener roles del usuario ID: " + userId, e);
            throw e;
        }

        return roles;
    }

    // Método auxiliar para mapear ResultSet a objeto User
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
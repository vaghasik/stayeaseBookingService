package com.apexon.stayease.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;

/**
 * User Repository
 * Provides database operations for User entity.
 *
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address
     *
     * @param emailAddress user's email
     * @return Optional containing user if found
     */
    Optional<User> findByEmailAddress(String emailAddress);

    /**
     * Find users by role
     *
     * @param userRole role to search for
     * @return List of users with specified role
     */
    List<User> findByUserRole(UserRole userRole);

    /**
     * Find active users by role
     *
     * @param userRole role to search for
     * @param isActive active status
     * @return List of active users with specified role
     */
    List<User> findByUserRoleAndIsActive(UserRole userRole, Boolean isActive);

    /**
     * Check if email already exists
     *
     * @param emailAddress email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmailAddress(String emailAddress);

    /**
     * Find all active users
     *
     * @param isActive active status
     * @return List of active users
     */
    List<User> findByIsActive(Boolean isActive);

    /**
     * Search users by full name (case-insensitive)
     *
     * @param fullName name to search for
     * @return List of matching users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    List<User> searchByFullName(@Param("fullName") String fullName);
}

package com.training.use_management.repository;

import com.training.use_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Kiểm tra người dùng có vai trof ADMIN hay không
    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.username = :username AND r.name = 'ADMIN'")
    Optional<User> findUserWithAdminRole(@Param("username") String username);

    List<User> findAllByEmailIn(Set<String> emails);

}

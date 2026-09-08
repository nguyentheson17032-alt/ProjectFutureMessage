package com.futuremessage.repository;

import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select u from User u
            where (lower(u.email) like lower(concat('%', coalesce(:q, ''), '%'))
                or lower(u.displayName) like lower(concat('%', coalesce(:q, ''), '%')))
              and (:enabled is null or u.enabled = :enabled)
              and (:role is null or u.role = :role)
            """)
    Page<User> search(
            @Param("q") String q,
            @Param("enabled") Boolean enabled,
            @Param("role") UserRole role,
            Pageable pageable
    );
}

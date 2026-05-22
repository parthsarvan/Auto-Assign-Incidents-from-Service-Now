package com.example.backend.repository;

import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query(
            value = """
                    select *
                    from users
                    where lower(trim(both '"' from trim(username))) = lower(trim(both '"' from trim(:username)))
                    limit 1
                    """,
            nativeQuery = true)
    Optional<User> findByNormalizedUsername(@Param("username") String username);

    @Query(
            value = """
                    select count(*) > 0
                    from users
                    where lower(trim(both '"' from trim(username))) = lower(trim(both '"' from trim(:username)))
                    """,
            nativeQuery = true)
    boolean existsByNormalizedUsername(@Param("username") String username);

    @Query(
            value = """
                    select count(*) > 0
                    from users
                    where lower(trim(work_email)) = lower(trim(:workEmail))
                    """,
            nativeQuery = true)
    boolean existsByNormalizedWorkEmail(@Param("workEmail") String workEmail);

    @Query(
            value = """
                    select *
                    from users
                    where lower(trim(work_email)) = lower(trim(:workEmail))
                    limit 1
                    """,
            nativeQuery = true)
    Optional<User> findByNormalizedWorkEmail(@Param("workEmail") String workEmail);

    long countByRole(String role);
}

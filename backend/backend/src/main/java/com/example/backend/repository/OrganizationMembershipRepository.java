package com.example.backend.repository;

import com.example.backend.entity.Organization;
import com.example.backend.entity.OrganizationMembership;
import com.example.backend.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    boolean existsByUserAndOrganization(User user, Organization organization);
    Optional<OrganizationMembership> findByUserAndOrganization(User user, Organization organization);
    long deleteByUser(User user);

    @Query("select om from OrganizationMembership om join fetch om.organization where om.user = :user")
    List<OrganizationMembership> findAllByUserWithOrganization(@Param("user") User user);
}

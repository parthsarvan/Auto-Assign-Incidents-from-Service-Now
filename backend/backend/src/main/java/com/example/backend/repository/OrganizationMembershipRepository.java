package com.example.backend.repository;

import com.example.backend.entity.Organization;
import com.example.backend.entity.OrganizationMembership;
import com.example.backend.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    boolean existsByUserAndOrganization(User user, Organization organization);
    Optional<OrganizationMembership> findByUserAndOrganization(User user, Organization organization);
}

package com.example.backend.repository;

import com.example.backend.entity.LeaveEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveEntryRepository extends JpaRepository<LeaveEntry, Long> {}

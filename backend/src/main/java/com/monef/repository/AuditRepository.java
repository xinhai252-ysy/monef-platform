package com.monef.repository;

import com.monef.entity.AuditEvent;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface AuditRepository extends JpaRepository<AuditEvent, String> {
    Page<AuditEvent> findByTenantIdOrderByCreatedAtDesc(String tenant, Pageable page);
}

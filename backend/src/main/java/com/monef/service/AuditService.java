package com.monef.service;

import com.monef.dto.AuditView;
import com.monef.dto.PageView;
import com.monef.exception.ApiException;
import com.monef.repository.AuditRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository audits;

    public AuditService(AuditRepository audits) {
        this.audits = audits;
    }

    public PageView<AuditView> list(String tenant, int page) {
        if (page < 0) throw new ApiException(400, "页码不能为负");
        var p = audits.findByTenantIdOrderByCreatedAtDesc(tenant, PageRequest.of(page, 20));
        return new PageView<>(
            p
                .map(a ->
                    new AuditView(
                        a.getId(),
                        a.getActor(),
                        a.getAction(),
                        a.getSubjectId(),
                        a.getDetail(),
                        a.getCreatedAt()
                    )
                )
                .getContent(),
            page,
            20,
            p.getTotalElements(),
            p.getTotalPages()
        );
    }
}

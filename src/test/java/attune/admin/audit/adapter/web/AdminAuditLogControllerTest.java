package attune.admin.audit.adapter.web;

import attune.admin.audit.application.AdminAuditLogService;
import attune.admin.audit.application.dto.AdminAuditLogResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuditLogControllerTest {

    @Test
    void delegatesLookupLimitToService() {
        AdminAuditLogService service = mock(AdminAuditLogService.class);
        AdminAuditLogController controller = new AdminAuditLogController(service);
        when(service.getLatest(10)).thenReturn(List.of());

        List<AdminAuditLogResponse> result = controller.getAuditLogs(10);

        assertThat(result).isEmpty();
        verify(service).getLatest(10);
    }
}

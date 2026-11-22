package com.monef;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monef.dto.ChatRequest;
import com.monef.dto.Dashboard;
import com.monef.dto.Metric;
import com.monef.entity.AppUser;
import com.monef.exception.ApiException;
import com.monef.security.RateLimiter;
import com.monef.service.AiService;
import com.monef.service.DashboardService;
import com.monef.service.FinanceService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

class AiBoundaryTest {

    @SuppressWarnings("unchecked")
    private AiService service(ChatModel model) {
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(model);
        var dashboards = mock(DashboardService.class);
        when(dashboards.month("2026-10")).thenReturn(YearMonth.of(2026, 10));
        var metric = new Metric(BigDecimal.TEN, null);
        var now = Instant.parse("2026-10-26T12:00:00Z");
        when(dashboards.dashboard("tenant-a", "2026-10")).thenReturn(
            new Dashboard(
                "2026-10",
                "USD",
                metric,
                metric,
                metric,
                BigDecimal.ZERO,
                2,
                List.of(),
                List.of(),
                now
            )
        );
        return new AiService(
            provider,
            dashboards,
            mock(FinanceService.class),
            mock(RateLimiter.class),
            new ObjectMapper().findAndRegisterModules(),
            Clock.fixed(now, ZoneOffset.UTC),
            true
        );
    }

    private AppUser user() {
        var user = new AppUser();
        user.setTenantId("tenant-a");
        user.setEmail("test@example.com");
        return user;
    }

    private ChatResponse toolResponse(String name) {
        return new ChatResponse(
            List.of(
                new Generation(
                    AssistantMessage.builder()
                        .content("")
                        .toolCalls(
                            List.of(new AssistantMessage.ToolCall("call-1", "function", name, "{}"))
                        )
                        .build()
                )
            )
        );
    }

    @Test
    void executesSpringAiToolThenReturnsModelAnswer() {
        var model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(
            toolResponse("monthlySummary"),
            new ChatResponse(List.of(new Generation(new AssistantMessage("已核验本月 2 笔交易。"))))
        );
        var result = service(model).chat(user(), new ChatRequest("分析收支", "2026-10"));
        assertThat(result.mode()).isEqualTo("SPRING_AI");
        assertThat(result.transactionCount()).isEqualTo(2);
        assertThat(result.tools()).containsExactly("monthlySummary");
        verify(model, times(2)).call(any(Prompt.class));
    }

    @Test
    void neverAllowsAnUnboundedModelToolLoop() {
        var model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(toolResponse("monthlySummary"));
        assertThatThrownBy(() ->
            service(model).chat(user(), new ChatRequest("继续查询", "2026-10"))
        ).isInstanceOf(ApiException.class);
        verify(model, times(4)).call(any(Prompt.class));
    }

    @Test
    void fabricatedToolNamesCannotExecute() {
        var model = mock(ChatModel.class);
        when(model.call(any(Prompt.class))).thenReturn(toolResponse("executeSql"));
        assertThatThrownBy(() ->
            service(model).chat(user(), new ChatRequest("执行SQL", "2026-10"))
        ).isInstanceOf(ApiException.class);
        verify(model, times(1)).call(any(Prompt.class));
    }
}

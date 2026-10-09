package com.financialcrm.controller;

import com.financialcrm.config.SecurityConfig;
import com.financialcrm.repository.AuditLogRepository;
import com.financialcrm.repository.EmployeeDailyWorkRepository;
import com.financialcrm.repository.EmployeeTargetRepository;
import com.financialcrm.service.AttendanceService;
import com.financialcrm.service.AuditService;
import com.financialcrm.service.EmployeeService;
import com.financialcrm.service.impl.CustomUserDetailsService;
import com.financialcrm.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkTrackingController.class)
@Import(SecurityConfig.class)
class WorkTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeDailyWorkRepository dailyWorkRepo;

    @MockBean
    private EmployeeTargetRepository targetRepo;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private AttendanceService attendanceService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveDailyWork_WithValidationErrors_ShouldRedirectWithErrorMessage() throws Exception {
        mockMvc.perform(post("/work-tracking/save")
                .with(csrf())
                .param("employeeId", "1")
                .param("workDate", "2026-08-07")
                .param("dailyLeads", "-1")
                .param("completionPercentage", "101"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/work-tracking?date=2026-08-07"))
            .andExpect(flash().attributeExists("errorMsg"));
    }
}

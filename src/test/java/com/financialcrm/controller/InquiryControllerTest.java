package com.financialcrm.controller;

import com.financialcrm.config.SecurityConfig;
import com.financialcrm.entity.Inquiry;
import com.financialcrm.repository.InquiryTypeRepository;
import com.financialcrm.service.AuditService;
import com.financialcrm.service.*;
import com.financialcrm.service.impl.CustomUserDetailsService;
import com.financialcrm.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for InquiryController.
 * Uses MockMvc to test HTTP behavior without a full server.
 */
@WebMvcTest(InquiryController.class)
@Import(SecurityConfig.class)
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InquiryService inquiryService;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private InquiryTypeRepository inquiryTypeRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_ShouldReturn200WithInquiriesPage() throws Exception {
        Page<Inquiry> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(inquiryService.searchByStatus(isNull(), isNull(), any(PageRequest.class))).thenReturn(emptyPage);
        when(inquiryService.countTodayInquiries()).thenReturn(0L);

        mockMvc.perform(get("/inquiries"))
            .andExpect(status().isOk())
            .andExpect(view().name("inquiry/list"))
            .andExpect(model().attributeExists("inquiries"))
            .andExpect(model().attributeExists("statuses"))
            .andExpect(model().attribute("totalPages", 0))
            .andExpect(model().attribute("totalElements", 0L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_ShouldReturn200WithEmptyInquiry() throws Exception {
        when(inquiryTypeRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of());
        when(userService.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/inquiries/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("inquiry/form"))
            .andExpect(model().attributeExists("inquiry"))
            .andExpect(model().attributeExists("inquiryTypes"))
            .andExpect(model().attributeExists("statuses"))
            .andExpect(model().attributeExists("priorities"));
    }

    @Test
    void list_UnauthenticatedUser_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/inquiries"))
            .andExpect(status().is3xxRedirection());
    }
}

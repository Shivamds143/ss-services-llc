package com.financialcrm.controller;

import com.financialcrm.config.SecurityConfig;
import com.financialcrm.service.AuditService;
import com.financialcrm.service.CustomerService;
import com.financialcrm.service.InquiryService;
import com.financialcrm.service.impl.CustomUserDetailsService;
import com.financialcrm.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for CustomerController.
 */
@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private InquiryService inquiryService;

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
    void list_ShouldReturn200WithCustomerList() throws Exception {
        when(customerService.search(isNull(), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/customers"))
            .andExpect(status().isOk())
            .andExpect(view().name("customer/list"))
            .andExpect(model().attributeExists("customers"));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void newForm_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/customers/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("customer/form"))
            .andExpect(model().attributeExists("customer"))
            .andExpect(model().attributeExists("today"));
    }

    @Test
    void list_Unauthenticated_ShouldRedirect() throws Exception {
        mockMvc.perform(get("/customers"))
            .andExpect(status().is3xxRedirection());
    }
}

package edu.kuet.classsync.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.kuet.classsync.support.IntegrationTest;
import edu.kuet.classsync.support.TestAuthHelper;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class AdminUserControllerIT {

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelper auth;

    @Test
    void list_asStudent_returns403() throws Exception {
        mvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, auth.bearer(RoleName.STUDENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void list_asAdmin_returnsPage() throws Exception {
        mvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, auth.bearer(RoleName.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void changeRoles_asAdmin_promotesStudentToTeacher() throws Exception {
        User student = auth.createUser(RoleName.STUDENT);

        mvc.perform(put("/api/v1/admin/users/{id}/roles", student.getId())
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(RoleName.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"TEACHER\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"));
    }

    @Test
    void disabledUser_cannotUseToken() throws Exception {
        User student = auth.createUser(RoleName.STUDENT);
        String studentBearer = auth.bearer(student);

        mvc.perform(patch("/api/v1/admin/users/{id}/status", student.getId())
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(RoleName.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, studentBearer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_self_returns400() throws Exception {
        User admin = auth.createUser(RoleName.ADMIN);

        mvc.perform(delete("/api/v1/admin/users/{id}", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_unknownUser_returns404() throws Exception {
        mvc.perform(get("/api/v1/admin/users/{id}", 999_999)
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(RoleName.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User 999999 not found"));
    }
}

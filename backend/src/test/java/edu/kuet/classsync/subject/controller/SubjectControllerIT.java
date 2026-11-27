package edu.kuet.classsync.subject.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.kuet.classsync.support.IntegrationTest;
import edu.kuet.classsync.support.TestAuthHelper;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class SubjectControllerIT {

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelper auth;

    private static String uniqueCode() {
        return "CSE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Test
    void createAndList_asCR_returns201AndStudentCanRead() throws Exception {
        String crBearer = auth.bearer(RoleName.CR);
        String studentBearer = auth.bearer(RoleName.STUDENT);
        User teacher = auth.createUser(RoleName.TEACHER);
        String code = uniqueCode();

        String body = mvc.perform(post("/api/v1/subjects")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","title":"Software Engineering","teacherIds":[%d]}"""
                                .formatted(code, teacher.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.teachers[0].id").value(teacher.getId()))
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/v1/subjects/" + id).header(HttpHeaders.AUTHORIZATION, studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Software Engineering"));

        mvc.perform(get("/api/v1/subjects").header(HttpHeaders.AUTHORIZATION, studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].code".formatted(id)).value(hasItem(code)));
    }

    @Test
    void create_asStudent_returns403() throws Exception {
        mvc.perform(post("/api/v1/subjects")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(RoleName.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","title":"Forbidden Course","teacherIds":[]}""".formatted(uniqueCode())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_duplicateCode_returns409_andNonTeacherReturns400() throws Exception {
        String crBearer = auth.bearer(RoleName.CR);
        User student = auth.createUser(RoleName.STUDENT);
        String code = uniqueCode();

        mvc.perform(post("/api/v1/subjects")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","title":"Databases","teacherIds":[]}""".formatted(code)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/subjects")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","title":"Duplicate","teacherIds":[]}""".formatted(code)))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/subjects")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","title":"Bad Teacher","teacherIds":[%d]}"""
                                .formatted(uniqueCode(), student.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDelete_asAdmin_returns200And204() throws Exception {
        String adminBearer = auth.bearer(RoleName.ADMIN);
        String code = uniqueCode();

        String body = mvc.perform(post("/api/v1/subjects")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","title":"Old Title","teacherIds":[]}""".formatted(code)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        mvc.perform(put("/api/v1/subjects/" + id)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","title":"New Title","teacherIds":[]}""".formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));

        mvc.perform(delete("/api/v1/subjects/" + id).header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/subjects/" + id).header(HttpHeaders.AUTHORIZATION, adminBearer))
                .andExpect(status().isNotFound());
    }
}

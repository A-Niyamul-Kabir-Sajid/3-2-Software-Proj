package edu.kuet.classsync.notice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.kuet.classsync.notice.repository.NoticeRepository;
import edu.kuet.classsync.support.IntegrationTest;
import edu.kuet.classsync.support.TestAuthHelper;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class NoticeControllerIT {

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelper auth;
    @Autowired NoticeRepository noticeRepository;

    @BeforeEach
    void cleanNotices() {
        noticeRepository.deleteAll();
    }

    @Test
    void create_asCR_returns201_andStudentCanRead() throws Exception {
        User cr = auth.createUser(RoleName.CR);
        String crBearer = auth.bearer(cr);
        String studentBearer = auth.bearer(RoleName.STUDENT);

        String body = mvc.perform(post("/api/v1/notices")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Mid-term schedule","body":"Check rooms","pinned":true}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.title").value("Mid-term schedule"))
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.author.id").value(cr.getId()))
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/v1/notices/" + id).header(HttpHeaders.AUTHORIZATION, studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Mid-term schedule"));
    }

    @Test
    void create_asStudent_returns403() throws Exception {
        mvc.perform(post("/api/v1/notices")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(RoleName.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Unauthorized","body":"Nope","pinned":false}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_ordersPinnedFirstThenNewest_andFiltersByQuery() throws Exception {
        String teacherBearer = auth.bearer(RoleName.TEACHER);
        String studentBearer = auth.bearer(RoleName.STUDENT);

        mvc.perform(post("/api/v1/notices").header(HttpHeaders.AUTHORIZATION, teacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Regular older","body":"First","pinned":false}"""))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/notices").header(HttpHeaders.AUTHORIZATION, teacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Pinned urgent","body":"DBMS room changed","pinned":true}"""))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/notices").header(HttpHeaders.AUTHORIZATION, teacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Regular newer","body":"Second","pinned":false}"""))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/notices").header(HttpHeaders.AUTHORIZATION, studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Pinned urgent"))
                .andExpect(jsonPath("$.content[1].title").value("Regular newer"))
                .andExpect(jsonPath("$.content[2].title").value("Regular older"));

        mvc.perform(get("/api/v1/notices?q=dbms").header(HttpHeaders.AUTHORIZATION, studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Pinned urgent"));
    }

    @Test
    void updateAndDelete_byNonAuthorReturns403_andByAuthorSucceeds() throws Exception {
        String crBearer = auth.bearer(RoleName.CR);
        String otherTeacherBearer = auth.bearer(RoleName.TEACHER);

        String body = mvc.perform(post("/api/v1/notices").header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Original","body":"Text","pinned":false}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        mvc.perform(put("/api/v1/notices/" + id).header(HttpHeaders.AUTHORIZATION, otherTeacherBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hijack","body":"No","pinned":true}"""))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/notices/" + id).header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated","body":"Revised","pinned":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.pinned").value(true));

        mvc.perform(delete("/api/v1/notices/" + id).header(HttpHeaders.AUTHORIZATION, otherTeacherBearer))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/v1/notices/" + id).header(HttpHeaders.AUTHORIZATION, crBearer))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/notices/" + id).header(HttpHeaders.AUTHORIZATION, crBearer))
                .andExpect(status().isNotFound());
    }
}

package edu.kuet.classsync.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.kuet.classsync.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class AuthControllerIT {

    @Autowired MockMvc mvc;

    private static String uniqueEmail() {
        return "it-" + UUID.randomUUID() + "@kuet.ac.bd";
    }

    private void register(String email, String password) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rafi","email":"%s","password":"%s","role":"CR"}""".formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.roles[0]").value("CR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void register_returns201_andLoginTokenWorksOnMe() throws Exception {
        String email = uniqueEmail();
        register(email, "secret123");
        String token = login(email, "secret123");

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        register(email, "secret123");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Again","email":"%s","password":"secret123"}""".formatted(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidBody_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","password":"secret123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        register(email, "secret123");

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong999"}""".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void logout_revokesToken() throws Exception {
        String email = uniqueEmail();
        register(email, "secret123");
        String bearer = "Bearer " + login(email, "secret123");

        mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
    }
}

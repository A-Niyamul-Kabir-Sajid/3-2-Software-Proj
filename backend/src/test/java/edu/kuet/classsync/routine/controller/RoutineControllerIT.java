package edu.kuet.classsync.routine.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import edu.kuet.classsync.routine.pattern.RoutineCaretaker;
import edu.kuet.classsync.routine.repository.RoutineSlotRepository;
import edu.kuet.classsync.routine.service.CachingRoutineServiceProxy;
import edu.kuet.classsync.subject.entity.Subject;
import edu.kuet.classsync.subject.repository.SubjectRepository;
import edu.kuet.classsync.support.IntegrationTest;
import edu.kuet.classsync.support.TestAuthHelper;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class RoutineControllerIT {

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelper auth;
    @Autowired SubjectRepository subjectRepository;
    @Autowired RoutineSlotRepository routineSlotRepository;
    @Autowired RoutineCaretaker caretaker;
    @Autowired CachingRoutineServiceProxy cacheProxy;

    private Subject subject;

    @BeforeEach
    void resetRoutineState() {
        routineSlotRepository.deleteAll();
        caretaker.clear();
        cacheProxy.invalidate();
        String code = "CSE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        subject = subjectRepository.save(new Subject(code, "Software Eng", Set.of()));
    }

    @Test
    void createSlot_thenGetRoutine_containsSlot() throws Exception {
        String crBearer = auth.bearer(RoleName.CR);
        String studentBearer = auth.bearer(RoleName.STUDENT);
        User teacher = auth.createUser(RoleName.TEACHER);

        String body = mvc.perform(post("/api/v1/routine/slots")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"day":"SUNDAY","startTime":"08:00","endTime":"08:50","room":"301","subjectId":%d,"teacherId":%d}"""
                                .formatted(subject.getId(), teacher.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.start").value("08:00"))
                .andExpect(jsonPath("$.end").value("08:50"))
                .andExpect(jsonPath("$.subject.code").value(subject.getCode()))
                .andExpect(jsonPath("$.teacher.id").value(teacher.getId()))
                .andReturn().getResponse().getContentAsString();

        Integer slotId = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/v1/routine/slots/" + slotId).header(HttpHeaders.AUTHORIZATION, studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room").value("301"));

        mvc.perform(get("/api/v1/routine").header(HttpHeaders.AUTHORIZATION, studentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(1))
                .andExpect(jsonPath("$.days[0].day").value("SUNDAY"))
                .andExpect(jsonPath("$.days[0].slots[0].id").value(slotId))
                .andExpect(jsonPath("$.days[0].slots[0].start").value("08:00"));
    }

    @Test
    void createSlot_asStudent_returns403() throws Exception {
        mvc.perform(post("/api/v1/routine/slots")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(RoleName.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"day":"MONDAY","startTime":"09:00","endTime":"09:50","room":"301","subjectId":%d}"""
                                .formatted(subject.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSlot_overlapping_returns409_andInvalidTimeReturns400() throws Exception {
        String crBearer = auth.bearer(RoleName.CR);

        mvc.perform(post("/api/v1/routine/slots")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"day":"TUESDAY","startTime":"08:00","endTime":"09:00","room":"301","subjectId":%d}"""
                                .formatted(subject.getId())))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/routine/slots")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"day":"TUESDAY","startTime":"08:30","endTime":"09:20","room":"302","subjectId":%d}"""
                                .formatted(subject.getId())))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/routine/slots")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"day":"WEDNESDAY","startTime":"10:00","endTime":"09:00","room":"301","subjectId":%d}"""
                                .formatted(subject.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDeleteAndUndo_asCR_restoresPreviousState() throws Exception {
        String crBearer = auth.bearer(RoleName.CR);

        mvc.perform(post("/api/v1/routine/undo").header(HttpHeaders.AUTHORIZATION, crBearer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Nothing to undo"));

        String body = mvc.perform(post("/api/v1/routine/slots")
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"day":"THURSDAY","startTime":"10:00","endTime":"10:50","room":"204","subjectId":%d}"""
                                .formatted(subject.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer slotId = JsonPath.read(body, "$.id");

        mvc.perform(put("/api/v1/routine/slots/" + slotId)
                        .header(HttpHeaders.AUTHORIZATION, crBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"day":"THURSDAY","startTime":"10:00","endTime":"10:50","room":"405","subjectId":%d}"""
                                .formatted(subject.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room").value("405"));

        mvc.perform(delete("/api/v1/routine/slots/" + slotId)
                        .header(HttpHeaders.AUTHORIZATION, crBearer))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/routine").header(HttpHeaders.AUTHORIZATION, crBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(0));

        // Undo delete -> restores room 405
        mvc.perform(post("/api/v1/routine/undo").header(HttpHeaders.AUTHORIZATION, crBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(1))
                .andExpect(jsonPath("$.days[0].slots[0].room").value("405"));

        // Undo update -> restores room 204
        mvc.perform(post("/api/v1/routine/undo").header(HttpHeaders.AUTHORIZATION, crBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(1))
                .andExpect(jsonPath("$.days[0].slots[0].room").value("204"));
    }
}

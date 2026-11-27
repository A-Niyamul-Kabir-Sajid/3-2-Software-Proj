package edu.kuet.classsync.routine.controller;

import edu.kuet.classsync.routine.dto.RoutineResponse;
import edu.kuet.classsync.routine.dto.RoutineSlotRequest;
import edu.kuet.classsync.routine.dto.RoutineSlotResponse;
import edu.kuet.classsync.routine.service.RoutineQueryService;
import edu.kuet.classsync.routine.service.RoutineService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routine")
public class RoutineController {

    private final RoutineQueryService queryService;
    private final RoutineService routineService;

    public RoutineController(RoutineQueryService queryService, RoutineService routineService) {
        this.queryService = queryService;
        this.routineService = routineService;
    }

    @GetMapping
    public RoutineResponse getWeek() {
        return queryService.getWeek();
    }

    @GetMapping("/slots/{id}")
    public RoutineSlotResponse getSlot(@PathVariable Long id) {
        return routineService.getSlot(id);
    }

    @PostMapping("/slots")
    @PreAuthorize("hasRole('CR')")
    public ResponseEntity<RoutineSlotResponse> createSlot(@Valid @RequestBody RoutineSlotRequest request) {
        RoutineSlotResponse created = routineService.createSlot(request);
        return ResponseEntity.created(URI.create("/api/v1/routine/slots/" + created.id())).body(created);
    }

    @PutMapping("/slots/{id}")
    @PreAuthorize("hasRole('CR')")
    public RoutineSlotResponse updateSlot(@PathVariable Long id, @Valid @RequestBody RoutineSlotRequest request) {
        return routineService.updateSlot(id, request);
    }

    @DeleteMapping("/slots/{id}")
    @PreAuthorize("hasRole('CR')")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        routineService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/undo")
    @PreAuthorize("hasRole('CR')")
    public RoutineResponse undo() {
        routineService.undo();
        return queryService.getWeek();
    }
}

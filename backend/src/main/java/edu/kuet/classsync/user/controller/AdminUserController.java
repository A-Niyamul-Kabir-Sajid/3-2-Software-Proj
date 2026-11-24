package edu.kuet.classsync.user.controller;

import edu.kuet.classsync.auth.CurrentUserProvider;
import edu.kuet.classsync.common.dto.PageResponse;
import edu.kuet.classsync.user.dto.UpdateRolesRequest;
import edu.kuet.classsync.user.dto.UpdateStatusRequest;
import edu.kuet.classsync.user.dto.UserResponse;
import edu.kuet.classsync.user.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService service;
    private final CurrentUserProvider currentUser;

    public AdminUserController(AdminUserService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)
                                           Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}/roles")
    public UserResponse changeRoles(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest request) {
        return service.changeRoles(id, request.roles(), currentUser.get());
    }

    @PatchMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.setEnabled(id, request.enabled(), currentUser.get());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, currentUser.get());
        return ResponseEntity.noContent().build();
    }
}

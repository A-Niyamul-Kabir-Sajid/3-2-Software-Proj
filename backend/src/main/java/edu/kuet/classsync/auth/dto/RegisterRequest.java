package edu.kuet.classsync.auth.dto;

import edu.kuet.classsync.user.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-registration. {@code role} may be STUDENT (default) or CR; other roles are assigned by an ADMIN. */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 72) String password,
        RoleName role) { }

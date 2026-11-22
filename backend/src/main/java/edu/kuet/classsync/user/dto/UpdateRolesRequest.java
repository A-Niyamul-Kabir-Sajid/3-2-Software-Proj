package edu.kuet.classsync.user.dto;

import edu.kuet.classsync.user.entity.RoleName;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateRolesRequest(@NotEmpty Set<RoleName> roles) { }

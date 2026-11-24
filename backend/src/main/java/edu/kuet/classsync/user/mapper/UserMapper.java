package edu.kuet.classsync.user.mapper;

import edu.kuet.classsync.user.dto.UserResponse;
import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() { }

    public static UserResponse toResponse(User user) {
        Set<RoleName> roles = user.getRoles().stream().map(Role::getName)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleName.class)));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), roles, user.isEnabled(), user.getCreatedAt());
    }
}

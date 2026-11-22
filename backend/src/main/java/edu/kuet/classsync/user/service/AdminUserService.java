package edu.kuet.classsync.user.service;

import edu.kuet.classsync.common.dto.PageResponse;
import edu.kuet.classsync.common.exception.BadRequestException;
import edu.kuet.classsync.common.exception.ResourceNotFoundException;
import edu.kuet.classsync.user.dto.UserResponse;
import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.mapper.UserMapper;
import edu.kuet.classsync.user.repository.RoleRepository;
import edu.kuet.classsync.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User management for ADMINs. The acting admin is passed in so self-destructive actions can be refused. */
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminUserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable), UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserMapper.toResponse(find(id));
    }

    @Transactional
    public UserResponse changeRoles(Long id, Set<RoleName> roleNames, User actingAdmin) {
        User user = find(id);
        if (isSelf(user, actingAdmin) && !roleNames.contains(RoleName.ADMIN)) {
            throw new BadRequestException("You cannot remove your own ADMIN role");
        }
        List<Role> roles = roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalStateException("Role " + name + " is not seeded")))
                .toList();
        user.replaceRoles(roles);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled, User actingAdmin) {
        User user = find(id);
        if (isSelf(user, actingAdmin) && !enabled) {
            throw new BadRequestException("You cannot disable your own account");
        }
        user.setEnabled(enabled);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id, User actingAdmin) {
        User user = find(id);
        if (isSelf(user, actingAdmin)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        userRepository.delete(user);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private static boolean isSelf(User user, User actingAdmin) {
        return Objects.equals(user.getId(), actingAdmin.getId());
    }
}

package edu.kuet.classsync.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.kuet.classsync.common.exception.BadRequestException;
import edu.kuet.classsync.common.exception.ResourceNotFoundException;
import edu.kuet.classsync.user.dto.UserResponse;
import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.repository.RoleRepository;
import edu.kuet.classsync.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @InjectMocks AdminUserService service;

    private static User user(long id, RoleName role) {
        User u = User.builder().name("u" + id).email("u" + id + "@x.com").passwordHash("h")
                .role(new Role(role)).build();
        u.setId(id);
        return u;
    }

    @Test
    void changeRoles_unknownUser_throwsNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRoles(99L, Set.of(RoleName.TEACHER), user(1, RoleName.ADMIN)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User 99 not found");
    }

    @Test
    void changeRoles_promotesStudentToTeacher() {
        User student = user(2, RoleName.STUDENT);
        given(userRepository.findById(2L)).willReturn(Optional.of(student));
        given(roleRepository.findByName(RoleName.TEACHER)).willReturn(Optional.of(new Role(RoleName.TEACHER)));

        UserResponse res = service.changeRoles(2L, Set.of(RoleName.TEACHER), user(1, RoleName.ADMIN));

        assertThat(res.roles()).containsExactly(RoleName.TEACHER);
    }

    @Test
    void changeRoles_removingOwnAdminRole_throwsBadRequest() {
        User admin = user(1, RoleName.ADMIN);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.changeRoles(1L, Set.of(RoleName.STUDENT), admin))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void delete_self_throwsBadRequest() {
        User admin = user(1, RoleName.ADMIN);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.delete(1L, admin)).isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).delete(admin);
    }
}

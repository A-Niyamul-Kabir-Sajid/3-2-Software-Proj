package edu.kuet.classsync.user.repository;

import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}

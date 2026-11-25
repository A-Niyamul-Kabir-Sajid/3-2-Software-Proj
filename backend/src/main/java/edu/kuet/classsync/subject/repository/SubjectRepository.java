package edu.kuet.classsync.subject.repository;

import edu.kuet.classsync.subject.entity.Subject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Subject> findAllByOrderByCodeAsc();
}

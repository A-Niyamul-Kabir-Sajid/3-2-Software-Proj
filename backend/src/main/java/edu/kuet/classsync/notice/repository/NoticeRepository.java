package edu.kuet.classsync.notice.repository;

import edu.kuet.classsync.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Notice> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("""
            SELECT n FROM Notice n
            WHERE LOWER(n.title) LIKE :pattern
               OR LOWER(COALESCE(n.body, '')) LIKE :pattern
            """)
    Page<Notice> search(@Param("pattern") String pattern, Pageable pageable);
}

package edu.kuet.classsync.notice.repository;

import edu.kuet.classsync.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}

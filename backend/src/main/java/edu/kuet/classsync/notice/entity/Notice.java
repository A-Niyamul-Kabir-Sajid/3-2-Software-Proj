package edu.kuet.classsync.notice.entity;

import edu.kuet.classsync.common.entity.BaseEntity;
import edu.kuet.classsync.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Class announcement published by a CR or teacher. */
@Entity
@Table(name = "notices")
public class Notice extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean pinned;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    protected Notice() { }

    public Notice(String title, String body, boolean pinned, User author) {
        this.title = title;
        this.body = body;
        this.pinned = pinned;
        this.author = author;
    }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }

    public void setBody(String body) { this.body = body; }

    public boolean isPinned() { return pinned; }

    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public User getAuthor() { return author; }

    public void setAuthor(User author) { this.author = author; }
}

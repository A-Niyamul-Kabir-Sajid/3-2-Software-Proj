package edu.kuet.classsync.user.entity;

/** Application roles. Spring Security authorities are "ROLE_" + name(). */
public enum RoleName {
    ADMIN, CR, TEACHER, STUDENT;

    public String authority() { return "ROLE_" + name(); }
}

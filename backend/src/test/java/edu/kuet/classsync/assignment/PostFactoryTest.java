package edu.kuet.classsync.assignment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PostFactoryTest {
    @Test
    void createsRightType() {
        assertEquals("NOTICE", PostFactory.forType("notice").create("t", "b").type());
        assertEquals("ASSIGNMENT", PostFactory.forType("ASSIGNMENT").create("t", "b").type());
        assertEquals("FORM", PostFactory.forType("form").create("t", "b").type());
    }

    @Test
    void rejectsUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> PostFactory.forType("x"));
    }
}

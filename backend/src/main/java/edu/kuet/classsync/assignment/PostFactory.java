package edu.kuet.classsync.assignment;

/** Factory Method: creates the right post type from an upload type string. */
public interface PostFactory {
    Post create(String title, String body);

    interface Post { String type(); String title(); }

    record Notice(String title, String body) implements Post { public String type() { return "NOTICE"; } }
    record Assignment(String title, String body) implements Post { public String type() { return "ASSIGNMENT"; } }
    record Form(String title, String body) implements Post { public String type() { return "FORM"; } }

    static PostFactory forType(String type) {
        return switch (type.toUpperCase()) {
            case "NOTICE" -> Notice::new;
            case "ASSIGNMENT" -> Assignment::new;
            case "FORM" -> Form::new;
            default -> throw new IllegalArgumentException("Unknown post type: " + type);
        };
    }
}

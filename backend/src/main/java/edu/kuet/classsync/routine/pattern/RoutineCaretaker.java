package edu.kuet.classsync.routine.pattern;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Memento caretaker holding a bounded history (max 10) of {@link RoutineMemento} snapshots. */
@Component
public class RoutineCaretaker {

    static final int MAX_HISTORY = 10;

    private final Deque<RoutineMemento> history = new ArrayDeque<>(MAX_HISTORY);

    public synchronized void save(RoutineMemento memento) {
        if (history.size() >= MAX_HISTORY) {
            history.removeFirst();
        }
        history.addLast(memento);
    }

    public synchronized Optional<RoutineMemento> pop() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.removeLast());
    }

    public synchronized void clear() {
        history.clear();
    }
}

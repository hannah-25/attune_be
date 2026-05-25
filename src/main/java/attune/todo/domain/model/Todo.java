package attune.todo.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "todos")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(name = "text", nullable = false, length = 100)
    private String text;

    @Column(nullable = false)
    private LocalDateTime dueAt;

    @Column(nullable = false)
    private boolean isAllDay = false;

    @Column(nullable = false)
    private boolean isCompleted = false;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void update(String text, LocalDateTime dueAt, Boolean isAllDay, Boolean isCompleted) {
        if (text != null) this.text = text;
        if (dueAt != null) this.dueAt = dueAt;
        if (isAllDay != null) this.isAllDay = isAllDay;
        if (isCompleted != null) this.isCompleted = isCompleted;
    }
}

package com.product.RealestateTokenization.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String investorSessionId;  // NEW: Unique ID per investor chat (e.g., "session_abc123")
    private String senderName;
    private String content;
    private LocalDateTime timestamp = LocalDateTime.now();
    private boolean fromAdmin = false;
}

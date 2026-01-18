package com.product.RealestateTokenization.controller;

import com.product.RealestateTokenization.model.ChatMessage;
import com.product.RealestateTokenization.repository.ChatMessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatMessageRepo chatMessageRepo;

    // ====================== USER WEBSOCKET ENDPOINTS ======================

    /**
     * User joins the chat
     */
    @MessageMapping("/chat.join")
    public void join(@Payload ChatJoinPayload payload) {

        String sessionId =
                "chat_" + System.currentTimeMillis() + "_" + Math.abs(payload.name().hashCode());

        ChatMessage joinMsg = new ChatMessage();
        joinMsg.setInvestorSessionId(sessionId);
        joinMsg.setSenderName("System");
        joinMsg.setContent(payload.name() + " joined the support chat");
        joinMsg.setFromAdmin(false);
        chatMessageRepo.save(joinMsg);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/private", joinMsg);
        messagingTemplate.convertAndSend("/topic/admin/new-chat", joinMsg);

        ChatSessionInit init = new ChatSessionInit(sessionId, payload.name());
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/session-init", init);
    }

    /**
     * User sends a message
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatPayload payload) {

        ChatMessage msg = new ChatMessage();
        msg.setInvestorSessionId(payload.sessionId());
        msg.setSenderName(payload.name());
        msg.setContent(payload.content());
        msg.setFromAdmin(false);
        chatMessageRepo.save(msg);

        messagingTemplate.convertAndSendToUser(payload.sessionId(), "/queue/private", msg);
        messagingTemplate.convertAndSend("/topic/admin/messages", msg);
    }

    // ====================== ADMIN INTERNAL METHOD ======================

    private void sendAdminReplyInternal(String sessionId, String content) {

        ChatMessage msg = new ChatMessage();
        msg.setInvestorSessionId(sessionId);
        msg.setSenderName("Admin");     // 🔒 forced
        msg.setContent(content);
        msg.setFromAdmin(true);
        chatMessageRepo.save(msg);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/private", msg);
        messagingTemplate.convertAndSend("/topic/admin/messages", msg);
    }

    // ====================== ADMIN REST ENDPOINTS ======================

    /**
     * Get all active chat sessions (ADMIN ONLY)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/sessions")
    public ResponseEntity<List<ChatSessionSummary>> getActiveSessions() {

        List<String> sessionIds = chatMessageRepo.findDistinctInvestorSessionId();

        List<ChatSessionSummary> summaries = sessionIds.stream()
                .map(sessionId -> {

                    List<ChatMessage> messages =
                            chatMessageRepo.findByInvestorSessionIdOrderByTimestampAsc(sessionId);

                    if (messages.isEmpty()) return null;

                    String investorName = messages.stream()
                            .filter(m -> !m.isFromAdmin() && !"System".equals(m.getSenderName()))
                            .map(ChatMessage::getSenderName)
                            .findFirst()
                            .orElse("Guest");

                    ChatMessage last = messages.get(messages.size() - 1);

                    return new ChatSessionSummary(
                            sessionId,
                            investorName,
                            last.getContent(),
                            last.getTimestamp(),
                            messages.size()
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatSessionSummary::lastMessageTime).reversed())
                .toList();

        return ResponseEntity.ok(summaries);
    }

    /**
     * Get chat history (ADMIN ONLY)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/session/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable String sessionId) {

        return ResponseEntity.ok(
                chatMessageRepo.findByInvestorSessionIdOrderByTimestampAsc(sessionId)
        );
    }

    /**
     * Admin sends reply (ADMIN ONLY)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/session/{sessionId}/reply")
    public ResponseEntity<String> adminReply(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {

        String content = body.get("content");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message content is required");
        }

        sendAdminReplyInternal(sessionId, content.trim());
        return ResponseEntity.ok("Message sent successfully");
    }

    // ====================== RECORDS / DTO ======================

    record ChatJoinPayload(String name) {}
    record ChatPayload(String sessionId, String name, String content) {}

    record ChatSessionInit(String sessionId, String name) {}

    record ChatSessionSummary(
            String sessionId,
            String investorName,
            String lastMessage,
            LocalDateTime lastMessageTime,
            int totalMessages
    ) {}

}

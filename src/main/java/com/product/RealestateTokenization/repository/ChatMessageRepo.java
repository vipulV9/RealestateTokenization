package com.product.RealestateTokenization.repository;

import com.product.RealestateTokenization.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    // Get full chat history for a session
    List<ChatMessage> findByInvestorSessionIdOrderByTimestampAsc(String investorSessionId);

    // Admin full view (optional)
    List<ChatMessage> findAllByOrderByTimestampAsc();

    // ✅ CORRECT WAY: get all distinct session IDs
    @Query("SELECT DISTINCT c.investorSessionId FROM ChatMessage c")
    List<String> findDistinctInvestorSessionId();



}

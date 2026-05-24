package work.daqian.ai.history.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HistoryRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findTopByConversationIdOrderByCreateTimeDesc(String conversationId, int lastN);

    void removeAllByConversationId(String conversationId);
}

package io.agentforge.controlplane.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.agentforge.controlplane.domain.InboxEventEntity;
import io.agentforge.controlplane.domain.InboxEventId;

public interface InboxEventRepository extends JpaRepository<InboxEventEntity, InboxEventId> {

    @Modifying
    @Query(value = """
            INSERT INTO inbox_events (consumer, event_id, processed_at)
            VALUES (:consumer, :eventId, CURRENT_TIMESTAMP)
            ON CONFLICT (consumer, event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("consumer") String consumer, @Param("eventId") UUID eventId);
}

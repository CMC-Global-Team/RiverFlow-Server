package com.riverflow.repository.mongodb;

import com.riverflow.model.mongodb.CollaborationInvitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollaborationInvitationRepository extends MongoRepository<CollaborationInvitation, String> {
    
    List<CollaborationInvitationRepository> findByMindmapIdAndStatus(String mindmapId, String status);
    
    List<CollaborationInvitation> findByInvitedEmailAndStatus(String invitedEmail, String status);
    
    List<CollaborationInvitation> findByInvitedUserIdAndStatus(Long invitedUserId, String status);
    
    Optional<CollaborationInvitation> findByToken(String token);
}


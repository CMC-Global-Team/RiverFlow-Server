package com.riverflow.repository.mindmap;

import com.riverflow.model.mindmap.CollaborationInvitation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollaborationInvitationRepository extends MongoRepository<CollaborationInvitation, String> {
    
    List<CollaborationInvitation> findByMindmapIdAndStatus(String mindmapId, String status);
    
    List<CollaborationInvitation> findByInvitedEmailAndStatus(String invitedEmail, String status);
    
    List<CollaborationInvitation> findByInvitedUserIdAndStatus(Long invitedUserId, String status);
    
    Optional<CollaborationInvitation> findByToken(String token);
}


package com.riverflow.repository.mindmap;

import com.riverflow.model.mindmap.MindmapVersion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MindmapVersionRepository extends MongoRepository<MindmapVersion, String> {
    
    List<MindmapVersion> findByMindmapIdOrderByVersionDesc(String mindmapId);
    
    Optional<MindmapVersion> findByMindmapIdAndVersion(String mindmapId, Integer version);
    
    Optional<MindmapVersion> findFirstByMindmapIdOrderByVersionDesc(String mindmapId);
}


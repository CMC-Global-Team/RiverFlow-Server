package com.riverflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class RiverFlowApplicationTests {

    @MockBean
    private com.riverflow.repository.mindmap.MindmapRepository mindmapRepository;

    @MockBean
    private com.riverflow.repository.mindmap.MindmapHistoryRepository mindmapHistoryRepository;

    @MockBean
    private com.riverflow.repository.mindmap.MindmapActivityRepository mindmapActivityRepository;

    @MockBean
    private com.riverflow.repository.mindmap.CommentRepository commentRepository;

    @MockBean
    private com.riverflow.repository.mindmap.CollaborationInvitationRepository collaborationInvitationRepository;

    @MockBean
    private com.riverflow.repository.mindmap.TemplateRepository templateRepository;

    @MockBean
    private com.riverflow.repository.mindmap.RealtimeSessionRepository realtimeSessionRepository;

    @MockBean
    private com.riverflow.repository.mindmap.MindmapVersionRepository mindmapVersionRepository;

    @MockBean
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Test
    void contextLoads() {
    }

}

package com.riverflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity to track user's AI workflow usage
 */
@Entity
@Table(name = "user_workflow_history", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_workflow_id", columnList = "workflow_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_mindmap_id", columnList = "mindmap_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWorkflowHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private AiWorkflow workflow;
    
    /**
     * User inputs for the workflow
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_data", columnDefinition = "JSON")
    private String inputData;
    
    /**
     * Generated output
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_data", columnDefinition = "JSON")
    private String outputData;
    
    /**
     * Execution time in milliseconds
     */
    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;
    
    /**
     * AI tokens used
     */
    @Column(name = "token_count")
    private Integer tokenCount;
    
    /**
     * User rating 1-5
     */
    @Column(columnDefinition = "TINYINT")
    private Integer rating;
    
    @Column(columnDefinition = "TEXT")
    private String feedback;
    
    /**
     * MongoDB mindmap ID if created
     */
    @Column(name = "mindmap_id", length = 50)
    private String mindmapId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}


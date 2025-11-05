package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing system-wide configuration settings
 * Key-value pairs for flexible configuration
 */
@Entity
@Table(name = "system_settings", indexes = {
    @Index(name = "idx_setting_key", columnList = "setting_key"),
    @Index(name = "idx_is_public", columnList = "is_public")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique setting key
     */
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Setting key is required")
    private String settingKey;
    
    /**
     * Setting value
     */
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Setting value is required")
    private String settingValue;
    
    /**
     * Data type of the setting
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_type", nullable = false, length = 20)
    private SettingType settingType = SettingType.STRING;
    
    /**
     * Description of what this setting does
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Whether this setting can be accessed by frontend
     */
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;
    
    /**
     * User who last updated this setting
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", foreignKey = @ForeignKey(name = "fk_system_setting_updated_by"))
    private User updatedBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Enums
    
    public enum SettingType {
        STRING,
        NUMBER,
        BOOLEAN,
        JSON
    }
}


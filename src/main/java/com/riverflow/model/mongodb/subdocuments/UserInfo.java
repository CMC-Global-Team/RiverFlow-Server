package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * User information for real-time sessions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {
    private String email;
    private String fullName;
    private String avatar;
    private String color; // Assigned color for cursor and selections
}


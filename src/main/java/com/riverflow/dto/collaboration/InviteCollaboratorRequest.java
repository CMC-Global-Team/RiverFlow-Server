package com.riverflow.dto.collaboration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.riverflow.model.mindmap.CollaborationInvitation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteCollaboratorRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotNull(message = "Quyền (role) không được để trống")
    private CollaborationInvitation.Role role;

    /**
     * Custom deserializer to handle string to enum conversion
     */
    @JsonCreator
    public static InviteCollaboratorRequest create(
            @JsonProperty("email") String email,
            @JsonProperty("role") String roleStr) {
        InviteCollaboratorRequest request = new InviteCollaboratorRequest();
        request.email = email;
        if (roleStr != null) {
            try {
                request.role = CollaborationInvitation.Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Let validation handle it
                request.role = null;
            }
        }
        return request;
    }
}
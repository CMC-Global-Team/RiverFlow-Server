package com.riverflow.dto.collaboration;

import com.riverflow.model.mindmap.CollaborationInvitation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteCollaboratorRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotNull(message = "Quyền (role) không được để trống")
    private CollaborationInvitation.Role role;
}
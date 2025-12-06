package com.riverflow.controller.authentication;

import com.riverflow.dto.authentication.GitHubSignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.service.authentication.GitHubAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class GitHubAuthController {

    private final GitHubAuthService gitHubAuthService;

    @PostMapping("/github")
    public ResponseEntity<SignInResponse> signInWithGitHub(@Valid @RequestBody GitHubSignInRequest request) {
        SignInResponse response = gitHubAuthService.authenticateWithGitHub(request);
        return ResponseEntity.ok(response);
    }
}

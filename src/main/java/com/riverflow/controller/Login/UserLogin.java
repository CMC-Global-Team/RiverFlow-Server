package com.riverflow.controller.Login;

import org.springframework.web.bind.annotation.GetMapping;

public class UserLogin {
    @GetMapping("/login")
    public String login() {
        return "login";
    }

}

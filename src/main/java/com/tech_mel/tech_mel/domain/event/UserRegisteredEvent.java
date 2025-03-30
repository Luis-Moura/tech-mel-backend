package com.tech_mel.tech_mel.domain.event;

import com.tech_mel.tech_mel.domain.model.User;

public record UserRegisteredEvent(User user, String verificationToken) {
}
package com.legalconnect.lawyerbooking.security;

import java.security.Principal;

public class UserPrincipal implements Principal {
    private final Long userId;
    private final String username;
    private final String userType;

    public UserPrincipal(Long userId, String username, String userType) {
        this.userId = userId;
        this.username = username;
        this.userType = userType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserType() {
        return userType;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }
}

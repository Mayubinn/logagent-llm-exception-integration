package com.example.demo;

public class ExceptionFlowDemo {

    public void handleRequest(String userId) {
        try {
            loadProfile(userId);
        } catch (IllegalStateException ex) {
            throw new RuntimeException("query user profile failed", ex);
        }
    }

    private void loadProfile(String userId) {
        String rawValue = fetchFromRepository(userId);
        Integer.parseInt(rawValue);
    }

    private String fetchFromRepository(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return "user-" + userId;
    }
}

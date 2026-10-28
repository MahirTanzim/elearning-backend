package com.example.elearningbackend.user;

public final class AvatarUrls {

    private AvatarUrls() {}

    /** The URL the browser should use for this user's picture, or null if there is none. */
    public static String of(User user) {
        String key = user.getAvatarKey();
        if (key != null) {
            return "/api/public/avatars/" + key.substring(key.lastIndexOf('/') + 1);
        }
        return user.getAvatarUrl(); // e.g. the Google profile picture
    }
}
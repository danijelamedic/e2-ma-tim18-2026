package com.example.slagalica.data;

import com.example.slagalica.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class PlayerProfileLoader {

    public interface Callback {
        void onLoaded(PlayerSummary summary);
    }

    public static class PlayerSummary {
        public final String username;
        public final String info;
        public final int avatarResId;

        public PlayerSummary(String username, String info, int avatarResId) {
            this.username = username;
            this.info = info;
            this.avatarResId = avatarResId;
        }
    }

    public static void load(String uid, Callback callback) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    String username = document.getString("username");
                    Long tokens = document.getLong("tokens");
                    Long stars = document.getLong("stars");
                    Long league = document.getLong("league");
                    String avatar = document.getString("avatar");

                    long tokensValue = tokens != null ? tokens : 0;
                    long starsValue = stars != null ? stars : 0;
                    long leagueValue = league != null ? league : 0;

                    callback.onLoaded(new PlayerSummary(
                            username != null ? username : "Player",
                            "\uD83E\uDE99" + tokensValue + " \u2B50" + starsValue + " L" + leagueValue,
                            getAvatarResource(avatar)
                    ));
                })
                .addOnFailureListener(e -> callback.onLoaded(
                        new PlayerSummary("Player", "\uD83E\uDE990 \u2B500 L0", R.drawable.avatar_owl)
                ));
    }

    public static int getAvatarResource(String avatar) {
        if (avatar == null) {
            return R.drawable.avatar_owl;
        }

        switch (avatar) {
            case "fox":
                return R.drawable.avatar_fox;
            case "penguin":
                return R.drawable.avatar_penguin;
            case "wolf":
                return R.drawable.avatar_wolf;
            case "cat":
                return R.drawable.avatar_cat;
            case "dog":
                return R.drawable.avatar_dog;
            case "guest":
                return R.drawable.avatar_guest;
            case "owl":
            default:
                return R.drawable.avatar_owl;
        }
    }
}

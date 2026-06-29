package com.example.slagalica.ranking;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.slagalica.R;

public class RankingRewardDialog {

    public static void show(Context context, String message) {
        show(context, message, null);
    }

    public static void show(Context context, String message, Runnable onDismiss) {
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_reward_notification, null);
        TextView title = dialogView.findViewById(R.id.tvRewardDialogTitle);
        TextView body = dialogView.findViewById(R.id.tvRewardDialogMessage);
        Button ok = dialogView.findViewById(R.id.btnRewardDialogOk);

        title.setText("Ranking reward");
        body.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
        ok.setOnClickListener(v -> dialog.dismiss());
        if (onDismiss != null) {
            dialog.setOnDismissListener(d -> onDismiss.run());
        }
        dialog.setOnShowListener(d -> {
            dialogView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce_in));
            playNotificationSound(context);
        });
        dialog.show();
    }

    private static void playNotificationSound(Context context) {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), uri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (RuntimeException ignored) {
            // Reward display must not fail if the device has notification sound disabled.
        }
    }
}

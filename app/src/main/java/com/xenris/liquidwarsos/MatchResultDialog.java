// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

/** Shared, persistent result actions for solo, host and client. UI thread only. */
public final class MatchResultDialog {
    private MatchResultDialog() {}

    public static AlertDialog show(Activity activity, CharSequence message,
                                   int primaryLabel, Runnable primaryAction,
                                   int exitLabel, Runnable exitAction,
                                   Runnable spectateAction) {
        View content = activity.getLayoutInflater().inflate(R.layout.match_result, null);
        ((TextView) content.findViewById(R.id.result_message)).setText(message);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content).setCancelable(false).create();
        bindAction(content, R.id.result_primary, primaryLabel, dialog, primaryAction);
        bindAction(content, R.id.result_exit, exitLabel, dialog, exitAction);
        if (spectateAction != null) {
            View spectate = content.findViewById(R.id.result_spectate);
            spectate.setVisibility(View.VISIBLE);
            bindAction(content, R.id.result_spectate, R.string.continue_spectating,
                    dialog, spectateAction);
        }
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        return dialog;
    }

    private static void bindAction(View content, int id, int label, AlertDialog dialog, Runnable action) {
        TextView button = content.findViewById(id);
        button.setText(label);
        button.setOnClickListener(v -> {
            // Block repeated taps before navigation; the existing action owns match shutdown.
            if (!dialog.isShowing()) return;
            dialog.dismiss();
            action.run();
        });
    }
}

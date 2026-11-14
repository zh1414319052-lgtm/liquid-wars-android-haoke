// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.online;

import android.content.Intent;

/** Android's share sheet lets the player choose an installed app and recipient. */
public final class GameShare {
    public static final String DOWNLOAD_URL = "https://www.haoke.org/software/f2a585b84fbf5b85?platform=android";
    private GameShare() {}

    public static Intent message(String roomCode) {
        boolean invite=roomCode!=null&&!roomCode.isEmpty();
        String text=invite
                ?"一起来玩液态战争！房间码："+roomCode+"\n"+PlatformApi.ORIGIN+"/invite/"+roomCode+"?game=liquid-wars\n游戏下载："+DOWNLOAD_URL
                :"一起来玩液态战争！\n"+DOWNLOAD_URL;
        return new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT,invite?"液态战争 · 好友邀请":"液态战争重制版")
                .putExtra(Intent.EXTRA_TEXT,text);
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.ui;

import java.nio.charset.StandardCharsets;
import com.xenris.liquidwarsos.server.LanRoom;

/** Visible room-name decoration only, not machine metadata or a protocol field. */
public final class DifficultyRoomName {
    private DifficultyRoomName() {}
    public static String format(String base,String label) {
        String suffix=" · "+trimBytes(label,30);
        int budget=60-suffix.getBytes(StandardCharsets.UTF_8).length;
        return trimBytes(LanRoom.cleanName(base),budget)+suffix;
    }
    private static String trimBytes(String value,int maximum) {
        StringBuilder result=new StringBuilder();int bytes=0;
        for(int at=0;at<value.length();) {
            int cp=value.codePointAt(at);String next=new String(Character.toChars(cp));int count=next.getBytes(StandardCharsets.UTF_8).length;
            if(bytes+count>maximum)break;
            result.append(next);bytes+=count;at+=Character.charCount(cp);
        }
        return result.toString().trim();
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos;
import android.os.LocaleList;
/** UI literals only. Protocol fields, player names and persisted keys never pass here. */
public final class BattleText {
 private static volatile boolean chinese=isChinese(android.content.res.Resources.getSystem().getConfiguration().getLocales());
 private BattleText(){}
 public static boolean isChinese(LocaleList locales){return !locales.isEmpty()&&"zh".equals(locales.get(0).getLanguage());}
 public static void configure(LocaleList locales){chinese=isChinese(locales);}
 public static String t(String zh,String en){return chinese?zh:en;}
}

package com.xenris.liquidwarsos;
import android.os.LocaleList;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BattleTextTest {
 @Test public void followsFirstSystemLanguageAndCanReconfigure(){
  BattleText.configure(new LocaleList(Locale.TRADITIONAL_CHINESE,Locale.ENGLISH));
  assertEquals("冻结",BattleText.t("冻结","Freeze"));
  BattleText.configure(new LocaleList(Locale.FRENCH,Locale.SIMPLIFIED_CHINESE));
  assertEquals("Freeze",BattleText.t("冻结","Freeze"));
  BattleText.configure(LocaleList.getEmptyLocaleList());
  assertEquals("Freeze",BattleText.t("冻结","Freeze"));
  BattleText.configure(new LocaleList(Locale.SIMPLIFIED_CHINESE));
  assertEquals("冻结",BattleText.t("冻结","Freeze"));
 }
}

package com.xenris.liquidwarsos.updates;
import org.junit.Test;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class LiquidWarsFeedTest {
    @Test public void actualProductionNoneFeedMatchesThisGame()throws Exception{
        byte[] bytes=getClass().getResourceAsStream("/updates/liquidwars-none-v1.json").readAllBytes();
        JSONObject envelope=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        JSONObject payload=new JSONObject(new String(Base64.getDecoder().decode(envelope.getString("payload")),StandardCharsets.UTF_8));
        long now=UpdateManifestValidator.utcDate(payload.get("issuedAt"))+1000;
        UpdateManifestValidator validator=new UpdateManifestValidator(Collections.singletonMap(HaokeUpdateController.KEY_ID,Base64.getDecoder().decode(HaokeUpdateController.PUBLIC_KEY)));
        UpdateManifestValidator.Identity identity=new UpdateManifestValidator.Identity("f2a585b84fbf5b85","com.xenris.liquidwarsos.haoke","stable",55,"4ca5a614020e7b450a504fbf2fec33242ad071d25443604a3b8dae7c0801aede",34,Arrays.asList("arm64-v8a"));
        UpdateManifestValidator.Manifest verified=validator.verify(bytes,identity,now);assertEquals("none",verified.status);assertEquals(0,verified.revision);assertNull(verified.release);
    }
}

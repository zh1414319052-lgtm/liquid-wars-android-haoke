// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.online;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.xenris.liquidwarsos.BuildConfig;
import com.xenris.liquidwarsos.StaticBits;
import java.io.IOException;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import okhttp3.*;
import org.json.*;

/** Platform credentials stay in Keystore-encrypted app-private storage. */
public final class PlatformApi {
    public static final String ORIGIN = "https://cleanbizai.com";
    public static final String GAME = "liquid-wars";
    public static final OkHttpClient HTTP = new OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS).pingInterval(12, TimeUnit.SECONDS)
        .followRedirects(false).build();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Context context;
    public final String origin;
    private final String storage;
    private volatile String token;
    public static final class ApiError extends IOException {public final int status;ApiError(int status,String message){super(message);this.status=status;}}
    public PlatformApi(Context context) { this.context=context.getApplicationContext();String target=ORIGIN;
        if(BuildConfig.DEBUG){java.io.File marker=new java.io.File(this.context.getCacheDir(),"expedition-local-193");if(marker.isFile())try(java.io.FileInputStream in=new java.io.FileInputStream(marker)){byte[] bytes=new byte[64];int n=in.read(bytes);if(n>0&&new String(bytes,0,n,java.nio.charset.StandardCharsets.UTF_8).trim().equals("http://127.0.0.1:19380"))target="http://127.0.0.1:19380";}catch(IOException ignored){}}
        origin=target;storage=BuildConfig.DEBUG&&new java.io.File(this.context.getCacheDir(),"guest193-public-fixture").isFile()?"platform-session-publicqa193":origin.equals(ORIGIN)?"platform-session":"platform-session-qa193";token=load(); }
    public boolean signedIn() { return token!=null&&!token.isEmpty(); }
    public JSONObject call(String method,String path,JSONObject body) throws Exception {
        Request.Builder builder=new Request.Builder().url(origin+"/api"+path).header("User-Agent","Haoke-LiquidWars/"+BuildConfig.VERSION_NAME+" (Android)");
        if(signedIn())builder.header("Authorization","Bearer "+token);
        builder.method(method,method.equals("GET")?null:RequestBody.create(body==null?"{}":body.toString(),JSON));
        try(Response response=HTTP.newCall(builder.build()).execute()){
            if(response.body()==null)throw new IOException("服务器没有返回内容，请稍后重试");
            JSONObject data=new JSONObject(response.body().string());
            if(!response.isSuccessful()){
                JSONObject error=data.optJSONObject("error");
                if(response.code()==401||(error!=null&&error.optString("code").equals("ACCOUNT_DISABLED")))clear();
                throw new ApiError(response.code(),error==null?"服务暂不可用（"+response.code()+"）":error.optString("message","请求失败"));
            }
            return data;
        }
    }
    public static JSONObject object(Object... pairs) throws JSONException {
        JSONObject j=new JSONObject();for(int i=0;i<pairs.length;i+=2)j.put((String)pairs[i],pairs[i+1]);return j;
    }
    public JSONObject hello() throws JSONException {return object("game_id",GAME,"protocol",StaticBits.VERSION_CODE,"build",BuildConfig.VERSION_CODE);}
    public void enter() throws Exception {call("POST","/online/enter",hello());}
    public synchronized JSONObject ensurePlayer() throws Exception {
        if(signedIn())try{JSONObject me=call("GET","/me",null);if(!me.isNull("user"))return me;clear();}
        catch(ApiError e){if(e.status!=401||loadEncrypted(storage+"-guest").isEmpty())throw e;}
        String store=storage+"-guest";String secret=loadEncrypted(store);
        if(secret.isEmpty()){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);secret=Base64.encodeToString(bytes,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);saveEncrypted(store,secret);}
        JSONObject result=call("POST","/auth/guest",object("installation_secret",secret));save(result.getString("access_token"));return result;
    }
    public void forgetGuest(){context.getSharedPreferences(storage+"-guest",0).edit().clear().commit();}
    public void save(String value) throws Exception {saveEncrypted(storage,value);token=value;}
    private void saveEncrypted(String name,String value) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
        String encrypted=Base64.encodeToString(c.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);
        context.getSharedPreferences(name,0).edit().putString("data",encrypted)
            .putString("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP)).commit();
    }
    public void clear(){token="";context.getSharedPreferences(storage,0).edit().clear().apply();}
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        String alias="haoke.platform.session";
        if(!store.containsAlias(alias)){
            KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();
        }
        return ((KeyStore.SecretKeyEntry)store.getEntry(alias,null)).getSecretKey();
    }
    private String load(){return loadEncrypted(storage);}
    private String loadEncrypted(String name){
        try{
            SharedPreferences p=context.getSharedPreferences(name,0);String data=p.getString("data","");if(data.isEmpty())return "";
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),Base64.NO_WRAP)));
            return new String(c.doFinal(Base64.decode(data,Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);
        }catch(Exception e){return "";}
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public final class ModelStore {
    private static final String ALIAS="lw.model-config.aes.v1";
    private static javax.crypto.SecretKey key()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias(ALIAS)){KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();}
        return (javax.crypto.SecretKey)store.getKey(ALIAS,null);
    }
    private static AtomicFile file(Context c,int team){if(team<0||team>=6)throw new IllegalArgumentException();return new AtomicFile(new File(c.getNoBackupFilesDir(),"model-team-"+team+".bin"));}
    public static synchronized ModelConfig load(Context c,int team)throws Exception{
        AtomicFile f=file(c,team);if(!f.getBaseFile().exists())return ModelConfig.empty();
        byte[] bytes=f.readFully();if(bytes.length<29||bytes.length>16384)throw new IOException("Config format");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,bytes,0,12));cipher.updateAAD(new byte[]{(byte)team});
        return ModelConfig.from(new JSONObject(new String(cipher.doFinal(bytes,12,bytes.length-12),StandardCharsets.UTF_8)));
    }
    public static synchronized void save(Context c,int team,ModelConfig config)throws Exception{
        if(config.enabled)config.validate();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());cipher.updateAAD(new byte[]{(byte)team});
        byte[] data=cipher.doFinal(config.json().toString().getBytes(StandardCharsets.UTF_8));AtomicFile f=file(c,team);FileOutputStream stream=null;
        try{stream=f.startWrite();stream.write(cipher.getIV());stream.write(data);f.finishWrite(stream);}catch(Exception e){if(stream!=null)f.failWrite(stream);throw e;}
    }
    public static synchronized void clear(Context c,int team){file(c,team).delete();}
    public static ModelConfig[] all(Context c){ModelConfig[] result=new ModelConfig[6];for(int i=0;i<6;i++)try{result[i]=load(c,i);}catch(Exception e){result[i]=ModelConfig.empty();}return result;}
}

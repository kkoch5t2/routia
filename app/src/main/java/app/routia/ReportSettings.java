package app.routia;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class ReportSettings {
    /** A request owns one credential revision; never include its value in diagnostics. */
    static final class Credential {
        final String value,revision;
        private Credential(String value,String revision){this.value=value;this.revision=revision;}
        @Override public String toString(){return "Credential[redacted]";}
    }
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("routia_reports",Context.MODE_PRIVATE);}
    private static javax.crypto.SecretKey key()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        String alias="routia_openai_reports";
        if(store.containsAlias(alias))return ((KeyStore.SecretKeyEntry)store.getEntry(alias,null)).getSecretKey();
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    static synchronized void saveKey(Context c,String value)throws Exception{
        value=value.trim();if(value.length()<16||value.length()>512||value.contains("\n")||value.contains("\r"))throw new IllegalArgumentException("APIキーを確認してください");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        String iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP),encrypted=Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if(!prefs(c).edit().putString("key_iv",iv).putString("key_cipher",encrypted)
            .putString("key_revision",java.util.UUID.randomUUID().toString()).commit())throw new IllegalStateException("キーを保存できませんでした");
        cancelCredentialWork(c);
    }
    static synchronized void deleteKey(Context c){
        if(!prefs(c).edit().remove("key_cipher").remove("key_iv")
            .putString("key_revision",java.util.UUID.randomUUID().toString()).commit())throw new IllegalStateException("キーを削除できませんでした");
        try{cancelCredentialWork(c);}finally{ReportJob.schedule(c);}
    }
    private static void cancelCredentialWork(Context c){
        try(ReportStore db=new ReportStore(c)){db.cancelAllUnsent("接続キーが変更・削除されたため、送信前に停止しました。もう一度料金を確認してください。");}
    }
    static synchronized Credential credential(Context c)throws Exception{
        return new Credential(readKey(c),prefs(c).getString("key_revision",""));
    }
    static synchronized boolean currentCredential(Context c,Credential credential){
        return configured(c)&&credential.revision.equals(prefs(c).getString("key_revision",""));
    }
    static synchronized String readKey(Context c)throws Exception{
        String encoded=prefs(c).getString("key_cipher","");if(encoded.isEmpty())return "";
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(prefs(c).getString("key_iv",""),Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(encoded,Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    static boolean configured(Context c){return !prefs(c).getString("key_cipher","").isEmpty();}
    static ReportBudget.Options options(Context c){
        return new ReportBudget.Options(prefs(c).getString("model",ReportBudget.MODEL),prefs(c).getInt("limit_yen",ReportBudget.LIMIT_YEN),ReportDetail.fromId(prefs(c).getString("detail","standard")));
    }
    static boolean habitEnabled(Context c,long habit){return prefs(c).getBoolean("habit_enabled_"+habit,false);}
    /** One-time migration preserves previously approved targets; new installations start off. */
    static synchronized boolean featureEnabled(Context c){
        SharedPreferences p=prefs(c);
        if(p.contains("feature_enabled"))return p.getBoolean("feature_enabled",false);
        boolean legacy=false;
        for(Store.Habit h:Store.get(c).habits())if(habitEnabled(c,h.id())){legacy=true;break;}
        return p.edit().putBoolean("feature_enabled",legacy).commit()&&legacy;
    }
    static synchronized void setFeatureEnabled(Context c,boolean on){
        boolean wasOn=featureEnabled(c);
        SharedPreferences.Editor e=prefs(c).edit().putBoolean("feature_enabled",on);
        if(on&&!wasOn)for(Store.Habit h:Store.get(c).habits())if(habitEnabled(c,h.id()))
            e.putString("habit_from_"+h.id(),java.time.LocalDate.now().plusDays(1).toString());
        if(!e.commit())throw new IllegalStateException("設定を保存できませんでした");
        try{if(!on)try(ReportStore db=new ReportStore(c)){db.cancelAllUnsent();}}
        finally{ReportJob.schedule(c);}
    }
    static boolean active(Context c,long habit){return featureEnabled(c)&&configured(c)&&habitEnabled(c,habit)&&Store.get(c).habit(habit)!=null;}
    static java.time.LocalDate enabledFrom(Context c,long habit){
        try{return java.time.LocalDate.parse(prefs(c).getString("habit_from_"+habit,java.time.LocalDate.now().plusDays(1).toString()));}
        catch(java.time.DateTimeException e){return java.time.LocalDate.now().plusDays(1);}
    }
    static boolean enabled(Context c){if(!featureEnabled(c)||!configured(c))return false;for(Store.Habit h:Store.get(c).habits())if(habitEnabled(c,h.id()))return true;return false;}
    static synchronized void saveOptions(Context c,ReportBudget.Options options,java.util.Map<Long,Boolean> targets){
        SharedPreferences.Editor e=prefs(c).edit().putBoolean("feature_enabled",featureEnabled(c)).putString("model",options.model()).putInt("limit_yen",options.limitYen()).putString("detail",options.detail().id).remove("automatic");
        for(java.util.Map.Entry<Long,Boolean> t:targets.entrySet()){
            if(Store.get(c).habit(t.getKey())==null)continue;
            if(t.getValue()&&!habitEnabled(c,t.getKey()))e.putString("habit_from_"+t.getKey(),java.time.LocalDate.now().plusDays(1).toString());
            e.putBoolean("habit_enabled_"+t.getKey(),t.getValue());
        }
        if(!e.commit())throw new IllegalStateException("設定を保存できませんでした");
        try(ReportStore db=new ReportStore(c)){if(!featureEnabled(c))db.cancelAllUnsent();else for(java.util.Map.Entry<Long,Boolean> t:targets.entrySet())if(!t.getValue())db.cancelUnsent(t.getKey());}
        Feedback.prefs(c).edit().putBoolean("pledgeEnabled",false).apply();ReportJob.schedule(c);
    }

    private ReportSettings(){}
}

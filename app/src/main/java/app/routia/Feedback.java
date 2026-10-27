package app.routia;

import android.content.*;
import android.media.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.Toast;

public final class Feedback {
    public static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("preferences",Context.MODE_PRIVATE); }
    public static void deliver(Context c,View view,boolean recovery,String message) {
        if(prefs(c).getBoolean("haptics",true)) {
            if(view!=null) view.performHapticFeedback(Build.VERSION.SDK_INT>=30?HapticFeedbackConstants.CONFIRM:HapticFeedbackConstants.KEYBOARD_TAP);
            else if(Settings.System.getInt(c.getContentResolver(),Settings.System.HAPTIC_FEEDBACK_ENABLED,1)==1) {
                Vibrator vibrator=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator!=null&&vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(recovery?60:30,VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
        AudioManager audio=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        if(view==null&&prefs(c).getBoolean("sound",false)&&audio.getRingerMode()==AudioManager.RINGER_MODE_NORMAL) {
            try { ToneGenerator tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,45);tone.startTone(recovery?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,120);new Handler(Looper.getMainLooper()).postDelayed(tone::release,200); } catch(RuntimeException ignored) { /* Audio unavailable: the record remains saved. */ }
        }
        Toast.makeText(c,message,Toast.LENGTH_SHORT).show();
    }
    static void celebrate(Context c,View view){
        if(prefs(c).getBoolean("haptics",true))view.performHapticFeedback(Build.VERSION.SDK_INT>=30?HapticFeedbackConstants.CONFIRM:HapticFeedbackConstants.KEYBOARD_TAP);
        AudioManager manager=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        if(!prefs(c).getBoolean("sound",false)||manager.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL)return;
        new Thread(()->{
            AudioTrack track=null;try{
                int rate=22050,count=(int)(rate*.85);short[] samples=new short[count];double[] notes={523.25,659.25,783.99,1046.5};
                for(int at=0;at<count;at++){double t=at/(double)rate,wave=0;for(int n=0;n<notes.length;n++){double local=t-n*.11;if(local>=0&&local<.5){double envelope=Math.min(1,local/.015)*Math.exp(-local*9);wave+=(Math.sin(2*Math.PI*notes[n]*local)+.17*Math.sin(4*Math.PI*notes[n]*local))*envelope;}}samples[at]=(short)(Math.max(-1,Math.min(1,wave*.19))*32767);}
                track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(samples.length*2).setTransferMode(AudioTrack.MODE_STATIC).build();
                track.write(samples,0,samples.length);track.play();Thread.sleep(950);
            }catch(RuntimeException|InterruptedException ignored){}finally{if(track!=null)track.release();}
        },"routia-chime").start();
    }
    private Feedback() {}
}

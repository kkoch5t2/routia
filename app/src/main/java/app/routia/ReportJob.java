package app.routia;

import android.app.job.*;
import android.content.*;

public final class ReportJob extends JobService {
    private static final int ID=3703;
    static void schedule(Context c){
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);
        if(!ReportSettings.enabled(c)){scheduler.cancel(ID);return;}
        JobInfo info=new JobInfo.Builder(ID,new ComponentName(c,ReportJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build();
        scheduler.schedule(info);
    }
    @Override public boolean onStartJob(JobParameters params){
        ReportRunner.work(this,()->jobFinished(params,false));return true;
    }
    @Override public boolean onStopJob(JobParameters params){return false;}
}

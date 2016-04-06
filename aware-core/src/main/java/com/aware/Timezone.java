
package com.aware;

import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.aware.providers.TimeZone_Provider;
import com.aware.providers.TimeZone_Provider.TimeZone_Data;
import com.aware.utils.Aware_Sensor;

/**
* Timezone module. Keeps track of changes in the device Timezone.
* @author Nikola
* Changes log:
* 17 June 2013
* - Added copyright notice, AWARE device ID to timezone context provider (@author Denzil Ferreira <denzil.ferreira@ee.oulu.fi>)
*/
public class Timezone extends Aware_Sensor {

	/**
     * Frequency of update of timeZone information. (default = 3600) seconds (=1 hour)
     */
    private static int TIMEZONE_UPDATE = 3600;
    
    /**
     * Broadcasted event: when there is new timezone information
     */
    public static final String ACTION_AWARE_TIMEZONE = "ACTION_AWARE_TIMEZONE";
    public static final String EXTRA_DATA = "data";
    
    private static Handler mHandler = new Handler();
    private final Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			
			String timeZone = java.util.TimeZone.getDefault().getID();
			ContentValues rowData = new ContentValues();
            rowData.put(TimeZone_Data.TIMESTAMP, System.currentTimeMillis());
            rowData.put(TimeZone_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            rowData.put(TimeZone_Data.TIMEZONE, timeZone);
            
            try{
                getContentResolver().insert(TimeZone_Data.CONTENT_URI, rowData);
            }catch( SQLiteException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }catch( IllegalStateException e ) {
                if(Aware.DEBUG) Log.d(TAG,e.getMessage());
            }
            
            Intent newTimeZone = new Intent(ACTION_AWARE_TIMEZONE);
            newTimeZone.putExtra(EXTRA_DATA, rowData);
            sendBroadcast(newTimeZone);
            
            TIMEZONE_UPDATE = Integer.parseInt(Aware.getSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_TIMEZONE));
            mHandler.postDelayed(mRunnable, TIMEZONE_UPDATE * 1000);
		}
	};
    
    private final IBinder serviceBinder = new ServiceBinder();
    /**
     * Activity-Service binder
     */
    public class ServiceBinder extends Binder {
        Timezone getService() {
            return Timezone.getService();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
    
    private static Timezone timezoneSrv = Timezone.getService();
    
    /**
     * Singleton instance of this service
     * @return {@link Timezone} obj
     */
    public static Timezone getService() {
        if( timezoneSrv == null ) timezoneSrv = new Timezone();
        return timezoneSrv;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        TAG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_TAG).length()>0?Aware.getSetting(getApplicationContext(),Aware_Preferences.DEBUG_TAG):"AWARE::Timezone";

        if( Aware.getSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_TIMEZONE).length() == 0) {
            Aware.setSetting(this, Aware_Preferences.FREQUENCY_TIMEZONE, TIMEZONE_UPDATE);
        }
        TIMEZONE_UPDATE = Integer.parseInt(Aware.getSetting(getApplicationContext(),Aware_Preferences.FREQUENCY_TIMEZONE));
        
        DATABASE_TABLES = TimeZone_Provider.DATABASE_TABLES;
        TABLES_FIELDS = TimeZone_Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ TimeZone_Data.CONTENT_URI };
        
        CONTEXT_PRODUCER = new ContextProducer() {
			@Override
			public void onContext() {
				Intent newTimeZone = new Intent(ACTION_AWARE_TIMEZONE);
	            sendBroadcast(newTimeZone);
			}
		};
        
        mHandler.post(mRunnable);

        Aware.setSetting(this, Aware_Preferences.STATUS_TIMEZONE, true);

        if(Aware.DEBUG) Log.d(TAG,"Timezone service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TAG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_TAG).length()>0?Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_TAG):"AWARE::Timezone";
        
        if( Integer.parseInt(Aware.getSetting(getApplicationContext(),Aware_Preferences.FREQUENCY_TIMEZONE)) != TIMEZONE_UPDATE ) {
            TIMEZONE_UPDATE = Integer.parseInt(Aware.getSetting(getApplicationContext(),Aware_Preferences.FREQUENCY_TIMEZONE));
        }
        
        if(Aware.DEBUG) Log.d(TAG,"Timezone service active...");
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        mHandler.removeCallbacks(mRunnable);

        if(Aware.DEBUG) Log.d(TAG,"Timezone service terminated...");
    }
}

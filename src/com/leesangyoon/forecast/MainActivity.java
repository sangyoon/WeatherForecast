package com.leesangyoon.forecast;

import java.util.Random;

import org.json.JSONException;

import android.app.Activity;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.leesangyoon.forecast.helper.WeatherHttpClient;
import com.leesangyoon.forecast.helper.WeatherJSONParser;
import com.leesangyoon.forecast.model.Weather;
import com.leesangyoon.forecast.service.LocationService;

public class MainActivity extends Activity
{
	private LocationManager locationManager = null;
	private LocationReceiver locationReceiver = null;
    private String locationProvider = null;
	
	double latitude = 0.0;
	double longitude = 0.0;
	
	private TextView actual;
	private TextView feels;
	private TextView temp;
	private TextView winds;
	private TextView pressure;
	
	private ImageView icon;
	private ImageView avatar;
	
	private WeatherTask task = null;
	
	SharedPreferences prefs = null;
	
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        if( savedInstanceState == null )
        {
            getFragmentManager().beginTransaction()
                    			.add( R.id.container , new PlaceholderFragment() )
                    			.commit();
        }
        
        prefs = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
        String unit = prefs.getString( "prefWeatherUnit" , "celsius" );
    	unit = ( unit.equalsIgnoreCase( "celsius" ) ) ? "metric" : "";
    	
    	if( latitude == 0 && longitude == 0 )
    	{
	    	locationManager = ( LocationManager )getSystemService( LOCATION_SERVICE );
	    	
	    	if( locationManager.isProviderEnabled( LocationManager.NETWORK_PROVIDER ) )
	    		locationProvider = LocationManager.NETWORK_PROVIDER;
	    	else
	    		locationProvider = LocationManager.GPS_PROVIDER;
	    	
	    	locationManager.requestLocationUpdates( locationProvider , 1000 , 0 , new LocationListener() {
				@Override
				public void onStatusChanged( String provider , int status , Bundle extras )	{}
				
				@Override
				public void onProviderEnabled( String provider ) {}
				
				@Override
				public void onProviderDisabled( String provider ) {}
				
				@Override
				public void onLocationChanged( Location location ) {}
			} );
	    	
	    	Location lastLocation = locationManager.getLastKnownLocation( locationProvider );
	    	latitude = lastLocation.getLatitude();
	    	longitude = lastLocation.getLongitude();
    	}
    	
        task = new WeatherTask();
        task.execute( String.valueOf( latitude ) , String.valueOf( longitude ) , unit );
    	
        // 환영 메세지
    	String name = prefs.getString( "prefUsername" , "" );
    	
        if( !name.equalsIgnoreCase( "" ) )
    		Toast.makeText( getApplicationContext() , "안녕하세요, " + name + "님!" , Toast.LENGTH_SHORT ).show();
    }
    
    @Override
	protected void onStart()
	{
		locationReceiver = new LocationReceiver();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction( LocationService.ACTION );
		
		registerReceiver( locationReceiver , intentFilter );
		
		Intent intent = new Intent( this , LocationService.class );		
		startService( intent );
		
		super.onStart();
	}
    
    @Override
    protected void onResume()
    {
    	prefs = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
    	String sex = prefs.getString( "prefSex" , "male" );
    	
    	avatar = ( ImageView )findViewById( R.id.avatar );
    	
    	if( sex.equalsIgnoreCase( "male" ) )
    		avatar.setImageResource( R.drawable.male );
    	else
    		avatar.setImageResource( R.drawable.female );
    	    	    	
    	super.onResume();
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    }
	
	@Override
	protected void onStop()
	{
		unregisterReceiver( locationReceiver );
		
		super.onStop();
	}
	
	@Override
	public void onDestroy()
	{
		if( task != null )
		{
			task.cancel( true );
			task = null;
		}
		
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		getMenuInflater().inflate( R.menu.main , menu );
	    
	    return true;
	}

    @Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.menu_setting:
				task.cancel( true );
				
				Intent intent = new Intent( getApplicationContext() , SettingActivity.class );
				startActivity( intent );
		            
				return true;
				
			case R.id.menu_weather:
				task.cancel( true );
				
				Intent weatherIntent = new Intent( getApplicationContext() , WeatherActivity.class );
				startActivity( weatherIntent );
				 
				return true;
				
			case R.id.menu_refresh:
				String unit = prefs.getString( "prefWeatherUnit" , "celsius" );
				unit = ( unit.equalsIgnoreCase( "celsius" ) ) ? "metric" : "";
				
				task.cancel( true );
				
				task = new WeatherTask();
		        task.execute( String.valueOf( latitude ) , String.valueOf( longitude ) , unit );
				
				return true;
				
			default:
				return super.onOptionsItemSelected( item );
		}
	}
	
	private class LocationReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive( Context context , Intent intent )
		{
			latitude = intent.getDoubleExtra( "latitude" , 0.0 );
			longitude = intent.getDoubleExtra( "longitude" , 0.0 );
		}
	}
		
	private class WeatherTask extends AsyncTask< String , Void , Weather >
	{
		private ProgressDialog dlg = null;
		
		@Override
        protected void onPreExecute()
		{
            super.onPreExecute();
            
            dlg = ProgressDialog.show( MainActivity.this , "데이터 로딩중" , "날씨정보를 불러오고 있습니다." , true );
        }
		
		@Override
		protected Weather doInBackground( String... params )
		{
			if( !isCancelled() )
			{			
				Weather weather = new Weather();
				
				String data = ( new WeatherHttpClient() ).getWeatherData( Float.parseFloat( params[0] ) , Float.parseFloat( params[1] ) , params[2] );
				
				try
				{
					weather = WeatherJSONParser.getWeather( data );
				}
				catch( JSONException e )
				{				
					e.printStackTrace();
				}
				
				return weather;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute( Weather weather )
		{
			dlg.dismiss();
			
			// 날씨 아이콘
			icon = ( ImageView )findViewById( R.id.wicon );
			
			if( weather.condition.getIcon().equalsIgnoreCase( "01d" ) )
				icon.setImageResource( R.drawable.d01 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "01n" ) )
				icon.setImageResource( R.drawable.d01 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "02d" ) )
				icon.setImageResource( R.drawable.d02 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "02n" ) )
				icon.setImageResource( R.drawable.d02 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "03d" ) )
				icon.setImageResource( R.drawable.d03 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "03n" ) )
				icon.setImageResource( R.drawable.d03 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "04d" ) )
				icon.setImageResource( R.drawable.d04 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "04n" ) )
				icon.setImageResource( R.drawable.d04 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "09d" ) )
				icon.setImageResource( R.drawable.d09 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "09n" ) )
				icon.setImageResource( R.drawable.d09 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "10d" ) )
				icon.setImageResource( R.drawable.d10 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "10n" ) )
				icon.setImageResource( R.drawable.d10 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "11d" ) )
				icon.setImageResource( R.drawable.d11 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "11n" ) )
				icon.setImageResource( R.drawable.d11 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "13d" ) )
				icon.setImageResource( R.drawable.d13 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "13n" ) )
				icon.setImageResource( R.drawable.d13 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "50d" ) )
				icon.setImageResource( R.drawable.d50 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "50n" ) )
				icon.setImageResource( R.drawable.d50 );
			
			// 날씨 정보들
			actual = ( TextView )findViewById( R.id.temp1 );
			feels = ( TextView )findViewById( R.id.temp2 );
			temp = ( TextView )findViewById( R.id.temp3 );
			winds = ( TextView )findViewById( R.id.winds );
			pressure = ( TextView )findViewById( R.id.pressure );
			
			String windsUnit = prefs.getString( "prefWindsUnit" , "mps" );			
			String weatherUnit = prefs.getString( "prefWeatherUnit" , "celsius" );
			double c = ( weatherUnit.equalsIgnoreCase( "celsius" ) ) ? weather.temperature.getTemp() : ( ( weather.temperature.getTemp() - 32 ) / 1.8 );
						
			double actualTemp = weather.temperature.getTemp();
			double feelsLike = Math.round( 13.12 + 0.6215 * c - 11.37 * Math.pow( 3.6 * weather.wind.getSpeed() , 0.16 ) + 0.3965 * ( Math.pow( 3.6 * weather.wind.getSpeed() , 0.16 ) * c ) );
			double minTemp = weather.temperature.getMinTemp();
			double maxTemp = weather.temperature.getMaxTemp();
			double windSpeed = ( windsUnit.equalsIgnoreCase( "mps" ) ) ? weather.wind.getSpeed() : 3.6 * weather.wind.getSpeed();
			String wind = String.format( "%.0f", windSpeed ) + ( ( windsUnit.equalsIgnoreCase( "mps" ) ) ? "m/s" : "km/h" );
			double press = weather.condition.getPressure();
			
			actual.setText( String.format( "%.0f", actualTemp ) + "°" );
			feels.setText( String.format( "%.0f", feelsLike ) + "°" );
			temp.setText( String.format( "%.0f", minTemp ) + "° / " + String.format( "%.0f", maxTemp ) + "°" );
			winds.setText( wind );
			pressure.setText( String.format( "%.0f", press ) + " hPa" );
			
			/*
			 * 패션 코디 추천
			 */
			int recommanedColor[] = {
					0xffff0000 , 0xffff7f00 ,
					0xffffff00 , 0xff00ff00 ,
					0xff0000ff , 0xff4b0082 ,
					0xff8f00ff , 0xffff00ff ,
					0xff444444 , 0xff888888
					};
			
			ImageView shirts = ( ImageView )findViewById( R.id.shirts );
			ImageView pants = ( ImageView )findViewById( R.id.pants );
			ImageView socks = ( ImageView )findViewById( R.id.socks );
			
			ImageView acc1 = ( ImageView )findViewById( R.id.accessory1 );
			ImageView acc3 = ( ImageView )findViewById( R.id.accessory3 );
		
			String sex = prefs.getString( "prefSex" , "male" );
			
			if( sex.equalsIgnoreCase( "male" ) )
			{
				if( feelsLike >= 16.0 )
				{
					shirts.setImageResource( R.drawable.male_shirts );
					pants.setImageResource( R.drawable.male_pants );
				}
				else
				{
					shirts.setImageResource( R.drawable.male_shirts_long );
					pants.setImageResource( R.drawable.male_pants_long );
				}
				
				socks.setImageResource( R.drawable.male_socks );
			}
			else
			{
				if( feelsLike >= 16.0 )
				{
					shirts.setImageResource( R.drawable.female_shirts );
					pants.setImageResource( R.drawable.female_pants );
				}
				else
				{
					shirts.setImageResource( R.drawable.female_shirts_long );
					pants.setImageResource( R.drawable.female_pants_long );
				}
				
				socks.setImageResource( R.drawable.female_socks );
			}
			
			// 태양이 비추는 날에는 선글라스 추천
			if( weather.condition.getIcon().equalsIgnoreCase( "01d" ) ||
				weather.condition.getIcon().equalsIgnoreCase( "01n" ) )
			{
				acc1.setImageResource( ( sex.equalsIgnoreCase( "male" ) ) ? R.drawable.male_sunglasses : R.drawable.female_sunglasses );
				acc1.setColorFilter( Color.WHITE );
			}
			else
				acc1.setImageResource( R.drawable.male_blank );
							
			// 비가 오는 날에 우산 추천
			if( weather.condition.getIcon().equalsIgnoreCase( "09d" ) ||
				weather.condition.getIcon().equalsIgnoreCase( "09n" ) ||
				weather.condition.getIcon().equalsIgnoreCase( "10d" ) ||
				weather.condition.getIcon().equalsIgnoreCase( "10n" ) ||
				weather.condition.getIcon().equalsIgnoreCase( "11d" ) ||
				weather.condition.getIcon().equalsIgnoreCase( "11n" ) )
				acc3.setImageResource( ( sex.equalsIgnoreCase( "male" ) ) ? R.drawable.male_umbrella : R.drawable.female_umbrella );
			
			Random random = new Random();
			shirts.setColorFilter( recommanedColor[ random.nextInt( recommanedColor.length ) ] );
			pants.setColorFilter( recommanedColor[ random.nextInt( recommanedColor.length ) ] );
			socks.setColorFilter( recommanedColor[ random.nextInt( recommanedColor.length ) ] );
			acc3.setColorFilter( recommanedColor[ random.nextInt( recommanedColor.length ) ] );
			
			/*
			 * 노티피케이션 알림
			 */
			NotificationManager notificationManager = ( NotificationManager )getSystemService( Context.NOTIFICATION_SERVICE );			
			PendingIntent pendingIntent = PendingIntent.getActivity( getApplicationContext() , 0 , new Intent( getApplicationContext() , MainActivity.class ) , PendingIntent.FLAG_UPDATE_CURRENT );
			
			boolean alert1 = prefs.getBoolean( "prefRain" , false );
			boolean alert2 = prefs.getBoolean( "prefSnow" , false );
			boolean alert3 = prefs.getBoolean( "prefDis" , false );
			boolean alert4 = prefs.getBoolean( "prefPut" , false );
			
			// 비가 옴 (우산 챙기라는 알림)
			if( weather.condition.getIcon().equalsIgnoreCase( "09d" ) ||
					weather.condition.getIcon().equalsIgnoreCase( "09n" ) ||
					weather.condition.getIcon().equalsIgnoreCase( "10d" ) ||
					weather.condition.getIcon().equalsIgnoreCase( "10n" ) ||
					weather.condition.getIcon().equalsIgnoreCase( "11d" ) ||
					weather.condition.getIcon().equalsIgnoreCase( "11n" ) )
			{
				Notification rainAlert = new NotificationCompat.Builder( getApplicationContext() )
				  .setContentTitle( "누나의 일기예보" )
				  .setContentText( "비가 오고 있습니다. 우산을 챙기세요!" )
				  .setSmallIcon( R.drawable.ic_action_cloud )
				  .setAutoCancel( false )
				  .setContentIntent( pendingIntent )
				  .build();
				
				if( alert1 == true )
					notificationManager.notify( 1 , rainAlert );
			}
			
			// 눈이 옴 (눈 조심하세요 알림)
			if( weather.condition.getIcon().equalsIgnoreCase( "13d" ) ||
					weather.condition.getIcon().equalsIgnoreCase( "13n" ) )
			{
				Notification snowAlert = new NotificationCompat.Builder( getApplicationContext() )
				  .setContentTitle( "누나의 일기예보" )
				  .setContentText( "눈이 내리고 있습니다. 미끄러지지 않도록 조심하세요!" )
				  .setSmallIcon( R.drawable.ic_action_cloud )
				  .setAutoCancel( false )
				  .setContentIntent( pendingIntent )
				  .build();
	
				if( alert2 == true )
					notificationManager.notify( 2 , snowAlert );
			}
			
			// 불쾌지수가 높음
			float humidity = weather.condition.getHumidity();
			double discomfort = ( 9 / 5 * c ) - 0.55 * ( 1 - humidity ) * ( 9 / 5 * c - 26 ) + 32;
			
			if( discomfort >= 5 )
			{
				Notification disAlert = new NotificationCompat.Builder( getApplicationContext() )
				  .setContentTitle( "누나의 일기예보" )
				  .setContentText( "불쾌지수가 높습니다. 주변 사람들과 일상생활 시 주의하세요!" )
				  .setSmallIcon( R.drawable.ic_action_person )
				  .setAutoCancel( false )
				  .setContentIntent( pendingIntent )
				  .build();
				
				if( alert3 == true )
					notificationManager.notify( 3 , disAlert );
			}
			
			// 부패지수가 높음
			double putrefaction = ( ( humidity - 65 ) / 14 ) * Math.pow( 1.054 , c );
			
			if( putrefaction >= 75 )
			{
				Notification putAlert = new NotificationCompat.Builder( getApplicationContext() )
				  .setContentTitle( "누나의 일기예보" )
				  .setContentText( "부패지수가 높습니다. 음식물 섭취에 주의하세요!" )
				  .setSmallIcon( R.drawable.ic_action_bad )
				  .setAutoCancel( false )
				  .setContentIntent( pendingIntent )
				  .build();
				
				if( alert4 == true )
					notificationManager.notify( 4 , putAlert );
			}
			
			super.onPostExecute( weather );
		}
		
		@Override
		protected void onCancelled() 
		{
			dlg.dismiss();
			
			super.onCancelled();
		}
	}
	
    public static class PlaceholderFragment extends Fragment
    {
        public PlaceholderFragment() {}

        @Override
        public View onCreateView( LayoutInflater inflater , ViewGroup container , Bundle savedInstanceState )
        {
            View rootView = inflater.inflate( R.layout.fragment_main , container , false );
            
            return rootView;
        }
    }
}

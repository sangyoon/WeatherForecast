package com.leesangyoon.forecast;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.leesangyoon.forecast.helper.WeatherHttpClient;
import com.leesangyoon.forecast.helper.WeatherJSONParser;
import com.leesangyoon.forecast.model.Weather;
import com.leesangyoon.forecast.service.LocationService;

public class WeatherActivity extends ListActivity
{
	private LocationManager locationManager = null;
	private LocationReceiver locationReceiver = null;
	private String locationProvider = null;
	
	double latitude = 0.0;
	double longitude = 0.0;
	
	DailyTask task = null;
	List< Weather > list = new ArrayList< Weather >();
	
	SharedPreferences prefs = null;
	
	@Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setListAdapter( new WeatherAdapter( getApplicationContext() , list ) );
        
        getActionBar().setDisplayHomeAsUpEnabled( true );
        
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
        
        prefs = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
        String unit = prefs.getString( "prefWeatherUnit" , "celsius" );
    	unit = ( unit.equalsIgnoreCase( "celsius" ) ) ? "metric" : "";
    	
        task = new DailyTask();
        task.execute( String.valueOf( latitude ) , String.valueOf( longitude ) , unit );
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
    	super.onResume();
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
		getMenuInflater().inflate( R.menu.weather , menu );
	    
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.menu_weather_refresh:		        
		        String unit = prefs.getString( "prefWeatherUnit" , "celsius" );
		    	unit = ( unit.equalsIgnoreCase( "celsius" ) ) ? "metric" : "";
		    	
		    	task.cancel( true );
				
		        task = new DailyTask();
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
	
	private class DailyTask extends AsyncTask< String , Void , List< Weather > >
	{
		private ProgressDialog dlg = null;
		
		@Override
        protected void onPreExecute()
		{
            super.onPreExecute();
            
            dlg = ProgressDialog.show( WeatherActivity.this , "데이터 로딩중" , "날씨 데이터를 불러오고 있습니다." , true );
        }
		
		@Override
		protected List< Weather > doInBackground( String... params )
		{
			if( !isCancelled() )
			{				
				List< Weather > weatherList = new ArrayList< Weather >();
				
		    	String cnt = prefs.getString( "prefDaily" , "7" );				
				String data = ( new WeatherHttpClient() ).getDailyData( Integer.parseInt( cnt ) , Float.parseFloat( params[0] ) , Float.parseFloat( params[1] ) , params[2] );
				
				try
				{
					weatherList = WeatherJSONParser.getDaily( data );
				}
				catch( Exception e )
				{				
					e.printStackTrace();
				}
				
				return weatherList;
			}
			
			return null;
		}
		
		protected void onPostExecute( List< Weather > list )
		{
			dlg.dismiss();
			setListAdapter( new WeatherAdapter( getApplicationContext() , list ) );
			
			super.onPostExecute( list );			
		}
		
		@Override
		protected void onCancelled() 
		{
			dlg.dismiss();
			
			super.onCancelled();
		}
	}
	
	private class WeatherAdapter extends BaseAdapter
	{
		private Context context;
		private List< Weather > item;
		
		public WeatherAdapter( Context context , List< Weather > item )
		{
			this.context = context;
			this.item = item;
		}

		@Override
		public int getCount()
		{			
			return item.size();
		}

		@Override
		public Object getItem( int position )
		{
			return item.get( position );
		}

		@Override
		public long getItemId( int position )
		{
			return item.indexOf( getItem( position ) );
		}

		@Override
		public View getView( int position , View convertView , ViewGroup parent )
		{
			if( convertView == null )
			{
				LayoutInflater inflater = ( LayoutInflater )context.getSystemService( LAYOUT_INFLATER_SERVICE );
				
				convertView = inflater.inflate( R.layout.list , null );
			}
			
			Weather weather = item.get( position );
			
			ImageView weatherIcon = ( ImageView )convertView.findViewById( R.id.weather_icon );
			TextView weatherText = ( TextView )convertView.findViewById( R.id.weather_desc );
			TextView weatherTemp = ( TextView )convertView.findViewById( R.id.weather_temp );
						
			if( weather.condition.getIcon().equalsIgnoreCase( "01d" ) )
				weatherIcon.setImageResource( R.drawable.d01 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "01n" ) )
				weatherIcon.setImageResource( R.drawable.d01 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "02d" ) )
				weatherIcon.setImageResource( R.drawable.d02 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "02n" ) )
				weatherIcon.setImageResource( R.drawable.d02 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "03d" ) )
				weatherIcon.setImageResource( R.drawable.d03 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "03n" ) )
				weatherIcon.setImageResource( R.drawable.d03 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "04d" ) )
				weatherIcon.setImageResource( R.drawable.d04 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "04n" ) )
				weatherIcon.setImageResource( R.drawable.d04 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "09d" ) )
				weatherIcon.setImageResource( R.drawable.d09 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "09n" ) )
				weatherIcon.setImageResource( R.drawable.d09 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "10d" ) )
				weatherIcon.setImageResource( R.drawable.d10 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "10n" ) )
				weatherIcon.setImageResource( R.drawable.d10 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "11d" ) )
				weatherIcon.setImageResource( R.drawable.d11 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "11n" ) )
				weatherIcon.setImageResource( R.drawable.d11 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "13d" ) )
				weatherIcon.setImageResource( R.drawable.d13 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "13n" ) )
				weatherIcon.setImageResource( R.drawable.d13 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "50d" ) )
				weatherIcon.setImageResource( R.drawable.d50 );
			else if( weather.condition.getIcon().equalsIgnoreCase( "50n" ) )
				weatherIcon.setImageResource( R.drawable.d50 );
			
			weatherText.setText( weather.condition.getCondition() );
			weatherTemp.setText( String.format( "%.0f", weather.temperature.getTemp() ) + "°" );
			
			return convertView;
		}
	}
}

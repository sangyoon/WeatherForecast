package com.leesangyoon.forecast.service;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.leesangyoon.forecast.helper.Constants;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class LocationService extends Service implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, com.google.android.gms.location.LocationListener
{
	public static final String ACTION = "BACKGROUND_LOCATION_SERVICE";
	
	IBinder localBinder = new LocalBinder();
	
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	
	private boolean inProgress;
	private boolean servicesAvailable = false;
	
	private double latitude = 0.0d;
	private double longitude = 0.0d;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		inProgress = false;
		
		locationRequest = LocationRequest.create();
		locationRequest.setPriority( LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY );
		locationRequest.setInterval( Constants.UPDATE_INTERVAL );
		locationRequest.setFastestInterval( Constants.FASTEST_INTERVAL );
		
		servicesAvailable = servicesConnected();
		
		locationClient = new LocationClient( this , this , this );
	}	
	
	@Override
	public int onStartCommand( Intent intent , int flags , int startId )
	{
		super.onStartCommand( intent , flags , startId );
		
		if( !servicesAvailable || locationClient.isConnected() || inProgress )
			return START_STICKY;
		
		setupLocationClientIfNeeded();		
		
		if( !locationClient.isConnected() || !locationClient.isConnecting() && !inProgress )
		{
			inProgress = true;
			
			locationClient.connect();
		}
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy()
	{
		try
		{
			inProgress = false;
			
			if( servicesAvailable && locationClient != null )
			{
				locationClient.removeLocationUpdates( this );
				locationClient = null;
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		
		super.onDestroy();
	}
	
	@Override
	public void onLocationChanged( Location location )
	{
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		
		Intent intent = new Intent();
		
		intent.setAction( ACTION );
		intent.putExtra( "latitude" , latitude );
		intent.putExtra( "longitude" , longitude );
		
		sendBroadcast( intent );
	}
	
	@Override
	public IBinder onBind( Intent intent )
	{
		return localBinder;
	}
	
	@Override
	public void onConnected( Bundle bundle )
	{
		locationClient.requestLocationUpdates( locationRequest , this );
	}
	
	@Override
	public void onDisconnected()
	{
		inProgress = false;
		locationClient = null;
	}
	
	@Override
	public void onConnectionFailed( ConnectionResult connectionResult )
	{
		if( connectionResult.hasResolution() )
		{			
		}
		else
		{
		}
	}

	@Override
	public void onProviderDisabled( String provider )
	{		
	}

	@Override
	public void onProviderEnabled( String provider )
	{	
	}

	@Override
	public void onStatusChanged( String provider , int status , Bundle extras )
	{		
	}
	
	private boolean servicesConnected()
	{
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable( this );
		
		if( ConnectionResult.SUCCESS == resultCode )
			return true;
		else
			return false;
	}
	
	private void setupLocationClientIfNeeded()
	{
		if( locationClient == null )
			locationClient = new LocationClient( this , this , this );
	}
	
	public class LocalBinder extends Binder
	{
		public LocationService getInstance()
		{
			return LocationService.this;
		}
	}
}
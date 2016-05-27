package com.leesangyoon.forecast.helper;

import com.leesangyoon.forecast.helper.HttpRequest;

public class WeatherHttpClient
{
	public String getWeatherData( double latitude , double longitude , String unit )
	{
		String url = WEATHER_DATA_PROVIDER + "?lat=" + latitude + "&lon=" + longitude + "&units=" + unit;
		
		try
		{
			HttpRequest request = HttpRequest.get( url );
		
			if( request.ok() )
				return request.body().toString();
			else
				return null;
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String getDailyData( int count , double latitude , double longitude , String unit )
	{
		String url = WEATHER_DAILY_DATA_PROVIDER + "?lat=" + latitude + "&lon=" + longitude + "&cnt=" + count + "&units=" + unit;
		
		try
		{
			HttpRequest request = HttpRequest.get( url );
		
			if( request.ok() )
				return request.body().toString();
			else
				return null;
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static String WEATHER_DATA_PROVIDER = "http://api.openweathermap.org/data/2.5/weather";
	private static String WEATHER_DAILY_DATA_PROVIDER = "http://api.openweathermap.org/data/2.5/forecast/daily";
}

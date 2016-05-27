package com.leesangyoon.forecast.helper;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.leesangyoon.forecast.model.Location;
import com.leesangyoon.forecast.model.Weather;

public class WeatherJSONParser
{
	public static Weather getWeather( String data ) throws JSONException
	{
		Weather weather = new Weather();

		JSONObject jObject = new JSONObject( data );

		Location location = new Location();

		JSONObject coord = getObject( "coord" , jObject );
		location.setLatitude( getFloat( "lat" , coord ) );
		location.setLongitude( getFloat( "lon" , coord ) );

		JSONObject sys = getObject( "sys" , jObject );
		location.setCountry( getString( "country" , sys ) );
		location.setSunrise( getInt( "sunrise" , sys ) );
		location.setSunset( getInt( "sunset" , sys ) );
		location.setCity( getString( "name" , jObject ) );
		
		weather.location = location;

		JSONArray jArray = jObject.getJSONArray( "weather" );

		JSONObject JSONWeather = jArray.getJSONObject( 0 );
		weather.condition.setWeatherId( getInt( "id" , JSONWeather ) );
		weather.condition.setDesc( getString( "description" , JSONWeather ) );
		weather.condition.setCondition( getString( "main" , JSONWeather ) );
		weather.condition.setIcon( getString( "icon" , JSONWeather ) );

		JSONObject main = getObject( "main", jObject );
		weather.condition.setHumidity( getInt( "humidity" , main ) );
		weather.condition.setPressure( getInt( "pressure" , main ) );
		weather.temperature.setMaxTemp( getFloat( "temp_max" , main ) );
		weather.temperature.setMinTemp( getFloat( "temp_min" , main  ));
		weather.temperature.setTemp( getFloat( "temp" , main ) );

		JSONObject wind = getObject( "wind" , jObject );
		weather.wind.setSpeed( getFloat( "speed" , wind ) );
		weather.wind.setDeg( getFloat( "deg" , wind ) );

		JSONObject clouds = getObject( "clouds" , jObject );
		weather.clouds.setPerc( getInt( "all" , clouds ) );

		return weather;
	}
	
	public static List< Weather > getDaily( String data ) throws JSONException
	{
		List< Weather > weatherList = new ArrayList< Weather >();
		
		JSONObject jObject = new JSONObject( data );
		
		JSONArray jArray = jObject.getJSONArray( "list" );		
		for( int i = 0 ; i < jArray.length() ; ++i )
		{
			Weather weather = new Weather();
						
			JSONObject list = jArray.getJSONObject( i );
			
			JSONObject temp = getObject( "temp", list );						
			weather.temperature.setTemp( getFloat( "day" , temp) );
			weather.temperature.setMinTemp( getFloat( "min" , temp) );
			weather.temperature.setMaxTemp( getFloat( "max" , temp) );
			
			weather.condition.setPressure( getFloat( "pressure" , list ) );
			weather.condition.setHumidity( getFloat( "humidity" , list ) );
			
			weather.clouds.setPerc( getInt( "clouds" , list ) );
			
			weather.wind.setDeg( getFloat( "deg" , list ) );
			weather.wind.setSpeed( getFloat( "speed" , list ) );
			
			JSONArray weatherArray = list.getJSONArray( "weather" );
			JSONObject JSONWeather = weatherArray.getJSONObject( 0 );
			weather.condition.setWeatherId( getInt( "id" , JSONWeather ) );
			weather.condition.setDesc( getString( "description" , JSONWeather ) );
			weather.condition.setCondition( getString( "main" , JSONWeather ) );
			weather.condition.setIcon( getString( "icon" , JSONWeather ) );
			
			weatherList.add( weather );
		}
		
		return weatherList;
	}

	private static JSONObject getObject( String tagName , JSONObject jObject )  throws JSONException
	{
		return jObject.getJSONObject( tagName );
	}

	private static String getString( String tagName , JSONObject jObject ) throws JSONException
	{
		return jObject.getString( tagName );
	}

	private static float  getFloat( String tagName , JSONObject jObject ) throws JSONException
	{
		return ( float )( jObject.getDouble( tagName ) );
	}

	private static int  getInt( String tagName , JSONObject jObject ) throws JSONException
	{
		return jObject.getInt( tagName );
	}
}

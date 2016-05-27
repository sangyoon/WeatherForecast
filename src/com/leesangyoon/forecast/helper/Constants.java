package com.leesangyoon.forecast.helper;

public final class Constants
{
	private static final int MILLISECONDS_PER_SECOND = 100;	
	private static final int UPDATE_INTERVAL_IN_SECONDS = 10;	
	public static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
	private static final int FASTEST_INTERVAL_IN_SECONDS = 10;
	public static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
	public static final String LOCATION_FILE = "sdcard/location.txt";
	public static final String LOG_FILE = "sdcard/log.txt";
	public static final float MINIMUM_DISTANCE_ACCOUNTABLE = 50;

	private Constants()
	{
	    throw new AssertionError();
	}
}
package com.leesangyoon.forecast;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingActivity extends Activity
{	 
	 @Override
	 protected void onCreate( Bundle savedInstanceState )
	 {
		 super.onCreate( savedInstanceState );
		 
		 getFragmentManager().beginTransaction()
		 					 .replace( android.R.id.content , new SettingFragment() )
		 					 .commit();
		 
		 getActionBar().setDisplayHomeAsUpEnabled( true );
	 }	 
	 
	 public static class SettingFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
	 {		 
	 	@Override
	 	public void onCreate( Bundle savedInstanceState )
	 	{
	 		super.onCreate( savedInstanceState );
	 		addPreferencesFromResource( R.xml.setting );
	 		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener( this );
	 	}

	 	@Override
	 	public void onSharedPreferenceChanged( SharedPreferences sharedPreferences , String key )
	 	{
	 	}
	 }
}
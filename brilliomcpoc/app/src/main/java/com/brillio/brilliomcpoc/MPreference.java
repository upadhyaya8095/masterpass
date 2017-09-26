package com.brillio.brilliomcpoc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class MPreference {
	private static final String PREF_NAME = "mpref";
	private SharedPreferences sharedPreferences;
	private Editor editor;
	private Context mContext;
	private int PRIVATE_MODE = 0;
	public static final String FINGER_PRINT_STATUS = "finerprintstatus";
	// Constructor
	public MPreference(Context context) {
		this.mContext = context;
		sharedPreferences = mContext.getSharedPreferences(PREF_NAME,
				PRIVATE_MODE);
		editor = sharedPreferences.edit();
	}

	public void setTimeStamp(long invalidUserName) {
		editor.putLong(FINGER_PRINT_STATUS, invalidUserName);
		editor.commit();
		editor.apply();
	}


	public Long getTimeStamp() {
		return sharedPreferences.getLong(FINGER_PRINT_STATUS, 0);
	}

}

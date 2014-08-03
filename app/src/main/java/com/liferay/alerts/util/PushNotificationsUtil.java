/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.alerts.util;

import android.app.Activity;

import android.content.Context;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.liferay.alerts.R;
import com.liferay.alerts.callback.RegistrationCallback;
import com.liferay.mobile.android.service.Session;
import com.liferay.mobile.android.service.SessionImpl;
import com.liferay.mobile.android.v62.pushnotificationsdevice.PushNotificationsDeviceService;

/**
 * @author Bruno Farache
 */
public class PushNotificationsUtil {

	public static GoogleCloudMessaging getGoogleCloudMessaging(
		Context context) {

		return GoogleCloudMessaging.getInstance(context);
	}

	public static boolean isGooglePlayServicesAvailable(Activity activity) {
		int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(
			activity);

		if (result == ConnectionResult.SUCCESS) {
			return true;
		}

		if (GooglePlayServicesUtil.isUserRecoverableError(result)) {
			GooglePlayServicesUtil.getErrorDialog(
				result, activity, 9000).show();
		}
		else {
			Log.i(_TAG, "This device is not supported.");
			activity.finish();
		}

		return false;
	}

	public static void register(Context context, String token) {
		Session session = new SessionImpl(SettingsUtil.getServer(context));
		session.setCallback(new RegistrationCallback(context));

		PushNotificationsDeviceService service =
			new PushNotificationsDeviceService(session);

		try {
			service.addPushNotificationsDevice(token, "android");
		}
		catch (Exception e) {
			ToastUtil.show(context, R.string.failed_to_register, true);
		}
	}

	private static final String _TAG =
		PushNotificationsUtil.class.getSimpleName();

}
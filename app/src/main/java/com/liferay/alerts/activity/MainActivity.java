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

package com.liferay.alerts.activity;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import android.os.Bundle;

import android.support.v4.content.LocalBroadcastManager;

import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.CycleInterpolator;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.liferay.alerts.R;
import com.liferay.alerts.database.AlertDAO;
import com.liferay.alerts.model.Alert;
import com.liferay.alerts.task.GCMRegistrationAsyncTask;
import com.liferay.alerts.util.GCMUtil;
import com.liferay.alerts.util.NotificationUtil;
import com.liferay.alerts.util.SettingsUtil;
import com.liferay.alerts.widget.card.CardViewContainer;
import com.liferay.mobile.android.util.Validator;

import java.util.ArrayList;

/**
 * @author Bruno Farache
 */
public class MainActivity extends Activity {

	public static final String ACTION_ADD_CARD = "ACTION_ADD_CARD";

	public static final String ACTION_ENABLE_POLLS_QUESTION =
		"ACTION_ENABLE_POLLS_QUESTION";

	public static final String EXTRA_ALERT = "EXTRA_ALERT";

	public static final String EXTRA_CHOICE_ID = "EXTRA_CHOICE_ID";

	public static final String EXTRA_ENABLED = "EXTRA_ENABLED";

	public static final String EXTRA_QUESTION_ID = "EXTRA_QUESTION_ID";

	public static void addCard(Context context, Alert alert) {
		Intent intent = new Intent(ACTION_ADD_CARD);
		intent.putExtra(EXTRA_ALERT, alert);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	public static void enablePollsQuestion(
		Context context, int questionId, int choiceId, boolean enabled) {

		Intent intent = new Intent(ACTION_ENABLE_POLLS_QUESTION);
		intent.putExtra(EXTRA_QUESTION_ID, questionId);
		intent.putExtra(EXTRA_CHOICE_ID, choiceId);
		intent.putExtra(EXTRA_ENABLED, enabled);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	public void enablePollsQuestion(
		int questionId, int choiceId, boolean enabled) {

		RadioGroup group = (RadioGroup)findViewById(questionId);

		if (group == null) {
			return;
		}

		for (int i = 0; i < group.getChildCount(); i++) {
			RadioButton choice = (RadioButton)group.getChildAt(i);

			if (choice.getId() == choiceId) {
				choice.setChecked(true);
			}

			choice.setEnabled(enabled);
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.main);

		_cardList = (LinearLayout)findViewById(R.id.card_list);
		_send = (TextView)findViewById(R.id.send);
		_topBarIcon = (ImageView)findViewById(R.id.top_bar_icon);
		_userName = (TextSwitcher)findViewById(R.id.user_name);

		_send.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Intent intent = new Intent(
					MainActivity.this, SendActivity.class);

				startActivity(intent);
			}

		});

		_topBarIcon.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				MainActivity.this._animateTopBarIcon();
			}

		});

		_userName.setInAnimation(
			AnimationUtils.loadAnimation(
				this, R.anim.top_bar_user_name_slide_in));

		_userName.setOutAnimation(
			AnimationUtils.loadAnimation(
				this, R.anim.top_bar_user_name_slide_out));

		try {
			TextView version = (TextView)findViewById(R.id.version);
			PackageInfo info = getPackageManager().getPackageInfo(
				getPackageName(), 0);

			version.setText(info.versionName);
		}
		catch (PackageManager.NameNotFoundException nnfe) {
		}

		if (state != null) {
			_alerts = state.getParcelableArrayList(_ALERTS);
		}
		else {
			_alerts = AlertDAO.getInstance(this).getInstance(this).get();
		}

		for (Alert alert : _alerts) {
			_addCard(alert, false);
		}

		_addPushNotificationsDevice();
		_registerAddCardReceiver();
		_checkSendPermission();
	}

	@Override
	protected void onDestroy() {
		_getBroadcastManager().unregisterReceiver(_receiver);

		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();

		_paused = true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		_paused = false;

		NotificationUtil.cancel(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);

		state.putParcelableArrayList(_ALERTS, _alerts);
	}

	private void _addCard(Alert alert, boolean animate) {
		_cardList.addView(new CardViewContainer(this, alert), 0);

		String userName = alert.getUser(this).getFullName();

		if (animate) {
			_animateTopBarIcon();

			String currentUserName =
				((TextView)_userName.getCurrentView()).getText().toString();

			if (!currentUserName.equals(userName)) {
				_userName.setText(userName);
			}
		}
		else {
			_userName.setCurrentText(userName);
		}
	}

	private void _addPushNotificationsDevice() {
		if (!SettingsUtil.isRegistered() &&
			GCMUtil.isGooglePlayServicesAvailable(this)) {

			String token = SettingsUtil.getToken();

			if (token.isEmpty()) {
				GCMRegistrationAsyncTask task = new GCMRegistrationAsyncTask(
					this);

				task.execute();
			}
			else {
				GCMUtil.addPushNotificationsDevice(this, token);
			}
		}
	}

	private void _animateTopBarIcon() {
		if (_topBarIconAnimation == null) {
			Resources resources = getResources();
			long duration = resources.getInteger(
				R.integer.top_bar_icon_animation_duration);

			_topBarIconAnimation = (AnimatorSet)AnimatorInflater.loadAnimator(
				this, R.animator.top_bar_icon_scale);

			_topBarIconAnimation.setTarget(_topBarIcon);
			_topBarIconAnimation.setDuration(duration);
			_topBarIconAnimation.setInterpolator(new CycleInterpolator(2));
		}

		if (!_topBarIconAnimation.isStarted()) {
			_topBarIconAnimation.start();
		}
	}

	private void _checkSendPermission() {
		String email = SettingsUtil.getEmail(this);
		String password = SettingsUtil.getPassword(this);

		if (Validator.isNull(email) || Validator.isNull(password)) {
			return;
		}

		_send.setVisibility(View.VISIBLE);
	}

	private LocalBroadcastManager _getBroadcastManager() {
		return LocalBroadcastManager.getInstance(this);
	}

	private void _registerAddCardReceiver() {
		_receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (ACTION_ADD_CARD.equals(action)) {
					Alert alert = intent.getParcelableExtra(EXTRA_ALERT);
					_alerts.add(alert);

					_addCard(alert, true);

					if (!_paused) {
						NotificationUtil.cancel(context);
					}
				}
				else if (ACTION_ENABLE_POLLS_QUESTION.equals(action)) {
					int questionId = intent.getIntExtra(EXTRA_QUESTION_ID, 0);
					int choiceId = intent.getIntExtra(EXTRA_CHOICE_ID, 0);
					boolean enabled = intent.getBooleanExtra(
						EXTRA_ENABLED, false);

					enablePollsQuestion(questionId, choiceId, enabled);
				}
			}

		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_ADD_CARD);
		filter.addAction(ACTION_ENABLE_POLLS_QUESTION);

		_getBroadcastManager().registerReceiver(_receiver, filter);
	}

	private static final String _ALERTS = "alerts";

	private ArrayList<Alert> _alerts;
	private LinearLayout _cardList;
	private boolean _paused;
	private BroadcastReceiver _receiver;
	private TextView _send;
	private ImageView _topBarIcon;
	private AnimatorSet _topBarIconAnimation;
	private TextSwitcher _userName;

}
/*
 * Copyright (C) 2012 Soomla Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.soomla.social.example;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.soomla.blueprint.rewards.Reward;
import com.soomla.social.IAuthProviderAggregator;
import com.soomla.social.IContextProvider;
import com.soomla.social.ISocialProvider;
import com.soomla.social.events.FacebookProfileEvent;
import com.soomla.social.events.SocialProfileEvent;
import com.soomla.social.providers.SoomlaAuthProviderAggregator;
import com.soomla.social.providers.facebook.FacebookSDKProvider;
import com.soomla.social.actions.ISocialAction;
import com.soomla.social.actions.UpdateStatusAction;
import com.soomla.social.actions.UpdateStoryAction;
import com.soomla.social.events.SocialActionPerformedEvent;
import com.soomla.social.events.SocialAuthProfileEvent;
import com.soomla.social.events.SocialLoginEvent;
import com.soomla.social.example.util.ImageUtils;
import com.soomla.social.rewards.SocialVirtualItemReward;
import com.soomla.store.BusProvider;
import com.squareup.otto.Subscribe;

import java.io.UnsupportedEncodingException;


public class MixedExampleActivity extends ActionBarActivity {

    private static final String TAG = "MainSocialActivity";

    private Button mBtnShare;

    private ViewGroup mProfileBar;
    private ImageView mProfileAvatar;
    private TextView mProfileName;

    private ViewGroup mPnlStatusUpdate;
    private Button mBtnUpdateStatus;
    private EditText mEdtStatus;

    private ViewGroup mPnlStoryUpdate;
    private Button mBtnUpdateStory;
    private EditText mEdtStory;

//    private SoomlaSocialAuthCenter soomlaSocialAuthCenter;
    private IAuthProviderAggregator socialProviderFactory;
    private ISocialProvider facebookProvider;
    private IContextProvider ctxProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.socialauth_example_main);

//        socialProviderFactory = new SoomlaSocialAuthProviderFactory();
        socialProviderFactory = new SoomlaAuthProviderAggregator();
//        soomlaSocialAuthCenter.addSocialProvider(ISocialCenter.FACEBOOK, R.drawable.facebook);
        ctxProvider = new IContextProvider() {
            @Override
            public Activity getActivity() {
                return MixedExampleActivity.this;
            }

//            @Override
//            public Fragment getFragment() {
//                return null;
//            }

            @Override
            public Context getContext() {
                return MixedExampleActivity.this;
            }
        };

        facebookProvider = socialProviderFactory.setCurrentProvider(
                ctxProvider, IAuthProviderAggregator.FACEBOOK);

        // important!
        // might be better to use FacebookEnabledActivity/Fragment, still evaluating
        if(facebookProvider instanceof FacebookSDKProvider) {
            ((FacebookSDKProvider) facebookProvider).onCreate(savedInstanceState);
        }

        mProfileBar = (ViewGroup) findViewById(R.id.profile_bar);
        mProfileAvatar = (ImageView) findViewById(R.id.prof_avatar);
        mProfileName = (TextView) findViewById(R.id.prof_name);

        mPnlStatusUpdate = (ViewGroup) findViewById(R.id.pnlStatusUpdate);
        mEdtStatus = (EditText) findViewById(R.id.edtStatusText);
        mBtnUpdateStatus = (Button) findViewById(R.id.btnStatusUpdate);
        mBtnUpdateStatus.setEnabled(false);
        mBtnUpdateStatus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String message = mEdtStatus.getText().toString();

                // create social action
                UpdateStatusAction updateStatusAction = new UpdateStatusAction(
                        IAuthProviderAggregator.FACEBOOK, message, false);

                // optionally attach rewards to it
                Reward noAdsReward = new SocialVirtualItemReward("Update Status for Ad-free", "no_ads", 1);
                updateStatusAction.getRewards().add(noAdsReward);

                // perform social action
                facebookProvider.updateStatusAsync(updateStatusAction);
            }
        });

        mPnlStoryUpdate = (ViewGroup) findViewById(R.id.pnlStoryUpdate);
        mEdtStory = (EditText) findViewById(R.id.edtStoryText);
        mBtnUpdateStory = (Button) findViewById(R.id.btnStoryUpdate);
        mBtnUpdateStory.setEnabled(false);
        mBtnUpdateStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = mEdtStory.getText().toString();
                // another example
                UpdateStoryAction updateStoryAction = new UpdateStoryAction(
                        IAuthProviderAggregator.FACEBOOK,
                        message, "name", "caption", "description",
                        "http://soom.la",
                        "https://s3.amazonaws.com/soomla_images/website/img/500_background.png");

                // optionally attach rewards to it
                Reward muffinsReward = new SocialVirtualItemReward("Update Story for muffins", "muffins_50", 1);
                updateStoryAction.getRewards().add(muffinsReward);

                try {
                    facebookProvider.updateStoryAsync(updateStoryAction);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        mBtnShare = (Button) findViewById(R.id.btnShare);
//        soomlaSocialAuthCenter.registerShareButton(mBtnShare);
        mBtnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookProvider.login();
            }
        });
    }

    @Subscribe public void onSocialLoginEvent(SocialLoginEvent socialLoginEvent) {
        // Variable to receive message status
        Log.d(TAG, "Authentication Successful");

        // Get name of provider after authentication
        String providerName = facebookProvider.getProviderName();
        final Bundle bundle = socialLoginEvent.result;
        // in socialauth may contain some stuff
        if (bundle != null) {
            Log.d(TAG, "onSocialLoginEvent bundle = " + bundle);
            // like this
//            providerName = bundle.getString(SocialAuthAdapter.PROVIDER);
        }
        Log.d(TAG, "Provider Name = " + providerName);
        Toast.makeText(this, providerName + " connected", Toast.LENGTH_SHORT).show();

        // Please avoid sending duplicate message. Social Media Providers
        // block duplicate messages.

        facebookProvider.getProfileAsync();

        updateUIOnLogin(providerName);
    }

    @Subscribe public void onSocialAuthProfileEvent(SocialAuthProfileEvent saProfileEvent) {
        Log.d(TAG, "onSocialAuthProfileEvent");
        showView(mProfileBar, true);

        new ImageUtils.DownloadImageTask(mProfileAvatar).execute(saProfileEvent.User.getProfileImageURL());
        mProfileName.setText(saProfileEvent.User.getFullName());
    }

    @Subscribe public void onFacebookProfileEvent(FacebookProfileEvent fbProfileEvent) {
        Log.d(TAG, "onFacebookProfileEvent");
        showView(mProfileBar, true);

        new ImageUtils.DownloadImageTask(mProfileAvatar).execute(fbProfileEvent.getProfileImageUrl());

        mProfileName.setText(
                fbProfileEvent.User.getFirstName() + " " +
                        fbProfileEvent.User.getLastName()
        );
    }

    // can also use generic profile event
    @Subscribe public void onSocialProfileEvent(SocialProfileEvent socialProfileEvent) {
        Log.d(TAG, "onSocialProfileEvent");
        showView(mProfileBar, true);
        new ImageUtils.DownloadImageTask(mProfileAvatar).execute(socialProfileEvent.Profile.getAvatarLink());
        mProfileName.setText(socialProfileEvent.Profile.getFullName());
    }

    @Subscribe public void onSocialActionPerformedEvent(
            SocialActionPerformedEvent socialActionPerformedEvent) {
        final ISocialAction socialAction = socialActionPerformedEvent.socialAction;
        final String msg = socialAction.getName() + " on " +
                socialAction.getProviderName() + " performed successfully";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateUIOnLogin(final String providerName) {
        mBtnShare.setCompoundDrawablesWithIntrinsicBounds(null, null,
                getResources().getDrawable(android.R.drawable.ic_lock_power_off),
                null);

        mBtnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookProvider.logout();
                updateUIOnLogout();

                // re-enable share button login
//                socialProviderFactory.registerShareButton(mBtnShare);
            }
        });

        showView(mPnlStatusUpdate, true);
        showView(mPnlStoryUpdate, true);

        mBtnUpdateStatus.setEnabled(true);
        mBtnUpdateStory.setEnabled(true);
    }

    private void updateUIOnLogout() {

        mBtnUpdateStatus.setEnabled(false);
        mBtnUpdateStory.setEnabled(false);

        showView(mProfileBar, false);
        showView(mPnlStatusUpdate, false);
        showView(mPnlStoryUpdate, false);

        mProfileAvatar.setImageBitmap(null);
        mProfileName.setText("");

        mBtnShare.setCompoundDrawablesWithIntrinsicBounds(null, null,
                getResources().getDrawable(android.R.drawable.ic_menu_share),
                null);
    }

    private void showView(final View view, boolean show) {
        final Animation animation = show ?
                AnimationUtils.makeInAnimation(view.getContext(), true) :
                AnimationUtils.makeOutAnimation(view.getContext(), true);
        animation.setFillAfter(true);
        animation.setDuration(500);
        view.startAnimation(animation);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(facebookProvider instanceof FacebookSDKProvider) {
            ((FacebookSDKProvider) facebookProvider).onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(facebookProvider instanceof FacebookSDKProvider) {
            ((FacebookSDKProvider) facebookProvider).onResume();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(facebookProvider instanceof FacebookSDKProvider) {
            ((FacebookSDKProvider) facebookProvider).onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(facebookProvider instanceof FacebookSDKProvider) {
            ((FacebookSDKProvider) facebookProvider).onDestroy();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(facebookProvider instanceof FacebookSDKProvider) {
            ((FacebookSDKProvider) facebookProvider).onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        BusProvider.getInstance().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(facebookProvider instanceof FacebookSDKProvider) {
            ((FacebookSDKProvider) facebookProvider).onStop();
        }
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_social, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
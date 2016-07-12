/*
 * Copyright (C) 2012-2014 Soomla Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soomla.example;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.soomla.BusProvider;
import com.soomla.SoomlaUtils;
import com.soomla.profile.SoomlaProfile;
import com.soomla.profile.domain.IProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.events.auth.LoginCancelledEvent;
import com.soomla.profile.events.auth.LoginFailedEvent;
import com.soomla.profile.events.auth.LoginFinishedEvent;
import com.soomla.profile.events.social.GetContactsFinishedEvent;
import com.soomla.profile.events.social.GetFeedFinishedEvent;
import com.soomla.profile.events.social.SocialActionFailedEvent;
import com.soomla.profile.events.social.SocialActionFinishedEvent;
import com.soomla.profile.exceptions.ProviderNotFoundException;
import com.soomla.profile.exceptions.UserProfileNotFoundException;
import com.soomla.rewards.Reward;
import com.soomla.rewards.VirtualItemReward;
import com.squareup.otto.Subscribe;

import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import io.fabric.sdk.android.Fabric;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * This class shows the main activity in which the user can socially interact
 * with different social providers.
 *
 * NOTE: See <code>activity_main_social.xml</code> for activity UI
 */
public class ExampleSocialActivity extends Activity {


    /** Private Members */

    private static final String TAG = "ExampleSocialActivity";

    private static final int SELECT_PHOTO_ACTION = 1;

    private Button mBtnShare;

    private ViewGroup mProfileBar;
    private ImageView mProfileAvatar;
    private TextView mProfileName;

    private ViewGroup mPnlStatusUpdate;
    private Button mBtnUpdateStatus;
    private EditText mEdtStatusText;

    private ViewGroup mPnlStoryUpdate;
    private Button mBtnUpdateStory;
    private EditText mEdtStoryText;

    private ViewGroup mPnlUploadImage;
    private ImageView mBtnChooseImage;
    private Button mBtnUploadImage;
    private EditText mEdtImageText;
    private String mImagePath;
    private ImageView mImagePreview;

    private ProgressDialog mProgressDialog;

    private String mItemId = "cream_cup";
    private String mItemName = "Cup Cup";
    private int mItemAmount = 15;
    private int mItemResId = R.drawable.ic_launcher;

    private IProvider.Provider mProvider = IProvider.Provider.FACEBOOK;

    Reward gameLoginReward = new VirtualItemReward("reward_login", "Login for VG", 15, mItemId);
    Reward gameUpdateStatusReward = new VirtualItemReward("reward_update_status", "Update Status for VG", 25, mItemId);
    Reward gameUpdateStoryReward = new VirtualItemReward("reward_update_story", "Update Story for VG", 35, mItemId);
    Reward gameUploadImageReward = new VirtualItemReward("reward_upload_image", "Upload Image for VG", 45, mItemId);
    Reward gameLikePageReward = new VirtualItemReward("reward_like_page", "Like Page for VG", 105, mItemId);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_social);

//        SoomlaConfig.logDebug = true;

        mProgressDialog = new ProgressDialog(this);

        final Bundle extras = getIntent().getExtras();
        if(extras != null) {
            final String provider = extras.getString("provider");
            mProvider = IProvider.Provider.getEnum(provider);
            mItemId = extras.getString("id");
            mItemAmount = extras.getInt("amount", 1);
            mItemName = extras.getString("name");
            mItemResId = extras.getInt("iconResId", R.drawable.ic_launcher);

            // set the social provider logo if possible
            final int resourceId = getResources().getIdentifier(provider, "drawable", getPackageName());
            Drawable drawableLogo = getResources().getDrawable(resourceId);
            if(drawableLogo != null) {
                final TextView topBarTextView = (TextView) findViewById(R.id.textview);
                if(topBarTextView != null) {
                    topBarTextView.setCompoundDrawablesWithIntrinsicBounds(drawableLogo, null, null, null);
                }
            }
        }

        final TextView vItemDisplay = (TextView) findViewById(R.id.vItem);
        if(vItemDisplay != null) {
            vItemDisplay.setText(mItemName);
            vItemDisplay.setCompoundDrawablesWithIntrinsicBounds(
                    null, getResources().getDrawable(mItemResId), null, null);
        }

        mProfileBar = (ViewGroup) findViewById(R.id.profile_bar);
        mProfileAvatar = (ImageView) findViewById(R.id.prof_avatar);
        mProfileName = (TextView) findViewById(R.id.prof_name);

        mPnlStatusUpdate = (ViewGroup) findViewById(R.id.pnlStatusUpdate);
        mEdtStatusText = (EditText) findViewById(R.id.edtStatusText);

        mEdtStatusText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    doUpdateStatus();
                    handled = true;
                }

                return handled;
            }
        });

        mBtnUpdateStatus = (Button) findViewById(R.id.btnStatusUpdate);
        mBtnUpdateStatus.setEnabled(false);
        mBtnUpdateStatus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                doUpdateStatus();
            }
        });

        mPnlUploadImage = (ViewGroup) findViewById(R.id.pnlUploadImage);
        mImagePreview = (ImageView) findViewById(R.id.imagePreview);
        mEdtImageText = (EditText) findViewById(R.id.edtImageText);

        mEdtImageText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    doUpdateStatus();
                    handled = true;
                }

                return handled;
            }
        });

        mBtnChooseImage = (ImageView) findViewById(R.id.btnChooseImage);
        mBtnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImageFile();
            }
        });

        mBtnUploadImage = (Button) findViewById(R.id.btnUploadImage);
        mBtnUploadImage.setEnabled(false);
        mBtnUploadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                doUploadImage();
            }
        });


        mPnlStoryUpdate = (ViewGroup) findViewById(R.id.pnlStoryUpdate);
        mEdtStoryText = (EditText) findViewById(R.id.edtStoryText);

        mEdtStoryText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    doUpdateStory();
                    handled = true;
                }

                return handled;
            }
        });

        mBtnUpdateStory = (Button) findViewById(R.id.btnStoryUpdate);
        mBtnUpdateStory.setEnabled(false);
        mBtnUpdateStory.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                doUpdateStory();
            }
        });

        mBtnShare = (Button) findViewById(R.id.btnShare);

        if (!SoomlaProfile.getInstance().isLoggedIn(this, mProvider)) {
            SoomlaProfile.getInstance().login(this, mProvider, gameLoginReward);

            mProgressDialog.setMessage("logging in...");
            mProgressDialog.show();
        }
        else {
            applyLoggedInUser(mProvider);
        }
    }

    @Subscribe
    public void onSocialActionFinishedEvent(SocialActionFinishedEvent socialActionFinishedEvent) {
        Log.d(TAG, "SocialActionFinishedEvent:" + socialActionFinishedEvent.SocialActionType.toString());
        Toast.makeText(this,
                "action "+socialActionFinishedEvent.SocialActionType.toString()+" success",
                Toast.LENGTH_SHORT).show();

        mProgressDialog.dismiss();

       switch (socialActionFinishedEvent.SocialActionType) {
           case UPDATE_STATUS: {
               mEdtStatusText.setText("");
               break;
           }
           case UPLOAD_IMAGE: {
               mEdtImageText.setText("");
           }
           case UPDATE_STORY: {
               mEdtStoryText.setText("");
           }
           default: {
               break;
           }
       }
    }

    @Subscribe
    public void onSocialActionFailedEvent(SocialActionFailedEvent socialActionFailedEvent) {
        Log.d(TAG, "SocialActionFailedEvent:" + socialActionFailedEvent.SocialActionType.toString());

        mProgressDialog.dismiss();

        Toast.makeText(this,
                "action "+socialActionFailedEvent.SocialActionType.toString()+" failed: " +
                socialActionFailedEvent.ErrorDescription, Toast.LENGTH_SHORT).show();
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();

        if(mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Subscribe
    public void onLoginFinishedEvent(LoginFinishedEvent loginFinishedEvent) {
        // Variable to receive message status
        Log.d(TAG, "Authentication Successful");

        if(mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        // Get name of provider after authentication
        final IProvider.Provider provider = loginFinishedEvent.getProvider();

        applyLoggedInUser(provider, loginFinishedEvent.UserProfile);

        if (gameLikePageReward.canGive()) {
            SoomlaProfile.getInstance().like(this, provider, "The.SOOMLA.Project", gameLikePageReward);
        }
    }

    private void applyLoggedInUser(final IProvider.Provider provider) {
        UserProfile loggedInProfile = SoomlaProfile.getInstance().getStoredUserProfile(provider);
        applyLoggedInUser(provider, loggedInProfile);
    }

    private void applyLoggedInUser(final IProvider.Provider provider, UserProfile targetProfile) {

        if (targetProfile == null)
        {
            SoomlaUtils.LogWarning(TAG, "Logged-in user profile was not found in " + provider + " provider");
            return;
        }

        Log.d(TAG, "Provider Name = " + provider);
        Toast.makeText(this, provider + " connected", Toast.LENGTH_SHORT).show();

        showView(mProfileBar, true);
        new DownloadImageTask(mProfileAvatar).execute(targetProfile.getAvatarLink());
        if(targetProfile.getFirstName() != null) {
            mProfileName.setText(targetProfile.getFullName());
        }
        else {
            mProfileName.setText(targetProfile.getUsername());
        }

        updateUIOnLogin(provider);

        // TEST
        // todo: it seems that FB no longer simply returns your friends via me/friends
        // todo: need to figure out what's best here
        SoomlaProfile.getInstance().getContacts(provider, null);
        SoomlaProfile.getInstance().getFeed(provider, null);
    }

    @Subscribe
    public void onSocialContactsEvent(GetContactsFinishedEvent contactsFinishedEvent) {
        Log.d(TAG, "GetContactsFinishedEvent");
        final List<UserProfile> contacts = contactsFinishedEvent.Contacts;
        for (UserProfile contact : contacts) {
            Log.d(TAG, "contact:" + contact.toJSONObject().toString());
        }
        if (contactsFinishedEvent.HasMore) {
            SoomlaProfile.getInstance().getContacts(contactsFinishedEvent.Provider, null);
        }
    }

    @Subscribe
    public void onSocialFeedEvent(GetFeedFinishedEvent feedFinishedEvent) {
        Log.d(TAG, "GetFeedFinishedEvent");
        final List<String> posts = feedFinishedEvent.Posts;
        for (String post : posts) {
            Log.d(TAG, "post:" + post);
        }
    }

    @Subscribe
    public void onSocialLoginErrorEvent(LoginFailedEvent loginFailedEvent) {
        if(mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        final String errMsg = "login error:" + loginFailedEvent.ErrorDescription;
        Log.e(TAG, errMsg);

        Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Subscribe
    public void onSocialLoginCancelledEvent(LoginCancelledEvent loginCancelledEvent) {
        if(mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        Toast.makeText(getApplicationContext(), "login cancelled", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void doUpdateStory() {
        // Please avoid sending duplicate message. Social Media Providers
        // block duplicate messages.

        final String message = mEdtStoryText.getText().toString();
        hideSoftKeyboard();
        // create social action
        // perform social action
        mProgressDialog.setMessage("updating status...");
        mProgressDialog.show();
        SoomlaProfile.getInstance().updateStory(mProvider,
                message,
                "The SOOMLA Project",
                "",
                "SOOMLA is a smart, free and open-source cross-platform framework that empowers indie developers’ productivity and drives game success.",
                "http://soom.la",
                "http://about.soom.la/wp-content/uploads/2014/05/330x268-bankerbot.png",
                gameUpdateStoryReward);

        // Or with dialog
        //SoomlaProfile.getInstance().updateStoryDialog(mProvider,
        //        "The SOOMLA Project",
        //        "",
        //        "SOOMLA is a smart, free and open-source cross-platform framework that empowers indie developers’ productivity and drives game success.",
        //        "http://soom.la",
        //        "http://about.soom.la/wp-content/uploads/2014/05/330x268-bankerbot.png",
        //        gameUpdateStoryReward);
    }

    private void doUpdateStatus() {
        // Please avoid sending duplicate message. Social Media Providers
        // block duplicate messages.

        final String message = mEdtStatusText.getText().toString();
        hideSoftKeyboard();
        mProgressDialog.setMessage("updating status...");
        mProgressDialog.show();
        SoomlaProfile.getInstance().updateStatus(mProvider, message, gameUpdateStatusReward);

        // Or with dialog
        // SoomlaProfile.getInstance().updateStatusDialog(mProvider, "http://www.soom.la", gameUpdateStatusReward);
    }

    private void chooseImageFile() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO_ACTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO_ACTION:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        mImagePath = getImagePath(imageUri);
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        mImagePreview.setImageBitmap(selectedImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    private String getImagePath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();

        cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }


    private void doUploadImage() {
        final String message = mEdtImageText.getText().toString();
        hideSoftKeyboard();

        mProgressDialog.setMessage("uploading image...");
        mProgressDialog.show();
        SoomlaProfile.getInstance().uploadImage(mProvider, message, mImagePath, gameUploadImageReward);
    }

    private void updateUIOnLogin(final IProvider.Provider provider) {
        mBtnShare.setCompoundDrawablesWithIntrinsicBounds(null, null,
                getResources().getDrawable(android.R.drawable.ic_lock_power_off),
                null);
        mBtnShare.setVisibility(View.VISIBLE);

        mBtnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoomlaProfile.getInstance().logout(mProvider);
                updateUIOnLogout();
            }
        });

        showView(mPnlStatusUpdate, true);
        showView(mPnlUploadImage, true);
        showView(mPnlStoryUpdate, true);
        mBtnShare.setEnabled(true);

        mBtnUpdateStatus.setEnabled(true);
        mBtnUploadImage.setEnabled(true);
        mBtnUpdateStory.setEnabled(true);
    }

    private void hideSoftKeyboard(){
        if(getCurrentFocus()!=null && getCurrentFocus() instanceof EditText){
            EditText edtCurrentFocusText = (EditText) getCurrentFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edtCurrentFocusText.getWindowToken(), 0);
        }
    }

    private void updateUIOnLogout() {

        mBtnUpdateStatus.setEnabled(false);
        mBtnUploadImage.setEnabled(false);
        mBtnUpdateStory.setEnabled(false);

        showView(mProfileBar, false);
        showView(mPnlStatusUpdate, false);
        showView(mPnlUploadImage, false);
        showView(mPnlStoryUpdate, false);

        mProfileAvatar.setImageBitmap(null);
        mProfileName.setText("");

        mBtnShare.setVisibility(View.INVISIBLE);
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

    /**
     * @{inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();
        BusProvider.getInstance().register(this);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        BusProvider.getInstance().unregister(this);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bmp = downloadBitmapWithClient(url);

            return bmp;
        }

        // doesn't follow https redirect!
        private Bitmap downloadBitmap(String stringUrl) {
            URL url = null;
            HttpURLConnection connection = null;
            InputStream inputStream = null;

            try {
                url = new URL(stringUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setUseCaches(true);
                inputStream = connection.getInputStream();

                return BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
            } catch (Exception e) {
                Log.w(TAG, "Error while retrieving bitmap from " + stringUrl, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return null;
        }

        private Bitmap downloadBitmapWithClient(String url) {
            final AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Android");
            HttpClientParams.setRedirecting(httpClient.getParams(), true);
            final HttpGet request = new HttpGet(url);

            try {
                HttpResponse response = httpClient.execute(request);
                final int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != HttpStatus.SC_OK) {
                    Header[] headers = response.getHeaders("Location");

                    if (headers != null && headers.length != 0) {
                        String newUrl = headers[headers.length - 1].getValue();
                        // call again with new URL
                        return downloadBitmap(newUrl);
                    } else {
                        return null;
                    }
                }

                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream inputStream = null;
                    try {
                        inputStream = entity.getContent();

                        // do your work here
                        return BitmapFactory.decodeStream(inputStream);
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        entity.consumeContent();
                    }
                }
            } catch (Exception e) {
                request.abort();
            } finally {
                if (httpClient != null) {
                    httpClient.close();
                }
            }

            return null;
        }

        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int byteValue = read();
                    if (byteValue < 0) {
                        break; // we reached EOF
                    } else
                    {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
}

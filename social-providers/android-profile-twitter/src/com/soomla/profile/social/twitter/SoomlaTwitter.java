/*
 * Copyright (C) 2012-2014 Soomla Inc.
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

package com.soomla.profile.social.twitter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.os.Debug;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.soomla.SoomlaUtils;
import com.soomla.data.KeyValueStorage;
import com.soomla.profile.auth.AuthCallbacks;
import com.soomla.profile.auth.IAuthProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.social.ISocialProvider;
import com.soomla.profile.social.SocialCallbacks;

import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Soomla wrapper for Twitter4J (unofficial SDK for Twitter API).
 *
 * This class uses the <code>SoomlaTwitterWebView</code> to authenticate.
 * All other operations are performed asynchronously via Twitter4J
 */
public class SoomlaTwitter implements IAuthProvider, ISocialProvider {

    private static final String TAG = "SOOMLA SoomlaTwitter";

    private static final String DB_KEY_PREFIX = "soomla.profile.twitter.";
    private static final String TWITTER_OAUTH_TOKEN = "oauth.token";
    private static final String TWITTER_OAUTH_SECRET = "oauth.secret";
    private static final String TWITTER_SCREEN_NAME = "oauth.screenName";

    private static final String OAUTH_VERIFIER = "oauth_verifier";
    private static final int PAGE_SIZE = 20;

    private boolean autoLogin;

    // some weak refs that are set before launching the wrapper SoomlaTwitterActivity
    // (need to be accessed by static context)
    private static WeakReference<Activity> WeakRefParentActivity;
    private static Provider RefProvider;
    private static AuthCallbacks.LoginListener RefLoginListener;
    private static AuthCallbacks.UserProfileListener RefUserProfileListener;
    private static SocialCallbacks.SocialActionListener RefSocialActionListener;
    private static SocialCallbacks.FeedListener RefFeedListener;
    private static SocialCallbacks.ContactsListener RefContactsListener;

    private String twitterConsumerKey;
    private String twitterConsumerSecret;
    private boolean isInitialized = false;

    private static AsyncTwitter twitter;
    private static String twitterScreenName;
    private static RequestToken mainRequestToken;
    private static boolean actionsListenerAdded = false;
    private static String oauthCallbackURL;

    public static final int ACTION_LOGIN = 0;

    public static final int ACTION_PUBLISH_STATUS = 10;
    public static final int ACTION_PUBLISH_STORY = 11;
    public static final int ACTION_UPLOAD_IMAGE = 12;
    public static final int ACTION_GET_FEED = 13;
    public static final int ACTION_GET_CONTACTS = 14;
    public static final int ACTION_PUBLISH_STATUS_DIALOG = 15;
    public static final int ACTION_PUBLISH_STORY_DIALOG = 16;
    public static final int ACTION_GET_USER_PROFILE = 17;

    private int preformingAction = -1;

    private long lastContactCursor = -1;
    private int lastFeedCursor = 1;

    /**
     * Twitter4J uses an old listener model in which you provide a listener
     * which listens to all possible operations done asynchronously.
     * Here we define all handling of the completion of such operations.
     */
    private TwitterAdapter actionsListener = new TwitterAdapter() {

        /**
         * Called when the request token has arrived from Twitter
         *
         * @param requestToken The request token to use to complete OAuth
         *                     process
         */
        @Override
        public void gotOAuthRequestToken(RequestToken requestToken) {
            mainRequestToken = requestToken;

            Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaTwitterActivity.class);

            intent.putExtra("url", mainRequestToken.getAuthenticationURL());
            WeakRefParentActivity.get().startActivity(intent);

            // Web browser version bad idea (take out of program)
            // startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mainRequestToken.getAuthenticationURL())));
        }

        /**
         * Called when OAuth authentication has been finalized and an Access
         * Token and Access Token Secret have been provided
         *
         * @param accessToken The access token to use to do REST calls
         */
        @Override
        public void gotOAuthAccessToken(AccessToken accessToken) {
            SoomlaUtils.LogDebug(TAG, "login/onComplete");

            twitter.setOAuthAccessToken(accessToken);

            // Keep in storage for logging in without web-authentication
            KeyValueStorage.setValue(getTwitterStorageKey(TWITTER_OAUTH_TOKEN), accessToken.getToken());
            KeyValueStorage.setValue(getTwitterStorageKey(TWITTER_OAUTH_SECRET), accessToken.getTokenSecret());

            // Keep screen name since Twitter4J does not have it when
            // logging in using authenticated tokens
            KeyValueStorage.setValue(getTwitterStorageKey(TWITTER_SCREEN_NAME), accessToken.getScreenName());

            twitterScreenName = accessToken.getScreenName();

            RefLoginListener.success(RefProvider);

            clearListener(ACTION_LOGIN);
        }

        /**
         * Called when a user's information has arrived from twitter
         *
         * @param user The user's details
         */
        @Override
        public void gotUserDetail(User user) {
            SoomlaUtils.LogDebug(TAG, "getUserProfile/onComplete");
            UserProfile userProfile = createUserProfile(user, true);

            RefUserProfileListener.success(userProfile);

            clearListener(ACTION_GET_USER_PROFILE);
        }

        /**
         * Called when the user's timeline has arrived
         *
         * @param statuses The user's latest statuses
         */
        @Override
        public void gotUserTimeline(ResponseList<Status> statuses) {
            SoomlaUtils.LogDebug(TAG, "getFeed/onComplete");


            List<String> feeds = new ArrayList<String>();
            for (Status post : statuses) {
                feeds.add(post.getText());
            }

            boolean hasMore;
            if (feeds.size() >= PAGE_SIZE) {
                lastFeedCursor ++;
                hasMore = true;
            } else {
                lastFeedCursor = 1;
                hasMore = false;
            }
            RefFeedListener.success(feeds, hasMore);
            clearListener(ACTION_GET_FEED);
        }

        /**
         * Called when the user's friends list has arrived
         *
         * @param users The user's friends (by Twitter definition)
         */
        @Override
        public void gotFriendsList(PagableResponseList<User> users) {
            SoomlaUtils.LogDebug(TAG, "getContacts/onComplete " + users.size());

            List<UserProfile> userProfiles = new ArrayList<UserProfile>();
            for (User profile : users) {
                userProfiles.add(createUserProfile(profile));
            }
            if (users.hasNext()) {
                lastContactCursor = users.getNextCursor();
            }
            RefContactsListener.success(userProfiles, users.hasNext());
            clearListener(ACTION_GET_CONTACTS);
        }

        /**
         * Called when a tweet has finished posting
         *
         * @param status The status which was posted
         */
        @Override
        public void updatedStatus(Status status) {
            SoomlaUtils.LogDebug(TAG, "updateStatus/onComplete");
            RefSocialActionListener.success();
            clearListener(ACTION_PUBLISH_STATUS);
        }

        /**
         * Called whenever an exception has occurred while running a Twitter4J
         * asynchronous action
         *
         * @param e The exception which was thrown
         * @param twitterMethod The method which failed
         */
        @Override
        public void onException(TwitterException e, TwitterMethod twitterMethod) {
            SoomlaUtils.LogDebug(TAG, "General fail " + e.getMessage());

            failListener(preformingAction, e.getMessage());
        }
    };

    /**
     * Soomla Twitter Activity
     * </p>
     * Exists only to show a WebView to login the user
     */
    public static class SoomlaTwitterActivity extends Activity {

        private static final String TAG = "SOOMLA SoomlaTwitter$SoomlaTwitterActivity";
        private SoomlaTwitterWebView webView = null;
        private boolean mFinishedVerifying = false;

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Edge case - start activity without twitter
            if (twitter == null) {
                finish();
                return;
            }

            SoomlaUtils.LogDebug(TAG, "onCreate");

            if (webView == null) {
                webView = new SoomlaTwitterWebView(this);
                webView.setWebViewClient(new WebViewClient(){
                    @Override
                    public boolean shouldOverrideUrlLoading (WebView view, String url) {
                        // See if the URL should be handled by the provider
                        // only if it's a callback which was passed by the
                        // provider
                        if (url.startsWith(oauthCallbackURL)) {
                            Uri uri = Uri.parse(url);
                            completeVerify(uri);
                            return true;
                        }
                        return false;
                    }
                });
            }

            // we should append additional param forcing login/pass request, otherwise app will be loaded with previous account
            // decision based on https://dev.twitter.com/oauth/reference/get/oauth/authorize
            String url = getIntent().getStringExtra("url")  + "&force_login=true";
            webView.loadUrlOnUiThread(url);
            webView.show(this);
        }

        private void completeVerify(Uri uri) {
            SoomlaUtils.LogDebug(TAG, "Verification complete");
            /**
             * Handle OAuth Callback
             */
            if (uri != null && uri.toString().startsWith(oauthCallbackURL)) {
                String verifier = uri.getQueryParameter(OAUTH_VERIFIER);
                if (!TextUtils.isEmpty(verifier)) {
                    twitter.getOAuthAccessTokenAsync(mainRequestToken, verifier);
                }
                else {
                    // Without a verifier an Access Token cannot be received
                    // happens when a user clicks "cancel"
                    cancelLogin();
                }
            }

            webView.hide();
            finish();

            mFinishedVerifying = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onResume() {
            super.onResume();
            SoomlaUtils.LogDebug(TAG, "onResume");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onDestroy() {
            super.onDestroy();

            if (!mFinishedVerifying) {
                cancelLogin();
            }

            SoomlaUtils.LogDebug(TAG, "onDestroy");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onStop() {
            super.onStop();

            SoomlaUtils.LogDebug(TAG, "onStop");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(final Activity parentActivity, final AuthCallbacks.LoginListener loginListener) {
        if (!isInitialized) {
            SoomlaUtils.LogError(TAG, "Consumer key and secret were not defined, please provide them in initialization");
            return;
        }

        SoomlaUtils.LogDebug(TAG, "login");
        WeakRefParentActivity = new WeakReference<Activity>(parentActivity);

        RefProvider = getProvider();
        RefLoginListener = loginListener;

        preformingAction = ACTION_LOGIN;

        mainRequestToken = null;
        twitter.setOAuthAccessToken(null);

        // Try logging in using store credentials
        String oauthToken = KeyValueStorage.getValue(getTwitterStorageKey(TWITTER_OAUTH_TOKEN));
        String oauthTokenSecret = KeyValueStorage.getValue(getTwitterStorageKey(TWITTER_OAUTH_SECRET));
        if (!TextUtils.isEmpty(oauthToken) && !TextUtils.isEmpty(oauthTokenSecret)) {
            twitter.setOAuthAccessToken(new AccessToken(oauthToken, oauthTokenSecret));
            twitterScreenName = KeyValueStorage.getValue(getTwitterStorageKey(TWITTER_SCREEN_NAME));

            loginListener.success(RefProvider);

            clearListener(ACTION_LOGIN);
        }
        else {
            // If no stored credentials start login process by requesting
            // a request token
            twitter.getOAuthRequestTokenAsync(oauthCallbackURL);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout(final AuthCallbacks.LogoutListener logoutListener) {
        SoomlaUtils.LogDebug(TAG, "logout");

        KeyValueStorage.deleteKeyValue(getTwitterStorageKey(TWITTER_OAUTH_TOKEN));
        KeyValueStorage.deleteKeyValue(getTwitterStorageKey(TWITTER_OAUTH_SECRET));

        mainRequestToken = null;

        twitter.setOAuthAccessToken(null);
        twitter.shutdown();

        logoutListener.success();
    }

    /**
     * @deprecated Use isLoggedIn() instead
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public boolean isLoggedIn(final Activity activity) {
        return isLoggedIn();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isLoggedIn() {
        SoomlaUtils.LogDebug(TAG, "isLoggedIn");

        try {
            return isInitialized &&
                    (twitter.getOAuthAccessToken() != null);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getUserProfile(final AuthCallbacks.UserProfileListener userProfileListener) {
        if (!isInitialized) {
            return;
        }

        SoomlaUtils.LogDebug(TAG, "getUserProfile");

        RefProvider = getProvider();
        RefUserProfileListener = userProfileListener;

        preformingAction = ACTION_GET_USER_PROFILE;

        try {
            twitter.showUser(twitterScreenName);
        } catch (Exception e) {
            failListener(ACTION_GET_USER_PROFILE, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(String status, final SocialCallbacks.SocialActionListener socialActionListener) {
        if (!isInitialized) {
            return;
        }

        SoomlaUtils.LogDebug(TAG, "updateStatus");

        RefProvider = getProvider();
        RefSocialActionListener = socialActionListener;

        preformingAction = ACTION_PUBLISH_STATUS;

        try {
            twitter.updateStatus(status);
        } catch (Exception e) {
            failListener(ACTION_PUBLISH_STATUS, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatusDialog(String link, SocialCallbacks.SocialActionListener socialActionListener) {
        if (!isInitialized) {
            return;
        }

        SoomlaUtils.LogDebug(TAG, "updateStatusDialog");
        socialActionListener.fail("Dialogs are not available in Twitter");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStory(String message, String name, String caption, String description, String link, String picture,
                            final SocialCallbacks.SocialActionListener socialActionListener) {
        if (!isInitialized) {
            return;
        }

        SoomlaUtils.LogDebug(TAG, "updateStory");

        RefProvider = getProvider();
        RefSocialActionListener = socialActionListener;

        preformingAction = ACTION_PUBLISH_STORY;

        try {
            twitter.updateStatus(message + " " + link);
        } catch (Exception e) {
            failListener(ACTION_PUBLISH_STORY, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStoryDialog(String name, String caption, String description, String link, String picture,
                                  SocialCallbacks.SocialActionListener socialActionListener) {
        if (!isInitialized) {
            return;
        }

        SoomlaUtils.LogDebug(TAG, "updateStoryDialog");

        socialActionListener.fail("Dialogs are not available in Twitter");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getContacts(boolean fromStart, final SocialCallbacks.ContactsListener contactsListener) {
        if (!isInitialized) {
            return;
        }

        SoomlaUtils.LogDebug(TAG, "getContacts");

        RefProvider = getProvider();
        RefContactsListener = contactsListener;

        preformingAction = ACTION_GET_USER_PROFILE;

        try {
            twitter.getFriendsList(twitterScreenName, fromStart ? -1 : this.lastContactCursor);
            this.lastContactCursor = -1;
        } catch (Exception e) {
            failListener(ACTION_GET_USER_PROFILE, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getFeed(Boolean fromStart, final SocialCallbacks.FeedListener feedListener) {
        if (!isInitialized) {
            return;
        }

        SoomlaUtils.LogDebug(TAG, "getFeed");

        RefProvider = getProvider();
        RefFeedListener = feedListener;

        preformingAction = ACTION_GET_FEED;

        try {
            if (fromStart) {
                this.lastFeedCursor = 1;
            }

            Paging paging = new Paging(this.lastFeedCursor, PAGE_SIZE);
            twitter.getUserTimeline(paging);
        } catch (Exception e) {
            failListener(ACTION_GET_FEED, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uploadImage(String message, String filePath, final SocialCallbacks.SocialActionListener socialActionListener) {
        if (!isInitialized) {
            return;
        }

        SoomlaUtils.LogDebug(TAG, "uploadImage");

        RefProvider = getProvider();
        RefSocialActionListener = socialActionListener;

        preformingAction = ACTION_UPLOAD_IMAGE;

        try {
            StatusUpdate updateImage = new StatusUpdate(message);
            updateImage.media(new File(filePath));
            twitter.updateStatus(updateImage);
        } catch (Exception e) {
            failListener(ACTION_UPLOAD_IMAGE, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invite(final Activity parentActivity, String inviteMessage, String dialogTitle, final SocialCallbacks.InviteListener inviteListener) {
        inviteListener.fail("Invitation isn't supported in Twitter.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void like(final Activity parentActivity, String pageId) {
        SoomlaUtils.LogDebug(TAG, "like");

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitter.com/" + pageId));
        parentActivity.startActivity(browserIntent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Map<String, String> providerParams) {
        autoLogin = false;

        if (providerParams != null) {
            twitterConsumerKey = providerParams.get("consumerKey");
            twitterConsumerSecret = providerParams.get("consumerSecret");

            // extract autoLogin
            String autoLoginStr = providerParams.get("autoLogin");
            autoLogin = autoLoginStr != null && Boolean.parseBoolean(autoLoginStr);
        }

        SoomlaUtils.LogDebug(TAG, String.format(
                    "ConsumerKey:%s ConsumerSecret:%s",
                    twitterConsumerKey, twitterConsumerSecret));

        if (TextUtils.isEmpty(twitterConsumerKey) || TextUtils.isEmpty(twitterConsumerSecret)) {
            SoomlaUtils.LogError(TAG, "You must provide the Consumer Key and Secret in the SoomlaProfile initialization parameters");
            isInitialized = false;
        }
        else {
            isInitialized = true;
        }

        oauthCallbackURL = "oauth://soomla_twitter" + twitterConsumerKey;

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setOAuthConsumerKey(twitterConsumerKey);
        configurationBuilder.setOAuthConsumerSecret(twitterConsumerSecret);
        Configuration configuration = configurationBuilder.build();
        twitter = new AsyncTwitterFactory(configuration).getInstance();

        if (!actionsListenerAdded) {
            SoomlaUtils.LogWarning(TAG, "added action listener");
            twitter.addListener(actionsListener);
            actionsListenerAdded = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Provider getProvider() {
        return Provider.TWITTER;
    }

    @Override
    public boolean isAutoLogin() {
        return autoLogin;
    }

    private String getTwitterStorageKey(String postfix) {
        return DB_KEY_PREFIX + postfix;
    }

    private UserProfile createUserProfile(User user, boolean withExtraFields) {
        String fullName = user.getName();
        String firstName = "";
        String lastName = "";

        if (!TextUtils.isEmpty(fullName)) {
            String[] splitName = fullName.split(" ");
            if (splitName.length > 0) {
                firstName = splitName[0];
                if (splitName.length > 1) {
                    lastName = splitName[1];
                }
            }
        }
        Map<String, Object> extraDict = Collections.<String, Object>emptyMap();
        if (withExtraFields) {
            extraDict = new HashMap<String, Object>();
            // TwitterException will throws when Twitter service or network is unavailable, or the user has not authorized
            try {
                extraDict.put("access_token", twitter.getOAuthAccessToken().getToken());
            } catch (TwitterException twitterExc) {
                SoomlaUtils.LogError(TAG, twitterExc.getErrorMessage());
            }
        }
        //Twitter does not supply email access: https://dev.twitter.com/faq#26
        UserProfile result = new UserProfile(RefProvider, String.valueOf(user.getId()), user.getScreenName(),
                "", firstName, lastName, extraDict);

        // No gender information on Twitter:
        // https://twittercommunity.com/t/how-to-find-male-female-accounts-in-following-list/7367
        result.setGender("");

        // No birthday on Twitter:
        // https://twittercommunity.com/t/how-can-i-get-email-of-user-if-i-use-api/7019/16
        result.setBirthday("");

        result.setLanguage(user.getLang());
        result.setLocation(user.getLocation());
        result.setAvatarLink(user.getBiggerProfileImageURL());

        return result;
    }

    private UserProfile createUserProfile(User user) {
        return createUserProfile(user, false);
    }

    private static void cancelLogin() {
        if (RefLoginListener != null) {
            RefLoginListener.cancel();
            clearListener(ACTION_LOGIN);
        }
    }

    private static void failListener(int requestedAction, String message) {
        switch (requestedAction) {
            case ACTION_LOGIN: {
                RefLoginListener.fail("Login failed: " + message);
                break;
            }
            case ACTION_PUBLISH_STATUS: {
                RefSocialActionListener.fail("Publish status failed: " + message);
                break;
            }
            case ACTION_PUBLISH_STATUS_DIALOG: {
                RefSocialActionListener.fail("Publish status dialog failed: " + message);
                break;
            }
            case ACTION_PUBLISH_STORY: {
                RefSocialActionListener.fail("Publish story failed: " + message);
                break;
            }
            case ACTION_PUBLISH_STORY_DIALOG: {
                RefSocialActionListener.fail("Publish story dialog failed: " + message);
                break;
            }
            case ACTION_UPLOAD_IMAGE: {
                RefSocialActionListener.fail("Upload Image failed: " + message);
                break;
            }
            case ACTION_GET_FEED: {
                RefFeedListener.fail("Get feed failed: " + message);
                break;
            }
            case ACTION_GET_CONTACTS: {
                RefContactsListener.fail("Get contacts failed: " + message);
                break;
            }
            case ACTION_GET_USER_PROFILE: {
                RefUserProfileListener.fail("Get user profile failed: " + message);
                break;
            }
            default: {
                SoomlaUtils.LogWarning(TAG, "action unknown fail listener:" + requestedAction);
                break;
            }
        }

        clearListener(requestedAction);
    }

    private static void clearListener(int requestedAction) {
        SoomlaUtils.LogDebug(TAG, "Clearing Listeners " + requestedAction);

        switch (requestedAction) {
            case ACTION_LOGIN: {
                RefLoginListener = null;
                break;
            }
            case ACTION_PUBLISH_STATUS: {
                RefSocialActionListener = null;
                break;
            }
            case ACTION_PUBLISH_STATUS_DIALOG: {
                RefSocialActionListener = null;
                break;
            }
            case ACTION_PUBLISH_STORY: {
                RefSocialActionListener = null;
                break;
            }
            case ACTION_PUBLISH_STORY_DIALOG: {
                RefSocialActionListener = null;
                break;
            }
            case ACTION_UPLOAD_IMAGE: {
                RefSocialActionListener = null;
                break;
            }
            case ACTION_GET_FEED: {
                RefFeedListener = null;
                break;
            }
            case ACTION_GET_CONTACTS: {
                RefContactsListener = null;
                break;
            }
            case ACTION_GET_USER_PROFILE: {
                RefUserProfileListener = null;
                break;
            }
            default: {
                SoomlaUtils.LogWarning(TAG, "action unknown clear listener:" + requestedAction);
                break;
            }
        }
    }
}

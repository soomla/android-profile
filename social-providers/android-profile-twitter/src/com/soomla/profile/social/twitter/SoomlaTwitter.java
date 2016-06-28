package com.soomla.profile.social.twitter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;

import com.soomla.SoomlaUtils;
import com.soomla.profile.auth.AuthCallbacks;
import com.soomla.profile.auth.IAuthProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.social.ISocialProvider;
import com.soomla.profile.social.SocialCallbacks;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.models.Media;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.User;
import com.twitter.sdk.android.core.services.StatusesService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.fabric.sdk.android.Fabric;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedInput;

/**
 * Soomla wrapper for Twitter4J (unofficial SDK for Twitter API).
 * <p/>
 * This class uses the <code>SoomlaTwitterWebView</code> to authenticate.
 * All other operations are performed asynchronously via Twitter4J
 */
public class SoomlaTwitter implements IAuthProvider, ISocialProvider {

    private static final String TAG = "SOOMLA SoomlaTwitter";

    private static final String TWITTER_OAUTH_SECRET = "oauth.secret";
    private static final String TWITTER_SCREEN_NAME = "oauth.screenName";
    private static boolean loginProcess;

    private static boolean autoLogin;

    //     some weak refs that are set before launching the wrapper SoomlaTwitterActivity
//     (need to be accessed by static context)
    private static Provider RefProvider;
    private static AuthCallbacks.LoginListener RefLoginListener;
    private static AuthCallbacks.UserProfileListener RefUserProfileListener;
    private static SocialCallbacks.SocialActionListener RefSocialActionListener;
    private static SocialCallbacks.FeedListener RefFeedListener;
    private static SocialCallbacks.ContactsListener RefContactsListener;

    private boolean isInitialized = false;

    private static boolean actionsListenerAdded = false;
    public static final int ACTION_LOGIN = 0;

    public static final int ACTION_PUBLISH_STATUS = 10;
    public static final int ACTION_PUBLISH_STORY = 11;
    public static final int ACTION_UPLOAD_IMAGE = 12;
    public static final int ACTION_GET_FEED = 13;
    public static final int ACTION_GET_CONTACTS = 14;
    public static final int ACTION_PUBLISH_STATUS_DIALOG = 15;
    public static final int ACTION_PUBLISH_STORY_DIALOG = 16;
    public static final int ACTION_GET_USER_PROFILE = 17;

    private static TwitterSession session;
    private static TwitterAuthToken twitterAuthToken;
    private User user;
    private static WeakReference<AuthCallbacks.LoginListener> WeakRefLoginListener;
    private static TwitterAuthClient twitterAuthClient;


    /**
     * Soomla Twitter Activity
     * </p>
     * Exists only to show a WebView to login the user
     */
    public static class SoomlaTwitterActivity extends Activity {

        private static final String TAG = "SOOMLA SoomlaTwitter$SoomlaTwitterActivity";
        private boolean mFinishedVerifying = false;
        private static boolean inProgress;

        @Override
        protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
            super.onActivityResult(requestCode, responseCode, intent);
            if (twitterAuthClient.getRequestCode() == requestCode) {
                twitterAuthClient.onActivityResult(requestCode, responseCode, intent);
            }
            finish();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            SoomlaUtils.LogDebug(TAG, "onCreate");
            login();
        }

        private void login() {
            if (inProgress) {
                return;
            }
            inProgress = true;
            Twitter.getSessionManager().clearActiveSession();
            twitterAuthClient.authorize(this, new Callback<TwitterSession>() {
                @Override
                public void success(Result<TwitterSession> twitterSessionResult) {
                    TwitterAuthConfig authConfig =
                            new TwitterAuthConfig(TWITTER_SCREEN_NAME,
                                    TWITTER_OAUTH_SECRET);
                    Fabric.with(SoomlaTwitterActivity.this, new Twitter(authConfig));
                    session = twitterSessionResult.data;
                    twitterAuthToken = session.getAuthToken();
                    WeakRefLoginListener.get().success(RefProvider);
                    loginProcess = false;
                }

                @Override
                public void failure(TwitterException e) {
                    SoomlaUtils.LogError(TAG, e.getMessage());
                    loginProcess = false;
                }
            });
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
            Twitter.getSessionManager().clearActiveSession();
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
        if (loginProcess) {
            return;
        }
        loginProcess = true;
        twitterAuthClient = new TwitterAuthClient();
        RefLoginListener = loginListener;
        WeakRefLoginListener = new WeakReference<>(loginListener);
        RefProvider = getProvider();
        WeakReference<Activity> WeakRefParentActivity = new WeakReference<>(parentActivity);
        Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaTwitterActivity.class);
        intent.putExtra("action", ACTION_LOGIN);
        parentActivity.startActivity(intent);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public void logout(final AuthCallbacks.LogoutListener logoutListener) {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();
        Twitter.getSessionManager().clearActiveSession();
        Twitter.logOut();
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
     * {@inheritDoc}
     */
    @Override
    public boolean isLoggedIn() {
        SoomlaUtils.LogDebug(TAG, "isLoggedIn");
        try {
            return isInitialized && twitterAuthToken != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getUserProfile(final AuthCallbacks.UserProfileListener userProfileListener) {
        SoomlaUtils.LogDebug(TAG, "getUserProfile");
        if (!isInitialized) {
            return;
        }
        RefUserProfileListener = userProfileListener;
        RefProvider = getProvider();
        Twitter.getApiClient(session).getAccountService()
                .verifyCredentials(true, false, new Callback<User>() {
                    @Override
                    public void failure(TwitterException e) {
                        SoomlaUtils.LogError(TAG, e.getMessage());
                        userProfileListener.fail("Unable to get user profile.");
                    }

                    @Override
                    public void success(Result<User> userResult) {
                        user = userResult.data;
                        String[] splitName = user.name.split(" ");
                        String firstName = splitName[0];
                        String lastName = "";
                        if (splitName.length > 1) {
                            lastName = splitName[1];
                        }
                        //Twitter does not supply email access: https://dev.twitter.com/faq#26
                        UserProfile userProfile = new UserProfile(getProvider(),
                                String.valueOf(user.getId()), user.name, "", firstName, lastName);
                        userProfile.setAvatarLink(user.profileImageUrl);
                        userProfile.setLocation(user.location);
                        userProfileListener.success(userProfile);
                    }
                });
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
        if (session == null) {
            session = Twitter.getSessionManager().getActiveSession();
            if (session == null) {
                SoomlaUtils.LogDebug(TAG, "Session expired");
                return;
            }
        }
        TwitterApiClient twitterApiClient = new TwitterApiClient(session);
        StatusesService statusesService = twitterApiClient.getStatusesService();
        RefSocialActionListener = socialActionListener;
        try {
            statusesService.update(status, null, null, null, null, null, null, null, null, new Callback<Tweet>() {
                @Override
                public void success(Result<Tweet> result) {
                    socialActionListener.success();
                }

                @Override
                public void failure(TwitterException exception) {
                    socialActionListener.fail("fail update status");
                }
            });
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
        if (session == null) {
            session = Twitter.getSessionManager().getActiveSession();
            if (session == null) {
                SoomlaUtils.LogDebug(TAG, "Session expired");
                return;
            }
        }
        TwitterApiClient twitterApiClient = new TwitterApiClient(session);
        StatusesService statusesService = twitterApiClient.getStatusesService();
        RefSocialActionListener = socialActionListener;
        statusesService.update(message + " " + link, null, null, null, null, null, null, null, null, new Callback<Tweet>() {
            @Override
            public void success(Result<Tweet> result) {
                socialActionListener.success();
            }

            @Override
            public void failure(TwitterException e) {
                failListener(ACTION_PUBLISH_STORY, e.getMessage());
            }
        });
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

        TwitterClientApiClient twitterApiClient = new TwitterClientApiClient(session);
        FollowersService followersService = twitterApiClient.getFollowersService();
        followersService.followers(user.getId(), null, null, 10, false, true, new Callback<Response>() {
            @Override
            public void success(Result<Response> result) {
                List<UserProfile> users = new ArrayList<>();
                Response res = result.data;
                TypedInput body = res.getBody();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(body.in()));
                    StringBuilder out = new StringBuilder();
                    String newLine = System.getProperty("line.separator");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line);
                        out.append(newLine);
                    }
                    JSONObject mainObject = new JSONObject(out.toString());
                    JSONArray usersArray = mainObject.optJSONArray("users");
                    for (int i = 0; i < usersArray.length(); i++) {
                        JSONObject user = usersArray.optJSONObject(i);
                        String userName = String.valueOf(user.opt("name"));
                        String userId = String.valueOf(user.opt("id_str"));
                        String[] fullName = userName.split(" ");
                        String firstName = fullName[0];
                        String lastName = fullName[1];
                        UserProfile userProfile = new UserProfile(getProvider(), userId, userName, null, firstName, lastName);
                        users.add(userProfile);
                    }
                    contactsListener.success(users, true); //TODO what is boolean b?
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(TwitterException exception) {
                SoomlaUtils.LogWarning(TAG, exception.getMessage());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getFeed(Boolean fromStart, final SocialCallbacks.FeedListener feedListener) {
        if (!isInitialized) {
            return;
        }
        RefProvider = getProvider();
        SoomlaUtils.LogDebug(TAG, "getFeed");
        RefFeedListener = feedListener;
        TwitterClientApiClient twitterApiClient = new TwitterClientApiClient(session);
        TweetsService tweetsService = twitterApiClient.getTweetsService();
        //todo change 9999....
        tweetsService.tweets(user.getId(), null, 1, 20, 999999999999999999L, true, true, true, true, new Callback<Response>() {
            @Override
            public void success(Result<Response> result) {
                List<String> list = new ArrayList<>();
                TypedInput body = result.data.getBody();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(body.in()));
                    StringBuilder out = new StringBuilder();
                    String newLine = System.getProperty("line.separator");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line);
                        out.append(newLine);
                    }
                    JSONArray mainObject = new JSONArray(out.toString());
                    for (int i = 0; i < mainObject.length(); i++) {
                        JSONObject user = mainObject.optJSONObject(i);
                        String text = String.valueOf(user.opt("text"));
                        list.add(text);
                    }
                    feedListener.success(list, true); //todo what is boolean b?
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(TwitterException exception) {
                SoomlaUtils.LogWarning(TAG, exception.getMessage());
            }
        });


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
        final String mess = message;
        RefProvider = getProvider();
        RefSocialActionListener = socialActionListener;
        if (session == null) {
            session = Twitter.getSessionManager().getActiveSession();
            if (session == null) {
                SoomlaUtils.LogDebug(TAG, "Session expired");
                return;
            }
        }
        final TwitterClientApiClient twitterApiClient = new TwitterClientApiClient(session);
        if (filePath == null || filePath.isEmpty()) {
            twitterApiClient.getStatusesService().update(mess, null, null, null, null, null, null, null, null, new Callback<Tweet>() {
                @Override
                public void success(Result<Tweet> result) {
                    SoomlaUtils.LogDebug(TAG, "file not found");
                    socialActionListener.success();
                }

                @Override
                public void failure(TwitterException exception) {
                    socialActionListener.fail("fail update status");
                }
            });
        } else {
            File photo = new File(filePath);
            TypedFile typedFile = new TypedFile("application/octet-stream", photo);
            twitterApiClient.getMediaService().upload(typedFile, null, null, new Callback<Media>() {
                @Override
                public void success(Result<Media> result) {
                    StatusesService statusesService = twitterApiClient.getStatusesService();
                    statusesService.update(mess, null, null, null, null, null, null, null, result.data.mediaIdString, new Callback<Tweet>() {
                        @Override
                        public void success(Result<Tweet> result) {
                            socialActionListener.success();
                        }

                        @Override
                        public void failure(TwitterException exception) {
                            socialActionListener.fail("fail update status");
                        }
                    });
                    socialActionListener.success();
                }

                @Override
                public void failure(TwitterException exception) {
                    socialActionListener.fail("failed load image");
                }
            });
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
        String twitterConsumerKey = providerParams.get("consumerKey");
        String twitterConsumerSecret = providerParams.get("consumerSecret");

        // extract autoLogin
        String autoLoginStr = providerParams.get("autoLogin");
        autoLogin = autoLoginStr != null && Boolean.parseBoolean(autoLoginStr);

        SoomlaUtils.LogDebug(TAG, String.format(
                "ConsumerKey:%s ConsumerSecret:%s",
                twitterConsumerKey, twitterConsumerSecret));

        if (TextUtils.isEmpty(twitterConsumerKey) || TextUtils.isEmpty(twitterConsumerSecret)) {
            SoomlaUtils.LogError(TAG, "You must provide the Consumer Key and Secret in the SoomlaProfile initialization parameters");
            isInitialized = false;
        } else {
            isInitialized = true;
        }

//        oauthCallbackURL = "oauth://soomla_twitter" + twitterConsumerKey;
//
//        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
//        configurationBuilder.setOAuthConsumerKey(twitterConsumerKey);
//        configurationBuilder.setOAuthConsumerSecret(twitterConsumerSecret);
//        Configuration configuration = configurationBuilder.build();
//        twitter = new        AsyncTwitterFactory(configuration).getInstance();

        if (!actionsListenerAdded) {
            SoomlaUtils.LogWarning(TAG, "added action listener");
//            twitter.addListener(actionsListener);
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

//    private String getTwitterStorageKey(String postfix) {
//        return DB_KEY_PREFIX + postfix;
//    }

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


    class TwitterClientApiClient extends TwitterApiClient {
        public TwitterClientApiClient(TwitterSession session) {
            super(session);
        }

        public FollowersService getFollowersService() {
            return getService(FollowersService.class);
        }

        public TweetsService getTweetsService() {
            return getService(TweetsService.class);
        }
    }

    interface FollowersService {
        @GET("/1.1/followers/list.json")
        void followers(@Query("user_id") long id,
                       @Query("screen_name") String screen_name,
                       @Query("cursor") Long cursor,
                       @Query("count") Integer count,
                       @Query("skip_status") boolean skip_status,
                       @Query("include_user_entities") boolean include_user_entities,
                       Callback<Response> cb);
    }


    interface TweetsService {
        @GET("/1.1/statuses/user_timeline.json")
        void tweets(@Query("user_id") long id,
                    @Query("screen_name") String screen_name,
                    @Query("since_id") long sinceId,
                    @Query("count") long count,
                    @Query("max_id") long maxId,
                    @Query("trim_user") boolean trimUser,
                    @Query("exclude_replies") boolean excludeReplies,
                    @Query("contributor_details") boolean contributor_details,
                    @Query("include_rts") boolean includeRts,
                    Callback<Response> cb);
    }
}
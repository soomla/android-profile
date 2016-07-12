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

package com.soomla.profile.social.facebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import com.soomla.SoomlaApp;
import com.soomla.SoomlaUtils;
import com.soomla.profile.auth.AuthCallbacks;
import com.soomla.profile.auth.IAuthProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.social.ISocialProvider;
import com.soomla.profile.social.SocialCallbacks;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.actions.Cursor;
import com.sromku.simple.fb.entities.Feed;
import com.sromku.simple.fb.entities.Photo;
import com.sromku.simple.fb.entities.Post;
import com.sromku.simple.fb.entities.Profile;
import com.sromku.simple.fb.listeners.*;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.security.AccessControlException;
import java.util.*;

/**
 * Soomla wrapper for SimpleFacebook (itself a wrapper to Android FB SDK).
 * <p/>
 * This class works by creating a transparent activity (SoomlaFBActivity) and working through it.
 * This is required to correctly integrate with FB activity lifecycle events
 */
public class SoomlaFacebook implements IAuthProvider, ISocialProvider {

    private static final String TAG = "SOOMLA SoomlaFacebook";

    private static final Permission[] DEFAULT_LOGIN_PERMISSIONS = new Permission[]{
            Permission.EMAIL,
            Permission.USER_ABOUT_ME,
            Permission.USER_BIRTHDAY,
            Permission.USER_PHOTOS,
            Permission.USER_FRIENDS, // GetContacts (but has limitations)
            Permission.USER_POSTS, //GetFeed
            Permission.PUBLISH_ACTION
    };

    private boolean autoLogin;

    // some weak refs that are set before launching the wrapper SoomlaFBActivity
    // (need to be accessed by static context)
    private static WeakReference<Activity> WeakRefParentActivity;
    private static Provider RefProvider;
    private static AuthCallbacks.LoginListener RefLoginListener;
    private static SocialCallbacks.SocialActionListener RefSocialActionListener;
    private static SocialCallbacks.FeedListener RefFeedListener;
    private static SocialCallbacks.ContactsListener RefContactsListener;
    private static SocialCallbacks.InviteListener RefInviteListener;

    private static Cursor<List<Profile>> lastContactCursor = null;
    private static Cursor<List<Post>> lastFeedCursor = null;

    public static final int ACTION_LOGIN = 0;

    public static final int ACTION_PUBLISH_STATUS = 10;
    public static final int ACTION_PUBLISH_STORY = 11;
    public static final int ACTION_UPLOAD_IMAGE = 12;
    public static final int ACTION_GET_FEED = 13;
    public static final int ACTION_GET_CONTACTS = 14;
    public static final int ACTION_PUBLISH_STATUS_DIALOG = 15;
    public static final int ACTION_PUBLISH_STORY_DIALOG = 16;
    public static final int ACTION_INVITE = 17;

    private List<Permission> loginPermissions;
    private List<Permission> permissions = null;

    private abstract class AsyncCallback {
        public abstract void call(String errorMessage);
        public void call() {
            call(null);
        }
    }

    /**
     * Constructor
     */
    public SoomlaFacebook() {

    }

    /**
     * The main SOOMLA Facebook activity
     * <p/>
     * This activity allows the framework to popup a window which in turns
     * communicates with Facebook to use the SDK
     */
    public static class SoomlaFBActivity extends Activity {

        private static final String TAG = "SOOMLA SoomlaFacebook$SoomlaFBActivity";
        private int preformingAction;

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            SimpleFacebook.getInstance(this);

            SoomlaUtils.LogDebug(TAG, "onCreate");

            // perform our wrapped action

            Intent intent = getIntent();
            preformingAction = intent.getIntExtra("action", -1);
            switch (preformingAction) {
                case ACTION_LOGIN: {
                    login(RefLoginListener);
                    break;
                }
                case ACTION_PUBLISH_STATUS: {
                    String status = intent.getStringExtra("status");
                    updateStatus(status, RefSocialActionListener);
                    break;
                }
                case ACTION_PUBLISH_STATUS_DIALOG: {
                    String link = intent.getStringExtra("link");
                    updateStatusDialog(link, RefSocialActionListener);
                    break;
                }
                case ACTION_PUBLISH_STORY: {
                    String message = intent.getStringExtra("message");
                    String name = intent.getStringExtra("name");
                    String caption = intent.getStringExtra("caption");
                    String description = intent.getStringExtra("description");
                    String link = intent.getStringExtra("link");
                    String picture = intent.getStringExtra("picture");
                    updateStory(message, name, caption, description, link, picture, RefSocialActionListener);
                    break;
                }
                case ACTION_PUBLISH_STORY_DIALOG: {
                    String name = intent.getStringExtra("name");
                    String caption = intent.getStringExtra("caption");
                    String description = intent.getStringExtra("description");
                    String link = intent.getStringExtra("link");
                    String picture = intent.getStringExtra("picture");
                    updateStoryDialog(name, caption, description, link, picture, RefSocialActionListener);
                    break;
                }
                case ACTION_UPLOAD_IMAGE: {
                    String message = intent.getStringExtra("message");
                    String filePath = intent.getStringExtra("filePath");
                    uploadImage(message, filePath, RefSocialActionListener);
                    break;
                }
                case ACTION_GET_FEED: {
                    boolean fromStart = intent.getBooleanExtra("fromStart", false);
                    getFeed(RefFeedListener, fromStart);
                    break;
                }
                case ACTION_GET_CONTACTS: {
                    boolean fromStart = intent.getBooleanExtra("fromStart", false);
                    getContacts(RefContactsListener, fromStart);
                    break;
                }
                case ACTION_INVITE: {
                    String message = intent.getStringExtra("message"),
                             title = intent.getStringExtra("title");
                    invite(RefInviteListener, message, title);
                    break;
                }
                default: {
                    SoomlaUtils.LogWarning(TAG, "action unknown:" + preformingAction);
                    break;
                }
            }
        }

        private void clearListeners() {
            SoomlaUtils.LogDebug(TAG, "Clearing Listeners");

            switch (preformingAction) {
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
                case ACTION_INVITE: {
                    RefInviteListener = null;
                    break;
                }
                default: {
                    SoomlaUtils.LogWarning(TAG, "action unknown:" + preformingAction);
                    break;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onResume() {
            super.onResume();
            SoomlaUtils.LogDebug(TAG, "onResume");
            SimpleFacebook.getInstance(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            SoomlaUtils.LogDebug(TAG, "onActivityResult");
            SimpleFacebook.getInstance().onActivityResult(requestCode, resultCode, data);
        }

        private void login(final AuthCallbacks.LoginListener loginListener) {
            SoomlaUtils.LogDebug(TAG, "login");
            SimpleFacebook.getInstance().login(new OnLoginListener() {

                @Override
                public void onLogin(String accessToken, List<Permission> acceptedPermissions, List<Permission> declinedPermissions) {
                    SoomlaUtils.LogDebug(TAG, "login/onLogin " + " [" + loginListener + "]");
                    try {
                        loginListener.success(RefProvider);
                    } catch (Exception ex) {
                        SoomlaUtils.LogError(TAG, "There was an error running success handler for login success. error: " + ex.getLocalizedMessage());
                        ex.printStackTrace();
                    }
                    clearListeners();
                    finish();
                }

                @Override
                public void onCancel() {
                    SoomlaUtils.LogDebug(TAG, "login/onNotAcceptingPermissions:onCancel[" + loginListener + "]");
                    loginListener.fail("onCancel");
                    clearListeners();
                    finish();
                }

                @Override
                public void onException(Throwable throwable) {
                    SoomlaUtils.LogDebug(TAG, "login/onException:" + throwable.getLocalizedMessage() + " [" + loginListener + "]");
                    loginListener.fail("onException: " + throwable.getLocalizedMessage());
                    clearListeners();
                    finish();
                }

                @Override
                public void onFail(String s) {
                    SoomlaUtils.LogDebug(TAG, "login/onFail:" + s + " [" + loginListener + "]");
                    loginListener.fail("onFail: " + s);
                    clearListeners();
                    finish();
                }
            });
        }

        private void updateStatusDialog(String link, final SocialCallbacks.SocialActionListener socialActionListener) {
            SoomlaUtils.LogDebug(TAG, "updateStatus -- " + SimpleFacebook.getInstance().toString());

            Feed.Builder feedBuilder = new Feed.Builder();
            if (!TextUtils.isEmpty(link)) {
                feedBuilder.setLink(link);
            }
            Feed feed = feedBuilder.build();

            SimpleFacebook.getInstance().publish(feed, true, new OnPublishListener() {

                @Override
                public void onComplete(String postId) {
                    super.onComplete(postId);
                    SoomlaUtils.LogDebug(TAG, "updateStatus/onComplete" + " [" + socialActionListener + "]");
                    socialActionListener.success();
                    clearListeners();
                    finish();
                }

                @Override
                public void onException(Throwable throwable) {
                    super.onException(throwable);
                    SoomlaUtils.LogWarning(TAG, "updateStatus/onException: " + throwable.getLocalizedMessage() + " [" + socialActionListener + "]");
                    socialActionListener.fail("onException: " + throwable.getLocalizedMessage());
                    clearListeners();
                    finish();
                }

                @Override
                public void onFail(String reason) {
                    super.onFail(reason);
                    SoomlaUtils.LogWarning(TAG, "updateStatus/onFail: " + reason + " [" + socialActionListener + "]");
                    socialActionListener.fail("onFail: " + reason);
                    clearListeners();
                    finish();
                }
            });
        }

        private void updateStatus(String status, final SocialCallbacks.SocialActionListener socialActionListener) {
            SoomlaUtils.LogDebug(TAG, "updateStatus -- " + SimpleFacebook.getInstance().toString());
            Feed feed = new Feed.Builder()
                    .setMessage(status)
                    .build();

            SimpleFacebook.getInstance().publish(feed, false, new OnPublishListener() {

                @Override
                public void onComplete(String postId) {
                    super.onComplete(postId);
                    SoomlaUtils.LogDebug(TAG, "updateStatus/onComplete" + " [" + socialActionListener + "]");
                    socialActionListener.success();
                    clearListeners();
                    finish();
                }

                @Override
                public void onException(Throwable throwable) {
                    super.onException(throwable);
                    SoomlaUtils.LogWarning(TAG, "updateStatus/onException: " + throwable.getLocalizedMessage() + " [" + socialActionListener + "]");
                    socialActionListener.fail("onException: " + throwable.getLocalizedMessage());
                    clearListeners();
                    finish();
                }

                @Override
                public void onFail(String reason) {
                    super.onFail(reason);
                    SoomlaUtils.LogWarning(TAG, "updateStatus/onFail: " + reason + " [" + socialActionListener + "]");
                    socialActionListener.fail("onFail: " + reason);
                    clearListeners();
                    finish();
                }
            });
        }

        private void updateStory(String message, String name, String caption, String description, String link, String picture,
                                 final SocialCallbacks.SocialActionListener socialActionListener) {
            SoomlaUtils.LogDebug(TAG, "updateStory -- " + SimpleFacebook.getInstance().toString());
            Feed feed = new Feed.Builder()
                    .setMessage(message)
                    .setName(name)
                    .setCaption(caption)
                    .setDescription(description)
                    .setLink(link)
                    .setPicture(picture)
                    .build();

            SimpleFacebook.getInstance().publish(feed, false, new OnPublishListener() {

                @Override
                public void onComplete(String postId) {
                    SoomlaUtils.LogDebug(TAG, "innerUpdateStory/onComplete" + " [" + socialActionListener + "]");
                    socialActionListener.success();
                    clearListeners();
                    finish();
                }

                @Override
                public void onException(Throwable throwable) {
                    super.onException(throwable);
                    SoomlaUtils.LogWarning(TAG, "innerUpdateStory/onException: " + throwable.getLocalizedMessage() + " [" + socialActionListener + "]");
                    socialActionListener.fail("onException: " + throwable.getLocalizedMessage());
                    clearListeners();
                    finish();
                }

                @Override
                public void onFail(String reason) {
                    super.onFail(reason);
                    SoomlaUtils.LogWarning(TAG, "innerUpdateStory/onFail: " + reason + " [" + socialActionListener + "]");
                    socialActionListener.fail("onFail: " + reason);
                    clearListeners();
                    finish();
                }
            });
        }

        private void updateStoryDialog(String name, String caption, String description, String link, String picture,
                                       final SocialCallbacks.SocialActionListener socialActionListener) {
            SoomlaUtils.LogDebug(TAG, "updateStoryDialog -- " + SimpleFacebook.getInstance().toString());

            Feed.Builder feedBuilder = new Feed.Builder();
            if (!TextUtils.isEmpty(link)) {
                feedBuilder.setLink(link);
                if (!TextUtils.isEmpty(name)) {
                    feedBuilder.setName(name);
                }
                if (!TextUtils.isEmpty(caption)) {
                    feedBuilder.setCaption(caption);
                }
                if (!TextUtils.isEmpty(description)) {
                    feedBuilder.setDescription(description);
                }
                if (!TextUtils.isEmpty(picture)) {
                    feedBuilder.setPicture(picture);
                }
            }
            Feed feed = feedBuilder.build();

            SimpleFacebook.getInstance().publish(feed, true, new OnPublishListener() {

                @Override
                public void onComplete(String postId) {
                    SoomlaUtils.LogDebug(TAG, "innerUpdateStoryDialog/onComplete" + " [" + socialActionListener + "]");
                    socialActionListener.success();
                    clearListeners();
                    finish();
                }

                @Override
                public void onException(Throwable throwable) {
                    super.onException(throwable);
                    SoomlaUtils.LogWarning(TAG, "innerUpdateStoryDialog/onException: " + throwable.getLocalizedMessage() + " [" + socialActionListener + "]");
                    socialActionListener.fail("onException: " + throwable.getLocalizedMessage());
                    clearListeners();
                    finish();
                }

                @Override
                public void onFail(String reason) {
                    super.onFail(reason);
                    SoomlaUtils.LogWarning(TAG, "innerUpdateStoryDialog/onFail: " + reason + " [" + socialActionListener + "]");
                    socialActionListener.fail("onFail: " + reason);
                    clearListeners();
                    finish();
                }
            });
        }

        private void uploadImage(String message, String filePath, final SocialCallbacks.SocialActionListener socialActionListener) {
            SoomlaUtils.LogDebug(TAG, "uploadImage -- " + SimpleFacebook.getInstance().toString());
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            Photo photo = new Photo.Builder()
                    .setImage(bitmap)
                    .setName(message)
                    .build();

            SimpleFacebook.getInstance().publish(photo, false, new OnPublishListener() {

                @Override
                public void onComplete(String response) {
                    super.onComplete(response);
                    SoomlaUtils.LogDebug(TAG, "uploadImage/onComplete" + " [" + socialActionListener + "]");
                    socialActionListener.success();
                    clearListeners();
                    finish();
                }

                @Override
                public void onException(Throwable throwable) {
                    super.onException(throwable);
                    SoomlaUtils.LogWarning(TAG, "uploadImage/onException:" + throwable.getLocalizedMessage() + " [" + socialActionListener + "]");
                    socialActionListener.fail("onException:" + throwable.getLocalizedMessage());
                    clearListeners();
                    finish();
                }

                @Override
                public void onFail(String reason) {
                    super.onFail(reason);
                    SoomlaUtils.LogWarning(TAG, "uploadImage/onFail:" + reason + " [" + socialActionListener + "]");
                    socialActionListener.fail("fail:" + reason);
                    clearListeners();
                    finish();
                }

                @Override
                public void onThinking() {
                    super.onThinking();
                }
            });
        }

        private void getContacts(final SocialCallbacks.ContactsListener contactsListener, boolean fromStart) {
            Profile.Properties properties = new Profile.Properties.Builder()
                    .add(Profile.Properties.ID)
//                    .add(Profile.Properties.USER_NAME) //deprecated in v2
                    .add(Profile.Properties.NAME)
                    .add(Profile.Properties.EMAIL)
                    .add(Profile.Properties.FIRST_NAME)
                    .add(Profile.Properties.LAST_NAME)
                    .add(Profile.Properties.PICTURE)
                    .build();

            Cursor<List<Profile>> lastContactCursor = SoomlaFacebook.lastContactCursor;
            SoomlaFacebook.lastContactCursor = null;

            if (fromStart || lastContactCursor == null) {
                SimpleFacebook.getInstance().getFriends(properties, new OnFriendsListener() {

                    @Override
                    public void onComplete(List<Profile> response) {
                        super.onComplete(response);
                        SoomlaUtils.LogDebug(TAG, "getContacts/onComplete " + response.size());

                        List<UserProfile> userProfiles = new ArrayList<UserProfile>();
                        for (Profile profile : response) {
                            userProfiles.add(new UserProfile(
                                    RefProvider, profile.getId(), profile.getName(), profile.getEmail(),
                                    profile.getFirstName(), profile.getLastName()));
                        }
                        boolean hasNext = this.getCursor().hasNext();
                        if (hasNext) {
                            SoomlaFacebook.lastContactCursor = this.getCursor();
                        }
                        contactsListener.success(userProfiles, hasNext);
                        if (!hasNext) {
                            clearListeners();
                        }
                        finish();
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        super.onException(throwable);
                        SoomlaUtils.LogWarning(TAG, "getContacts/onException:" + throwable.getLocalizedMessage() + " [" + contactsListener + "]");
                        contactsListener.fail("onException: " + throwable.getLocalizedMessage());
                        clearListeners();
                        finish();
                    }

                    @Override
                    public void onFail(String reason) {
                        contactsListener.fail("onFail: " + reason);
                        SoomlaUtils.LogWarning(TAG, "getContacts/onFail:" + reason + " [" + contactsListener + "]");
                        clearListeners();
                        finish();
                    }
                });
            } else {
                lastContactCursor.next();
            }

        }

        public void getFeed(final SocialCallbacks.FeedListener feedListener, boolean fromStart) {
            Cursor<List<Post>> lastFeedCursor = SoomlaFacebook.lastFeedCursor;
            SoomlaFacebook.lastFeedCursor = null;

            if (fromStart || lastFeedCursor == null) {
                SimpleFacebook.getInstance().getPosts(Post.PostType.ALL, new OnPostsListener() {

                    @Override
                    public void onComplete(List<Post> posts) {
                        super.onComplete(posts);
                        SoomlaUtils.LogDebug(TAG, "getFeed/onComplete" + " [" + feedListener + "]");

                        List<String> feeds = new ArrayList<String>();
                        for (Post post : posts) {
                            feeds.add(post.getMessage());
                        }

                        boolean hasNext = this.getCursor().hasNext();
                        if (hasNext) {
                            SoomlaFacebook.lastFeedCursor = this.getCursor();
                        }
                        feedListener.success(feeds, hasNext);
                        if (!hasNext) {
                            clearListeners();
                        }
                        finish();
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        super.onException(throwable);
                        SoomlaUtils.LogWarning(TAG, "getFeed/onException:" + throwable.getLocalizedMessage() + " [" + feedListener + "]");
                        feedListener.fail("onException: " + throwable.getLocalizedMessage());
                        clearListeners();
                        finish();
                    }

                    @Override
                    public void onFail(String reason) {
                        super.onFail(reason);
                        SoomlaUtils.LogWarning(TAG, "getFeed/onFail:" + reason + " [" + feedListener + "]");
                        feedListener.fail("onFail: " + reason);
                        clearListeners();
                        finish();
                    }
                });
            } else {
                lastFeedCursor.next();
            }
        }

        private void invite(final SocialCallbacks.InviteListener inviteListener, String message, String title) {
            SimpleFacebook.getInstance().invite(message, new OnInviteListener() {
                @Override
                public void onComplete(List<String> invitedFriends, String requestId) {
                    inviteListener.success(requestId, invitedFriends);
                }

                @Override
                public void onCancel() {
                    inviteListener.cancel();
                }

                @Override
                public void onException(Throwable throwable) {
                    inviteListener.fail(throwable.getLocalizedMessage());
                }

                @Override
                public void onFail(String message) {
                    inviteListener.fail(message);
                }
            }, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(final Activity parentActivity, final AuthCallbacks.LoginListener loginListener) {
        SoomlaUtils.LogDebug(TAG, "login");
        SimpleFacebook.getInstance(parentActivity);
        WeakRefParentActivity = new WeakReference<Activity>(parentActivity);

        RefProvider = getProvider();
        RefLoginListener = loginListener;
        Intent intent = new Intent(parentActivity, SoomlaFBActivity.class);

        intent.putExtra("action", ACTION_LOGIN);
        parentActivity.startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout(final AuthCallbacks.LogoutListener logoutListener) {
        // todo: check if SimpleFacebook.getInstance().clean() is required to prevent leaking memory
        SimpleFacebook.getInstance().logout(new OnLogoutListener() {
            @Override
            public void onLogout() {
                logoutListener.success();
//                SimpleFacebook.getInstance().clean();
            }

        });
    }

    /**
     * @deprecated Use isLoggedIn() instead
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public boolean isLoggedIn(final Activity activity) {
        return this.isLoggedIn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoggedIn() {
        SoomlaUtils.LogDebug(TAG, "isLoggedIn");

        return (SimpleFacebook.getInstance() != null) &&
                SimpleFacebook.getInstance().isLogin();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getUserProfile(final AuthCallbacks.UserProfileListener userProfileListener) {
        SoomlaUtils.LogDebug(TAG, "getUserProfile -- " + SimpleFacebook.getInstance().toString());

        checkPermissions(Arrays.asList(Permission.USER_ABOUT_ME, Permission.USER_BIRTHDAY,
                Permission.USER_LIKES, Permission.USER_LOCATION), new AsyncCallback() {
            @Override
            public void call(String errorMessage) {
                Profile.Properties properties = new Profile.Properties.Builder()
                        .add(Profile.Properties.ID)
//                    .add(Profile.Properties.USER_NAME) //deprecated in v2
                        .add(Profile.Properties.NAME)
                        .add(Profile.Properties.EMAIL)
                        .add(Profile.Properties.FIRST_NAME)
                        .add(Profile.Properties.LAST_NAME)
                        .add(Profile.Properties.PICTURE)
                        .add(Profile.Properties.GENDER)
                        .add(Profile.Properties.LOCATION)
                        .add(Profile.Properties.LANGUAGE)
                        .build();

                SimpleFacebook.getInstance().getProfile(properties, new OnProfileListener() {
                    @Override
                    public void onComplete(Profile response) {
                        super.onComplete(response);
                        HashMap<String, Object> extraDict = new HashMap<String, Object>();
                        extraDict.put("access_token", SimpleFacebook.getInstance().getToken());
                        extraDict.put("permissions", new JSONArray(SimpleFacebook.getInstance().getGrantedPermissions()));
                        final UserProfile userProfile = new UserProfile(getProvider(),
                                response.getId(), response.getName(), response.getEmail(),
                                response.getFirstName(), response.getLastName(), extraDict);
                        userProfile.setAvatarLink(response.getPicture());
                        userProfile.setBirthday(response.getBirthday());

                        userProfile.setGender(response.getGender());
                        if (response.getLanguages() != null
                                && response.getLanguages().size() > 0
                                && response.getLanguages().get(0) != null) {
                            userProfile.setLanguage(response.getLanguages().get(0).getName());
                        }
                        if (response.getLocation() != null) {
                            userProfile.setLocation(response.getLocation().getName());
                        }
                        SoomlaUtils.LogDebug(TAG, "getUserProfile/onComplete" + " [" + userProfileListener + "]");
                        userProfileListener.success(userProfile);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        super.onException(throwable);
                        SoomlaUtils.LogWarning(TAG, "getUserProfile/onException: " + throwable.getLocalizedMessage() + " [" + userProfileListener + "]");
                        userProfileListener.fail("onException: " + throwable.getLocalizedMessage());
                    }

                    @Override
                    public void onFail(String reason) {
                        super.onFail(reason);
                        SoomlaUtils.LogWarning(TAG, "getUserProfile/onFail: " + reason + " [" + userProfileListener + "]");
                        userProfileListener.fail("onFail: " + reason);
                    }
                });
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(final String status, final SocialCallbacks.SocialActionListener socialActionListener) {
        SoomlaUtils.LogDebug(TAG, "updateStatus -- " + SimpleFacebook.getInstance().toString());

//        checkPermission(Permission.PUBLISH_ACTION, new AsyncCallback() {
//            @Override
//            public void call(String errorMessage) {
//                if (errorMessage != null) {
//                    socialActionListener.fail(errorMessage);
//                    return;
//                }
//                RefProvider = getProvider();
//                RefSocialActionListener = socialActionListener;
//                Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
//                intent.putExtra("action", ACTION_PUBLISH_STATUS);
//                intent.putExtra("status", status);
//                WeakRefParentActivity.get().startActivity(intent);
//            }
//        });
        RefProvider = getProvider();
        RefSocialActionListener = socialActionListener;
        Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
        intent.putExtra("action", ACTION_PUBLISH_STATUS);
        intent.putExtra("status", status);
        WeakRefParentActivity.get().startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatusDialog(final String link, final SocialCallbacks.SocialActionListener socialActionListener) {
        SoomlaUtils.LogDebug(TAG, "updateStatus -- " + SimpleFacebook.getInstance().toString());

        checkPermission(Permission.PUBLISH_ACTION, new AsyncCallback() {
            @Override
            public void call(String errorMessage) {
                if (errorMessage != null) {
                    socialActionListener.fail(errorMessage);
                    return;
                }
                RefProvider = getProvider();
                RefSocialActionListener = socialActionListener;
                Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
                intent.putExtra("action", ACTION_PUBLISH_STATUS_DIALOG);
                intent.putExtra("link", link);
                WeakRefParentActivity.get().startActivity(intent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStory(final String message, final String name, final String caption, final String description, final String link, final String picture,
                            final SocialCallbacks.SocialActionListener socialActionListener) {

        checkPermission(Permission.PUBLISH_ACTION, new AsyncCallback() {
            @Override
            public void call(String errorMessage) {
                if (errorMessage != null) {
                    socialActionListener.fail(errorMessage);
                    return;
                }

                RefProvider = getProvider();
                RefSocialActionListener = socialActionListener;
                Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
                intent.putExtra("action", ACTION_PUBLISH_STORY);
                intent.putExtra("message", message);
                intent.putExtra("name", name);
                intent.putExtra("caption", caption);
                intent.putExtra("description", description);
                intent.putExtra("link", link);
                intent.putExtra("picture", picture);
                WeakRefParentActivity.get().startActivity(intent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStoryDialog(final String name, final String caption, final String description, final String link, final String picture,
                                  final SocialCallbacks.SocialActionListener socialActionListener) {

        checkPermission(Permission.PUBLISH_ACTION, new AsyncCallback() {
            @Override
            public void call(String errorMessage) {
                if (errorMessage != null) {
                    socialActionListener.fail(errorMessage);
                    return;
                }

                RefProvider = getProvider();
                RefSocialActionListener = socialActionListener;
                Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
                intent.putExtra("action", ACTION_PUBLISH_STORY_DIALOG);
                intent.putExtra("name", name);
                intent.putExtra("caption", caption);
                intent.putExtra("description", description);
                intent.putExtra("link", link);
                intent.putExtra("picture", picture);
                WeakRefParentActivity.get().startActivity(intent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getContacts(final boolean fromStart, final SocialCallbacks.ContactsListener contactsListener) {

        checkPermission(Permission.USER_FRIENDS, new AsyncCallback() {
            @Override
            public void call(String errorMessage) {
                if (errorMessage != null) {
                    contactsListener.fail(errorMessage);
                    return;
                }

                RefProvider = getProvider();
                RefContactsListener = contactsListener;
                Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
                intent.putExtra("action", ACTION_GET_CONTACTS);
                intent.putExtra("fromStart", fromStart);
                WeakRefParentActivity.get().startActivity(intent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getFeed(final Boolean fromStart, final SocialCallbacks.FeedListener feedListener) {

        checkPermission(Permission.USER_POSTS, new AsyncCallback() {
            @Override
            public void call(String errorMessage) {
                if (errorMessage != null) {
                    feedListener.fail(errorMessage);
                    return;
                }

                RefProvider = getProvider();
                RefFeedListener = feedListener;
                Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
                intent.putExtra("action", ACTION_GET_FEED);
                intent.putExtra("fromStart", fromStart);
                WeakRefParentActivity.get().startActivity(intent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uploadImage(final String message, final String filePath, final SocialCallbacks.SocialActionListener socialActionListener) {
        SoomlaUtils.LogDebug(TAG, "uploadImage");

        checkPermission(Permission.PUBLISH_ACTION, new AsyncCallback() {
            @Override
            public void call(String errorMessage) {
                if (errorMessage != null) {
                    socialActionListener.fail(errorMessage);
                    return;
                }

                RefProvider = getProvider();
                RefSocialActionListener = socialActionListener;
                Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
                intent.putExtra("action", ACTION_UPLOAD_IMAGE);
                intent.putExtra("message", message);
                intent.putExtra("filePath", filePath);
                WeakRefParentActivity.get().startActivity(intent);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invite(final Activity parentActivity, String inviteMessage, String dialogTitle, final SocialCallbacks.InviteListener inviteListener) {
        WeakRefParentActivity = new WeakReference<Activity>(parentActivity);

        RefProvider = getProvider();
        RefInviteListener = inviteListener;
        Intent intent = new Intent(parentActivity, SoomlaFBActivity.class);

        intent.putExtra("action", ACTION_INVITE);
        intent.putExtra("message", inviteMessage);
        intent.putExtra("title", dialogTitle);
        parentActivity.startActivity(intent);

        WeakRefParentActivity.get().startActivity(intent);
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public void like(final Activity parentActivity, String pageId) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + pageId));
        parentActivity.startActivity(browserIntent);
    }

    @Override
    public void configure(Map<String, String> providerParams) {
        autoLogin = false;
        if (providerParams != null) {
            // extract autoLogin
            String autoLoginStr = providerParams.get("autoLogin");
            autoLogin = autoLoginStr != null && Boolean.parseBoolean(autoLoginStr);
        }

//        if (providerParams != null && providerParams.containsKey("permissions")) {
//            this.loginPermissions = parsePermissions(providerParams.get("permissions"));
//        } else {
            this.loginPermissions = Arrays.asList(DEFAULT_LOGIN_PERMISSIONS);
//        }

        configure();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Provider getProvider() {
        return Provider.FACEBOOK;
    }

    @Override
    public boolean isAutoLogin() {
        return autoLogin;
    }

    private void configure() {
        String fbAppId = "<fbAppId>";
        String fbAppNS = "<fbAppNS>";
        try {
            final Context appContext = SoomlaApp.getAppContext();
            ApplicationInfo ai = appContext.getPackageManager().
                    getApplicationInfo(appContext.getPackageName(),
                            PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            fbAppId = bundle.getString("com.facebook.sdk.ApplicationId");
            fbAppNS = bundle.getString("com.facebook.sdk.AppNS");
            SoomlaUtils.LogDebug(TAG, String.format(
                    "com.facebook.sdk.ApplicationId:%s com.facebook.sdk.AppNS:%s",
                    fbAppId, fbAppNS));
        } catch (PackageManager.NameNotFoundException e) {
            SoomlaUtils.LogError(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            SoomlaUtils.LogError(TAG, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }

        SimpleFacebookConfiguration configuration = new SimpleFacebookConfiguration.Builder()
                .setAppId(fbAppId)
                .setNamespace(fbAppNS)
                .setPermissions(this.loginPermissions.toArray(new Permission[this.loginPermissions.size()]))
                .build();

        SimpleFacebook.setConfiguration(configuration);
    }

    private void checkPermissions(List<Permission> requestedPermissions, final AsyncCallback callback) {
//        if (this.permissions == null) {
//            this.permissions = parsePermissions(SimpleFacebook.getInstance().getGrantedPermissions());
//        }
//
//        if (!this.permissions.containsAll(requestedPermissions)) {
//            List<Permission> missedPermissions = new ArrayList<Permission>(requestedPermissions);
//            missedPermissions.removeAll(this.permissions);
//            SoomlaUtils.LogDebug(TAG, "Requesting new permissions: " + missedPermissions);
//
//            SimpleFacebook.getInstance().requestNewPermissions(
//                    missedPermissions.toArray(new Permission[missedPermissions.size()]),
//                    new OnNewPermissionsListener() {
//
//                        @Override
//                        public void onSuccess(String accessToken, List<Permission> acceptedPermissions, List<Permission> declinedPermissions) {
//                            callback.call();
//                        }
//
//                        @Override
//                        public void onThinking() {
//                            // nothing do here
//                        }
//
//                        @Override
//                        public void onException(Throwable throwable) {
//                            callback.call("Exception happened while trying to request permissions: " + throwable);
//                        }
//
//                        @Override
//                        public void onFail(String message) {
//                            callback.call("Failed to request permissions with message: " + message);
//                        }
//                    });
//        } else {
            callback.call();
//        }
    }

    private void checkPermission(Permission requestedPermission, final AsyncCallback callback) {
        checkPermissions(Collections.singletonList(requestedPermission), callback);
    }

    private List<Permission> parsePermissions(Set<String> permissionStrArr) {
        List<Permission> permissionList = new ArrayList<Permission>();
        for (String permissionStr : permissionStrArr) {
            Permission permission = Permission.fromValue(permissionStr.trim());
            if (permission != null) {
                permissionList.add(permission);
            } else {
                SoomlaUtils.LogError(TAG, "Cannot recognize permission: '" + permissionStr + "' skipping it");
            }
        }

        return permissionList;
    }

    private List<Permission> parsePermissions(String permissionsStr) {
        if (permissionsStr == null) {
            throw new IllegalArgumentException();
        }

        return parsePermissions(new HashSet<String>(Arrays.asList(permissionsStr.split(","))));
    }

}


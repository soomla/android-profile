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
import android.os.Bundle;
import android.net.Uri;

import com.soomla.SoomlaApp;
import com.soomla.SoomlaUtils;
import com.soomla.profile.auth.AuthCallbacks;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.social.ISocialProvider;
import com.soomla.profile.social.SocialCallbacks;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.entities.Feed;
import com.sromku.simple.fb.entities.Photo;
import com.sromku.simple.fb.entities.Post;
import com.sromku.simple.fb.entities.Profile;
import com.sromku.simple.fb.entities.Story;
import com.sromku.simple.fb.listeners.OnFriendsListener;
import com.sromku.simple.fb.listeners.OnLoginListener;
import com.sromku.simple.fb.listeners.OnLogoutListener;
import com.sromku.simple.fb.listeners.OnPostsListener;
import com.sromku.simple.fb.listeners.OnProfileListener;
import com.sromku.simple.fb.listeners.OnPublishListener;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Soomla wrapper for SimpleFacebook (itself a wrapper to Android FB SDK).
 *
 * This class works by creating a transparent activity (SoomlaFBActivity) and working through it.
 * This is required to correctly integrate with FB activity lifecycle events
 */
public class SoomlaFacebook implements ISocialProvider {

    private static final String TAG = "SOOMLA SoomlaFacebook";

    // some weak refs that are set before launching the wrapper SoomlaFBActivity
    // (need to be accessed by static context)
    private static WeakReference<Activity> WeakRefParentActivity;
    private static Provider RefProvider;
    private static AuthCallbacks.LoginListener RefLoginListener;
    private static SocialCallbacks.SocialActionListener RefSocialActionListener;
    private static SocialCallbacks.FeedListener RefFeedListener;
    private static SocialCallbacks.ContactsListener RefContactsListener;

    public static final int ACTION_LOGIN = 0;

    public static final int ACTION_PUBLISH_STATUS = 10;
    public static final int ACTION_PUBLISH_STORY = 11;
    public static final int ACTION_UPLOAD_IMAGE = 12;
    public static final int ACTION_GET_FEED = 13;
    public static final int ACTION_GET_CONTACTS = 14;

    static {
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

        Permission[] permissions = new Permission[] {
                Permission.USER_PHOTOS,
                Permission.EMAIL,
                Permission.USER_FRIENDS, // GetContacts (but has limitations)
                Permission.READ_STREAM,  // GetFeed
                Permission.PUBLISH_ACTION
        };

        SimpleFacebookConfiguration configuration = new SimpleFacebookConfiguration.Builder()
                .setAppId(fbAppId)
                .setNamespace(fbAppNS)
                .setPermissions(permissions)
                .build();

        SimpleFacebook.setConfiguration(configuration);
    }

    /**
     * Constructor
     */
    public SoomlaFacebook() {

    }

    /**
     * The main SOOMLA Facebook activity
     *
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
                    login(this, RefLoginListener);
                    break;
                }
                case ACTION_PUBLISH_STATUS: {
                    String status = intent.getStringExtra("status");
                    updateStatus(status, RefSocialActionListener);
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

                case ACTION_UPLOAD_IMAGE: {
                    String message = intent.getStringExtra("message");
                    String filePath = intent.getStringExtra("filePath");
                    uploadImage(message, filePath, RefSocialActionListener);
                    break;
                }
                case ACTION_GET_FEED: {
                    getFeed(RefFeedListener);
                    break;
                }
                case ACTION_GET_CONTACTS: {
                    getContacts(RefContactsListener);
                    break;
                }
                default: {
                    SoomlaUtils.LogWarning(TAG, "action unknown:" + preformingAction);
                    break;
                }
            }
        }

        private void clearListeners()
        {
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
                case ACTION_PUBLISH_STORY: {
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
            SimpleFacebook.getInstance().onActivityResult(this, requestCode, resultCode, data);
        }

        private void login(Activity activity, final AuthCallbacks.LoginListener loginListener) {
            SoomlaUtils.LogDebug(TAG, "login");
            SimpleFacebook.getInstance().login(new OnLoginListener() {

                @Override
                public void onLogin() {
                    SoomlaUtils.LogDebug(TAG, "login/onLogin " + " [" + loginListener + "]");
                    loginListener.success(RefProvider);
                    clearListeners();
                    finish();
                }

                @Override
                public void onNotAcceptingPermissions(Permission.Type type) {
                    SoomlaUtils.LogDebug(TAG, "login/onNotAcceptingPermissions:" + type + " [" + loginListener + "]");
                    loginListener.fail("onNotAcceptingPermissions: " + type);
                    clearListeners();
                    finish();
                }

                @Override
                public void onThinking() {

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

        private void updateStatus(String status, final SocialCallbacks.SocialActionListener socialActionListener) {
            SoomlaUtils.LogDebug(TAG, "updateStatus -- " + SimpleFacebook.getInstance().toString());
            Feed feed = new Feed.Builder()
                    .setMessage(status)
                    .build();

            boolean withDialog = false;//todo: give another API with dialog
            SimpleFacebook.getInstance().publish(feed, withDialog, new OnPublishListener() {

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

            boolean withDialog = false;//todo: give another API with dialog
            SimpleFacebook.getInstance().publish(feed, withDialog, new OnPublishListener() {

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

        private void uploadImage(String message, String filePath, final SocialCallbacks.SocialActionListener socialActionListener) {
            SoomlaUtils.LogDebug(TAG, "uploadImage -- " + SimpleFacebook.getInstance().toString());
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            Photo photo = new Photo.Builder()
                    .setImage(bitmap)
                    .setName(message)
                    .build();

            SimpleFacebook.getInstance().publish(photo, new OnPublishListener() {

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

        private void getContacts(final SocialCallbacks.ContactsListener contactsListener) {
            Profile.Properties properties = new Profile.Properties.Builder()
                    .add(Profile.Properties.ID)
//                    .add(Profile.Properties.USER_NAME) //deprecated in v2
                    .add(Profile.Properties.NAME)
                    .add(Profile.Properties.EMAIL)
                    .add(Profile.Properties.FIRST_NAME)
                    .add(Profile.Properties.LAST_NAME)
                    .add(Profile.Properties.PICTURE)
                    .build();
            SimpleFacebook.getInstance().getFriends(properties, new OnFriendsListener() {

                @Override
                public void onComplete(List<Profile> response) {
                    super.onComplete(response);
                    SoomlaUtils.LogDebug(TAG, "getContacts/onComplete " + response.size());

                    List<UserProfile> userProfiles = new ArrayList<UserProfile>();
                    for (Profile profile : response) {
                        userProfiles.add(new UserProfile(
                                RefProvider, profile.getId(), profile.getUsername(), profile.getEmail(),
                                profile.getFirstName(), profile.getLastName(), Collections.<String, Object>emptyMap()));
                    }
                    contactsListener.success(userProfiles);
                    clearListeners();
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
        }

        public void getFeed(final SocialCallbacks.FeedListener feedListener) {
            SimpleFacebook.getInstance().getPosts(Post.PostType.ALL, new OnPostsListener() {

                @Override
                public void onComplete(List<Post> posts) {
                    super.onComplete(posts);
                    SoomlaUtils.LogDebug(TAG, "getFeed/onComplete" + " [" + feedListener + "]");

                    List<String> feeds = new ArrayList<String>();
                    for (Post post : posts) {
                        feeds.add(post.getMessage());
                    }
                    feedListener.success(feeds);
                    clearListeners();
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
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(final Activity parentActivity, final AuthCallbacks.LoginListener loginListener) {
        SoomlaUtils.LogDebug(TAG, "login");
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

            @Override
            public void onThinking() {

            }

            @Override
            public void onException(Throwable throwable) {
                logoutListener.fail("onException: " + throwable.getLocalizedMessage() + " [" + logoutListener + "]");
//                SimpleFacebook.getInstance().clean();
            }

            @Override
            public void onFail(String s) {
                logoutListener.fail("onFail: " + s + " [" + logoutListener + "]");
//                SimpleFacebook.getInstance().clean();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoggedIn(final Activity activity) {
        SoomlaUtils.LogDebug(TAG, "isLoggedIn");

        if (SimpleFacebook.getInstance() == null) {
            // SimpleFacebook was not initialized (should happen in login)
            WeakRefParentActivity = new WeakReference<Activity>(activity);
            return SimpleFacebook.getInstance(activity).isLogin();
        }
        else {
            return SimpleFacebook.getInstance().isLogin();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getUserProfile(final AuthCallbacks.UserProfileListener userProfileListener) {
        SoomlaUtils.LogDebug(TAG, "getUserProfile -- " + SimpleFacebook.getInstance().toString());
        Profile.Properties properties = new Profile.Properties.Builder()
                .add(Profile.Properties.ID)
//                    .add(Profile.Properties.USER_NAME) //deprecated in v2
                .add(Profile.Properties.NAME)
                .add(Profile.Properties.EMAIL)
                .add(Profile.Properties.FIRST_NAME)
                .add(Profile.Properties.LAST_NAME)
                .add(Profile.Properties.PICTURE)
                .build();

        SimpleFacebook.getInstance().getProfile(properties, new OnProfileListener() {
            @Override
            public void onComplete(Profile response) {
                super.onComplete(response);
                Map<String, Object> extra = new HashMap<String, Object>();
                extra.put("accessToken", SimpleFacebook.getInstance().getSession().getAccessToken());
                extra.put("expirationDate", SimpleFacebook.getInstance().getSession().getExpirationDate().getTime());
                final UserProfile userProfile = new UserProfile(getProvider(),
                        response.getId(), response.getName(), response.getEmail(),
                        response.getFirstName(), response.getLastName(), extra);
                userProfile.setAvatarLink(response.getPicture());
                // todo: verify extra permissions for these
//                    userProfile.setBirthday(response.getBirthday());
//                    userProfile.setGender(response.getGender());
//                    userProfile.setLanguage(response.getLanguages().get(0).getName());
//                    userProfile.setLocation(response.getLocation().getName());
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(String status, final SocialCallbacks.SocialActionListener socialActionListener) {
        SoomlaUtils.LogDebug(TAG, "updateStatus -- " + SimpleFacebook.getInstance().toString());

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
    public void updateStory(String message, String name, String caption, String description, String link, String picture,
                            final SocialCallbacks.SocialActionListener socialActionListener) {
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

    private void fbUpdateStory() {
        // set object to be shared
        Story.StoryObject storyObject = new Story.StoryObject.Builder()
                .setUrl("http://romkuapps.com/github/simple-facebook/object-apple.html")
                .setNoun("food")
                .build();

        // set action to be done
        Story.StoryAction storyAction = new Story.StoryAction.Builder()
                .setAction("eat")
                .addProperty("taste", "sweet")
                .build();

        // build story
        Story story = new Story.Builder()
                .setObject(storyObject)
                .setAction(storyAction)
                .build();

        SimpleFacebook.getInstance().publish(story, new OnPublishListener() {
            @Override
            public void onComplete(String response) {
                super.onComplete(response);
            }

            @Override
            public void onException(Throwable throwable) {
                super.onException(throwable);
            }

            @Override
            public void onFail(String reason) {
                super.onFail(reason);
            }

            @Override
            public void onThinking() {
                super.onThinking();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getContacts(final SocialCallbacks.ContactsListener contactsListener) {
        RefProvider = getProvider();
        RefContactsListener = contactsListener;
        Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
        intent.putExtra("action", ACTION_GET_CONTACTS);
        WeakRefParentActivity.get().startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getFeed(final SocialCallbacks.FeedListener feedListener) {
        RefProvider = getProvider();
        RefFeedListener = feedListener;
        Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
        intent.putExtra("action", ACTION_GET_FEED);
        WeakRefParentActivity.get().startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uploadImage(String message, String filePath, final SocialCallbacks.SocialActionListener socialActionListener) {
        SoomlaUtils.LogDebug(TAG, "uploadImage");
        RefProvider = getProvider();
        RefSocialActionListener = socialActionListener;
        Intent intent = new Intent(WeakRefParentActivity.get(), SoomlaFBActivity.class);
        intent.putExtra("action", ACTION_UPLOAD_IMAGE);
        intent.putExtra("message", message);
        intent.putExtra("filePath", filePath);
        WeakRefParentActivity.get().startActivity(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uploadImage(String message, String fileName, Bitmap bitmap, int jpegQuality, final SocialCallbacks.SocialActionListener socialActionListener) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void like(final Activity parentActivity, String pageName) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + pageName));
        parentActivity.startActivity(browserIntent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Provider getProvider() {
        return Provider.FACEBOOK;
    }
}


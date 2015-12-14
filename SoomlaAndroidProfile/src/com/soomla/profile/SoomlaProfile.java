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

package com.soomla.profile;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;

import android.net.Uri;
import android.os.*;
import android.view.View;
import com.soomla.BusProvider;
import com.soomla.SoomlaApp;
import com.soomla.SoomlaMarketUtils;
import com.soomla.SoomlaUtils;
import com.soomla.data.KeyValueStorage;
import com.soomla.profile.auth.AuthCallbacks;
import com.soomla.profile.auth.IAuthProvider;
import com.soomla.profile.data.UserProfileStorage;
import com.soomla.profile.domain.IProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.events.UserRatingEvent;
import com.soomla.profile.events.ProfileInitializedEvent;
import com.soomla.profile.events.auth.*;
import com.soomla.profile.events.social.*;
import com.soomla.profile.exceptions.ProviderNotFoundException;
import com.soomla.profile.social.ISocialProvider;
import com.soomla.profile.social.SocialCallbacks;
import com.soomla.rewards.Reward;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This is the main class for the SOOMLA User Profile module.  This class
 * should be initialized once, after <code>Soomla.initialize()</code> is invoked.
 * Use this class to perform authentication and social actions on behalf of
 * the user that will grant him \ her rewards in your game.
 */
@SuppressWarnings("UnusedDeclaration")
public class SoomlaProfile {

    public static final String VERSION = "1.1.9";
    private static final String DB_KEY_PREFIX = "soomla.profile";

    /**
     * see {@link #initialize(Activity, boolean, Map)}
     */
    public boolean initialize() {
        return initialize(null, null);
    }

    /**
     * see {@link #initialize(Activity, boolean, Map)}
     */
    public boolean initialize(Activity activity) {
        return initialize(activity, null);
    }

    /**
     * see {@link #initialize(Activity, boolean, Map)}
     */
    public boolean initialize(Map<IProvider.Provider, ? extends Map<String, String>> providerParams) {
        return initialize(null, false, providerParams);
    }

    /**
     * see {@link #initialize(Activity, boolean, Map)}
     */
    public boolean initialize(Activity activity, Map<IProvider.Provider, ? extends Map<String, String>> providerParams) {
        return initialize(activity, false, providerParams);
    }

    public boolean initialize(boolean usingExternalProvider, Map<IProvider.Provider, ? extends Map<String, String>> providerParams) {
        return initialize(null, false, providerParams);
    }

    /**
     * Initializes the Profile module.  Call this method after <code>Soomla.initialize()</code>
     * @param activity The parent activity
     * @param usingExternalProvider If using an external SDK (like Unity FB SDK) pass true
     *                              here so we know not to complain about native providers
     *                              not found
     * @param providerParams provides custom values for specific social providers
     */
    public boolean initialize(Activity activity, boolean usingExternalProvider, Map<IProvider.Provider, ? extends Map<String, String>> providerParams) {
        if (mInitialized) {
            String err = "SoomlaStore is already initialized. You can't initialize it twice!";
            SoomlaUtils.LogError(TAG, err);
            return false;
        }

        mProviderManager = new ProviderManager(providerParams,
                "com.soomla.profile.social.facebook.SoomlaFacebook",
                "com.soomla.profile.social.google.SoomlaGooglePlus",
                "com.soomla.profile.social.twitter.SoomlaTwitter"
        );

        mInitialized = true;

        BusProvider.getInstance().post(new ProfileInitializedEvent());

        if (activity == null) {
            SoomlaUtils.LogWarning(TAG,
                    "You want to use `autoLogin`, but have not provided the `activity`. Skipping auto login");
            return false;
        }
        this.settleAutoLogin(activity);

        return true;
    }

    public void settleAutoLogin(Activity activity) {
        List<IAuthProvider> authProviders = mProviderManager.getAllAuthProviders();
        for (IAuthProvider authProvider : authProviders) {
            if (authProvider.isAutoLogin()) {
                final IProvider.Provider provider = authProvider.getProvider();
                if (this.wasLoggedInWithProvider(provider)) {
                    final String payload = "";
                    final Reward reward = null;
                    if (authProvider.isLoggedIn()) {
                        setLoggedInForProvider(provider, false);
                        BusProvider.getInstance().post(new LoginStartedEvent(provider, true, payload));
                        authProvider.getUserProfile(new AuthCallbacks.UserProfileListener() {
                            @Override
                            public void success(UserProfile userProfile) {
                                UserProfileStorage.setUserProfile(userProfile);
                                setLoggedInForProvider(provider, true);
                                BusProvider.getInstance().post(new LoginFinishedEvent(userProfile, true, payload));
                            }

                            @Override
                            public void fail(String message) {
                                BusProvider.getInstance().post(new LoginFailedEvent(provider, message, true, payload));
                            }
                        });
                    } else {
                        login(activity, provider, true, payload, reward);
                    }
                }
            }
        }
    }

    private void afterLogin(final IProvider.Provider provider,
                            IAuthProvider authProvider, final boolean autoLogin, final String payload, final Reward reward) {
        authProvider.getUserProfile(new AuthCallbacks.UserProfileListener() {
            @Override
            public void success(UserProfile userProfile) {
                UserProfileStorage.setUserProfile(userProfile);
                setLoggedInForProvider(provider, true);
                BusProvider.getInstance().post(new LoginFinishedEvent(userProfile, autoLogin, payload));

                if (reward != null) {
                    reward.give();
                }
            }

            @Override
            public void fail(String message) {
                BusProvider.getInstance().post(new LoginFailedEvent(provider, message, autoLogin, payload));
            }
        });
    }

    private void setLoggedInForProvider(IProvider.Provider provider, boolean value) {
        String key = getLoggedInStorageKeyForProvider(provider);
        if (value) {
            KeyValueStorage.setValue(key, "true");
        } else {
            KeyValueStorage.deleteKeyValue(key);
        }
    }

    private boolean wasLoggedInWithProvider(IProvider.Provider provider) {
        return "true".equals(KeyValueStorage.getValue(getLoggedInStorageKeyForProvider(provider)));
    }

    private String getLoggedInStorageKeyForProvider(IProvider.Provider provider) {
        return String.format("%s.%s.%s", DB_KEY_PREFIX, provider.toString(), "loggedIn");
    }

    /**
     * Login to the given provider
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void login(Activity activity, final IProvider.Provider provider) throws ProviderNotFoundException {
        login(activity, provider, "", null);
    }

    /**
     * Login to the given provider and grant the user a reward.
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param reward   The reward to give the user for logging in.
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void login(Activity activity, final IProvider.Provider provider, final Reward reward) throws ProviderNotFoundException {
        login(activity, provider, "", reward);
    }

    /**
     * Login to the given provider and grant the user a reward.
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user for logging in.
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void login(Activity activity, final IProvider.Provider provider,
                      String payload, final Reward reward) throws ProviderNotFoundException {
        this.login(activity, provider, false, payload, reward);
    }

    /**
     * Login to the given provider and grant the user a reward.
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param autoLogin Allows to login automatically after launch
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user for logging in.
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void login(final Activity activity, final IProvider.Provider provider, final boolean autoLogin,
                      final String payload, final Reward reward) throws ProviderNotFoundException {
        final IAuthProvider authProvider = mProviderManager.getAuthProvider(provider);

        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                setLoggedInForProvider(provider, false);
                BusProvider.getInstance().post(new LoginStartedEvent(provider, autoLogin, payload));
                authProvider.login(activity, new AuthCallbacks.LoginListener() {
                    @Override
                    public void success(final IProvider.Provider provider) {
                        afterLogin(provider, authProvider, autoLogin, payload, reward);
                    }

                    @Override
                    public void fail(String message) {
                        BusProvider.getInstance().post(new LoginFailedEvent(provider, message, autoLogin, payload));
                    }

                    @Override
                    public void cancel() {
                        BusProvider.getInstance().post(new LoginCancelledEvent(provider, autoLogin, payload));
                    }
                });
            }
        });
    }

    /**
     * Checks if the user is logged-in to the given provider
     *
     * @deprecated Use isLoggedIn(IProvider.Provider provider) instead
     * @param activity The parent activity
     * @param provider The provider to use
     * @return true if the user is logged-in with the authentication provider,
     * false otherwise
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    @Deprecated
    public boolean isLoggedIn(Activity activity, final IProvider.Provider provider) throws ProviderNotFoundException {
        return this.isLoggedIn(provider);
    }

    /**
     * Checks if the user is logged-in to the given provider
     *
     *
     * @param provider The provider to use
     * @return true if the user is logged-in with the authentication provider,
     * false otherwise
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public boolean isLoggedIn(final IProvider.Provider provider) throws ProviderNotFoundException {
        final IAuthProvider authProvider = mProviderManager.getAuthProvider(provider);
        return authProvider.isLoggedIn();
    }

    /**
     * Logout from the given provider
     *
     * @param provider The provider to use
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void logout(final IProvider.Provider provider) throws ProviderNotFoundException {
        final IAuthProvider authProvider = mProviderManager.getAuthProvider(provider);
        final UserProfile userProfile = getStoredUserProfile(provider);

        if (!isLoggedIn(provider) && userProfile == null) {
            return;
        }

        BusProvider.getInstance().post(new LogoutStartedEvent(provider));
        setLoggedInForProvider(provider, false);

        if (!isLoggedIn(provider)) {
            UserProfileStorage.removeUserProfile(userProfile);
            BusProvider.getInstance().post(new LogoutFinishedEvent(provider));
            return;
        }

        authProvider.logout(new AuthCallbacks.LogoutListener() {
            @Override
            public void success() {
                if (userProfile != null) {
                    UserProfileStorage.removeUserProfile(userProfile);
                }
                // if caller needs stuff from the user, they should get it before logout
                // pass only the provider here
                BusProvider.getInstance().post(new LogoutFinishedEvent(provider));
            }

            @Override
            public void fail(String message) {
                BusProvider.getInstance().post(new LogoutFailedEvent(provider, message));
            }
        });
    }

    /**
     * Logout from all available providers
     */
    public void logoutFromAllProviders() {
        for (IProvider.Provider provider : IProvider.Provider.values()) {
            try {
                SoomlaProfile.getInstance().logout(provider);
            } catch (ProviderNotFoundException e) {
                // Skip
            }
        }
    }

    /**
     * Fetches the user's profile for the given provider from the local device storage
     *
     * @param provider The provider to use
     * @return The user profile
     */
    public UserProfile getStoredUserProfile(IProvider.Provider provider) {
        UserProfile userProfile = UserProfileStorage.getUserProfile(provider);
        if (userProfile == null) {
            return null;
        }
        return UserProfileStorage.getUserProfile(provider);
    }

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param status   The text to share
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatus(IProvider.Provider provider, String status, final Reward reward) throws ProviderNotFoundException {
        updateStatus(provider, status, "", reward);
    }

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param status   The text to share
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatus(IProvider.Provider provider, String status, String payload, final Reward reward) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);
        internalUpdateStatus(socialProvider, provider, status, payload, reward);
    }

    /**
     * Overloaded version of {@link #updateStatusWithConfirmation(com.soomla.profile.domain.IProvider.Provider, String, String, com.soomla.rewards.Reward, android.app.Activity, String)} without "customMessage"
     */
    public void updateStatusWithConfirmation(IProvider.Provider provider, String status, String payload, final Reward reward, final Activity activity) throws ProviderNotFoundException {
        this.updateStatusWithConfirmation(provider, status, payload, reward, activity, null);
    }

    /**
     * Shares the given status to the user's feed with confirmation dialog and grants the user a reward.
     *
     * @param provider The provider to use
     * @param status   The text to share
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @param activity activity to use as context for the dialog
     * @param customMessage the message to show in the dialog
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatusWithConfirmation(final IProvider.Provider provider, final String status, final String payload, final Reward reward, final Activity activity, final String customMessage) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        if (activity != null) {
            final String message = customMessage != null ? customMessage :
                    String.format("Are you sure you want to publish this message to %s: %s?",
                            provider.toString(), status);

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(activity)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Confirmation")
                            .setMessage(message)
                            .setPositiveButton("yes", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    internalUpdateStatus(socialProvider, provider, status, payload, reward);
                                }

                            })
                            .setNegativeButton("no", null)
                            .show();
                }
            });
        } else {
            internalUpdateStatus(socialProvider, provider, status, payload, reward);
        }
    }

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param provider The provider to us
     * @param link     The link to share (could be null)
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatusDialog(IProvider.Provider provider, String link, final Reward reward) throws ProviderNotFoundException {
        updateStatusDialog(provider, link, "", reward);
    }

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param provider The provider to us
     * @param link     The link to share (could be null)
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStatusDialog(final IProvider.Provider provider, final String link, final String payload, final Reward reward) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        final ISocialProvider.SocialActionType updateStatusType = ISocialProvider.SocialActionType.UPDATE_STATUS;
        BusProvider.getInstance().post(new SocialActionStartedEvent(provider, updateStatusType, payload));
        socialProvider.updateStatusDialog(link, new SocialCallbacks.SocialActionListener() {
            @Override
            public void success() {
                BusProvider.getInstance().post(new SocialActionFinishedEvent(provider, updateStatusType, payload));

                if (reward != null) {
                    reward.give();
                }
            }

            @Override
            public void fail(String message) {
                BusProvider.getInstance().post(new SocialActionFailedEvent(provider, updateStatusType, message, payload));
            }
        });
    }

    /**
     * Shares a story to the user's feed and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     The main text which will appear in the story
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStory(IProvider.Provider provider, String message, String name, String caption,
                            String description, String link, String picture,
                            final Reward reward) throws ProviderNotFoundException {

        updateStory(provider, message, name, caption, description, link, picture, "", reward);
    }

    /**
     * Shares a story to the user's feed and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     The main text which will appear in the story
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStory(IProvider.Provider provider, String message, String name, String caption,
                            String description, String link, String picture, String payload,
                            final Reward reward) throws ProviderNotFoundException {

        this.updateStoryWithConfirmation(provider, message, name, caption, description, link, picture, payload, reward, null, null);
    }

    /**
     * Overloaded version of {@link #updateStoryWithConfirmation(com.soomla.profile.domain.IProvider.Provider, String, String, String, String, String, String, String, com.soomla.rewards.Reward, android.app.Activity, String)} without "customMessage"
     */
    public void updateStoryWithConfirmation(IProvider.Provider provider, String message, String name, String caption,
                            String description, String link, String picture, String payload,
                            final Reward reward, final Activity activity) throws ProviderNotFoundException {

        this.updateStoryWithConfirmation(provider, message, name, caption, description, link, picture, payload, reward, activity, null);
    }

    /**
     * Shares a story to the user's feed with confirmation dialog and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     The main text which will appear in the story
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @param activity activity to use as context for the dialog
     * @param customMessage the message to show in the dialog
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStoryWithConfirmation(final IProvider.Provider provider, final String message, final String name, final String caption,
                            final String description, final String link, final String picture, final String payload,
                            final Reward reward, final Activity activity, final String customMessage) throws ProviderNotFoundException {

        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        if (activity != null) {
            final String messageToShow = customMessage != null ? customMessage :
                    String.format("Are you sure you want to publish to %s?", provider.toString());

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(activity)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Confirmation")
                            .setMessage(messageToShow)
                            .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    internalUpdateStory(provider, message, name, caption, description, link, picture, payload, reward, socialProvider);
                                }
                            })
                            .setNegativeButton("no", null)
                            .show();
                }
            });
        } else {
            internalUpdateStory(provider, message, name, caption, description, link, picture, payload, reward, socialProvider);
        }
    }

    /**
     * Shares a story to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param provider    The provider to use
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStoryDialog(IProvider.Provider provider, String name, String caption,
                                  String description, String link, String picture,
                                  final Reward reward) throws ProviderNotFoundException {

        updateStoryDialog(provider, name, caption, description, link, picture, "", reward);
    }

    /**
     * Shares a story to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param provider    The provider to use
     * @param name        The headline for the link which will be integrated in the
     *                    story
     * @param caption     The sub-headline for the link which will be
     *                    integrated in the story
     * @param description description The description for the link which will be
     *                    integrated in the story
     * @param link        The link which will be integrated into the user's story
     * @param picture     a Link to a picture which will be featured in the link
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward which will be granted to the user upon a
     *                    successful update
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void updateStoryDialog(final IProvider.Provider provider, final String name, final String caption,
                                  final String description, final String link, final String picture, final String payload,
                                  final Reward reward) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        final ISocialProvider.SocialActionType updateStoryType = ISocialProvider.SocialActionType.UPDATE_STORY;
        BusProvider.getInstance().post(new SocialActionStartedEvent(provider, updateStoryType, payload));
        socialProvider.updateStoryDialog(name, caption, description, link, picture,
                new SocialCallbacks.SocialActionListener() {
                    @Override
                    public void success() {
                        BusProvider.getInstance().post(new SocialActionFinishedEvent(provider, updateStoryType, payload));

                        if (reward != null) {
                            reward.give();
                        }
                    }

                    @Override
                    public void fail(String message) {
                        BusProvider.getInstance().post(new SocialActionFailedEvent(provider, updateStoryType, message, payload));
                    }
                }
        );
    }

    /**
     * Shares a photo to the user's feed and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     A text that will accompany the image
     * @param fileName    The desired image's file name
     * @param bitmap      The image to share
     * @param jpegQuality The image's numeric quality
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward to grant for sharing the photo
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImage(IProvider.Provider provider,
                            String message, String fileName, Bitmap bitmap, int jpegQuality,
                            String payload, final Reward reward) throws ProviderNotFoundException {

        this.uploadImage(provider, message, fileName, bitmap, jpegQuality, payload, reward, null, null);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void uploadImage(final IProvider.Provider provider,
                            final String message, final String fileName, final Bitmap bitmap, final int jpegQuality,
                            final String payload, final Reward reward, final Activity activity, String customMessage) throws ProviderNotFoundException {

        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        if (activity != null) {
            final String messageToShow = customMessage != null ? customMessage :
                    String.format("Are you sure you want to upload image to %s?", provider.toString());

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(activity)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Confirmation")
                            .setMessage(messageToShow)
                            .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    internalUploadImage(provider, message, fileName, bitmap, jpegQuality, payload, reward, socialProvider);
                                }
                            })
                            .setNegativeButton("no", null)
                            .show();
                }
            });
        } else {
            internalUploadImage(provider, message, fileName, bitmap, jpegQuality, payload, reward, socialProvider);
        }
    }

    /**
     * Shares a photo to the user's feed with confirmation dialog and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     A text that will accompany the image
     * @param fileName    The desired image's file name
     * @param bitmap      The image to share
     * @param jpegQuality The image's numeric quality
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward to grant for sharing the photo
     * @param activity If defined, confirmation confirmation dialog will be shown before the action
     * @param customMessage the message to show in the dialog
     * @throws ProviderNotFoundException if the supplied provider is not supported by the framework
     */
    public void uploadImageWithConfirmation(IProvider.Provider provider,
                                            String message, String fileName, Bitmap bitmap, int jpegQuality,
                                            String payload, final Reward reward, Activity activity, String customMessage) throws ProviderNotFoundException {

        this.uploadImage(provider, message, fileName, bitmap, jpegQuality, payload, reward, activity, customMessage);
    }

    /**
     * Overloaded version of {@link #uploadImageWithConfirmation(com.soomla.profile.domain.IProvider.Provider, String, String, android.graphics.Bitmap, int, String, com.soomla.rewards.Reward, android.app.Activity, String)} without "customMessage"
     */
    public void uploadImageWithConfirmation(IProvider.Provider provider,
                            String message, String fileName, Bitmap bitmap, int jpegQuality,
                            String payload, final Reward reward) throws ProviderNotFoundException {
        this.uploadImage(provider, message, fileName, bitmap, jpegQuality, payload, reward, null, null);
    }

    /**
     * Shares a photo to the user's feed and grants the user a reward.
     *
     * @param provider    The provider to use
     * @param message     A text that will accompany the image
     * @param file        An image file handler
     * @param payload     a String to receive when the function returns.
     * @param reward      The reward to grant for sharing the photo
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImage(IProvider.Provider provider, String message, File file,  String payload, final Reward reward) throws ProviderNotFoundException{
        if (file == null){
            SoomlaUtils.LogError(TAG, "(uploadImage) File is null!");
            return;
        }

        this.uploadImage(provider, message, file.getAbsolutePath(), payload, reward);
    }

    /**
     * Shares a photo to the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param message  A text that will accompany the image
     * @param filePath The desired image's location on the device
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImage(IProvider.Provider provider,
                            String message, String filePath,
                            final Reward reward) throws ProviderNotFoundException {
        uploadImage(provider, message, filePath, "", reward);
    }


    /**
     * Shares a photo to the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param message  A text that will accompany the image
     * @param filePath The desired image's location on the device
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImage(IProvider.Provider provider,
                            String message, String filePath, String payload,
                            final Reward reward) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);
        internalUploadImage(provider, message, filePath, payload, reward, socialProvider);
    }

    /**
     * Overloaded version of {@link #uploadImageWithConfirmation(com.soomla.profile.domain.IProvider.Provider, String, String, String, com.soomla.rewards.Reward, android.app.Activity, String)}} without "customMessage"
     */
    public void uploadImageWithConfirmation(IProvider.Provider provider,
                            String message, String filePath, String payload,
                            final Reward reward, Activity activity) throws ProviderNotFoundException {
        this.uploadImageWithConfirmation(provider, message, filePath, payload, reward, activity, null);
    }

    /**
     * Shares a photo to the user's feed with confirmation dialog and grants the user a reward.
     *
     * @param provider The provider to use
     * @param message  A text that will accompany the image
     * @param filePath The desired image's location on the device
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @param activity activity to use as context for the dialog
     * @param customMessage the message to show in the dialog
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadImageWithConfirmation(final IProvider.Provider provider,
                            final String message, final String filePath, final String payload,
                            final Reward reward, final Activity activity, final String customMessage) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        if (activity != null) {
            final String messageToShow = customMessage != null ? customMessage :
                    String.format("Are you sure you want to upload image to %s?", provider.toString());

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(activity)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Confirmation")
                            .setMessage(messageToShow)
                            .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    internalUploadImage(provider, message, filePath, payload, reward, socialProvider);
                                }
                            })
                            .setNegativeButton("no", null)
                            .show();
                }
            });
        } else {
            internalUploadImage(provider, message, filePath, payload, reward, socialProvider);
        }
    }

    /**
     * Shares a current screenshot to the user's feed and grants the user a reward.
     * @param activity activity to use as context for the screenshot
     * @param provider The provider to use
     * @param message  A text that will accompany the image
     * @param title  A title of post
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadCurrentScreenshot(final Activity activity, IProvider.Provider provider, String title, String message) throws ProviderNotFoundException {
        uploadCurrentScreenshot(activity, provider, title, message, "", null);
    }

    /**
     * Shares a current screenshot to the user's feed and grants the user a reward.
     * @param activity activity to use as context for the screenshot
     * @param provider The provider to use
     * @param message  A text that will accompany the image
     * @param title  A title of post
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to give the user
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void uploadCurrentScreenshot(final Activity activity, IProvider.Provider provider, String title, String message, String payload, Reward reward)
            throws ProviderNotFoundException {
        this.uploadImage(provider, message, takeScreenshot(activity), payload, reward);
    }

    /**
     * Takes curent app screenshot
     *
     * @param activity An activity used as context
     * @return A file points to current screenshot
     */
    private File takeScreenshot(final Activity activity) {
        try {
            String mPath = Environment.getExternalStorageDirectory().toString() + "/current_screenshot.jpg";

            // create bitmap screen capture
            View v1 = activity.getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            return imageFile;
        } catch (Throwable e) {

            // Several error may come out with file handling or OOM
            e.printStackTrace();
            return null;
        }
    }

    /**

    /**
     * Fetches the user's contact list and grants the user a reward.
     *
     * @param provider The provider to use
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void getContacts(IProvider.Provider provider, final Reward reward) throws ProviderNotFoundException {
        getContacts(provider, false, "", reward);
    }

    /**
     * Fetches the user's contact list and grants the user a reward.
     *
     * @param provider The provider to use
     * @param fromStart Should we reset pagination or request the next page
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void getContacts(IProvider.Provider provider, boolean fromStart, final Reward reward) throws ProviderNotFoundException {
        getContacts(provider, fromStart, "", reward);
    }

    /**
     * Fetches the user's contact list and grants the user a reward.
     *
     * @param provider The provider to use
     * @param fromStart Should we reset pagination or request the next page
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void getContacts(final IProvider.Provider provider, final boolean fromStart, final String payload, final Reward reward) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        final ISocialProvider.SocialActionType getContactsType = ISocialProvider.SocialActionType.GET_CONTACTS;
        BusProvider.getInstance().post(new GetContactsStartedEvent(provider, getContactsType, fromStart, payload));
        socialProvider.getContacts(fromStart, new SocialCallbacks.ContactsListener() {
                    @Override
                    public void success(List<UserProfile> contacts, boolean hasMore) {
                        BusProvider.getInstance().post(new GetContactsFinishedEvent(provider, getContactsType, contacts, payload, hasMore));

                        if (reward != null) {
                            reward.give();
                        }
                    }

                    @Override
                    public void fail(String message) {
                        BusProvider.getInstance().post(new GetContactsFailedEvent(provider, getContactsType, message, fromStart, payload));
                    }
                }
        );
    }

    /**
     * Fetches the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void getFeed(IProvider.Provider provider, final Reward reward) throws ProviderNotFoundException {
        getFeed(provider, false, "", reward);
    }

    /**
     * Fetches the user's feed and grants the user a reward.
     *
     * @param provider The provider to use
     * @param fromStart Should we reset pagination or request the next page
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not supported by the framework
     */
    public void getFeed(final IProvider.Provider provider, final Boolean fromStart, final String payload, final Reward reward) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        final ISocialProvider.SocialActionType getFeedType = ISocialProvider.SocialActionType.GET_FEED;
        BusProvider.getInstance().post(new GetFeedStartedEvent(provider, getFeedType, fromStart, payload));
        socialProvider.getFeed(fromStart, new SocialCallbacks.FeedListener() {
                    @Override
                    public void success(List<String> feedPosts, boolean hasMore) {
                        BusProvider.getInstance().post(new GetFeedFinishedEvent(provider, getFeedType, feedPosts, payload, hasMore));

                        if (reward != null) {
                            reward.give();
                        }
                    }

                    @Override
                    public void fail(String message) {
                        BusProvider.getInstance().post(new GetFeedFailedEvent(provider, getFeedType, message, fromStart, payload));
                    }
                }
        );
    }

    /**
     * Sends an invite.
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param inviteMessage a message will send to recipients.
     * @throws ProviderNotFoundException if the supplied provider is not supported by the framework
     */
    public void invite(final Activity activity, IProvider.Provider provider, String inviteMessage) {
        invite(activity, provider, inviteMessage, null, "", null);
    }

    /**
     * Sends an invite.
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param inviteMessage a message will send to recipients.
     * @param dialogTitle a title of app request dialog.
     * @param payload  a String to receive when the function returns.
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not supported by the framework
     */
    public void invite(final Activity activity, final IProvider.Provider provider, final String inviteMessage, final String dialogTitle, final String payload, final Reward reward) {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);

        final ISocialProvider.SocialActionType inviteType = ISocialProvider.SocialActionType.INVITE;
        BusProvider.getInstance().post(new InviteStartedEvent(provider, inviteType, payload));
        socialProvider.invite(activity, inviteMessage, dialogTitle, new SocialCallbacks.InviteListener() {
            @Override
            public void success(String requestId, List<String> invitedIds) {
                BusProvider.getInstance().post(new InviteFinishedEvent(provider, inviteType, requestId, invitedIds, payload));

                if (reward != null) {
                    reward.give();
                }
            }

            @Override
            public void fail(String message) {
                BusProvider.getInstance().post(new InviteFailedEvent(provider, inviteType, message, payload));
            }

            @Override
            public void cancel() {
                BusProvider.getInstance().post(new InviteCancelledEvent(provider, inviteType, payload));
            }
        });
    }

    /**
     * Opens up a provider page to "like" (external), and grants the user the supplied reward
     *
     * @param activity The parent activity
     * @param provider The provider to use
     * @param pageId The page to open up
     * @param reward   The reward to grant
     * @throws ProviderNotFoundException if the supplied provider is not
     *                                   supported by the framework
     */
    public void like(final Activity activity, final IProvider.Provider provider,
                     String pageId,
                     final Reward reward) throws ProviderNotFoundException {
        final ISocialProvider socialProvider = mProviderManager.getSocialProvider(provider);
        socialProvider.like(activity, pageId);

        if (reward != null) {
            reward.give();
        }
    }

    /**
     * Utility method to open up the market application rating page
     *
     * @param context The main context of the app
     */
    public void openAppRatingPage(Context context) {
        SoomlaMarketUtils.openMarketAppPage(context);

        BusProvider.getInstance().post(new UserRatingEvent());
    }

    /**
     * Shares text and/or image using native sharing functionality of your target platform.
     * @param text Text to share
     * @param imageFilePath Path to an image file to share
     */
    public void multiShare(String text, String imageFilePath) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        if (imageFilePath != null) {
            sharingIntent.setType("*/*");
            Uri uri = Uri.parse("file://" + imageFilePath);
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        } else {
            sharingIntent.setType("text/plain");
        }
        sharingIntent.putExtra(Intent.EXTRA_TEXT, text);
        Intent chooser = Intent.createChooser(sharingIntent, "Share");
        chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SoomlaApp.getAppContext().startActivity(chooser);
    }

    /*
     * Helper methods
     */

    private final Handler mainThread = new Handler(Looper.getMainLooper());
    protected void runOnMainThread(Runnable toRun) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            toRun.run();
        } else {
            mainThread.post(toRun);
        }
    }

    private void internalUpdateStatus(ISocialProvider socialProvider, final IProvider.Provider provider, String status, final String payload, final Reward reward) {
        final ISocialProvider.SocialActionType updateStatusType = ISocialProvider.SocialActionType.UPDATE_STATUS;
        BusProvider.getInstance().post(new SocialActionStartedEvent(provider, updateStatusType, payload));
        socialProvider.updateStatus(status, new SocialCallbacks.SocialActionListener() {
            @Override
            public void success() {
                BusProvider.getInstance().post(new SocialActionFinishedEvent(provider, updateStatusType, payload));

                if (reward != null) {
                    reward.give();
                }
            }

            @Override
            public void fail(String message) {
                BusProvider.getInstance().post(new SocialActionFailedEvent(provider, updateStatusType, message, payload));
            }
        });
    }

    private void internalUpdateStory(final IProvider.Provider provider, String message, String name, String caption, String description, String link, String picture, final String payload, final Reward reward, ISocialProvider socialProvider) {
        final ISocialProvider.SocialActionType updateStoryType = ISocialProvider.SocialActionType.UPDATE_STORY;
        BusProvider.getInstance().post(new SocialActionStartedEvent(provider, updateStoryType, payload));
        socialProvider.updateStory(message, name, caption, description, link, picture,
                new SocialCallbacks.SocialActionListener() {
                    @Override
                    public void success() {
                        BusProvider.getInstance().post(new SocialActionFinishedEvent(provider, updateStoryType, payload));

                        if (reward != null) {
                            reward.give();
                        }
                    }

                    @Override
                    public void fail(String message) {
                        BusProvider.getInstance().post(new SocialActionFailedEvent(provider, updateStoryType, message, payload));
                    }
                }
        );
    }

    private void internalUploadImage(final IProvider.Provider provider, String message, String filePath, final String payload, final Reward reward, ISocialProvider socialProvider) {
        final ISocialProvider.SocialActionType uploadImageType = ISocialProvider.SocialActionType.UPLOAD_IMAGE;
        BusProvider.getInstance().post(new SocialActionStartedEvent(provider, uploadImageType, payload));
        socialProvider.uploadImage(message, filePath, new SocialCallbacks.SocialActionListener() {
                    @Override
                    public void success() {
                        BusProvider.getInstance().post(new SocialActionFinishedEvent(provider, uploadImageType, payload));

                        if (reward != null) {
                            reward.give();
                        }
                    }

                    @Override
                    public void fail(String message) {
                        BusProvider.getInstance().post(new SocialActionFailedEvent(provider, uploadImageType, message, payload));
                    }
                }
        );
    }

    private void internalUploadImage(final IProvider.Provider provider, final String message, String fileName, Bitmap bitmap, int jpegQuality, final String payload, final Reward reward, final ISocialProvider socialProvider) {
        final ISocialProvider.SocialActionType uploadImageType = ISocialProvider.SocialActionType.UPLOAD_IMAGE;
        BusProvider.getInstance().post(new SocialActionStartedEvent(provider, uploadImageType, payload));

        //Save a temp image to external storage in background and try to upload it when finished
        new AsyncTask<TempImage, Object, File>() {

            @Override
            protected File doInBackground(TempImage... params) {
                try {
                    return params[0].writeToStorage();
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final File result){
                if (result == null){
                    BusProvider.getInstance().post(new SocialActionFailedEvent(provider, uploadImageType, "No image file to upload.", payload));
                    return;
                }

                socialProvider.uploadImage(message, result.getAbsolutePath(), new SocialCallbacks.SocialActionListener() {
                            @Override
                            public void success() {
                                BusProvider.getInstance().post(new SocialActionFinishedEvent(provider, uploadImageType, payload));

                                if (reward != null) {
                                    reward.give();
                                }

                                result.delete();
                            }

                            @Override
                            public void fail(String message) {
                                BusProvider.getInstance().post(new SocialActionFailedEvent(provider, uploadImageType, message, payload));

                                result.delete();
                            }
                        }
                );
            }
        }.execute(new TempImage(fileName, bitmap, jpegQuality));
    }

    /*
     * Temp image class - required to correct usage of `uploadImage` methods
     */
    private class TempImage {

        public TempImage(String aFileName, Bitmap aBitmap, int aJpegQuality){
            this.mFileName = aFileName;
            this.mImageBitmap = aBitmap;
            this.mJpegQuality = aJpegQuality;
        }

        protected File writeToStorage() throws IOException {
            SoomlaUtils.LogDebug(TAG, "Saving temp image file.");

            File tempDir = new File(getTempImageDir());
            tempDir.mkdirs();
            BufferedOutputStream bos = null;

            try{
                File file = new File(tempDir.toString() + this.mFileName);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                bos = new BufferedOutputStream(fileOutputStream);

                String extension = this.mFileName.substring((this.mFileName.lastIndexOf(".") + 1), this.mFileName.length());
                Bitmap.CompressFormat format = ("png".equals(extension) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG);

                this.mImageBitmap.compress(format, this.mJpegQuality, bos);

                bos.flush();
                return file;

            } catch (Exception e){
                SoomlaUtils.LogError(TAG, "(save) Failed saving temp image file: " + this.mFileName + " with error: " + e.getMessage());

            } finally {
                if (bos != null){
                    bos.close();
                }
            }

            return null;
        }

        private String getTempImageDir(){
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                SoomlaUtils.LogDebug(TAG, "(getTempImageDir) External storage not ready.");
                return null;
            }

            ContextWrapper soomContextWrapper = new ContextWrapper(SoomlaApp.getAppContext());

            return Environment.getExternalStorageDirectory() + soomContextWrapper.getFilesDir().getPath() + "/temp/";
        }

        final String TAG = "TempImageFile";
        Bitmap mImageBitmap;
        String mFileName;
        int mJpegQuality;
    }

    /**
     * Private Members *
     */

    private ProviderManager mProviderManager;


    /**
     * Singleton *
     */

    private SoomlaProfile() {
    }

    private static final SoomlaProfile mInstance = new SoomlaProfile();

    public static SoomlaProfile getInstance() {
        return mInstance;
    }

    private static final String TAG = "SOOMLA SoomlaProfile";

    private boolean mInitialized = false;
}

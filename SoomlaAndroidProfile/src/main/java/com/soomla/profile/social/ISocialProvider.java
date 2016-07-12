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

package com.soomla.profile.social;

import android.app.Activity;

import com.soomla.profile.auth.IAuthProvider;
import com.soomla.profile.domain.IProvider;

/**
 * A provider that exposes social capabilities such as sharing, fetching user
 * feeds, uploading images etc.
 */
public interface ISocialProvider extends IProvider {

    /**
     * Shares the given status to the user's feed
     *
     * @param status               the text to share
     * @param socialActionListener a callback for this action
     */
    void updateStatus(String status, SocialCallbacks.SocialActionListener socialActionListener);

    /**
     * Shares the given status to the user's feed and grants the user a reward.
     * Using the provider's native dialog (when available).
     *
     * @param link                 The link to share (could be null)
     * @param socialActionListener a callback for this action
     */
    void updateStatusDialog(String link, SocialCallbacks.SocialActionListener socialActionListener);

    /**
     * Share a story to the user's feed.  This is very oriented for Facebook.
     *
     * @param message              The main text which will appear in the story
     * @param name                 The headline for the link which will be integrated in the
     *                             story
     * @param caption              The sub-headline for the link which will be
     *                             integrated in the story
     * @param description          description The description for the link which will be
     *                             integrated in the story
     * @param link                 The link which will be integrated into the user's story
     * @param picture              a Link to a picture which will be featured in the link
     * @param socialActionListener a callback for this action
     */
    void updateStory(String message, String name, String caption, String description,
                     String link, String picture,
                     SocialCallbacks.SocialActionListener socialActionListener);

    /**
     * Share a story to the user's feed.  This is very oriented for Facebook.
     *
     * @param name                 The headline for the link which will be integrated in the
     *                             story
     * @param caption              The sub-headline for the link which will be
     *                             integrated in the story
     * @param description          description The description for the link which will be
     *                             integrated in the story
     * @param link                 The link which will be integrated into the user's story
     * @param picture              a Link to a picture which will be featured in the link
     * @param socialActionListener a callback for this action
     */
    void updateStoryDialog(String name, String caption, String description,
                           String link, String picture,
                           SocialCallbacks.SocialActionListener socialActionListener);

    /**
     * Fetches the user's contact list
     * @param fromStart Should we reset pagination or request the next page
     * @param contactsListener a callback for this action
     */
    void getContacts(boolean fromStart, SocialCallbacks.ContactsListener contactsListener);

    /**
     * Fetches the user's feed.
     *
     * @param fromStart Should we reset pagination or request the next page
     * @param feedsListener a callback for this action
     */
    void getFeed(Boolean fromStart, SocialCallbacks.FeedListener feedsListener);


    /**
     * Send an invite.
     *
     * @param activity The parent activity
     * @param inviteMessage a message which will send
     * @param dialogTitle a title of invitation dialog
     * @param inviteListener a callback for this action
     */
    void invite(final Activity activity, String inviteMessage, String dialogTitle, SocialCallbacks.InviteListener inviteListener);

    /**
     * Shares a photo to the user's feed
     *
     * @param message              A text that will accompany the image
     * @param filePath             The desired image's location on the device (full path)
     * @param socialActionListener a callback for this action
     */
    void uploadImage(String message, String filePath,
                     SocialCallbacks.SocialActionListener socialActionListener);

    /**
     * Opens up a "like" page for current provider (external)
     *
     * @param activity The parent activity
     * @param pageId The page to open up
     */
    void like(final Activity activity, String pageId);

    /**
     * an Enumeration which lists all available social actions
     */
    public enum SocialActionType {
        UPDATE_STATUS(0), UPDATE_STORY(1), UPLOAD_IMAGE(2), GET_CONTACTS(3), GET_FEED(4), INVITE(5);

        SocialActionType(final int value) {
            this.mValue = value;
        }

        private final int mValue;

        public int getValue() {
            return mValue;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            String result = "";
            switch (this) {
                case UPDATE_STATUS:
                    result = "UPDATE_STATUS";
                    break;
                case UPDATE_STORY:
                    result = "UPDATE_STORY";
                    break;
                case UPLOAD_IMAGE:
                    result = "UPLOAD_IMAGE";
                    break;
                case GET_CONTACTS:
                    result = "GET_CONTACTS";
                    break;
                case GET_FEED:
                    result = "GET_FEED";
                    break;
                case INVITE:
                    result = "INVITE";
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            return result;
        }

        /**
         * Converts the supplied <code>String</code> to
         * <code>SocialActionType</code> if possible
         *
         * @param value The string to convert to <code>SocialActionType</code>
         * @return value corresponding to the supplied string
         * @throws IllegalArgumentException if the supplied string does not
         *                                  have a corresponding <code>SocialActionType</code>
         */
        public static SocialActionType getEnum(String value) throws IllegalArgumentException {
            for (SocialActionType t : values()) {
                if (t.toString().equalsIgnoreCase(value)) return t;
            }
            throw new IllegalArgumentException();
        }
    }
}

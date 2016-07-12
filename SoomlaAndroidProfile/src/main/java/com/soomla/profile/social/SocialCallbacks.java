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

package com.soomla.profile.social;

import com.soomla.profile.domain.UserProfile;

import java.util.List;

/**
 * A utility class that defines interfaces for passing callbacks to social events.
 */
public class SocialCallbacks {

    /**
     * Listens for social action event without return value
     */
   public interface SocialActionListener {

        /**
         * Performs the following function upon success.
         */
       public void success();

        /**
         * Performs the following function upon failure and prints the given message.
         *
         * @param message reason for failure
         */
       public void fail(String message);
   }

    /**
     * Listens for fetching user profile event
     */
    public interface UserProfileListener {

        /**
         * Performs the following function upon success.
         */
        public void success(UserProfile userProfile);

        /**
         * Performs the following function upon failure and prints the given message.
         *
         * @param message reason for failure
         */
        public void fail(String message);
    }

    /**
     * Listens for fetching contacts event
     */
    public interface ContactsListener {

        /**
         * Performs the following function upon success.
         */
        public void success(List<UserProfile> userProfiles, boolean hasMore);

        /**
         * Performs the following function upon failure and prints the given message.
         *
         * @param message reason for failure
         */
        public void fail(String message);
    }

    /**
     * Listens for fetching feed event
     */
    public interface FeedListener {

        /**
         * Performs the following function upon success.
         */
        //todo: model feed
        public void success(List<String> feeds, boolean hasMore);

        /**
         * Performs the following function upon failure and prints the given message.
         *
         * @param message reason for failure
         */
        public void fail(String message);
    }

    public interface InviteListener {
        /**
         * Performs the following function upon success.
         */
        public void success(String requestId, List<String> invitedIds);

        /**
         * Performs the following function upon failure and prints the given message.
         *
         * @param message reason for failure
         */
        public void fail(String message);

        /**
         * Performs the following function if invitation is cancelled.
         */
        public void cancel();
    }
}

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

package com.soomla.profile.data;

import android.text.TextUtils;

import com.soomla.BusProvider;
import com.soomla.SoomlaUtils;
import com.soomla.data.KeyValueStorage;
import com.soomla.profile.domain.IProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.events.UserProfileUpdatedEvent;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A utility class for fetching and storing user profile info locally on
 * the device.
 */
public class UserProfileStorage {

    /**
     * Persists the given user profile to the device storage and sends out
     * a <code>UserProfileUpdatedEvent</code> event
     *
     * @param userProfile the user profile to save
     */
    public static void setUserProfile(UserProfile userProfile) {
        setUserProfile(userProfile, true);
    }

    /**
     * Persists the given user profile to the device storage, and adds the
     * ability to suppress related events
     *
     * @param userProfile the user profile to save
     * @param notify should an event regarding the save be fired
     */
    public static void setUserProfile(UserProfile userProfile, boolean notify) {
        String userProfileJSON = userProfile.toJSONObject().toString();
        String key = keyUserProfile(userProfile.getProvider());

        KeyValueStorage.setValue(key, userProfileJSON);

        if (notify) {
            BusProvider.getInstance().post(new UserProfileUpdatedEvent(userProfile));
        }
    }

    /**
     * Removes the given user profile from the device storage
     *
     * @param userProfile the user profile to remove
     */
    public static void removeUserProfile(UserProfile userProfile) {
        String key = keyUserProfile(userProfile.getProvider());

        KeyValueStorage.deleteKeyValue(key);
    }

    /**
     * Fetches the user profile stored for the given provider
     *
     * @param provider the provider which will be used to fetch the user profile
     * @return a user profile
     */
    public static UserProfile getUserProfile(IProvider.Provider provider) {
        String userProfileJSON = KeyValueStorage.getValue(keyUserProfile(provider));
        if (TextUtils.isEmpty(userProfileJSON)) {
            return null;
        }

        try {
            JSONObject upJSON = new JSONObject(userProfileJSON);
            return new UserProfile(upJSON);
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "Couldn't create UserProfile from json: " + userProfileJSON);
        }

        return null;
    }


    /** Private Members **/

    private static String keyUserProfile(IProvider.Provider provider) {
        return DB_KEY_PREFIX + "userprofile." + provider.toString();
    }

    private static final String DB_KEY_PREFIX = "soomla.profile.";
    private static final String TAG = "SOOMLA UserProfileStorage";
}

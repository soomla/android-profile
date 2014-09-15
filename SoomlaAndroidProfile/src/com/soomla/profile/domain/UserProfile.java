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

package com.soomla.profile.domain;

import com.soomla.SoomlaUtils;
import com.soomla.profile.data.PJSONConsts;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A domain object that represents the user's profile attributes.
 */
public class UserProfile {

    private static final String TAG = "SOOMLA UserProfile";

    /**
     * Constructor
     *  @param provider the provider which the user's data is associated to
     * @param profileId the profile ID for the given provider
     * @param username the user's username as used with the given provider
     * @param email the user's email
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param extra map of extra data specific to provider
     */
    public UserProfile(IProvider.Provider provider, String profileId, String username,
                       String email, String firstName, String lastName, Map<String, Object> extra) {
        mProvider = provider;
        mProfileId = profileId;
        mUsername = username;
        mEmail = email;
        mFirstName = firstName;
        mLastName = lastName;
        mExtra = extra;
    }

    /**
     * Constructor.
     * Generates an instance of <code>UserProfile</code> from the given
     * <code>JSONObject</code>.
     *
     * @param jsonObject A JSONObject representation of the wanted
     *                   <code>UserProfile</code>.
     * @throws JSONException if the provided JSON is missing some of the data
     */
    public UserProfile(JSONObject jsonObject) throws JSONException {
        this.mProvider = IProvider.Provider.getEnum(jsonObject.getString(PJSONConsts.UP_PROVIDER));
        this.mProfileId = jsonObject.getString(PJSONConsts.UP_PROFILEID);
        this.mUsername = jsonObject.getString(PJSONConsts.UP_USERNAME);
        this.mEmail = jsonObject.getString(PJSONConsts.UP_EMAIL);
        this.mFirstName = jsonObject.getString(PJSONConsts.UP_FIRSTNAME);
        this.mLastName = jsonObject.getString(PJSONConsts.UP_LASTNAME);
        this.mAvatarLink = jsonObject.optString(PJSONConsts.UP_AVATAR, null);
        this.mLocation = jsonObject.optString(PJSONConsts.UP_LOCATION, null);
        this.mGender = jsonObject.optString(PJSONConsts.UP_GENDER, null);
        this.mLanguage = jsonObject.optString(PJSONConsts.UP_LANGUAGE, null);
        this.mBirthday = jsonObject.optString(PJSONConsts.UP_BIRTHDAY, null);

        JSONObject extraJson = jsonObject.optJSONObject(PJSONConsts.UP_EXTRA);
        if (extraJson != null) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            while (extraJson.keys().hasNext()) {
                String key = extraJson.keys().next();
                map.put(key, extraJson.get(key));
            }
            this.mExtra = map;
        } else {
            this.mExtra = Collections.emptyMap();
        }
    }

    /**
     * Converts the current <code>UserProfile</code> to a JSONObject.
     *
     * @return A <code>JSONObject</code> representation of the current
     * <code>UserProfile</code>.
     */
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PJSONConsts.UP_PROVIDER, mProvider.toString());
            jsonObject.put(PJSONConsts.UP_PROFILEID, mProfileId);
            jsonObject.put(PJSONConsts.UP_USERNAME, mUsername);
            jsonObject.put(PJSONConsts.UP_EMAIL, mEmail);
            jsonObject.put(PJSONConsts.UP_FIRSTNAME, mFirstName);
            jsonObject.put(PJSONConsts.UP_LASTNAME, mLastName);
            jsonObject.put(PJSONConsts.UP_AVATAR, mAvatarLink);
            jsonObject.put(PJSONConsts.UP_LOCATION, mLocation);
            jsonObject.put(PJSONConsts.UP_GENDER, mGender);
            jsonObject.put(PJSONConsts.UP_LANGUAGE, mLanguage);
            jsonObject.put(PJSONConsts.UP_BIRTHDAY, mBirthday);

            JSONObject extraJson = new JSONObject();
            for (String key : mExtra.keySet()) {
                extraJson.put(key, mExtra.get(key));
            }
            jsonObject.put(PJSONConsts.UP_EXTRA, extraJson);
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }


    /** Setters and Getters **/

    public IProvider.Provider getProvider() {
        return mProvider;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getUsername() { return mUsername; }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    public String getAvatarLink() {
        return mAvatarLink;
    }

    public void setAvatarLink(String avatarLink) {
        this.mAvatarLink = avatarLink;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        this.mLocation = location;
    }

    public String getGender() {
        return mGender;
    }

    public void setGender(String gender) {
        this.mGender = gender;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public void setLanguage(String language) {
        this.mLanguage = language;
    }

    public String getBirthday() {
        return mBirthday;
    }

    public void setBirthday(String birthday) {
        this.mBirthday = birthday;
    }

    public Map<String, Object> getExtra() {
        return mExtra;
    }

    public void setExtra(Map<String, Object> mExtra) {
        this.mExtra = mExtra;
    }

    /** Private Members **/

    private IProvider.Provider mProvider;

    private String mProfileId;
    private String mEmail;
    private String mUsername;
    private String mFirstName;
    private String mLastName;
    private String mAvatarLink;
    private String mLocation;
    private String mGender;
    private String mLanguage;
    private String mBirthday;

    private Map<String, Object> mExtra;
}

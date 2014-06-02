/*
 * Copyright (C) 2012 Soomla Inc.
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

import com.soomla.store.StoreUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class UserProfile {

    private static final String TAG = "SOOMLA UserProfile";

    public UserProfile(IProvider.Provider provider, String profileId, String username,
                       String email, String firstName, String lastName) {
        mProvider = provider;
        mProfileId = profileId;
        mUsername = username;
        mEmail = email;
        mFirstName = firstName;
        mLastName = lastName;
    }

    public UserProfile(JSONObject jsonObject) throws JSONException{
        this.mProvider = IProvider.Provider.getEnum(jsonObject.getString(PJSONConsts.UP_PROVIDER));
        this.mProfileId = jsonObject.getString(PJSONConsts.UP_PROFILEID);
        this.mUsername = jsonObject.getString(PJSONConsts.UP_USERNAME);
        this.mEmail = jsonObject.getString(PJSONConsts.UP_EMAIL);
        this.mFirstName = jsonObject.getString(PJSONConsts.UP_FIRSTNAME);
        this.mLastName = jsonObject.getString(PJSONConsts.UP_LASTNAME);
        try {
            this.mAvatarLink = jsonObject.getString(PJSONConsts.UP_AVATAR);
        } catch (JSONException ignored) {}
        try {
        this.mLocation = jsonObject.getString(PJSONConsts.UP_LOCATION);
        } catch (JSONException ignored) {}
        try {
        this.mGender = jsonObject.getString(PJSONConsts.UP_GENDER);
        } catch (JSONException ignored) {}
        try {
        this.mLanguage = jsonObject.getString(PJSONConsts.UP_LANGUAGE);
        } catch (JSONException ignored) {}
        try {
        this.mBirthday = jsonObject.getString(PJSONConsts.UP_BIRTHDAY);
        } catch (JSONException ignored) {}
    }

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
        } catch (JSONException e) {
            StoreUtils.LogError(TAG, "An error occurred while generating JSON object.");
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
}
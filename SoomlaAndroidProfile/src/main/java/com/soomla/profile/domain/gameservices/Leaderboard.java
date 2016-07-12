/*
 * Copyright (C) 2012-2015 Soomla Inc.
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

package com.soomla.profile.domain.gameservices;

import com.soomla.SoomlaUtils;
import com.soomla.data.JSONConsts;
import com.soomla.profile.data.PJSONConsts;
import com.soomla.profile.domain.IProvider;
import org.json.JSONException;
import org.json.JSONObject;

public class Leaderboard {

    private static final String TAG = "SOOMLA Leaderboard";

    /**
     * Constructor
     *
     * @param id the leaderboard id for the given provider
     * @param provider the provider which the user's data is associated to
     * @param name the name of this leaderboard
     * @param iconUrl url to icon of this leaderboard
     */
    public Leaderboard(String id, IProvider.Provider provider, String name, String iconUrl) {
        mId = id;
        mProvider = provider;
        mName = name;
        mIconUrl = iconUrl;
    }

    /**
     * Constructor.
     * Generates an instance of <code>Leaderboard</code> from the given
     * <code>JSONObject</code>.
     *
     * @param jsonObject A JSONObject representation of the wanted
     *                   <code>Leaderboard</code>.
     * @throws JSONException if the provided JSON is missing some of the data
     */
    public Leaderboard(JSONObject jsonObject) throws JSONException {
        this.mId = jsonObject.getString(PJSONConsts.UP_IDENTIFIER);
        this.mProvider = IProvider.Provider.getEnum(jsonObject.getString(PJSONConsts.UP_PROVIDER));
        this.mName = jsonObject.getString(PJSONConsts.UP_NAME);
        this.mIconUrl = jsonObject.getString(PJSONConsts.UP_ICON_URL);
    }

    /**
     * Converts the current <code>Leaderboard</code> to a JSONObject.
     *
     * @return A <code>JSONObject</code> representation of the current
     * <code>Leaderboard</code>.
     */
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSONConsts.SOOM_CLASSNAME, SoomlaUtils.getClassName(this));
            jsonObject.put(PJSONConsts.UP_IDENTIFIER, mId);
            jsonObject.put(PJSONConsts.UP_PROVIDER, mProvider.toString());
            jsonObject.put(PJSONConsts.UP_NAME, mName != null ? mName : "");
            jsonObject.put(PJSONConsts.UP_ICON_URL, mIconUrl != null ? mName : "");
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }

    /** Setters and Getters **/

    public String getId() {
        return mId;
    }

    public IProvider.Provider getProvider() {
        return mProvider;
    }

    public String getName() {
        return mName;
    }

    public String getIconURL() {
        return mIconUrl;
    }

    /** Private Members **/

    private String mId;
    private IProvider.Provider mProvider;
    private String mIconUrl;
    private String mName;
}

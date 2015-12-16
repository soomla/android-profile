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
import com.soomla.profile.domain.UserProfile;
import org.json.JSONException;
import org.json.JSONObject;

public class Score {
    private static final String TAG = "SOOMLA Score";

    /**
     * Constructor
     *
     * @param leaderboard the owner leaderboard
     * @param rank the position of this score in the leaderboard
     * @param player the owner of this score
     * @param value the value of this score
     */
    public Score(Leaderboard leaderboard, long rank, UserProfile player, long value) {
        this.mLeaderboard = leaderboard;
        this.mRank = rank;
        this.mPlayer = player;
        this.mValue = value;
    }

    /**
     * Constructor.
     * Generates an instance of <code>Score</code> from the given
     * <code>JSONObject</code>.
     *
     * @param jsonObject A JSONObject representation of the wanted
     *                   <code>Score</code>.
     * @throws JSONException if the provided JSON is missing some of the data
     */
    public Score(JSONObject jsonObject) throws JSONException {
        this.mLeaderboard = new Leaderboard(jsonObject.getJSONObject(PJSONConsts.UP_LEADERBOARD));
        this.mRank = jsonObject.getInt(PJSONConsts.UP_SCORE_RANK);
        this.mPlayer = new UserProfile(jsonObject.getJSONObject(PJSONConsts.UP_USER_PROFILE));
        this.mValue = jsonObject.getInt(PJSONConsts.UP_SCORE_VALUE);
    }

    /**
     * Converts the current <code>Score</code> to a JSONObject.
     *
     * @return A <code>JSONObject</code> representation of the current
     * <code>Score</code>.
     */
    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSONConsts.SOOM_CLASSNAME, SoomlaUtils.getClassName(this));
            jsonObject.put(PJSONConsts.UP_LEADERBOARD, mLeaderboard.toJSONObject());
            jsonObject.put(PJSONConsts.UP_SCORE_RANK, mRank);
            jsonObject.put(PJSONConsts.UP_USER_PROFILE, mPlayer.toJSONObject());
            jsonObject.put(PJSONConsts.UP_SCORE_VALUE, mValue);
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }

    /** Setters and Getters **/

    public Leaderboard getLeaderboard() {
        return mLeaderboard;
    }

    public long getRank() {
        return mRank;
    }

    public UserProfile getPlayer() {
        return mPlayer;
    }

    public long getValue() {
        return mValue;
    }

    /** Private Members **/

    private Leaderboard mLeaderboard;
    private long mRank;
    private UserProfile mPlayer;
    private long mValue;
}
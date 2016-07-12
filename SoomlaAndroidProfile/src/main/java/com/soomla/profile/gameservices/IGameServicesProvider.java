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

package com.soomla.profile.gameservices;

import android.app.Activity;
import com.soomla.profile.auth.IAuthProvider;
import com.soomla.profile.domain.IProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.domain.gameservices.*;

/**
 A provider that exposes game services capabilities such as leaderboards, achievements, challenges and scoring
 */
public interface IGameServicesProvider extends IProvider {

    /**
     Fetches the game's leaderboards list

     @param leaderboardsListener a callback for this action
     */
    void getLeaderboards(GameServicesCallbacks.SuccessWithListListener<Leaderboard> leaderboardsListener);

    /**
     Fetches the game's scores list from specified leaderboard

     @param leaderboardId Leaderboard containing desired scores list
     @param fromStart Should we reset pagination or request the next page
     @param scoreListener a callback for this action
     */
    void getScores(String leaderboardId, boolean fromStart, GameServicesCallbacks.SuccessWithListListener<Score> scoreListener);

    /**
     Submits scores to specified leaderboard

     @param leaderboardId Target leaderboard
     @param value Value to report
     @param submitScoreListener a callback for this action
     */
    void submitScore(String leaderboardId, long value, GameServicesCallbacks.SuccessWithScoreListener submitScoreListener);

    /**
     * Opens native dialog displaying leaderboards list
     *
     * @param activity The parent activity
     */
    void showLeaderboards(final Activity activity);
}

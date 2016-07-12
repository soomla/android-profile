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

package com.soomla.profile.events.auth;


import com.soomla.profile.domain.IProvider;
import com.soomla.profile.domain.UserProfile;

/**
 * This event is fired when the login process finishes successfully
 */
public class LoginFinishedEvent {

    /**
     * Constructor
     * @param userProfile The user's profile from the logged in provider
     * @param autoLogin comes "true" if user login automatically
     * @param payload an identification String sent from the caller of the action
     */
    public LoginFinishedEvent(UserProfile userProfile, boolean autoLogin, String payload) {
        UserProfile = userProfile;
        AutoLogin = autoLogin;
        Payload = payload;
    }

    /**
     * Retrieves the provider to which the user has logged-in
     *
     * @return The provider to which the user has logged-in
     */
    public IProvider.Provider getProvider() {
        return UserProfile.getProvider();
    }

    /**
     * The user's profile from the logged in provider
     */
    public final UserProfile UserProfile;

    /**
     * Comes "true" if user login automatically
     */
    public final boolean AutoLogin;

    /**
     * an identification String sent from the caller of the action
     */
    public final String Payload;
}

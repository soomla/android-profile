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

/**
 * This event is fired when the login to a provider was cancelled
 * by the user
 */
public class LoginCancelledEvent {
    /**
     * The provider which the user has cancelled login to
     */
    public IProvider.Provider Provider;

    /**
     * Comes "true" if user login automatically
     */
    public final boolean AutoLogin;

    /**
     * an identification String sent from the caller of the action
     */
    public final String Payload;

    /**
     * Constructor
     * @param provider The provider which the user has cancelled login to
     * @param autoLogin comes "true" if user login automatically
     * @param payload an identification String sent from the caller of the action
     */
    public LoginCancelledEvent(IProvider.Provider provider, boolean autoLogin, String payload) {
        Provider = provider;
        AutoLogin = autoLogin;
        Payload = payload;
    }
}

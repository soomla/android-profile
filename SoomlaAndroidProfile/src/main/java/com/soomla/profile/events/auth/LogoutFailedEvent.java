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
 * This event is fired when the logout process from a provider has failed
 */
public class LogoutFailedEvent {
    /**
     * The provider on which the logout has failed
     */
    public final IProvider.Provider Provider;

    /**
     * a Description of the reason for failure
     */
    public final String ErrorDescription;

    /**
     * Constructor
     *
     * @param provider The provider on which the logout has failed
     * @param errorDescription a Description of the reason for failure
     */
    public LogoutFailedEvent(IProvider.Provider provider, String errorDescription) {
        Provider = provider;
        ErrorDescription = errorDescription;
    }
}

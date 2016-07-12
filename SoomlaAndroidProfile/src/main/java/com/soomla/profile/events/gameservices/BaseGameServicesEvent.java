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

package com.soomla.profile.events.gameservices;

import com.soomla.profile.domain.IProvider;

/**
 * The base class for all game services events
 */
public abstract class BaseGameServicesEvent {
    /**
     * The provider on which the game services event has occurred
     */
    public final IProvider.Provider Provider;

    /**
     * an identification String sent from the caller of the action
     */
    public final String Payload;

    protected BaseGameServicesEvent(IProvider.Provider provider, String payload) {
        Provider = provider;
        Payload = payload;
    }
}

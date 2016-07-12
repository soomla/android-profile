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

package com.soomla.profile.events.social;

import com.soomla.profile.domain.IProvider;
import com.soomla.profile.social.ISocialProvider;

/**
 * The base class for all social action events
 */
public abstract class BaseSocialActionEvent {
    /**
     * The provider on which the social action event has occurred
     */
    public final IProvider.Provider Provider;

    /**
     * an identification String sent from the caller of the action
     */
    public final String Payload;

    /**
     * The social action which the event represents
     */
    public final ISocialProvider.SocialActionType SocialActionType;

    protected BaseSocialActionEvent(IProvider.Provider provider, ISocialProvider.SocialActionType socialActionType, String payload) {
        Provider = provider;
        SocialActionType = socialActionType;
        Payload = payload;
    }
}

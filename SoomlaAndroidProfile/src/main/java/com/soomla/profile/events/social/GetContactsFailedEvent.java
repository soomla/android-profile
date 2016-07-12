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
 * This event is fired when the get contacts process from a provider has failed
 */
public class GetContactsFailedEvent extends BaseSocialActionEvent {
    /**
     * a Description of the reason for failure
     */
    public final String ErrorDescription;

    public final boolean FromStart;

    /**
     * Constructor
     * @param provider The provider on which the get contacts process has failed
     * @param socialActionType The social action preformed
     * @param errorDescription a Description of the reason for failure
     * @param fromStart Should we reset pagination or request the next page
     * @param payload an identification String sent from the caller of the action
     */
    public GetContactsFailedEvent(IProvider.Provider provider,
                                  ISocialProvider.SocialActionType socialActionType,
                                  String errorDescription, boolean fromStart, String payload) {
        super(provider, socialActionType, payload);
        this.ErrorDescription = errorDescription;
        this.FromStart = fromStart;
    }
}

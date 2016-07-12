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
 * This event is fired when the get feed process from a provider has failed
 */
public class GetFeedFailedEvent extends BaseSocialActionEvent {
    /**
     * a Description of the reason for failure
     */
    public final String ErrorDescription;

    public final Boolean FromStart;

    /**
     * Constructor
     *  @param provider The provider on which the get feed process has
     * @param getFeedType The social action preformed
     * @param errorDescription a Description of the reason for failure
     * @param fromStart Should we reset pagination or request the next page
     * @param payload an identification String sent from the caller of the action
     */
    public GetFeedFailedEvent(IProvider.Provider provider,
                              ISocialProvider.SocialActionType getFeedType,
                              String errorDescription, Boolean fromStart, String payload) {
        super(provider, getFeedType, payload);
        ErrorDescription = errorDescription;
        FromStart = fromStart;
    }
}

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
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.social.ISocialProvider;

import java.util.List;

/**
 * This event is fired when the get contacts process from a provider has
 * finished
 */
public class GetContactsFinishedEvent extends BaseSocialActionEvent {
    /**
     * an Array of contacts represented by <code>UserProfile</code>
     */
    public final List<UserProfile> Contacts;
    public final boolean HasMore;

    /**
     * Constructor
     *  @param provider The provider on which the get contacts process finished
     * @param socialActionType The social action preformed
     * @param contacts an Array of contacts represented by <code>UserProfile</code>
     * @param payload an identification String sent from the caller of the action
     * @param hasMore Should we reset pagination or request the next page
     */
    public GetContactsFinishedEvent(IProvider.Provider provider,
                                    ISocialProvider.SocialActionType socialActionType,
                                    List<UserProfile> contacts, String payload, boolean hasMore) {
        super(provider, socialActionType, payload);
        this.Contacts = contacts;
        this.HasMore = hasMore;
    }
}

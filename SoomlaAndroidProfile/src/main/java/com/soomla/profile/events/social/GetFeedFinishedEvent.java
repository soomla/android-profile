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

import java.util.List;

/**
 * This event is fired when the get feed process from a provider has finished
 */
public class GetFeedFinishedEvent extends BaseSocialActionEvent {
    /**
     * an Array of feed entries represented by strings
     */
    public final List<String> Posts;
    public final boolean HasMore;

    /**
     * Constructor
     * @param provider The provider on which the get feed process finished
     * @param getFeedType The social action performed
     * @param feedPosts an Array of feed entries represented by strings
     * @param payload an identification String sent from the caller of the action
     * @param hasMore Should we reset pagination or request the next page
     */
    public GetFeedFinishedEvent(IProvider.Provider provider,
                                ISocialProvider.SocialActionType getFeedType,
                                List<String> feedPosts, String payload, boolean hasMore) {

        super(provider, getFeedType, payload);
        Posts = feedPosts;
        HasMore = hasMore;
    }
}

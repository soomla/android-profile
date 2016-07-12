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

package com.soomla.profile.domain;

import java.util.Map;

/**
 * An interface that represents a provider, which will be used later for
 * authentication and social actions.
 */
public interface IProvider {

    /**
     * The place, where you can configure the provider, using params passed by user.
     * @param providerParams params of this provider, passed during Profile initialization
     */
    void configure(Map<String, String> providerParams);

    Provider getProvider();

    /**
     * Lists all the supported or to-be supported social platforms (providers)
     */
    public enum Provider {
        FACEBOOK(0),
        GOOGLE(2),
        TWITTER(5);

        Provider(final int value) {
            this.mValue = value;
        }

        private final int mValue;


        /** Setters and Getters **/

        public int getValue() {
            return mValue;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            String result = "";
            switch (this)
            {
                case FACEBOOK: result = "facebook";
                    break;
                case GOOGLE: result = "google";
                    break;
                case TWITTER: result = "twitter";
                    break;
                default: throw new IllegalArgumentException();
            }

            return result;
        }

        /**
         * Converts a string to its enum <code>Provider</code> value if possible
         * @param value The string to convert to <code>Provider</code>
         * @return The <code>Provider</code> value corresponding to the
         * supplied string
         * @throws IllegalArgumentException if the provided string does not
         * correspond to an enum value
         */
        public static Provider getEnum(String value) throws IllegalArgumentException{
            for(Provider v : values()) {
                if (v.toString().equalsIgnoreCase(value)) return v;
            }
            throw new IllegalArgumentException();
        }
    }
}

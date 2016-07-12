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

package com.soomla.profile;

import com.soomla.SoomlaUtils;
import com.soomla.profile.auth.IAuthProvider;
import com.soomla.profile.domain.IProvider;
import com.soomla.profile.exceptions.ProviderNotFoundException;
import com.soomla.profile.gameservices.IGameServicesProvider;
import com.soomla.profile.social.ISocialProvider;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A  class that provides functionality for dynamic loading and management of providers.
 */
public class ProviderManager {

    public ProviderManager(String... providerNames) {
        this(null, providerNames);
    }

    public ProviderManager(Map<IProvider.Provider, ? extends Map<String, String>> profileParams, String... providerNames) {
        List<Class<? extends IProvider>> providerClass = tryFetchProviders(providerNames);
        if (providerClass == null || providerClass.size() == 0) {
            SoomlaUtils.LogWarning(TAG, "No attached providers found! Most of Profile functionality fill be unavaliable.");
        }

        mProviders = new HashMap<>();
        for (Class<? extends IProvider> aClass : providerClass) {
            try {
                IProvider provider = aClass.newInstance();
                IProvider.Provider targetProvider = provider.getProvider();
                if (profileParams != null) {
                    Map<String, String> providerParams = profileParams.get(targetProvider);
                    provider.configure(providerParams);
                }
                mProviders.put(targetProvider, provider);
            } catch (Exception e) {
                String err = "Couldn't instantiate provider class. Something's totally wrong here. " + e.getLocalizedMessage();
                SoomlaUtils.LogError(TAG, err);
            }
        }
    }

    private List<Class<? extends IProvider>> tryFetchProviders(String... providerNames) {
        List<Class<? extends IProvider>> providers = new ArrayList<>();
        for(String providerItem : providerNames) {
            try {
                SoomlaUtils.LogDebug(TAG, "Trying to load class " + providerItem);
                Class<? extends IProvider> aClass = (Class<? extends IProvider>) Class.forName(providerItem);

                providers.add(aClass);
            } catch (ClassNotFoundException e) {
                SoomlaUtils.LogDebug(TAG, "Failed loading class " + providerItem + " Exception: " + e.getLocalizedMessage());
            }
        }

        return providers;
    }

    protected IProvider getProvider(IProvider.Provider provider, Class<? extends IProvider> providerClass) throws ProviderNotFoundException {
        final IProvider providerObj = mProviders.get(provider);
        if(providerObj == null || !providerClass.isInstance(providerObj)) {
            throw new ProviderNotFoundException(provider);
        }

        return providerObj;
    }

    public IAuthProvider getAuthProvider(IProvider.Provider provider) throws ProviderNotFoundException {
        return (IAuthProvider)getProvider(provider, IAuthProvider.class);
    }

    public List<IAuthProvider> getAllAuthProviders() {
        List<IAuthProvider> result = new ArrayList<>();
        for (IProvider provider : mProviders.values()) {
            if (provider instanceof IAuthProvider) {
                result.add((IAuthProvider)provider);
            }
        }
        return result;
    }

    public ISocialProvider getSocialProvider(IProvider.Provider provider) throws ProviderNotFoundException {
        return (ISocialProvider)getProvider(provider, ISocialProvider.class);
    }

    public List<ISocialProvider> getAllSocialProviders() {
        List<ISocialProvider> result = new ArrayList<>();
        for (IProvider provider : mProviders.values()) {
            if (provider instanceof ISocialProvider) {
                result.add((ISocialProvider)provider);
            }
        }
        return result;
    }

    public IGameServicesProvider getGameServicesProvider(IProvider.Provider provider) throws ProviderNotFoundException {
        return (IGameServicesProvider)getProvider(provider, ISocialProvider.class);
    }

    public List<IGameServicesProvider> getAllGameServicesProviders() {
        List<IGameServicesProvider> result = new ArrayList<>();
        for (IProvider provider : mProviders.values()) {
            if (provider instanceof IGameServicesProvider) {
                result.add((IGameServicesProvider)provider);
            }
        }
        return result;
    }

    /** Private Members **/

    private Map<IProvider.Provider, IProvider> mProviders = new HashMap<>();

    private static String TAG = "SOOMLA ProviderManager";
}

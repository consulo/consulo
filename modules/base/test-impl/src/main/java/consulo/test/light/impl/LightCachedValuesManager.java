/*
 * Copyright 2013-2023 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.*;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2023-11-05
 */
@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
@Singleton
public class LightCachedValuesManager extends CachedValuesManager {
    
    @Override
    public <T> CachedValue<T> createCachedValue(CachedValueProvider<T> provider, boolean trackValue) {
        return new LightCachedValue<>(provider);
    }

    
    @Override
    public <T, P> ParameterizedCachedValue<T, P> createParameterizedCachedValue(
        ParameterizedCachedValueProvider<T, P> provider,
        boolean trackValue
    ) {
        return new LightParameterizedCachedValue<>(provider);
    }

    @Override
    protected void trackKeyHolder(UserDataHolder dataHolder, Key<?> key) {

    }

    @Override
    public <T> T getCachedValue(
        UserDataHolder dataHolder,
        Key<CachedValue<T>> key,
        CachedValueProvider<T> provider,
        boolean trackValue
    ) {
        CachedValue<T> value = dataHolder.getUserData(key);
        if (value != null) {
            return value.getValue();
        }

        CachedValue<T> cachedValue = createCachedValue(provider, trackValue);
        dataHolder.putUserData(key, value);
        return cachedValue.getValue();
    }
}

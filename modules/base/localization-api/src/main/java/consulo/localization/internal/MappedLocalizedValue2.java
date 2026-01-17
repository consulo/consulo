/*
 * Copyright 2013-2026 consulo.io
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
package consulo.localization.internal;

import consulo.localization.LocalizationManager;
import consulo.localization.LocalizedValue;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2020-07-30
 */
public /*final*/ class MappedLocalizedValue2 extends CachingLocalizedValue {
    @Nonnull
    private final LocalizedValue myDelegate;
    @Nonnull
    private final BiFunction<LocalizationManager, String, String> myMapper;

    public MappedLocalizedValue2(
        @Nonnull LocalizationManager manager,
        @Nonnull LocalizedValue delegate,
        @Nonnull BiFunction<LocalizationManager, String, String> mapper
    ) {
        super(manager);
        myDelegate = delegate;
        myMapper = mapper;
    }

    @Nonnull
    @Override
    protected String calcValue() {
        return myMapper.apply(myLocalizationManager, myDelegate.getValue());
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || o instanceof MappedLocalizedValue2 that
            && Objects.equals(myDelegate, that.myDelegate)
            && Objects.equals(myMapper, that.myMapper);
    }

    @Override
    public int calcHashCode() {
        return myDelegate.hashCode() + 29 * myMapper.hashCode();
    }
}

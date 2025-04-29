/*
 * Copyright 2013-2020 consulo.io
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
package consulo.localize.internal;

import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2020-07-30
 */
public final class MapLocalizeValue extends BaseLocalizeValue {
    private final LocalizeValue myDelegate;
    private final BiFunction<LocalizeManager, String, String> myMapper;

    public MapLocalizeValue(LocalizeValue delegate, BiFunction<LocalizeManager, String, String> mapper) {
        super(ourEmptyArgs);
        myDelegate = delegate;
        myMapper = mapper;
    }

    @Nonnull
    @Override
    protected Map.Entry<Locale, String> getUnformattedText(@Nonnull LocalizeManager localizeManager) {
        String value = myDelegate.getValue();
        return Map.entry(localizeManager.getLocale(), myMapper.apply(localizeManager, value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MapLocalizeValue that = (MapLocalizeValue) o;
        return Objects.equals(myDelegate, that.myDelegate) &&
            Objects.equals(myMapper, that.myMapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myDelegate, myMapper);
    }
}

/*
 * Copyright 2013-2021 consulo.io
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

/**
 * @author VISTALL
 * @since 24/09/2021
 */
public final class JoinLocalizeValue extends BaseLocalizeValue {
    public JoinLocalizeValue(LocalizeValue[] values) {
        super(values);
    }

    @Nonnull
    @Override
    protected Map.Entry<Locale, String> getUnformattedText(@Nonnull LocalizeManager localizeManager) {
        throw new UnsupportedOperationException("this method will never called");
    }

    @Nonnull
    @Override
    protected String calcValue(LocalizeManager manager) {
        StringBuilder builder = new StringBuilder();

        for (Object arg : myArgs) {
            String value = arg instanceof LocalizeValue lv ? lv.getValue() : String.valueOf(arg);

            builder.append(value);
        }
        return builder.toString();
    }
}

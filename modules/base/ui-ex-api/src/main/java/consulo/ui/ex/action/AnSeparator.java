/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.action;

import consulo.annotation.DeprecationInfo;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a separator.
 */
public final class AnSeparator extends AnAction implements DumbAware {
    @SuppressWarnings("deprecation")
    private static final AnSeparator ourInstance = new AnSeparator();

    @Nonnull
    public static AnSeparator getInstance() {
        return ourInstance;
    }

    @Nonnull
    public static AnSeparator create() {
        return ourInstance;
    }

    @Nonnull
    @Deprecated
    @DeprecationInfo("Use #create(LocalizeValue)")
    public static AnSeparator create(@Nullable String text) {
        return create(StringUtil.isEmptyOrSpaces(text) ? LocalizeValue.empty() : LocalizeValue.of(text));
    }

    @Nonnull
    @SuppressWarnings("deprecation")
    public static AnSeparator create(@Nonnull LocalizeValue textValue) {
        return textValue == LocalizeValue.empty() ? ourInstance : new AnSeparator(textValue);
    }

    @Nonnull
    private final LocalizeValue myTextValue;

    @Deprecated
    @DeprecationInfo("Use #create()")
    public AnSeparator() {
        this(LocalizeValue.empty());
    }

    @Deprecated
    @DeprecationInfo("Use #create()")
    public AnSeparator(@Nullable String text) {
        myTextValue = StringUtil.isEmptyOrSpaces(text) ? LocalizeValue.empty() : LocalizeValue.of(text);
    }

    @Deprecated
    @DeprecationInfo("Use #create()")
    public AnSeparator(@Nonnull LocalizeValue textValue) {
        myTextValue = textValue;
    }

    @Nullable
    @Deprecated
    @DeprecationInfo("Use #getTextValue()")
    public String getText() {
        return StringUtil.nullize(myTextValue.getValue());
    }

    @Nonnull
    public LocalizeValue getTextValue() {
        return myTextValue;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return myTextValue.toString();
    }
}

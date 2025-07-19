/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.module;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class ConfigurationErrorDescription {
    private final String myElementName;
    @Nonnull
    private final LocalizeValue myDescription;
    private final ConfigurationErrorType myErrorType;

    protected ConfigurationErrorDescription(String elementName, @Nonnull LocalizeValue description, ConfigurationErrorType errorType) {
        myElementName = elementName;
        myErrorType = errorType;
        myDescription = description;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    protected ConfigurationErrorDescription(String elementName, String description, ConfigurationErrorType errorType) {
        this(elementName, LocalizeValue.ofNullable(description), errorType);
    }

    public String getElementName() {
        return myElementName;
    }

    public ConfigurationErrorType getErrorType() {
        return myErrorType;
    }

    @Nonnull
    public LocalizeValue getDescription() {
        return myDescription;
    }

    @RequiredUIAccess
    public abstract void ignoreInvalidElement();

    @Nonnull
    public abstract LocalizeValue getIgnoreConfirmationMessage();

    public boolean isValid() {
        return true;
    }
}

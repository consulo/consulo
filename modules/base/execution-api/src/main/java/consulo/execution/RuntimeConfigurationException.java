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
package consulo.execution;

import consulo.annotation.DeprecationInfo;
import consulo.configurable.ConfigurationException;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

public class RuntimeConfigurationException extends ConfigurationException {
    public RuntimeConfigurationException(@Nonnull LocalizeValue message) {
        super(message);
    }

    public RuntimeConfigurationException(@Nonnull LocalizeValue message, @Nonnull LocalizeValue title) {
        super(message, title);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public RuntimeConfigurationException(String message, String title) {
        super(message, title);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public RuntimeConfigurationException(String message) {
        super(LocalizeValue.ofNullable(message), ExecutionLocalize.runConfigurationErrorDialogTitle());
    }
}
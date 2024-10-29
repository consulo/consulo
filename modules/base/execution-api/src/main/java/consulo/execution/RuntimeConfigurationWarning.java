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
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

public class RuntimeConfigurationWarning extends RuntimeConfigurationException {
    public RuntimeConfigurationWarning(@Nonnull LocalizeValue message) {
        this(message, null);
    }

    public RuntimeConfigurationWarning(@Nonnull LocalizeValue message, Runnable quickFix) {
        super(message, ExecutionLocalize.warningCommonTitle());
        setQuickFix(quickFix);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public RuntimeConfigurationWarning(String message) {
        this(message, null);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public RuntimeConfigurationWarning(String message, Runnable quickFix) {
        this(LocalizeValue.ofNullable(message), quickFix);
    }
}
/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.action;

import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;

public abstract class CompatibleRunConfigurationProducer<T extends RunConfiguration> extends RunConfigurationProducer<T> {
    protected CompatibleRunConfigurationProducer(@Nonnull ConfigurationType configurationType) {
        super(configurationType);
    }

    @Override
    protected boolean setupConfigurationFromContext(T configuration, ConfigurationContext context, Ref<PsiElement> sourceElement) {
        if (configuration == null || context == null || sourceElement == null || !isContextCompatible(context)) {
            return false;
        }
        return setupConfigurationFromCompatibleContext(configuration, context, sourceElement);
    }

    protected abstract boolean setupConfigurationFromCompatibleContext(
        @Nonnull T configuration,
        @Nonnull ConfigurationContext context,
        @Nonnull Ref<PsiElement> sourceElement
    );

    @Override
    public final boolean isConfigurationFromContext(T configuration, ConfigurationContext context) {
        if (configuration == null || context == null || !isContextCompatible(context)) {
            return false;
        }
        return isConfigurationFromCompatibleContext(configuration, context);
    }

    protected abstract boolean isConfigurationFromCompatibleContext(@Nonnull T configuration, @Nonnull ConfigurationContext context);

    protected boolean isContextCompatible(@Nonnull ConfigurationContext context) {
        ConfigurationType type = getConfigurationType();
        return context.isCompatibleWithOriginalRunConfiguration(type);
    }
}

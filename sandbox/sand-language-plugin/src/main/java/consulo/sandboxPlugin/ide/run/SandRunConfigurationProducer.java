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
package consulo.sandboxPlugin.ide.run;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.execution.configuration.ConfigurationType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.util.lang.ref.SimpleReference;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class SandRunConfigurationProducer extends RunConfigurationProducer<SandConfiguration> {
    @Inject
    public SandRunConfigurationProducer(Application application) {
        super((ConfigurationType) application.getExtensionPoint(ConfigurationType.class).findExtensionOrFail(SandConfigurationType.class));
    }

    @Override
    @RequiredReadAction
    protected boolean setupConfigurationFromContext(
        SandConfiguration configuration,
        ConfigurationContext context,
        SimpleReference<PsiElement> sourceElement
    ) {
        SandClass sandClass = findSandClass(context);
        String className = sandClass == null ? null : sandClass.getName();
        if (sandClass == null || className == null) {
            return false;
        }

        configuration.setName(className);
        configuration.setEntryPoint(className);
        sourceElement.set(sandClass);
        return true;
    }

    @Override
    @RequiredReadAction
    public boolean isConfigurationFromContext(SandConfiguration configuration, ConfigurationContext context) {
        SandClass sandClass = findSandClass(context);
        return sandClass != null && Objects.equals(sandClass.getName(), configuration.getEntryPoint());
    }

    private static @Nullable SandClass findSandClass(ConfigurationContext context) {
        return PsiTreeUtil.getParentOfType(context.getPsiLocation(), SandClass.class, false);
    }
}

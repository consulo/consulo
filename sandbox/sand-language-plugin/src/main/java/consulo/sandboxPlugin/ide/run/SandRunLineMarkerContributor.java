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
import consulo.execution.icon.ExecutionIconGroup;
import consulo.execution.lineMarker.ExecutorAction;
import consulo.execution.lineMarker.RunLineMarkerContributor;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class SandRunLineMarkerContributor extends RunLineMarkerContributor {
    @Override
    @RequiredReadAction
    public @Nullable Info getInfo(PsiElement element) {
        if (!(element.getParent() instanceof SandClass sandClass) || sandClass.getNameIdentifier() != element) {
            return null;
        }
        return new Info(
            ExecutionIconGroup.gutterRun(),
            identifier -> LocalizeValue.localizeTODO("Run sand class " + identifier.getText()).get(),
            ExecutorAction.getActions(0)
        );
    }

    @Override
    public Language getLanguage() {
        return SandLanguage.INSTANCE;
    }
}

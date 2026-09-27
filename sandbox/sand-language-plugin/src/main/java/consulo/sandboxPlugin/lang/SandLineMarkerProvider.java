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
package consulo.sandboxPlugin.lang;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.sandboxPlugin.lang.psi.SandClass;
import org.jspecify.annotations.Nullable;

/**
 * Marks the name of every class in the gutter, with the class name in the tooltip.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class SandLineMarkerProvider implements LineMarkerProvider {
    @RequiredReadAction
    @Override
    public @Nullable LineMarkerInfo getLineMarkerInfo(PsiElement element) {
        if (!(element.getParent() instanceof SandClass sandClass) || sandClass.getNameIdentifier() != element) {
            return null;
        }
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            PlatformIconGroup.nodesClass(),
            Pass.LINE_MARKERS,
            identifier -> "Sand class " + identifier.getText(),
            null,
            GutterIconRenderer.Alignment.RIGHT
        );
    }

    @Override
    public Language getLanguage() {
        return SandLanguage.INSTANCE;
    }
}

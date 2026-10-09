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
package consulo.language.editor.impl.internal.documentation;

import consulo.application.dumb.DumbAware;
import consulo.colorScheme.FontSize;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
final class DocumentationFontSizeGroup extends ActionGroup implements DumbAware, HintManager.ActionToIgnore {
    private final AnAction[] myChildren;

    DocumentationFontSizeGroup(DocumentationSettings settings) {
        super(CodeInsightLocalize.javadocActionFontSize(), true);

        FontSize[] sizes = FontSize.values();
        myChildren = new AnAction[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            myChildren[i] = new DocumentationFontSizeAction(settings, sizes[i]);
        }
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return myChildren;
    }
}

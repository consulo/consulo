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
package consulo.language.editor.impl.internal.hierarchy;

import consulo.annotation.component.ActionImpl;
import consulo.language.editor.hierarchy.BrowseHierarchyActionBase;
import consulo.language.editor.hierarchy.StandardHierarchyKinds;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.concurrent.coroutine.Coroutine;

@ActionImpl(id = "MethodHierarchy")
public final class BrowseMethodHierarchyAction extends BrowseHierarchyActionBase {
    public BrowseMethodHierarchyAction() {
        super(
            ActionLocalize.actionMethodhierarchyText(),
            ActionLocalize.actionMethodhierarchyDescription(),
            StandardHierarchyKinds.METHOD
        );
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        if (!ActionPlaces.MAIN_MENU.equals(e.getPlace())) {
            e.getPresentation().setText(LanguageEditorLocalize.actionBrowseMethodHierarchy());
        }

        return super.updateAsync(e);
    }
}

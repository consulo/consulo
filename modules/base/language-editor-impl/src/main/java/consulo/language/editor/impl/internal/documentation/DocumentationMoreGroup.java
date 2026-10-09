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
import consulo.language.editor.hint.HintManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
final class DocumentationMoreGroup extends ActionGroup implements DumbAware, HintManager.ActionToIgnore {
    private final AnAction[] myChildren;

    DocumentationMoreGroup(List<? extends AnAction> secondaryActions, List<? extends AnAction> primaryActions) {
        super(LocalizeValue.empty(), LocalizeValue.empty(), PlatformIconGroup.actionsMorevertical());
        setPopup(true);

        List<AnAction> children = new ArrayList<>(secondaryActions);
        children.add(AnSeparator.create());
        children.addAll(primaryActions);
        myChildren = children.toArray(AnAction.EMPTY_ARRAY);
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return myChildren;
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }
}

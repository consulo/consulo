// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.markup;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface InspectionWidgetActionProvider {
    /**
     * Creates action for the given editor.
     * User may return ActionGroup containing several actions and separators if needed.
     * All groups will be flattened upon adding to the inspection widget toolbar.
     * <p>
     * May return null if no action should be created for the given editor.
     * <p>
     * AnAction may implement Disposable, its {@code dispose} method will be called on editor disposal on the action unregistration
     */
    @RequiredUIAccess
    @Nullable
    AnAction createAction(Editor editor);
}

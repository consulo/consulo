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
package consulo.web.editor.impl.internal.floating;

import com.vaadin.flow.component.html.Div;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.internal.inspection.InspectionWidgetActions;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.impl.internal.action.UnifiedActionToolbarImpl;
import consulo.web.editor.impl.internal.ArquillEditorElement;
import consulo.web.ui.impl.internal.base.TargetVaadin;

import java.util.List;

/**
 * The actions of the inspection widget, at the top right corner of the editor.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public final class WebEditorInspectionToolbar {
    private static final String PLACE = "EditorInspectionsToolbar";

    private WebEditorInspectionToolbar() {
    }

    @RequiredUIAccess
    public static void install(Editor editor, ArquillEditorElement element, Disposable parentDisposable) {
        if (!InspectionWidgetActions.isShownIn(editor)) {
            return;
        }
        List<AnAction> actions = InspectionWidgetActions.create(editor, parentDisposable);
        if (actions.isEmpty()) {
            return;
        }

        UnifiedActionToolbarImpl toolbar = new UnifiedActionToolbarImpl(PLACE, new DefaultActionGroup(actions), ActionToolbar.Style.HORIZONTAL);
        toolbar.setDataContextProvider(editor::getDataContext);

        Div holder = new Div();
        holder.addClassName("arquill-inspection-toolbar");
        holder.add(TargetVaadin.to(toolbar.getUIComponent()));

        Div layer = element.getFloatingToolbarLayer();
        layer.addComponentAsFirst(holder);
        Disposer.register(parentDisposable, () -> layer.remove(holder));

        toolbar.updateActionsAsync();
    }
}

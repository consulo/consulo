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
package consulo.diagram.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.diagram.DiagramDataKeys;
import consulo.diagram.DiagramNode;
import consulo.diagram.impl.internal.editor.DiagramEditorController;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
@ActionImpl(id = "Diagram.RemoveFromDiagram")
public class DiagramRemoveFromDiagramAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public DiagramRemoveFromDiagramAction() {
        super(LocalizeValue.localizeTODO("Remove from Diagram"), LocalizeValue.localizeTODO("Remove the selected nodes from this diagram"));
    }

    @Override
    public void update(AnActionEvent e) {
        DiagramEditorController<?> controller = e.getData(DiagramEditorController.KEY);
        e.getPresentation().setVisible(controller != null);
        List<DiagramNode<?>> nodes = e.getData(DiagramDataKeys.SELECTED_NODES);
        e.getPresentation().setEnabled(controller != null && nodes != null && !nodes.isEmpty());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DiagramEditorController<?> controller = e.getData(DiagramEditorController.KEY);
        List<DiagramNode<?>> nodes = e.getData(DiagramDataKeys.SELECTED_NODES);
        if (controller != null && nodes != null) {
            remove(controller, nodes);
        }
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private static <T> void remove(DiagramEditorController<T> controller, List<DiagramNode<?>> nodes) {
        for (DiagramNode<?> node : nodes) {
            controller.getModel().removeNode((DiagramNode<T>) node);
        }
        controller.refresh();
    }
}

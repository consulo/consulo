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
import consulo.diagram.DiagramEdgeCreationPolicy;
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
@ActionImpl(id = "Diagram.CreateEdge")
public class DiagramCreateEdgeAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public DiagramCreateEdgeAction() {
        super(LocalizeValue.localizeTODO("Create Edge"), LocalizeValue.localizeTODO("Connect the first selected node to the second one"));
    }

    @Override
    public void update(AnActionEvent e) {
        DiagramEditorController<?> controller = e.getData(DiagramEditorController.KEY);
        e.getPresentation().setVisible(controller != null && controller.getProvider().getEdgeCreationPolicy() != null);
        e.getPresentation().setEnabled(controller != null && canCreate(controller, selectedNodes(e)));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DiagramEditorController<?> controller = e.getData(DiagramEditorController.KEY);
        if (controller != null) {
            create(controller, selectedNodes(e));
        }
    }

    private static List<DiagramNode<?>> selectedNodes(AnActionEvent e) {
        List<DiagramNode<?>> nodes = e.getData(DiagramDataKeys.SELECTED_NODES);
        return nodes == null ? List.of() : nodes;
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean canCreate(DiagramEditorController<T> controller, List<DiagramNode<?>> selected) {
        DiagramEdgeCreationPolicy<T> policy = controller.getProvider().getEdgeCreationPolicy();
        List<DiagramNode<T>> nodes = (List<DiagramNode<T>>) (List<?>) selected;
        return policy != null && nodes.size() == 2 && policy.acceptSource(nodes.get(0)) && policy.acceptTarget(nodes.get(1));
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private static <T> void create(DiagramEditorController<T> controller, List<DiagramNode<?>> selected) {
        if (!canCreate(controller, selected)) {
            return;
        }

        List<DiagramNode<T>> nodes = (List<DiagramNode<T>>) (List<?>) selected;
        if (controller.getModel().createEdge(nodes.get(0), nodes.get(1)) != null) {
            controller.refresh();
        }
    }
}

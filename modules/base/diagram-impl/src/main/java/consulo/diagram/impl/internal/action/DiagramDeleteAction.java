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
import consulo.diagram.DiagramDeleteProvider;
import consulo.diagram.DiagramNode;
import consulo.diagram.impl.internal.editor.DiagramEditorController;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.MessageBoxes;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
@ActionImpl(id = "Diagram.Delete")
public class DiagramDeleteAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public DiagramDeleteAction() {
        super(LocalizeValue.localizeTODO("Delete"), LocalizeValue.localizeTODO("Delete the elements of the selected nodes"), PlatformIconGroup.actionsCancel());
    }

    @Override
    public void update(AnActionEvent e) {
        DiagramEditorController<?> controller = e.getData(DiagramEditorController.KEY);
        e.getPresentation().setVisible(controller != null && controller.getProvider().getDeleteProvider() != null);
        e.getPresentation().setEnabled(controller != null && !deletable(controller, selectedNodes(e)).isEmpty());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DiagramEditorController<?> controller = e.getData(DiagramEditorController.KEY);
        if (controller != null) {
            delete(controller, selectedNodes(e));
        }
    }

    private static List<DiagramNode<?>> selectedNodes(AnActionEvent e) {
        List<DiagramNode<?>> nodes = e.getData(DiagramDataKeys.SELECTED_NODES);
        return nodes == null ? List.of() : nodes;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<DiagramNode<T>> deletable(DiagramEditorController<T> controller, List<DiagramNode<?>> selected) {
        DiagramDeleteProvider<T> deleteProvider = controller.getProvider().getDeleteProvider();
        List<DiagramNode<T>> nodes = new ArrayList<>();
        if (deleteProvider != null) {
            for (DiagramNode<?> node : selected) {
                DiagramNode<T> typed = (DiagramNode<T>) node;
                if (deleteProvider.canDeleteNode(typed)) {
                    nodes.add(typed);
                }
            }
        }
        return nodes;
    }

    @RequiredUIAccess
    private static <T> void delete(DiagramEditorController<T> controller, List<DiagramNode<?>> selected) {
        DiagramDeleteProvider<T> deleteProvider = controller.getProvider().getDeleteProvider();
        List<DiagramNode<T>> nodes = deletable(controller, selected);
        if (deleteProvider == null || nodes.isEmpty()) {
            return;
        }

        MessageBoxes.okCancel()
            .asQuestion()
            .title(LocalizeValue.localizeTODO("Delete"))
            .text(LocalizeValue.localizeTODO("Delete the selected elements?"))
            .showAsync()
            .whenComplete((confirmed, throwable) -> {
                if (!Boolean.TRUE.equals(confirmed)) {
                    return;
                }

                for (DiagramNode<T> node : nodes) {
                    deleteProvider.deleteNode(node);
                }
                controller.rebuild();
            });
    }
}

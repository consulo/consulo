/*
 * Copyright 2013-2025 consulo.io
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
package consulo.diagram.impl.internal.editor;

import consulo.dataContext.UiDataProvider;
import consulo.diagram.DiagramDataKeys;
import consulo.diagram.DiagramDataModel;
import consulo.diagram.DiagramExtras;
import consulo.diagram.DiagramNode;
import consulo.diagram.DiagramProvider;
import consulo.diagram.impl.internal.action.DiagramPopupGroup;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramTarget;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramVirtualFile;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.graph.Graph;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
public class DiagramFileEditor extends UserDataHolderBase implements FileEditor {
    private static final String POPUP_PLACE = "DiagramPopup";

    private final Project myProject;
    private final DiagramVirtualFile myVirtualFile;

    private @Nullable LoadingLayout<DockLayout> myLoadingLayout;
    private @Nullable DiagramEditorController<?> myController;
    private volatile boolean myDisposed;

    public DiagramFileEditor(Project project, DiagramVirtualFile virtualFile) {
        myProject = project;
        myVirtualFile = virtualFile;
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        if (myLoadingLayout == null) {
            LoadingLayout<DockLayout> loadingLayout = LoadingLayout.create(DockLayout.create(), this);
            myLoadingLayout = loadingLayout;

            loadingLayout.setLoadingText(LocalizeValue.localizeTODO("Building Diagram..."));
            loadingLayout.startLoading(this::buildSession, (layout, session) -> {
                if (session == null) {
                    layout.center(Label.create(LocalizeValue.localizeTODO("Error. Invalid Diagram")));
                }
                else if (myDisposed) {
                    Disposer.dispose(session.model());
                }
                else {
                    layout.center(createGraph(session));
                }
            });
        }
        return myLoadingLayout;
    }

    private @Nullable DiagramSession<?> buildSession() {
        return DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
            DiagramTarget<Object> target = myVirtualFile.resolve(myProject);
            if (target == null) {
                return null;
            }
            return createSession(target);
        });
    }

    private <T> DiagramSession<T> createSession(DiagramTarget<T> target) {
        DiagramProvider<T> provider = target.provider();
        DiagramDataModel<T> model = provider.createDataModel(myProject, target.element(), myVirtualFile);
        return new DiagramSession<>(provider, model, DiagramGraphSnapshot.of(provider, model));
    }

    @RequiredUIAccess
    private <T> Component createGraph(DiagramSession<T> session) {
        DiagramGraphModel graphModel = new DiagramGraphModel(session.snapshot());
        Graph<DiagramGraphNode> graph = Graph.create(graphModel);

        DiagramEditorController<T> controller = new DiagramEditorController<>(myProject, myVirtualFile, session, graphModel, graph);
        myController = controller;

        graph.setNodeRender((presentation, item) -> {
            DiagramGraphNode node = item.getValue();
            if (node == null) {
                return;
            }

            presentation.header().withIcon(node.getIcon());
            presentation.header().append(node.getName(), TextAttribute.REGULAR_BOLD);
            presentation.withTooltip(LocalizeValue.of(node.getTooltip()));

            for (List<DiagramGraphRow> section : node.getSections()) {
                presentation.addSeparator();
                for (DiagramGraphRow row : section) {
                    TextItemPresentation rowPresentation = presentation.addRow();
                    rowPresentation.withIcon(row.icon());
                    for (DiagramGraphFragment fragment : row.fragments()) {
                        rowPresentation.append(fragment.text(), fragment.attribute());
                    }
                }
            }
        });

        graph.setEdgeRender((presentation, source, target) -> {
            DiagramGraphEdgeStyle style = graphModel.getEdgeStyle(source, target);
            if (style == null) {
                return;
            }

            presentation.withLineStyle(style.lineStyle())
                .withSourceArrow(style.sourceArrow())
                .withTargetArrow(style.targetArrow())
                .withLabel(LocalizeValue.of(style.label()))
                .withTooltip(LocalizeValue.of(style.tooltip()))
                .withColor(style.color());
        });

        graph.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(DiagramEditorController.KEY, controller);
            sink.set(DiagramDataKeys.PROVIDER, controller.getProvider());
            sink.set(DiagramDataKeys.DATA_MODEL, controller.getModel());
            sink.set(DiagramDataKeys.SELECTED_NODES, List.copyOf(controller.getSelectedNodes()));
        });

        graph.addContextMenuListener(event -> showPopup(graph, controller, event));
        return graph;
    }

    @RequiredUIAccess
    private static <T> void showPopup(Graph<DiagramGraphNode> graph, DiagramEditorController<T> controller, ContextMenuEvent event) {
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup group = new DefaultActionGroup();

        DiagramExtras<T> extras = controller.getProvider().getExtras();
        if (extras != null) {
            List<DiagramNode<T>> selected = controller.getSelectedNodes();
            ActionGroup custom = selected.isEmpty() ? extras.getPaperActionGroup() : extras.getNodeActionGroup(selected.get(0));
            if (custom != null) {
                group.add(custom);
                group.addSeparator();
            }
        }

        if (actionManager.getAction(DiagramPopupGroup.ID) instanceof ActionGroup diagramGroup) {
            group.addAll(diagramGroup);
        }

        ActionPopupMenu menu = actionManager.createActionPopupMenu(POPUP_PLACE, group);
        menu.setTargetComponent(graph);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @Override
    public String getName() {
        return myVirtualFile.getName();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void dispose() {
        myDisposed = true;

        DiagramEditorController<?> controller = myController;
        if (controller != null) {
            controller.dispose();
        }
    }
}

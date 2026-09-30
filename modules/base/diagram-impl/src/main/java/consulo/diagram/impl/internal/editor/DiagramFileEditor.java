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

import consulo.application.ReadAction;
import consulo.diagram.DiagramDataModel;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramTarget;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramVirtualFile;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
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
    private final Project myProject;
    private final DiagramVirtualFile myVirtualFile;

    private @Nullable LoadingLayout<DockLayout> myLoadingLayout;

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
            loadingLayout.startLoading(this::buildSnapshot, (layout, snapshot) -> {
                if (snapshot == null) {
                    layout.center(Label.create(LocalizeValue.localizeTODO("Error. Invalid Diagram")));
                }
                else {
                    Graph<DiagramGraphNode> graph = Graph.create(snapshot);
                    graph.setNodeRender((presentation, item) -> {
                        DiagramGraphNode node = item.getValue();
                        if (node == null) {
                            return;
                        }

                        presentation.header().withIcon(node.getIcon());
                        presentation.header().append(node.getName(), TextAttribute.REGULAR_BOLD);

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
                        DiagramGraphEdgeStyle style = snapshot.getEdgeStyle(source, target);
                        if (style == null) {
                            return;
                        }

                        presentation.withLineStyle(style.lineStyle())
                            .withSourceArrow(style.sourceArrow())
                            .withTargetArrow(style.targetArrow())
                            .withLabel(LocalizeValue.of(style.label()))
                            .withColor(style.color());
                    });
                    layout.center(graph);
                }
            });
        }
        return myLoadingLayout;
    }

    private @Nullable DiagramGraphSnapshot buildSnapshot() {
        return ReadAction.compute(() -> {
            DiagramTarget<Object> target = myVirtualFile.resolve(myProject);
            if (target == null) {
                return null;
            }

            DiagramDataModel<Object> model = target.provider().createDataModel(myProject, target.element(), myVirtualFile);
            try {
                return DiagramGraphSnapshot.of(target.provider(), model);
            }
            finally {
                Disposer.dispose(model);
            }
        });
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
    }
}

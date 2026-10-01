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
package consulo.diagram.impl.internal.editor;

import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.diagram.DiagramDataModel;
import consulo.diagram.DiagramNode;
import consulo.diagram.DiagramProvider;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramTarget;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramVirtualFile;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.graph.Graph;
import consulo.util.dataholder.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
public final class DiagramEditorController<T> {
    public static final Key<DiagramEditorController<?>> KEY = Key.create("DiagramEditorController");

    private final Project myProject;
    private final DiagramVirtualFile myFile;
    private final DiagramProvider<T> myProvider;
    private final DiagramGraphModel myGraphModel;
    private final Graph<DiagramGraphNode> myGraph;

    private volatile DiagramDataModel<T> myModel;
    private volatile boolean myDisposed;

    public DiagramEditorController(Project project,
                                   DiagramVirtualFile file,
                                   DiagramSession<T> session,
                                   DiagramGraphModel graphModel,
                                   Graph<DiagramGraphNode> graph) {
        myProject = project;
        myFile = file;
        myProvider = session.provider();
        myModel = session.model();
        myGraphModel = graphModel;
        myGraph = graph;
    }

    public DiagramProvider<T> getProvider() {
        return myProvider;
    }

    public DiagramDataModel<T> getModel() {
        return myModel;
    }

    @SuppressWarnings("unchecked")
    public List<DiagramNode<T>> getSelectedNodes() {
        List<DiagramNode<T>> nodes = new ArrayList<>();
        for (DiagramGraphNode node : myGraph.getSelectedValues()) {
            nodes.add((DiagramNode<T>) node.getDiagramNode());
        }
        return nodes;
    }

    @RequiredUIAccess
    public void refresh() {
        DiagramDataModel<T> model = myModel;
        update(() -> ReadAction.compute(() -> DiagramGraphSnapshot.of(myProvider, model)));
    }

    @RequiredUIAccess
    public void rebuild() {
        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.supplyAsync(() -> ReadAction.compute(() -> {
                DiagramTarget<T> target = myFile.resolve(myProject);
                return target == null ? null : myProvider.createDataModel(myProject, target.element(), myFile);
            }), AppExecutorUtil.getAppExecutorService())
            .thenAcceptAsync(model -> {
                if (model == null) {
                    refresh();
                    return;
                }

                if (myDisposed || myProject.isDisposed()) {
                    Disposer.dispose(model);
                    return;
                }

                DiagramDataModel<T> old = myModel;
                myModel = model;
                Disposer.dispose(old);
                refresh();
            }, uiAccess);
    }

    @RequiredUIAccess
    private void update(Supplier<DiagramGraphSnapshot> snapshotSupplier) {
        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.supplyAsync(snapshotSupplier, AppExecutorUtil.getAppExecutorService())
            .thenAcceptAsync(snapshot -> {
                if (myDisposed || myProject.isDisposed()) {
                    return;
                }
                myGraphModel.setSnapshot(snapshot);
                myGraph.refresh();
            }, uiAccess);
    }

    public void dispose() {
        myDisposed = true;
        Disposer.dispose(myModel);
    }
}

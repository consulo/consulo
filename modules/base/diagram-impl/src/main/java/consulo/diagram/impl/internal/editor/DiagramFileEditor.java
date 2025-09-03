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
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.diagram.GraphBuilder;
import consulo.diagram.GraphProvider;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramVirtualFile;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ex.awt.JBLoadingPanel;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
public class DiagramFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project myProject;
    private final DiagramVirtualFile myVirtualFile;

    private JBLoadingPanel myLoadingPanel;

    private Future<?> myLoadingFuture = CompletableFuture.completedFuture(null);

    private JPanel myPanel;

    public DiagramFileEditor(Project project, DiagramVirtualFile virtualFile) {
        myProject = project;
        myVirtualFile = virtualFile;
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        myLoadingPanel = new JBLoadingPanel(new BorderLayout(), this);

        myPanel = new JPanel(new BorderLayout());

        myLoadingPanel.add(ScrollPaneFactory.createScrollPane(myPanel, true), BorderLayout.CENTER);

        UiNotifyConnector.doWhenFirstShown(myLoadingPanel, () -> {
            myLoadingPanel.setLoadingText(LocalizeValue.localizeTODO("Building Diagram..."));
            myLoadingPanel.startLoading();
            buildData();
        });

        return myLoadingPanel;
    }

    private void buildData() {
        myLoadingFuture = AppExecutorUtil.getAppExecutorService().submit(() -> {

            Map.Entry<GraphProvider<Object>, Object> entry = ReadAction.compute(() -> myVirtualFile.resolve(myProject));
            if (entry == null) {
                onReady(new JLabel("Error. Invalid Diagram"));
            }
            else {
                GraphProvider<Object> key = entry.getKey();
                Object value = entry.getValue();

                GraphBuilder builder = ReadAction.compute(() -> key.createBuilder(value));

                Component component = builder.getComponent();

                onReady(TargetAWT.to(component));
            }
        });
    }

    private void onReady(java.awt.Component component) {
        SwingUtilities.invokeLater(() -> {
            myPanel.add(component);

            myLoadingPanel.invalidate();

            myLoadingPanel.stopLoading();
        });
    }

    @Nonnull
    @Override
    public String getName() {
        return myVirtualFile.getName();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void selectNotify() {

    }

    @Override
    public void deselectNotify() {

    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {

    }

    @Override
    public void dispose() {
        myLoadingFuture.cancel(false);
    }
}

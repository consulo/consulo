/*
 * Copyright 2013-2016 consulo.io
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

import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.dataContext.DataContext;
import consulo.diagram.DiagramProvider;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramVirtualFile;
import consulo.diagram.impl.internal.virtualFileSystem.DiagramVirtualFileSystem;
import consulo.fileEditor.FileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.virtualFileSystem.VirtualFile;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2013-10-15
 */
public class ShowDiagramAction extends AnAction implements AnActionWithAsyncUpdate {
    private final ProgressBuilderFactory myProgressBuilderFactory;

    public ShowDiagramAction(ProgressBuilderFactory progressBuilderFactory) {
        super(ActionLocalize.actionShowdiagramText(), ActionLocalize.actionShowdiagramText(), PlatformIconGroup.filetypesDiagram());
        myProgressBuilderFactory = progressBuilderFactory;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        DiagramProvider<?> provider = DiagramProvider.findProvider(e.getDataContext());
        if (provider == null) {
            return;
        }

        openDiagram(project, provider, e.getDataContext());
    }

    @RequiredUIAccess
    private <T> void openDiagram(Project project, DiagramProvider<T> provider, DataContext dataContext) {
        T element = provider.getElementManager().findInDataContext(dataContext);
        if (element == null) {
            return;
        }

        CompletableFuture<String> future = myProgressBuilderFactory.newProgressBuilder(project, LocalizeValue.localizeTODO("Preparing Diagram…"))
            .cancelable()
            .execute(UIAccess.current(), () -> ReadLock.apply(o -> DiagramVirtualFile.buildPath(provider, element)).toCoroutine());

        UIAccess uiAccess = UIAccess.current();

        future.whenCompleteAsync((path, throwable) -> {
            if (path == null) {
                return;
            }

            VirtualFile file = DiagramVirtualFileSystem.getInstance().findFileByPath(path);
            if (file != null) {
                FileEditorManager.getInstance(project).openFile(file, true);
            }
        }, uiAccess);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            presentation.setEnabledAndVisible(DiagramProvider.findProvider(e.getDataContext()) != null);
        }).toCoroutine();
    }
}

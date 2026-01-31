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

import consulo.application.Application;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.diagram.GraphProvider;
import consulo.diagram.impl.internal.DiagramSupportEapDescriptor;
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
import consulo.ui.ex.action.Presentation;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2013-10-15
 */
public class ShowDiagramAction extends AnAction {
    private final Application myApplication;
    private final ProgressBuilderFactory myProgressBuilderFactory;

    public ShowDiagramAction(Application application, ProgressBuilderFactory progressBuilderFactory) {
        super(ActionLocalize.actionShowdiagramText(), ActionLocalize.actionShowdiagramText(), PlatformIconGroup.filetypesDiagram());
        myApplication = application;
        myProgressBuilderFactory = progressBuilderFactory;
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        Map.Entry<GraphProvider, Object> entry = myApplication.getExtensionPoint(GraphProvider.class)
            .computeSafeIfAny(p -> {
                Object element = p.findSupportedElement(e.getDataContext());
                if (element == null) {
                    return null;
                }
                return Map.entry(p, element);
            });

        if (entry == null) {
            return;
        }

        GraphProvider p = entry.getKey();
        Object graphValue = entry.getValue();

        CompletableFuture<String> future = myProgressBuilderFactory.newProgressBuilder(project, LocalizeValue.localizeTODO("Preparing Diagram..."))
            .cancelable()
            .execute(UIAccess.current(), coroutine -> {
                return coroutine.then(ReadLock.apply(o -> {
                    return p.getId() + URLUtil.ARCHIVE_SEPARATOR + p.getName(graphValue) + URLUtil.ARCHIVE_SEPARATOR + p.getURL(graphValue);
                }));
            });

        UIAccess uiAccess = UIAccess.current();

        future.whenCompleteAsync((graphURL, throwable) -> {
            VirtualFile file = DiagramVirtualFileSystem.getInstance().findFileByPath(graphURL);
            if (file != null) {
                FileEditorManager.getInstance(project).openFile(file, true);
            }
        }, uiAccess);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (!EarlyAccessProgramManager.is(DiagramSupportEapDescriptor.class)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        Map.Entry<GraphProvider, Object> entry = myApplication.getExtensionPoint(GraphProvider.class)
            .computeSafeIfAny(p -> {
                Object element = p.findSupportedElement(e.getDataContext());
                if (element == null) {
                    return null;
                }
                return Map.entry(p, element);
            });

        presentation.setEnabledAndVisible(entry != null);
    }
}

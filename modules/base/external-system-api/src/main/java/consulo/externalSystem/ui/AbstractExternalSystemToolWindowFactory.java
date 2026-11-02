/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.ui;

import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposable;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.manage.ExternalProjectsManager;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.view.ExternalProjectsViewImpl;
import consulo.localize.LocalizeValue;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;

import java.util.Objects;

/**
 * @author Denis Zhdanov
 * @since 2013-05-13
 */
public abstract class AbstractExternalSystemToolWindowFactory implements ToolWindowFactory, DumbAware {

    private final ProjectSystemId myExternalSystemId;

    protected AbstractExternalSystemToolWindowFactory(ProjectSystemId id) {
        myExternalSystemId = id;
    }

    @Override
    public final String getId() {
        return myExternalSystemId.getToolWindowId();
    }

    @Override
    public final LocalizeValue getDisplayName() {
        return myExternalSystemId.getDisplayName();
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        ExternalProjectsManager.getInstance(project).runWhenInitialized(() -> {
            Disposable parentDisposable = Disposable.newDisposable("ExternalProjectsView[" + myExternalSystemId.getId() + "]");
            ExternalProjectsViewImpl view = new ExternalProjectsViewImpl(
                parentDisposable,
                project,
                toolWindow,
                myExternalSystemId
            );
            ExternalProjectsManager.getInstance(project).registerView(myExternalSystemId, view);
            view.init();
            Content content = ContentFactory.getInstance().createUIContent(view.getComponent(), "", true);
            content.setDisposer(parentDisposable);
            toolWindow.getContentManager().addUiDataProvider(view);
            contentManager.addContent(content);
        });
    }

    /**
     * Available for a project this system has just imported, for one with a module of this system - its extension,
     * looked up by the system's id: the extensions of all the external systems share one class - or with a linked
     * project of it.
     */
    @Override
    public boolean validate(Project project) {
        ProjectSystemId importedBy = project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_SYSTEM_ID);
        if (importedBy != null) {
            if (importedBy.equals(myExternalSystemId)) {
                return true;
            }
        }
        else if (Objects.equals(project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT), Boolean.TRUE)) {
            // imported without telling by which system: every system's tool window, as before
            return true;
        }

        if (ModuleExtensionHelper.getInstance(project).hasModuleExtension(myExternalSystemId.getId())) {
            return true;
        }

        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
        if (manager == null) {
            return false;
        }

        AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().apply(project);
        return settings != null && !settings.getLinkedProjectsSettings().isEmpty();
    }
}

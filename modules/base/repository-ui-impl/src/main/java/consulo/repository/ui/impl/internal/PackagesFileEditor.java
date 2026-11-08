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
package consulo.repository.ui.impl.internal;

import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.repository.ui.PackageManagementService;
import consulo.repository.ui.PackageManagementServiceProvider;
import consulo.repository.ui.localize.RepositoryUILocalize;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import kava.beans.PropertyChangeListener;

public class PackagesFileEditor extends UserDataHolderBase implements FileEditor {
    private final Project myProject;
    private final PackagesVirtualFile myFile;

    private @Nullable Component myComponent;
    private @Nullable PackagesPanel myPanel;

    public PackagesFileEditor(Project project, PackagesVirtualFile file) {
        myProject = project;
        myFile = file;
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        if (myComponent == null) {
            PackageManagementServiceProvider provider = PackagesVirtualFileSystem.getInstance().findProvider(myFile.getProviderId());
            PackageManagementService service = provider == null ? null : provider.create(myProject, myFile.getTarget());
            if (service == null) {
                DockLayout layout = DockLayout.create();
                layout.center(Label.create(RepositoryUILocalize.packagesNotAvailable()));
                myComponent = layout;
            }
            else {
                myPanel = new PackagesPanel(myProject, service);
                Disposer.register(this, myPanel);
                myComponent = myPanel.getComponent();
            }
        }
        return myComponent;
    }

    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        return myPanel == null ? null : myPanel.getPreferredFocusedComponent();
    }

    @Override
    public String getName() {
        return RepositoryUILocalize.editorName().get();
    }

    @Override
    public VirtualFile getFile() {
        return myFile;
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

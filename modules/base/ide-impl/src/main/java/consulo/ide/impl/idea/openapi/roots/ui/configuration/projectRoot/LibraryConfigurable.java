/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.configurable.MasterDetailsConfigurable;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.ui.LibraryEditor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.LibraryRootsComponent;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author anna
 * @since 2006-06-02
 */
public class LibraryConfigurable implements MasterDetailsConfigurable<Library> {
    private final Project myProject;
    private final LibraryTableModifiableModelProvider myModel;
    private final Library myLibrary;
    private final Consumer<Library> myNameChanged;
    private final Consumer<Library> myRootsChanged;

    private @Nullable LibraryRootsComponent myLibraryEditorComponent;
    private @Nullable TextBox myNameComponent;
    private @Nullable DockLayout myMainPanel;
    private boolean myPropertiesLoaded;

    public LibraryConfigurable(
        Project project,
        LibraryTableModifiableModelProvider modelProvider,
        Library library,
        Consumer<Library> nameChanged,
        Consumer<Library> rootsChanged
    ) {
        myProject = project;
        myModel = modelProvider;
        myLibrary = library;
        myNameChanged = nameChanged;
        myRootsChanged = rootsChanged;
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentUIDisposable) {
        DockLayout mainPanel = myMainPanel;
        if (mainPanel == null) {
            mainPanel = createMainPanel(parentUIDisposable);
            myMainPanel = mainPanel;
        }
        return mainPanel;
    }

    @RequiredUIAccess
    private DockLayout createMainPanel(Disposable parentUIDisposable) {
        LibraryRootsComponent libraryEditorComponent = new LibraryRootsComponent(myProject, this::getLibraryEditor);
        Disposer.register(parentUIDisposable, libraryEditorComponent);
        libraryEditorComponent.addListener(() -> myRootsChanged.accept(myLibrary));
        myLibraryEditorComponent = libraryEditorComponent;

        TextBox nameComponent = TextBox.create(getLibraryName());
        nameComponent.addValueListener(event -> {
            String name = event.getValue();
            if (name != null && !name.equals(getLibraryName())) {
                getLibraryEditor().setName(name);
                myNameChanged.accept(myLibrary);
            }
        });
        myNameComponent = nameComponent;

        FormBuilder formBuilder = FormBuilder.create();
        formBuilder.addLabeled(CommonLocalize.nameLabelText(), nameComponent);

        DockLayout mainPanel = DockLayout.create();
        mainPanel.top(formBuilder.build());
        mainPanel.center(libraryEditorComponent.createUIComponent(parentUIDisposable));
        return mainPanel;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        LibraryRootsComponent libraryEditorComponent = myLibraryEditorComponent;
        return libraryEditorComponent != null && libraryEditorComponent.hasChanges();
    }

    @RequiredUIAccess
    @Override
    public void apply() {
        applyProperties();
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        resetProperties();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        LibraryRootsComponent libraryEditorComponent = myLibraryEditorComponent;
        if (libraryEditorComponent != null) {
            Disposer.dispose(libraryEditorComponent);
        }
        myLibraryEditorComponent = null;
        myNameComponent = null;
        myMainPanel = null;
        myPropertiesLoaded = false;
    }

    public LibraryEditor getLibraryEditor() {
        return ((LibrariesModifiableModel) myModel.getModifiableModel()).getLibraryEditor(myLibrary);
    }

    public String getLibraryName() {
        LibrariesModifiableModel model = (LibrariesModifiableModel) myModel.getModifiableModel();
        String name = model.hasLibraryEditor(myLibrary) ? getLibraryEditor().getName() : myLibrary.getName();
        return name == null ? "" : name;
    }

    @RequiredUIAccess
    @Override
    public void updateName() {
        TextBox nameComponent = myNameComponent;
        if (nameComponent != null) {
            nameComponent.setValue(getLibraryName(), false);
        }
    }

    @Override
    public Library getEditableObject() {
        return myLibrary;
    }

    @Override
    public LocalizeValue getBannerSlogan() {
        LibraryTable libraryTable = myLibrary.getTable();
        String libraryType = libraryTable == null
            ? ProjectLocalize.moduleLibraryDisplayName(1).get()
            : libraryTable.getPresentation().getDisplayName(false);
        return ProjectLocalize.projectRootsLibraryBannerText(getLibraryName(), libraryType);
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(getLibraryName());
    }

    @Override
    public @Nullable Image getIcon() {
        return PlatformIconGroup.nodesPplib();
    }

    @RequiredUIAccess
    public void onSelected() {
        resetProperties();
    }

    @RequiredUIAccess
    public void onUnselected() {
        applyProperties();
    }

    @RequiredUIAccess
    private void resetProperties() {
        LibraryRootsComponent libraryEditorComponent = myLibraryEditorComponent;
        if (libraryEditorComponent != null) {
            libraryEditorComponent.updatePropertiesLabel();
            libraryEditorComponent.resetProperties();
            myPropertiesLoaded = true;
        }
    }

    @RequiredUIAccess
    private void applyProperties() {
        LibraryRootsComponent libraryEditorComponent = myLibraryEditorComponent;
        if (libraryEditorComponent != null && myPropertiesLoaded) {
            libraryEditorComponent.applyProperties();
            myPropertiesLoaded = false;
        }
    }

    @RequiredUIAccess
    public void updateComponent() {
        LibraryRootsComponent libraryEditorComponent = myLibraryEditorComponent;
        if (libraryEditorComponent != null) {
            libraryEditorComponent.updateRootsTree();
        }
    }
}

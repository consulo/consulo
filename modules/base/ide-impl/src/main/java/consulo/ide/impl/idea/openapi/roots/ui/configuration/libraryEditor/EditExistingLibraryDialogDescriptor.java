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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.application.Application;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablePresentation;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.impl.internal.layer.library.ModuleLibraryTable;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.TextBox;
import consulo.ui.WidthAndHeight;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class EditExistingLibraryDialogDescriptor extends DialogDescriptor {
    private static final String DIMENSION_SERVICE_KEY = "#consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.LibraryEditorDialog";

    private final LibraryTable.ModifiableModel myTableModifiableModel;
    private final ExistingLibraryEditor myLibraryEditor;
    private final boolean myCommitChanges;
    private final LibraryRootsComponent myLibraryRootsComponent;
    private final LibrariesConfigurator myLibrariesConfigurator;

    private @Nullable TextBox myNameField;

    private EditExistingLibraryDialogDescriptor(
        LocalizeValue title,
        LibraryTable.ModifiableModel tableModifiableModel,
        @Nullable Project project,
        ExistingLibraryEditor libraryEditor,
        boolean commitChanges,
        LibrariesConfigurator librariesConfigurator
    ) {
        super(title);
        myTableModifiableModel = tableModifiableModel;
        myLibraryEditor = libraryEditor;
        myCommitChanges = commitChanges;
        myLibrariesConfigurator = librariesConfigurator;
        myLibraryRootsComponent = new LibraryRootsComponent(project, libraryEditor);
    }

    @RequiredUIAccess
    public static CompletableFuture<DialogValue> showAsync(
        Component parent,
        LibraryTableModifiableModelProvider modelProvider,
        Library library,
        @Nullable Project project,
        LibraryTablePresentation presentation,
        LibrariesConfigurator librariesConfigurator,
        @Nullable Module contextModule
    ) {
        LibraryTable.ModifiableModel modifiableModel = modelProvider.getModifiableModel();
        boolean commitChanges = false;
        ExistingLibraryEditor libraryEditor;
        if (modifiableModel instanceof LibrariesModifiableModel librariesModifiableModel) {
            libraryEditor = librariesModifiableModel.getLibraryEditor(library);
        }
        else {
            libraryEditor = new ExistingLibraryEditor(library, librariesConfigurator);
            commitChanges = true;
        }

        EditExistingLibraryDialogDescriptor descriptor = new EditExistingLibraryDialogDescriptor(
            ProjectLocalize.libraryConfigureDialogTitle(presentation.getDisplayName(false)),
            modifiableModel,
            project,
            libraryEditor,
            commitChanges,
            librariesConfigurator
        );
        if (contextModule != null) {
            descriptor.myLibraryRootsComponent.setContextModule(contextModule);
        }

        DialogService dialogService = Application.get().getInstance(DialogService.class);
        return dialogService.build(parent, descriptor).showAsync();
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return DIMENSION_SERVICE_KEY;
    }

    @Override
    public WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(50, 30);
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        Disposer.register(uiDisposable, myLibraryRootsComponent);
        if (myCommitChanges) {
            Disposer.register(uiDisposable, myLibraryEditor);
        }

        TextBox nameField = TextBox.create(StringUtil.notNullize(myLibraryEditor.getName()));
        myNameField = nameField;

        myLibrariesConfigurator.addLibraryEditorListener(
            (library, oldName, newName) -> {
                if (library.equals(myLibraryEditor.getLibrary())) {
                    nameField.setValue(newName);
                }
            },
            uiDisposable
        );

        Component rootsComponent = myLibraryRootsComponent.createUIComponent(uiDisposable);
        myLibraryRootsComponent.resetProperties();

        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(CommonLocalize.nameLabelText(), nameField);
        builder.setBottom(rootsComponent);
        return builder.build();
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedComponent() {
        return myNameField;
    }

    @Override
    @RequiredUIAccess
    public boolean canHandle(AnAction action, @Nullable DialogValue value, Window window) {
        if (!isDefaultAction(action)) {
            return super.canHandle(action, value, window);
        }
        return validateAndApply(window);
    }

    @RequiredUIAccess
    private boolean validateAndApply(Window window) {
        TextBox nameField = myNameField;
        String text = nameField == null ? "" : StringUtil.notNullize(nameField.getValue()).trim();
        String newName = text.isEmpty() ? null : text;

        if (!Objects.equals(newName, myLibraryEditor.getName())) {
            if (!(myTableModifiableModel instanceof ModuleLibraryTable)) {
                if (newName == null) {
                    MessageBoxes.okError(ProjectLocalize.libraryNameNotSpecifiedError())
                        .title(ProjectLocalize.libraryNameNotSpecifiedTitle())
                        .showAsync(window);
                    return false;
                }
                if (LibraryEditingUtil.libraryAlreadyExists(myTableModifiableModel, newName)) {
                    MessageBoxes.okError(ProjectLocalize.libraryNameAlreadyExistsError(newName))
                        .title(ProjectLocalize.libraryNameAlreadyExistsTitle())
                        .showAsync(window);
                    return false;
                }
            }
            myLibraryRootsComponent.renameLibrary(newName);
        }

        myLibraryRootsComponent.applyProperties();

        if (myCommitChanges) {
            myLibraryEditor.commit();
        }
        return true;
    }
}

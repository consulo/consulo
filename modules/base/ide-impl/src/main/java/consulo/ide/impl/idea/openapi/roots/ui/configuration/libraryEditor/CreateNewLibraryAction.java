/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.application.WriteAction;
import consulo.component.extension.ExtensionPoint;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryType;
import consulo.content.library.LibraryTypeService;
import consulo.content.library.NewLibraryConfiguration;
import consulo.content.library.ui.DefaultLibraryRootsComponentDescriptor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.BaseLibrariesConfigurable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.ProjectLibrariesConfigurable;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class CreateNewLibraryAction extends DumbAwareAction {
    @Nullable
    private final LibraryType myType;
    private final BaseLibrariesConfigurable myLibrariesConfigurable;
    @Nonnull
    private final Project myProject;

    private CreateNewLibraryAction(
        @Nonnull LocalizeValue text,
        @Nullable Image icon,
        @Nullable LibraryType type,
        @Nonnull BaseLibrariesConfigurable librariesConfigurable,
        @Nonnull Project project
    ) {
        super(text, LocalizeValue.empty(), icon);
        myType = type;
        myLibrariesConfigurable = librariesConfigurable;
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Library library = createLibrary(
            myType,
            myLibrariesConfigurable.getTree(),
            myProject,
            (LibrariesModifiableModel)myLibrariesConfigurable.getModelProvider().getModifiableModel()
        );
        if (library == null) {
            return;
        }

        BaseLibrariesConfigurable rootConfigurable = myLibrariesConfigurable;
        DefaultMutableTreeNode libraryNode =
            MasterDetailsComponent.findNodeByObject((TreeNode)rootConfigurable.getTree().getModel().getRoot(), library);
        rootConfigurable.selectNodeInTree(libraryNode);
        LibraryEditingUtil.showDialogAndAddLibraryToDependencies(library, myProject, true);
    }


    @Nullable
    public static Library createLibrary(
        @Nullable LibraryType type,
        @Nonnull JComponent parentComponent,
        @Nonnull Project project,
        @Nonnull LibrariesModifiableModel modifiableModel
    ) {
        NewLibraryConfiguration configuration = createNewLibraryConfiguration(type, parentComponent, project);
        if (configuration == null) {
            return null;
        }
        LibraryType<?> libraryType = configuration.getLibraryType();
        Library library = modifiableModel.createLibrary(
            LibraryEditingUtil.suggestNewLibraryName(modifiableModel, configuration.getDefaultLibraryName()),
            libraryType != null ? libraryType.getKind() : null
        );

        NewLibraryEditor editor = new NewLibraryEditor(libraryType, configuration.getProperties());
        configuration.addRoots(editor);
        Library.ModifiableModel model = library.getModifiableModel();
        editor.applyTo((LibraryEx.ModifiableModelEx)model);
        WriteAction.run(model::commit);
        return library;
    }

    @Nullable
    public static NewLibraryConfiguration createNewLibraryConfiguration(
        @Nullable LibraryType type,
        @Nonnull JComponent parentComponent,
        @Nonnull Project project
    ) {
        NewLibraryConfiguration configuration;
        VirtualFile baseDir = project.getBaseDir();
        if (type != null) {
            configuration = type.createNewLibrary(parentComponent, baseDir, project);
        }
        else {
            configuration = LibraryTypeService.getInstance()
                .createLibraryFromFiles(new DefaultLibraryRootsComponentDescriptor(), parentComponent, baseDir, null, project);
        }
        if (configuration == null) {
            return null;
        }
        return configuration;
    }

    public static AnAction[] createActionOrGroup(
        @Nonnull String text,
        @Nonnull BaseLibrariesConfigurable librariesConfigurable,
        @Nonnull Project project
    ) {
        ExtensionPoint<LibraryType> extensionPoint = project.getApplication().getExtensionPoint(LibraryType.class);

        List<LibraryType> suitableTypes;
        if (librariesConfigurable instanceof ProjectLibrariesConfigurable) {
            suitableTypes = extensionPoint
                .collectFiltered(extension -> !LibraryEditingUtil.getSuitableModules(project, extension.getKind(), null).isEmpty());
        }
        else {
            suitableTypes = extensionPoint.getExtensionList();
        }

        if (suitableTypes.isEmpty()) {
            return new AnAction[]{new CreateNewLibraryAction(
                LocalizeValue.localizeTODO(text),
                PlatformIconGroup.nodesPplib(),
                null,
                librariesConfigurable,
                project
            )};
        }
        List<AnAction> actions = new ArrayList<>();
        actions.add(new CreateNewLibraryAction(
            IdeLocalize.createDefaultLibraryTypeActionName(),
            PlatformIconGroup.nodesPplib(),
            null,
            librariesConfigurable,
            project
        ));
        for (LibraryType<?> type : suitableTypes) {
            String actionName = type.getCreateActionName();
            if (actionName != null) {
                actions.add(new CreateNewLibraryAction(LocalizeValue.of(actionName), type.getIcon(), type, librariesConfigurable, project));
            }
        }
        return actions.toArray(new AnAction[actions.size()]);
    }
}

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

import consulo.application.AllIcons;
import consulo.content.library.ui.DefaultLibraryRootsComponentDescriptor;
import consulo.ide.IdeBundle;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.WriteAction;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryType;
import consulo.content.library.LibraryTypeService;
import consulo.content.library.NewLibraryConfiguration;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.BaseLibrariesConfigurable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.ProjectLibrariesConfigurable;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

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
    private final Project myProject;

    private CreateNewLibraryAction(
        @Nonnull String text,
        @Nullable Image icon,
        @Nullable LibraryType type,
        @Nonnull BaseLibrariesConfigurable librariesConfigurable,
        final @Nonnull Project project
    ) {
        super(text, null, icon);
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

        final BaseLibrariesConfigurable rootConfigurable = myLibrariesConfigurable;
        final DefaultMutableTreeNode libraryNode =
            MasterDetailsComponent.findNodeByObject((TreeNode)rootConfigurable.getTree().getModel().getRoot(), library);
        rootConfigurable.selectNodeInTree(libraryNode);
        LibraryEditingUtil.showDialogAndAddLibraryToDependencies(library, myProject, true);
    }


    @Nullable
    public static Library createLibrary(
        @Nullable final LibraryType type,
        @Nonnull final JComponent parentComponent,
        @Nonnull final Project project,
        @Nonnull final LibrariesModifiableModel modifiableModel
    ) {
        final NewLibraryConfiguration configuration = createNewLibraryConfiguration(type, parentComponent, project);
        if (configuration == null) {
            return null;
        }
        final LibraryType<?> libraryType = configuration.getLibraryType();
        final Library library = modifiableModel.createLibrary(
            LibraryEditingUtil.suggestNewLibraryName(modifiableModel, configuration.getDefaultLibraryName()),
            libraryType != null ? libraryType.getKind() : null
        );

        final NewLibraryEditor editor = new NewLibraryEditor(libraryType, configuration.getProperties());
        configuration.addRoots(editor);
        final Library.ModifiableModel model = library.getModifiableModel();
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
        final NewLibraryConfiguration configuration;
        final VirtualFile baseDir = project.getBaseDir();
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
        final @Nonnull Project project
    ) {
        final List<LibraryType> extensions = LibraryType.EP_NAME.getExtensionList();
        List<LibraryType> suitableTypes = new ArrayList<>();
        if (librariesConfigurable instanceof ProjectLibrariesConfigurable) {
            for (LibraryType<?> extension : extensions) {
                if (!LibraryEditingUtil.getSuitableModules(project, extension.getKind(), null).isEmpty()) {
                    suitableTypes.add(extension);
                }
            }
        }
        else {
            suitableTypes.addAll(extensions);
        }

        if (suitableTypes.isEmpty()) {
            return new AnAction[]{new CreateNewLibraryAction(text, AllIcons.Nodes.PpLib, null, librariesConfigurable, project)};
        }
        List<AnAction> actions = new ArrayList<>();
        actions.add(new CreateNewLibraryAction(
            IdeBundle.message("create.default.library.type.action.name"),
            AllIcons.Nodes.PpLib,
            null,
            librariesConfigurable,
            project
        ));
        for (LibraryType<?> type : suitableTypes) {
            final String actionName = type.getCreateActionName();
            if (actionName != null) {
                actions.add(new CreateNewLibraryAction(actionName, type.getIcon(), type, librariesConfigurable, project));
            }
        }
        return actions.toArray(new AnAction[actions.size()]);
    }
}

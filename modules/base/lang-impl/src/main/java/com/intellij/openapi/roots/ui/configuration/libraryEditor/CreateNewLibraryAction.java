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
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.LibraryTypeService;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import com.intellij.openapi.roots.ui.configuration.projectRoot.BaseLibrariesConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ModuleStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectLibrariesConfigurable;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.application.AccessRule;
import consulo.application.CallChain;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class CreateNewLibraryAction extends DumbAwareAction {
  @Nullable
  private final LibraryType myType;
  private final BaseLibrariesConfigurable myLibrariesConfigurable;
  private final Project myProject;

  private CreateNewLibraryAction(@Nonnull String text, @Nullable Image icon, @Nullable LibraryType type, @Nonnull BaseLibrariesConfigurable librariesConfigurable, final @Nonnull Project project) {
    super(text, null, icon);
    myType = type;
    myLibrariesConfigurable = librariesConfigurable;
    myProject = project;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    CallChain.first(UIAccess.current())
            .linkAsync(() -> createLibrary(myType, myLibrariesConfigurable.getTree(), myProject, myLibrariesConfigurable.getModelProvider().getModifiableModel()))
            .linkUI((library) -> {
              final BaseLibrariesConfigurable rootConfigurable = ProjectStructureConfigurable.getInstance(myProject).getConfigurableFor(library);
              final DefaultMutableTreeNode libraryNode = MasterDetailsComponent.findNodeByObject((TreeNode)rootConfigurable.getTree().getModel().getRoot(), library);
              rootConfigurable.selectNodeInTree(libraryNode);
              LibraryEditingUtil.showDialogAndAddLibraryToDependencies(library, myProject, true);
              return null;
            })
            .toss();
  }

  @Nonnull
  @RequiredUIAccess
  public static AsyncResult<Library> createLibrary(@Nullable final LibraryType type,
                                                   @Nonnull final JComponent parentComponent,
                                                   @Nonnull final Project project,
                                                   @Nonnull final LibrariesModifiableModel modifiableModel) {
    AsyncResult<Library> libraryAsync = AsyncResult.undefined();

    createNewLibraryConfiguration(type, parentComponent, project).doWhenDone(configuration -> {
      final LibraryType<?> libraryType = configuration.getLibraryType();
      final Library library =
              modifiableModel.createLibrary(LibraryEditingUtil.suggestNewLibraryName(modifiableModel, configuration.getDefaultLibraryName()), libraryType != null ? libraryType.getKind() : null);

      final NewLibraryEditor editor = new NewLibraryEditor(libraryType, configuration.getProperties());
      configuration.addRoots(editor);
      final Library.ModifiableModel model = library.getModifiableModel();
      editor.applyTo((LibraryEx.ModifiableModelEx)model);

      AccessRule.writeAsync(model::commit).doWhenDone(() -> libraryAsync.setDone(library));
    });

    return libraryAsync;
  }

  @RequiredUIAccess
  @Nonnull
  public static AsyncResult<NewLibraryConfiguration> createNewLibraryConfiguration(@Nullable LibraryType type, @Nonnull JComponent parentComponent, @Nonnull Project project) {
    final AsyncResult<NewLibraryConfiguration> configuration;
    final VirtualFile baseDir = project.getBaseDir();
    if (type != null) {
      configuration = AsyncResult.resolved(type.createNewLibrary(parentComponent, baseDir, project));
    }
    else {
      configuration = LibraryTypeService.getInstance().createLibraryFromFiles(new DefaultLibraryRootsComponentDescriptor(), parentComponent, baseDir, null, project);
    }
    return configuration;
  }

  public static AnAction[] createActionOrGroup(@Nonnull String text, @Nonnull BaseLibrariesConfigurable librariesConfigurable, final @Nonnull Project project) {
    final LibraryType<?>[] extensions = LibraryType.EP_NAME.getExtensions();
    List<LibraryType<?>> suitableTypes = new ArrayList<>();
    if (librariesConfigurable instanceof ProjectLibrariesConfigurable) {
      final ModuleStructureConfigurable configurable = ModuleStructureConfigurable.getInstance(project);
      for (LibraryType<?> extension : extensions) {
        if (!LibraryEditingUtil.getSuitableModules(configurable, extension.getKind(), null).isEmpty()) {
          suitableTypes.add(extension);
        }
      }
    }
    else {
      Collections.addAll(suitableTypes, extensions);
    }

    if (suitableTypes.isEmpty()) {
      return new AnAction[]{new CreateNewLibraryAction(text, AllIcons.Nodes.PpLib, null, librariesConfigurable, project)};
    }
    List<AnAction> actions = new ArrayList<>();
    actions.add(new CreateNewLibraryAction(IdeBundle.message("create.default.library.type.action.name"), AllIcons.Nodes.PpLib, null, librariesConfigurable, project));
    for (LibraryType<?> type : suitableTypes) {
      final String actionName = type.getCreateActionName();
      if (actionName != null) {
        actions.add(new CreateNewLibraryAction(actionName, type.getIcon(), type, librariesConfigurable, project));
      }
    }
    return actions.toArray(new AnAction[actions.size()]);
  }
}

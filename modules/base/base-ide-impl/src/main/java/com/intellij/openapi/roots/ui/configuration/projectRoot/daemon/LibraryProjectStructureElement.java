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
package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.ExistingLibraryEditor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibraryConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectLibrariesConfigurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.intellij.xml.util.XmlStringUtil;
import consulo.ide.settings.impl.ProjectStructureSettingsUtil;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.DocumentationOrderRootType;
import consulo.roots.types.SourcesOrderRootType;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.preferences.MasterDetailsConfigurable;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author nik
 */
public class LibraryProjectStructureElement extends ProjectStructureElement {
  private final Library myLibrary;

  public LibraryProjectStructureElement(@Nonnull Library library) {
    myLibrary = library;
  }

  public Library getLibrary() {
    return myLibrary;
  }

  @Override
  public void check(@Nonnull Project project, ProjectStructureProblemsHolder problemsHolder) {
    if (((LibraryEx)myLibrary).isDisposed()) return;

    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();

    LibrariesConfigurator librariesConfigurator = util.getLibrariesModel(project);

    final LibraryEx library = (LibraryEx)librariesConfigurator.getLibraryModel(myLibrary);
    if (library == null || library.isDisposed()) return;

    reportInvalidRoots(project, problemsHolder, library, BinariesOrderRootType.getInstance(), "classes", ProjectStructureProblemType.error("library-invalid-classes-path"));
    final String libraryName = library.getName();
    if (libraryName == null || !libraryName.startsWith("Maven: ")) {
      reportInvalidRoots(project, problemsHolder, library, SourcesOrderRootType.getInstance(), "sources", ProjectStructureProblemType.warning("library-invalid-source-javadoc-path"));
      reportInvalidRoots(project, problemsHolder, library, DocumentationOrderRootType.getInstance(), "javadoc", ProjectStructureProblemType.warning("library-invalid-source-javadoc-path"));
    }
  }

  private void reportInvalidRoots(Project project,
                                  ProjectStructureProblemsHolder problemsHolder,
                                  LibraryEx library,
                                  final OrderRootType type,
                                  String rootName,
                                  final ProjectStructureProblemType problemType) {
    final List<String> invalidUrls = library.getInvalidRootUrls(type);
    if (!invalidUrls.isEmpty()) {
      final String description = createInvalidRootsDescription(invalidUrls, rootName, library.getName());
      final PlaceInProjectStructure place = createPlace();
      final String message = ProjectBundle.message("project.roots.error.message.invalid.roots", rootName, invalidUrls.size());
      ProjectStructureProblemDescription.ProblemLevel level = library.getTable().getTableLevel().equals(LibraryTablesRegistrar.PROJECT_LEVEL)
                                                              ? ProjectStructureProblemDescription.ProblemLevel.PROJECT
                                                              : ProjectStructureProblemDescription.ProblemLevel.GLOBAL;
      problemsHolder.registerProblem(
              new ProjectStructureProblemDescription(message, description, place, problemType, level, List.of(new RemoveInvalidRootsQuickFix(project, library, type, invalidUrls)), true));
    }
  }

  private static String createInvalidRootsDescription(List<String> invalidClasses, String rootName, String libraryName) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("Library '").append(StringUtil.escapeXml(libraryName)).append("' has broken ").append(rootName).append(" ").append(StringUtil.pluralize("path", invalidClasses.size())).append(":");
    for (String url : invalidClasses) {
      buffer.append("<br>&nbsp;&nbsp;");
      buffer.append(PathUtil.toPresentableUrl(url));
    }
    return XmlStringUtil.wrapInHtml(buffer);
  }

  @Nonnull
  private PlaceInProjectStructure createPlace() {
    return new PlaceInProjectStructureBase(this::librariesNavigator, this);
  }

  @RequiredUIAccess
  private AsyncResult<Void> librariesNavigator(Project project) {
    return ShowSettingsUtil.getInstance().showProjectStructureDialog(project, projectStructureSelector -> {
      projectStructureSelector.selectProjectOrGlobalLibrary(myLibrary, true);
    });
  }

  @Override
  public List<ProjectStructureElementUsage> getUsagesInElement() {
    return Collections.emptyList();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LibraryProjectStructureElement)) return false;

    return getSourceOrThis() == (((LibraryProjectStructureElement)o).getSourceOrThis());
  }

  public AsyncResult<Void> navigate(Project project) {
    return createPlace().navigate(project);
  }

  @Nonnull
  private Library getSourceOrThis() {
    final InvocationHandler invocationHandler = Proxy.isProxyClass(myLibrary.getClass()) ? Proxy.getInvocationHandler(myLibrary) : null;
    final Library realLibrary = invocationHandler instanceof ModuleEditor.ProxyDelegateAccessor ? (Library)((ModuleEditor.ProxyDelegateAccessor)invocationHandler).getDelegate() : myLibrary;
    final Library source = realLibrary instanceof LibraryImpl ? ((LibraryImpl)realLibrary).getSource() : null;
    return source != null ? source : myLibrary;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(getSourceOrThis());
  }

  @Override
  public boolean shouldShowWarningIfUnused() {
    final LibraryTable libraryTable = myLibrary.getTable();
    if (libraryTable == null) return false;
    return LibraryTablesRegistrar.PROJECT_LEVEL.equals(libraryTable.getTableLevel());
  }

  @Override
  public ProjectStructureProblemDescription createUnusedElementWarning(@Nonnull Project project) {
    final List<ConfigurationErrorQuickFix> fixes = Arrays.asList(new AddLibraryToDependenciesFix(project), new RemoveLibraryFix(project));
    return new ProjectStructureProblemDescription("Library '" + StringUtil.escapeXml(myLibrary.getName()) + "'" + " is not used", null, createPlace(),
                                                  ProjectStructureProblemType.unused("unused-library"), ProjectStructureProblemDescription.ProblemLevel.PROJECT, fixes, false);
  }

  @Override
  public String getPresentableName() {
    return "Library '" + myLibrary.getName() + "'";
  }

  @Override
  public String getTypeName() {
    return "Library";
  }

  @Override
  public String getId() {
    return "library:" + myLibrary.getTable().getTableLevel() + ":" + myLibrary.getName();
  }

  private class RemoveInvalidRootsQuickFix extends ConfigurationErrorQuickFix {
    private final Project myProject;
    private final Library myLibrary;
    private final OrderRootType myType;
    private final List<String> myInvalidUrls;

    public RemoveInvalidRootsQuickFix(Project project, Library library, OrderRootType type, List<String> invalidUrls) {
      super("Remove invalid " + StringUtil.pluralize("root", invalidUrls.size()));
      myProject = project;
      myLibrary = library;
      myType = type;
      myInvalidUrls = invalidUrls;
    }

    @Override
    public void performFix(@Nonnull DataContext dataContext) {
      ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();

      LibrariesConfigurator librariesConfigurator = util.getLibrariesModel(myProject);

      final LibraryTable.ModifiableModel libraryTable = librariesConfigurator.getModifiableLibraryTable(myLibrary.getTable());
      if (libraryTable instanceof LibrariesModifiableModel) {
        for (String invalidRoot : myInvalidUrls) {
          final ExistingLibraryEditor libraryEditor = ((LibrariesModifiableModel)libraryTable).getLibraryEditor(myLibrary);
          libraryEditor.removeRoot(invalidRoot, myType);
        }
        // todo context.getDaemonAnalyzer().queueUpdate(LibraryProjectStructureElement.this);

        Settings settings = Objects.requireNonNull(dataContext.getData(Settings.KEY));
        ProjectLibrariesConfigurable librariesConfigurable = settings.findConfigurable(ProjectLibrariesConfigurable.class);

        navigate(myProject).doWhenDone(() -> {
          final MasterDetailsConfigurable configurable = librariesConfigurable.getSelectedConfigurable();
          if (configurable instanceof LibraryConfigurable) {
            ((LibraryConfigurable)configurable).updateComponent();
          }
        });
      }
    }
  }

  private class AddLibraryToDependenciesFix extends ConfigurationErrorQuickFix {
    private final Project myProject;

    private AddLibraryToDependenciesFix(Project project) {
      super("Add to Dependencies...");
      myProject = project;
    }

    @Override
    public void performFix(DataContext dataContext) {
      LibraryEditingUtil.showDialogAndAddLibraryToDependencies(myLibrary, myProject, false);
    }
  }

  private class RemoveLibraryFix extends ConfigurationErrorQuickFix {
    private RemoveLibraryFix(Project project) {
      super("Remove Library");
    }

    @Override
    public void performFix(@Nonnull DataContext dataContext) {
      Settings settings = dataContext.getData(Settings.KEY);
      if (settings == null) {
        return;
      }

      ProjectLibrariesConfigurable configurable = settings.findConfigurable(ProjectLibrariesConfigurable.class);
      if (configurable != null) {
        configurable.removeLibrary(LibraryProjectStructureElement.this);
      }
    }
  }
}

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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.configurable.MasterDetailsConfigurable;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.application.content.impl.internal.library.LibraryImpl;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablesRegistrar;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.ExistingLibraryEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibraryConfigurable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.ProjectLibrariesConfigurable;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.configurable.Settings;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import jakarta.annotation.Nonnull;

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

    LibraryEx library = (LibraryEx)librariesConfigurator.getLibraryModel(myLibrary);
    if (library == null || library.isDisposed()) return;

    reportInvalidRoots(project, problemsHolder, library, BinariesOrderRootType.getInstance(), "classes", ProjectStructureProblemType.error("library-invalid-classes-path"));
    String libraryName = library.getName();
    if (libraryName == null || !libraryName.startsWith("Maven: ")) {
      reportInvalidRoots(project, problemsHolder, library, SourcesOrderRootType.getInstance(), "sources", ProjectStructureProblemType.warning("library-invalid-source-javadoc-path"));
      reportInvalidRoots(project, problemsHolder, library, DocumentationOrderRootType.getInstance(), "javadoc", ProjectStructureProblemType.warning("library-invalid-source-javadoc-path"));
    }
  }

  private void reportInvalidRoots(
    Project project,
    ProjectStructureProblemsHolder problemsHolder,
    LibraryEx library,
    OrderRootType type,
    String rootName,
    ProjectStructureProblemType problemType
  ) {
    List<String> invalidUrls = library.getInvalidRootUrls(type);
    if (!invalidUrls.isEmpty()) {
      String description = createInvalidRootsDescription(invalidUrls, rootName, library.getName());
      PlaceInProjectStructure place = createPlace();
      LocalizeValue message = ProjectLocalize.projectRootsErrorMessageInvalidRoots(rootName, invalidUrls.size());
      ProjectStructureProblemDescription.ProblemLevel level =
        library.getTable().getTableLevel().equals(LibraryTablesRegistrar.PROJECT_LEVEL)
          ? ProjectStructureProblemDescription.ProblemLevel.PROJECT
          : ProjectStructureProblemDescription.ProblemLevel.GLOBAL;
      problemsHolder.registerProblem(new ProjectStructureProblemDescription(
        message.get(),
        description,
        place,
        problemType,
        level,
        List.of(new RemoveInvalidRootsQuickFix(project, library, type, invalidUrls)),
        true
      ));
    }
  }

  private static String createInvalidRootsDescription(List<String> invalidClasses, String rootName, String libraryName) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("Library '").append(StringUtil.escapeXml(libraryName)).append("' has broken ").append(rootName).append(" ").append(StringUtil.pluralize("path", invalidClasses.size())).append(":");
    for (String url : invalidClasses) {
      buffer.append("<br>&nbsp;&nbsp;");
      buffer.append(VirtualFilePathUtil.toPresentableUrl(url));
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
    InvocationHandler invocationHandler = Proxy.isProxyClass(myLibrary.getClass()) ? Proxy.getInvocationHandler(myLibrary) : null;
    Library realLibrary = invocationHandler instanceof ModuleEditor.ProxyDelegateAccessor ? (Library)((ModuleEditor.ProxyDelegateAccessor)invocationHandler).getDelegate() : myLibrary;
    Library source = realLibrary instanceof LibraryImpl ? ((LibraryImpl)realLibrary).getSource() : null;
    return source != null ? source : myLibrary;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(getSourceOrThis());
  }

  @Override
  public boolean shouldShowWarningIfUnused() {
    LibraryTable libraryTable = myLibrary.getTable();
    if (libraryTable == null) return false;
    return LibraryTablesRegistrar.PROJECT_LEVEL.equals(libraryTable.getTableLevel());
  }

  @Override
  public ProjectStructureProblemDescription createUnusedElementWarning(@Nonnull Project project) {
    List<ConfigurationErrorQuickFix> fixes = Arrays.asList(new AddLibraryToDependenciesFix(project), new RemoveLibraryFix(project));
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

      LibraryTable.ModifiableModel libraryTable = librariesConfigurator.getModifiableLibraryTable(myLibrary.getTable());
      if (libraryTable instanceof LibrariesModifiableModel) {
        for (String invalidRoot : myInvalidUrls) {
          ExistingLibraryEditor libraryEditor = ((LibrariesModifiableModel)libraryTable).getLibraryEditor(myLibrary);
          libraryEditor.removeRoot(invalidRoot, myType);
        }
        // todo context.getDaemonAnalyzer().queueUpdate(LibraryProjectStructureElement.this);

        Settings settings = dataContext.getRequiredData(Settings.KEY);
        ProjectLibrariesConfigurable librariesConfigurable = settings.findConfigurable(ProjectLibrariesConfigurable.class);

        navigate(myProject).doWhenDone(() -> {
          MasterDetailsConfigurable configurable = librariesConfigurable.getSelectedConfigurable();
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

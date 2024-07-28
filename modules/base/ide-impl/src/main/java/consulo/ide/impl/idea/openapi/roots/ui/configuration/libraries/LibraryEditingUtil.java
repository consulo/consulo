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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries;

import consulo.application.AllIcons;
import consulo.content.OrderRootType;
import consulo.application.content.impl.internal.library.LibraryImpl;
import consulo.content.internal.LibraryEx;
import consulo.content.library.*;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureValidator;
import consulo.ide.impl.idea.openapi.vfs.newvfs.ArchiveFileSystem;
import consulo.ide.impl.idea.util.ParameterizedRunnable;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.library.ModuleAwareLibraryType;
import consulo.module.content.library.ModuleLibraryTablePresentation;
import consulo.module.impl.internal.layer.library.LibraryTableImplUtil;
import consulo.project.Project;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.VirtualFileManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author nik
 */
public class LibraryEditingUtil {
  private static final Logger LOG = Logger.getInstance(LibraryEditingUtil.class);

  private LibraryEditingUtil() {
  }

  public static boolean libraryAlreadyExists(LibraryTable.ModifiableModel table, String libraryName) {
    for (Iterator<Library> it = table.getLibraryIterator(); it.hasNext(); ) {
      final Library library = it.next();
      final String libName;
      if (table instanceof LibrariesModifiableModel) {
        libName = ((LibrariesModifiableModel)table).getLibraryEditor(library).getName();
      }
      else {
        libName = library.getName();
      }
      if (libraryName.equals(libName)) {
        return true;
      }
    }
    return false;
  }

  public static String suggestNewLibraryName(LibraryTable.ModifiableModel table, final String baseName) {
    String candidateName = baseName;
    int idx = 1;
    while (libraryAlreadyExists(table, candidateName)) {
      candidateName = baseName + (idx++);
    }
    return candidateName;
  }

  public static Condition<Library> getNotAddedLibrariesCondition(final ModuleRootModel rootModel) {
    final OrderEntry[] orderEntries = rootModel.getOrderEntries();
    final Set<Library> result = new HashSet<Library>(orderEntries.length);
    for (OrderEntry orderEntry : orderEntries) {
      if (orderEntry instanceof LibraryOrderEntry && orderEntry.isValid()) {
        final LibraryImpl library = (LibraryImpl)((LibraryOrderEntry)orderEntry).getLibrary();
        if (library != null) {
          final Library source = library.getSource();
          result.add(source != null ? source : library);
        }
      }
    }
    return new Condition<Library>() {
      @Override
      public boolean value(Library library) {
        if (result.contains(library)) return false;
        if (library instanceof LibraryImpl) {
          final Library source = ((LibraryImpl)library).getSource();
          if (source != null && result.contains(source)) return false;
        }
        return true;
      }
    };
  }

  public static void copyLibrary(LibraryEx from, Map<String, String> rootMapping, LibraryEx.ModifiableModelEx target) {
    target.setProperties(from.getProperties());
    for (OrderRootType type : OrderRootType.getAllTypes()) {
      final String[] urls = from.getUrls(type);
      for (String url : urls) {
        final String protocol = VirtualFileManager.extractProtocol(url);
        if (protocol == null) continue;
        final String fullPath = VirtualFileManager.extractPath(url);
        final int sep = fullPath.indexOf(ArchiveFileSystem.ARCHIVE_SEPARATOR);
        String localPath;
        String pathInJar;
        if (sep != -1) {
          localPath = fullPath.substring(0, sep);
          pathInJar = fullPath.substring(sep);
        }
        else {
          localPath = fullPath;
          pathInJar = "";
        }
        final String targetPath = rootMapping.get(localPath);
        String targetUrl = targetPath != null ? VirtualFileManager.constructUrl(protocol, targetPath + pathInJar) : url;

        if (from.isJarDirectory(url, type)) {
          target.addJarDirectory(targetUrl, false, type);
        }
        else {
          target.addRoot(targetUrl, type);
        }
      }
    }
  }

  public static LibraryTablePresentation getLibraryTablePresentation(@Nonnull Project project, @Nonnull String level) {
    if (level.equals(LibraryTableImplUtil.MODULE_LEVEL)) {
      return ModuleLibraryTablePresentation.INSTANCE;
    }
    final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(level, project);
    LOG.assertTrue(table != null, level);
    return table.getPresentation();
  }

  public static List<LibraryType> getSuitableTypes(ClasspathPanel classpathPanel) {
    List<LibraryType> suitableTypes = new ArrayList<LibraryType>();
    suitableTypes.add(null);
    for (LibraryType libraryType : LibraryType.EP_NAME.getExtensionList()) {
      if (libraryType.getCreateActionName() != null && isAvailable(libraryType, classpathPanel.getRootModel())) {
        suitableTypes.add(libraryType);
      }
    }
    return suitableTypes;
  }

  public static boolean hasSuitableTypes(ClasspathPanel panel) {
    return getSuitableTypes(panel).size() > 1;
  }

  public static BaseListPopupStep<LibraryType> createChooseTypeStep(final ClasspathPanel classpathPanel, final ParameterizedRunnable<LibraryType> action) {
    return new BaseListPopupStep<LibraryType>(IdeBundle.message("popup.title.select.library.type"), getSuitableTypes(classpathPanel)) {
      @Nonnull
      @Override
      public String getTextFor(LibraryType value) {
        String createActionName = value != null ? value.getCreateActionName() : null;
        return createActionName != null ? createActionName : IdeBundle.message("create.default.library.type.action.name");
      }

      @Override
      public Image getIconFor(LibraryType aValue) {
        return aValue != null ? aValue.getIcon() : AllIcons.Nodes.PpLib;
      }

      @Override
      public PopupStep onChosen(final LibraryType selectedValue, boolean finalChoice) {
        return doFinalStep(() -> action.run(selectedValue));
      }
    };
  }

  public static List<Module> getSuitableModules(@Nonnull Project project, final @Nullable LibraryKind kind, @Nullable Library library) {
    final List<Module> modules = new ArrayList<>();
    LibraryType type = kind == null ? null : LibraryType.findByKind(kind);

    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();
    ModulesConfigurator modulesModel = util.getModulesModel(project);

    for (Module module : modulesModel.getModules()) {
      final ModuleRootModel rootModel = modulesModel.getRootModel(module);

      if (type != null && !isAvailable(type, rootModel)) {
        continue;
      }
      if (library != null) {

        if (!getNotAddedLibrariesCondition(rootModel).value(library)) {
          continue;
        }
      }

      modules.add(module);
    }
    return modules;
  }

  public static void showDialogAndAddLibraryToDependencies(@Nonnull Library library, @Nonnull Project project, boolean allowEmptySelection) {
    ProjectStructureValidator.showDialogAndAddLibraryToDependencies(library, project, allowEmptySelection);
  }

  public static boolean isAvailable(LibraryType<?> type, ModuleRootModel moduleRootModel) {
    if (type instanceof ModuleAwareLibraryType moduleAwareLibraryType) {
      return moduleAwareLibraryType.isAvailable(moduleRootModel);
    }

    return type.isAvailable();
  }
}

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

import consulo.application.Application;
import consulo.application.content.impl.internal.library.LibraryImpl;
import consulo.content.OrderRootType;
import consulo.content.internal.LibraryEx;
import consulo.content.internal.LibraryKindRegistry;
import consulo.content.library.*;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureValidator;
import consulo.ide.impl.idea.util.ParameterizedRunnable;
import consulo.ide.localize.IdeLocalize;
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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author nik
 */
public class LibraryEditingUtil {
    private static final Logger LOG = Logger.getInstance(LibraryEditingUtil.class);

    private LibraryEditingUtil() {
    }

    public static boolean libraryAlreadyExists(LibraryTable.ModifiableModel table, String libraryName) {
        for (Iterator<Library> it = table.getLibraryIterator(); it.hasNext(); ) {
            Library library = it.next();
            String libName;
            if (table instanceof LibrariesModifiableModel librariesModifiableModel) {
                libName = librariesModifiableModel.getLibraryEditor(library).getName();
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

    public static String suggestNewLibraryName(LibraryTable.ModifiableModel table, String baseName) {
        String candidateName = baseName;
        int idx = 1;
        while (libraryAlreadyExists(table, candidateName)) {
            candidateName = baseName + (idx++);
        }
        return candidateName;
    }

    public static Predicate<Library> getNotAddedLibrariesCondition(ModuleRootModel rootModel) {
        OrderEntry[] orderEntries = rootModel.getOrderEntries();
        Set<Library> result = new HashSet<>(orderEntries.length);
        for (OrderEntry orderEntry : orderEntries) {
            if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry && orderEntry.isValid()) {
                LibraryImpl library = (LibraryImpl)libraryOrderEntry.getLibrary();
                if (library != null) {
                    Library source = library.getSource();
                    result.add(source != null ? source : library);
                }
            }
        }
        return library -> {
            if (result.contains(library)) {
                return false;
            }
            if (library instanceof LibraryImpl libraryImpl) {
                Library source = libraryImpl.getSource();
                if (source != null && result.contains(source)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static void copyLibrary(LibraryEx from, Map<String, String> rootMapping, LibraryEx.ModifiableModelEx target) {
        target.setProperties(from.getProperties());
        for (OrderRootType type : OrderRootType.getAllTypes()) {
            String[] urls = from.getUrls(type);
            for (String url : urls) {
                String protocol = VirtualFileManager.extractProtocol(url);
                if (protocol == null) {
                    continue;
                }
                String fullPath = VirtualFileManager.extractPath(url);
                int sep = fullPath.indexOf(URLUtil.ARCHIVE_SEPARATOR);
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
                String targetPath = rootMapping.get(localPath);
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
        if (level.equals(LibraryEx.MODULE_LEVEL)) {
            return ModuleLibraryTablePresentation.INSTANCE;
        }
        LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(level, project);
        LOG.assertTrue(table != null, level);
        return table.getPresentation();
    }

    public static List<LibraryType> getSuitableTypes(ClasspathPanel classpathPanel) {
        List<LibraryType> suitableTypes = new ArrayList<>();
        suitableTypes.add(null);
        Application.get().getExtensionPoint(LibraryType.class).collectExtensionsSafe(
            suitableTypes,
            libraryType -> libraryType.getCreateActionName() != null && isAvailable(libraryType, classpathPanel.getRootModel())
                ? libraryType : null
        );
        return suitableTypes;
    }

    public static boolean hasSuitableTypes(ClasspathPanel panel) {
        return getSuitableTypes(panel).size() > 1;
    }

    public static BaseListPopupStep<LibraryType> createChooseTypeStep(
        ClasspathPanel classpathPanel,
        ParameterizedRunnable<LibraryType> action
    ) {
        return new BaseListPopupStep<LibraryType>(IdeLocalize.popupTitleSelectLibraryType().get(), getSuitableTypes(classpathPanel)) {
            @Nonnull
            @Override
            public String getTextFor(LibraryType value) {
                String createActionName = value != null ? value.getCreateActionName() : null;
                return createActionName != null ? createActionName : IdeLocalize.createDefaultLibraryTypeActionName().get();
            }

            @Override
            public Image getIconFor(LibraryType aValue) {
                return aValue != null ? aValue.getIcon() : PlatformIconGroup.nodesPplib();
            }

            @Override
            public PopupStep onChosen(LibraryType selectedValue, boolean finalChoice) {
                return doFinalStep(() -> action.run(selectedValue));
            }
        };
    }

    public static List<Module> getSuitableModules(@Nonnull Project project, @Nullable LibraryKind kind, @Nullable Library library) {
        List<Module> modules = new ArrayList<>();
        LibraryType type = kind == null ? null : LibraryKindRegistry.getInstance().findLibraryTypeByKindId(kind.getKindId());

        ProjectStructureSettingsUtil util = ShowSettingsUtil.getInstance();
        ModulesConfigurator modulesModel = util.getModulesModel(project);

        for (Module module : modulesModel.getModules()) {
            ModuleRootModel rootModel = modulesModel.getRootModel(module);

            if (type != null && !isAvailable(type, rootModel)
                || library != null && !getNotAddedLibrariesCondition(rootModel).test(library)) {
                continue;
            }

            modules.add(module);
        }
        return modules;
    }

    @RequiredUIAccess
    public static void showDialogAndAddLibraryToDependencies(
        @Nonnull Library library,
        @Nonnull Project project,
        boolean allowEmptySelection
    ) {
        ProjectStructureValidator.showDialogAndAddLibraryToDependencies(library, project, allowEmptySelection);
    }

    public static boolean isAvailable(LibraryType<?> type, ModuleRootModel moduleRootModel) {
        return type instanceof ModuleAwareLibraryType moduleAwareLibraryType
            ? moduleAwareLibraryType.isAvailable(moduleRootModel)
            : type.isAvailable();
    }
}

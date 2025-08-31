/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.impl.internal.service.project.manage;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.externalSystem.impl.internal.service.project.ProjectStructureHelper;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.project.*;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.externalSystem.util.Order;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleLibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.*;

import static consulo.externalSystem.model.ProjectKeys.MODULE;

/**
 * @author Denis Zhdanov
 * @since 4/12/13 6:19 PM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
@ExtensionImpl
public class LibraryDependencyDataService extends AbstractDependencyDataService<LibraryDependencyData, LibraryOrderEntry> {
    private static final Logger LOG = Logger.getInstance(LibraryDependencyDataService.class);

    @Nonnull
    @Override
    public Key<LibraryDependencyData> getTargetDataKey() {
        return ProjectKeys.LIBRARY_DEPENDENCY;
    }

    @Override
    public void importData(@Nonnull Collection<DataNode<LibraryDependencyData>> toImport, @Nonnull Project project, boolean synchronous) {
        if (toImport.isEmpty()) {
            return;
        }

        Map<DataNode<ModuleData>, List<DataNode<LibraryDependencyData>>> byModule = ExternalSystemApiUtil.groupBy(toImport, MODULE);
        for (Map.Entry<DataNode<ModuleData>, List<DataNode<LibraryDependencyData>>> entry : byModule.entrySet()) {
            Module module = ProjectStructureHelper.findIdeModule(entry.getKey().getData(), project);
            if (module == null) {
                ModuleDataService.getInstance().importData(Collections.singleton(entry.getKey()), project, true);
                module = ProjectStructureHelper.findIdeModule(entry.getKey().getData(), project);
                if (module == null) {
                    LOG.warn(String.format(
                        "Can't import library dependencies %s. Reason: target module (%s) is not found at the ide and can't be imported",
                        entry.getValue(),
                        entry.getKey()));
                    continue;
                }
            }
            importData(entry.getValue(), module, synchronous);
        }
    }

    public void importData(
        @Nonnull final Collection<DataNode<LibraryDependencyData>> nodesToImport,
        @Nonnull final Module module,
        final boolean synchronous
    ) {
        ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(module) {
            @RequiredUIAccess
            @Override
            public void execute() {
                importMissingProjectLibraries(module, nodesToImport, synchronous);

                // The general idea is to import all external project library dependencies and module libraries which don't present at the
                // ide side yet and remove all project library dependencies and module libraries which present at the ide but not at
                // the given collection.
                // The trick is that we should perform module settings modification inside try/finally block against target root model.
                // That means that we need to prepare all necessary data, obtain a model and modify it as necessary.
                Map<Set<String>/* library paths */, LibraryDependencyData> moduleLibrariesToImport = new HashMap<>();
                Map<String/* library name + scope */, LibraryDependencyData> projectLibrariesToImport = new HashMap<>();
                Set<LibraryDependencyData> toImport = new LinkedHashSet<>();

                boolean hasUnresolved = false;
                for (DataNode<LibraryDependencyData> dependencyNode : nodesToImport) {
                    LibraryDependencyData dependencyData = dependencyNode.getData();
                    LibraryData libraryData = dependencyData.getTarget();
                    hasUnresolved |= libraryData.isUnresolved();
                    switch (dependencyData.getLevel()) {
                        case MODULE:
                            if (!libraryData.isUnresolved()) {
                                Set<String> paths = new HashSet<>();
                                for (String path : libraryData.getPaths(LibraryPathType.BINARY)) {
                                    paths.add(ExternalSystemApiUtil.toCanonicalPath(path) + dependencyData.getScope().name());
                                }
                                moduleLibrariesToImport.put(paths, dependencyData);
                                toImport.add(dependencyData);
                            }
                            break;
                        case PROJECT:
                            projectLibrariesToImport.put(libraryData.getInternalName() + dependencyData.getScope().name(), dependencyData);
                            toImport.add(dependencyData);
                    }
                }

                ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                ModifiableRootModel moduleRootModel = moduleRootManager.getModifiableModel();
                LibraryTable moduleLibraryTable = moduleRootModel.getModuleLibraryTable();
                LibraryTable libraryTable = ProjectLibraryTable.getInstance(module.getProject());
                try {
                    filterUpToDateAndRemoveObsolete(moduleLibrariesToImport, projectLibrariesToImport, toImport, moduleRootModel, hasUnresolved);

                    // Import missing library dependencies.
                    if (!toImport.isEmpty()) {
                        importMissing(toImport, moduleRootModel, moduleLibraryTable, libraryTable, module);
                    }
                }
                finally {
                    moduleRootModel.commit();
                }
            }
        });
    }

    private void importMissing(
        @Nonnull Set<LibraryDependencyData> toImport,
        @Nonnull ModifiableRootModel moduleRootModel,
        @Nonnull LibraryTable moduleLibraryTable,
        @Nonnull LibraryTable libraryTable,
        @Nonnull Module module
    ) {
        for (LibraryDependencyData dependencyData : toImport) {
            LibraryData libraryData = dependencyData.getTarget();
            String libraryName = libraryData.getInternalName();
            switch (dependencyData.getLevel()) {
                case MODULE:
                    Library moduleLib = moduleLibraryTable.createLibrary(libraryName);
                    Library.ModifiableModel libModel = moduleLib.getModifiableModel();
                    try {
                        LibraryDataService libraryDataService = LibraryDataService.getInstance();

                        Map<OrderRootType, Collection<File>> files = libraryDataService.prepareLibraryFiles(libraryData);
                        libraryDataService.registerPaths(files, libModel, libraryName);
                        LibraryOrderEntry orderEntry = moduleRootModel.findLibraryOrderEntry(moduleLib);
                        assert orderEntry != null;
                        setLibraryScope(orderEntry, moduleLib, module, dependencyData);
                    }
                    finally {
                        libModel.commit();
                    }
                    break;
                case PROJECT:
                    Library projectLib = libraryTable.getLibraryByName(libraryName);
                    if (projectLib == null) {
                        assert false;
                        continue;
                    }
                    LibraryOrderEntry orderEntry = moduleRootModel.addLibraryEntry(projectLib);
                    setLibraryScope(orderEntry, projectLib, module, dependencyData);
            }
        }
    }

    private static void setLibraryScope(
        @Nonnull LibraryOrderEntry orderEntry,
        @Nonnull Library lib,
        @Nonnull Module module,
        @Nonnull LibraryDependencyData dependencyData
    ) {
        LOG.info(String.format("Adding library dependency '%s' to module '%s'", lib.getName(), module.getName()));
        orderEntry.setExported(dependencyData.isExported());
        orderEntry.setScope(dependencyData.getScope());
        LOG.info(String.format(
            "Configuring library dependency '%s' of module '%s' to be%s exported and have scope %s",
            lib.getName(),
            module.getName(),
            dependencyData.isExported() ? " not" : "",
            dependencyData.getScope()
        ));
    }

    private static void filterUpToDateAndRemoveObsolete(
        @Nonnull Map<Set<String>, LibraryDependencyData> moduleLibrariesToImport,
        @Nonnull Map<String, LibraryDependencyData> projectLibrariesToImport,
        @Nonnull Set<LibraryDependencyData> toImport,
        @Nonnull ModifiableRootModel moduleRootModel,
        boolean hasUnresolvedLibraries
    ) {
        Set<String> moduleLibraryKey = new HashSet<>();
        for (OrderEntry entry : moduleRootModel.getOrderEntries()) {
            if (entry instanceof ModuleLibraryOrderEntry moduleLibraryOrderEntry) {
                Library library = moduleLibraryOrderEntry.getLibrary();
                if (library == null) {
                    LOG.warn("Skipping module-level library entry because it doesn't have backing Library object. Entry: " + entry);
                    continue;
                }
                moduleLibraryKey.clear();
                for (VirtualFile file : library.getFiles(BinariesOrderRootType.getInstance())) {
                    moduleLibraryKey.add(ExternalSystemApiUtil.getLocalFileSystemPath(file) + moduleLibraryOrderEntry.getScope().name());
                }
                LibraryDependencyData existing = moduleLibrariesToImport.remove(moduleLibraryKey);
                if (existing == null) {
                    moduleRootModel.removeOrderEntry(entry);
                }
                else {

                    toImport.remove(existing);
                }
            }
            else if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
                String libraryName = libraryOrderEntry.getLibraryName();
                LibraryDependencyData existing = projectLibrariesToImport.remove(libraryName + libraryOrderEntry.getScope().name());
                if (existing != null) {
                    toImport.remove(existing);
                }
                else if (!hasUnresolvedLibraries) {
                    // There is a possible case that a project has been successfully imported from external model and after
                    // that network/repo goes down. We don't want to drop existing binary mappings then.
                    moduleRootModel.removeOrderEntry(entry);
                }
            }
        }
    }

    private void importMissingProjectLibraries(
        @Nonnull Module module,
        @Nonnull Collection<DataNode<LibraryDependencyData>> nodesToImport,
        boolean synchronous
    ) {
        LibraryTable libraryTable = ProjectLibraryTable.getInstance(module.getProject());
        List<DataNode<LibraryData>> librariesToImport = new ArrayList<>();
        for (DataNode<LibraryDependencyData> dataNode : nodesToImport) {
            LibraryDependencyData dependencyData = dataNode.getData();
            if (dependencyData.getLevel() != LibraryLevel.PROJECT) {
                continue;
            }
            Library library = libraryTable.getLibraryByName(dependencyData.getInternalName());
            if (library == null) {
                DataNode<ProjectData> projectNode = dataNode.getDataNode(ProjectKeys.PROJECT);
                if (projectNode != null) {
                    DataNode<LibraryData> libraryNode =
                        ExternalSystemApiUtil.find(projectNode, ProjectKeys.LIBRARY, node -> node.getData().equals(dependencyData.getTarget()));
                    if (libraryNode != null) {
                        librariesToImport.add(libraryNode);
                    }
                }
            }
        }
        if (!librariesToImport.isEmpty()) {
            LibraryDataService.getInstance().importData(librariesToImport, module.getProject(), synchronous);
        }
    }
}

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
import consulo.externalSystem.model.project.LibraryData;
import consulo.externalSystem.model.project.LibraryPathType;
import consulo.externalSystem.service.project.ExternalLibraryPathTypeMapper;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.externalSystem.util.Order;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.File;
import java.util.*;

/**
 * @author Denis Zhdanov
 * @since 2/15/12 11:32 AM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
@ExtensionImpl
public class LibraryDataService implements ProjectDataService<LibraryData, Library> {
    public static LibraryDataService getInstance() {
        return EP_NAME.findExtension(LibraryDataService.class);
    }

    private static final Logger LOG = Logger.getInstance(LibraryDataService.class);

    @Nonnull
    private final ExternalLibraryPathTypeMapper myLibraryPathTypeMapper;

    @Inject
    public LibraryDataService(@Nonnull ExternalLibraryPathTypeMapper mapper) {
        myLibraryPathTypeMapper = mapper;
    }

    @Nonnull
    @Override
    public Key<LibraryData> getTargetDataKey() {
        return ProjectKeys.LIBRARY;
    }

    @Override
    public void importData(@Nonnull Collection<DataNode<LibraryData>> toImport, @Nonnull Project project, boolean synchronous) {
        for (DataNode<LibraryData> dataNode : toImport) {
            importLibrary(dataNode.getData(), project, synchronous);
        }
    }

    @RequiredUIAccess
    public void importLibrary(@Nonnull LibraryData toImport, @Nonnull Project project, boolean synchronous) {
        Map<OrderRootType, Collection<File>> libraryFiles = prepareLibraryFiles(toImport);

        Library library = ProjectStructureHelper.findIdeLibrary(toImport, project);
        if (library != null) {
            syncPaths(toImport, library, project, synchronous);
            return;
        }
        importLibrary(toImport.getInternalName(), libraryFiles, project, synchronous);
    }

    @Nonnull
    public Map<OrderRootType, Collection<File>> prepareLibraryFiles(@Nonnull LibraryData data) {
        Map<OrderRootType, Collection<File>> result = new HashMap<>();
        for (LibraryPathType pathType : LibraryPathType.values()) {
            Set<String> paths = data.getPaths(pathType);
            if (paths.isEmpty()) {
                continue;
            }
            result.put(myLibraryPathTypeMapper.map(pathType), ContainerUtil.map(paths, File::new));
        }
        return result;
    }

    @RequiredUIAccess
    public void importLibrary(
        @Nonnull String libraryName,
        @Nonnull Map<OrderRootType, Collection<File>> libraryFiles,
        @Nonnull Project project,
        boolean synchronous
    ) {
        ExternalSystemApiUtil.executeProjectChangeAction(
            synchronous,
            new DisposeAwareProjectChange(project) {
                @Override
                @RequiredUIAccess
                public void execute() {
                    // Is assumed to be called from the EDT.
                    LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
                    LibraryTable.ModifiableModel projectLibraryModel = libraryTable.getModifiableModel();
                    Library intellijLibrary;
                    try {
                        intellijLibrary = projectLibraryModel.createLibrary(libraryName);
                    }
                    finally {
                        projectLibraryModel.commit();
                    }
                    Library.ModifiableModel libraryModel = intellijLibrary.getModifiableModel();
                    try {
                        registerPaths(libraryFiles, libraryModel, libraryName);
                    }
                    finally {
                        libraryModel.commit();
                    }
                }
            }
        );
    }

    @SuppressWarnings("MethodMayBeStatic")
    public void registerPaths(
        @Nonnull Map<OrderRootType, Collection<File>> libraryFiles,
        @Nonnull Library.ModifiableModel model,
        @Nonnull String libraryName
    ) {
        for (Map.Entry<OrderRootType, Collection<File>> entry : libraryFiles.entrySet()) {
            for (File file : entry.getValue()) {
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                if (virtualFile == null) {
                    if (ExternalSystemConstants.VERBOSE_PROCESSING && entry.getKey() == BinariesOrderRootType.getInstance()) {
                        LOG.warn(String.format(
                            "Can't find %s of the library '%s' at path '%s'",
                            entry.getKey(),
                            libraryName,
                            file.getAbsolutePath()
                        ));
                    }
                    String url = VirtualFileUtil.getUrlForLibraryRoot(file);
                    model.addRoot(url, entry.getKey());
                    continue;
                }
                if (virtualFile.isDirectory()) {
                    model.addRoot(virtualFile, entry.getKey());
                }
                else {
                    VirtualFile archiveRoot = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);
                    if (archiveRoot == null) {
                        LOG.warn(String.format(
                            "Can't parse contents of the jar file at path '%s' for the library '%s''",
                            file.getAbsolutePath(),
                            libraryName
                        ));
                        continue;
                    }
                    model.addRoot(archiveRoot, entry.getKey());
                }
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void removeData(@Nonnull Collection<? extends Library> libraries, @Nonnull Project project, boolean synchronous) {
        if (libraries.isEmpty()) {
            return;
        }
        ExternalSystemApiUtil.executeProjectChangeAction(
            synchronous,
            new DisposeAwareProjectChange(project) {
                @Override
                @RequiredUIAccess
                public void execute() {
                    LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
                    LibraryTable.ModifiableModel model = libraryTable.getModifiableModel();
                    try {
                        for (Library library : libraries) {
                            String libraryName = library.getName();
                            if (libraryName != null) {
                                Library libraryToRemove = model.getLibraryByName(libraryName);
                                if (libraryToRemove != null) {
                                    model.removeLibrary(libraryToRemove);
                                }
                            }
                        }
                    }
                    finally {
                        model.commit();
                    }
                }
            }
        );
    }

    @RequiredUIAccess
    public void syncPaths(
        @Nonnull LibraryData externalLibrary,
        @Nonnull Library ideLibrary,
        @Nonnull Project project,
        boolean synchronous
    ) {
        if (externalLibrary.isUnresolved()) {
            return;
        }
        Map<OrderRootType, Set<String>> toRemove = new HashMap<>();
        Map<OrderRootType, Set<String>> toAdd = new HashMap<>();
        for (LibraryPathType pathType : LibraryPathType.values()) {
            OrderRootType ideType = myLibraryPathTypeMapper.map(pathType);
            HashSet<String> toAddPerType = new HashSet<>(externalLibrary.getPaths(pathType));
            toAdd.put(ideType, toAddPerType);

            HashSet<String> toRemovePerType = new HashSet<>();
            toRemove.put(ideType, toRemovePerType);

            for (VirtualFile ideFile : ideLibrary.getFiles(ideType)) {
                String idePath = ExternalSystemApiUtil.getLocalFileSystemPath(ideFile);
                if (!toAddPerType.remove(idePath)) {
                    toRemovePerType.add(ideFile.getUrl());
                }
            }
        }
        if (toRemove.isEmpty() && toAdd.isEmpty()) {
            return;
        }
        ExternalSystemApiUtil.executeProjectChangeAction(
            synchronous,
            new DisposeAwareProjectChange(project) {
                @RequiredUIAccess
                @Override
                public void execute() {
                    Library.ModifiableModel model = ideLibrary.getModifiableModel();
                    try {
                        for (Map.Entry<OrderRootType, Set<String>> entry : toRemove.entrySet()) {
                            for (String path : entry.getValue()) {
                                model.removeRoot(path, entry.getKey());
                            }
                        }

                        for (Map.Entry<OrderRootType, Set<String>> entry : toAdd.entrySet()) {
                            Map<OrderRootType, Collection<File>> roots = new HashMap<>();
                            roots.put(entry.getKey(), ContainerUtil.map(entry.getValue(), File::new));
                            registerPaths(roots, model, externalLibrary.getInternalName());
                        }
                    }
                    finally {
                        model.commit();
                    }
                }
            }
        );
    }
}

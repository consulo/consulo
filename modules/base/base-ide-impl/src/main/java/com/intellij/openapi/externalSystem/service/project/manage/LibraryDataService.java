package com.intellij.openapi.externalSystem.service.project.manage;

import consulo.logging.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.service.project.ExternalLibraryPathTypeMapper;
import com.intellij.openapi.externalSystem.service.project.ProjectStructureHelper;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.roots.types.BinariesOrderRootType;
import consulo.vfs.util.ArchiveVfsUtil;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 2/15/12 11:32 AM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
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

  public void importLibrary(@Nonnull final LibraryData toImport, @Nonnull final Project project, boolean synchronous) {
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
    Map<OrderRootType, Collection<File>> result = ContainerUtilRt.newHashMap();
    for (LibraryPathType pathType : LibraryPathType.values()) {
      final Set<String> paths = data.getPaths(pathType);
      if (paths.isEmpty()) {
        continue;
      }
      result.put(myLibraryPathTypeMapper.map(pathType), ContainerUtil.map(paths, File::new));
    }
    return result;
  }

  public void importLibrary(@Nonnull final String libraryName, @Nonnull final Map<OrderRootType, Collection<File>> libraryFiles, @Nonnull final Project project, boolean synchronous) {
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      @RequiredUIAccess
      @Override
      public void execute() {
        // Is assumed to be called from the EDT.
        final LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
        final LibraryTable.ModifiableModel projectLibraryModel = libraryTable.getModifiableModel();
        final Library intellijLibrary;
        try {
          intellijLibrary = projectLibraryModel.createLibrary(libraryName);
        }
        finally {
          projectLibraryModel.commit();
        }
        final Library.ModifiableModel libraryModel = intellijLibrary.getModifiableModel();
        try {
          registerPaths(libraryFiles, libraryModel, libraryName);
        }
        finally {
          libraryModel.commit();
        }
      }
    });
  }

  @SuppressWarnings("MethodMayBeStatic")
  public void registerPaths(@Nonnull final Map<OrderRootType, Collection<File>> libraryFiles, @Nonnull Library.ModifiableModel model, @Nonnull String libraryName) {
    for (Map.Entry<OrderRootType, Collection<File>> entry : libraryFiles.entrySet()) {
      for (File file : entry.getValue()) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if (virtualFile == null) {
          if (ExternalSystemConstants.VERBOSE_PROCESSING && entry.getKey() == BinariesOrderRootType.getInstance()) {
            LOG.warn(String.format("Can't find %s of the library '%s' at path '%s'", entry.getKey(), libraryName, file.getAbsolutePath()));
          }
          String url = VfsUtil.getUrlForLibraryRoot(file);
          model.addRoot(url, entry.getKey());
          continue;
        }
        if (virtualFile.isDirectory()) {
          model.addRoot(virtualFile, entry.getKey());
        }
        else {
          VirtualFile archiveRoot = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);
          if (archiveRoot == null) {
            LOG.warn(String.format("Can't parse contents of the jar file at path '%s' for the library '%s''", file.getAbsolutePath(), libraryName));
            continue;
          }
          model.addRoot(archiveRoot, entry.getKey());
        }
      }
    }
  }

  @Override
  public void removeData(@Nonnull final Collection<? extends Library> libraries, @Nonnull final Project project, boolean synchronous) {
    if (libraries.isEmpty()) {
      return;
    }
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      @RequiredUIAccess
      @Override
      public void execute() {
        final LibraryTable libraryTable = ProjectLibraryTable.getInstance(project);
        final LibraryTable.ModifiableModel model = libraryTable.getModifiableModel();
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
    });
  }

  public void syncPaths(@Nonnull final LibraryData externalLibrary, @Nonnull final Library ideLibrary, @Nonnull final Project project, boolean synchronous) {
    if (externalLibrary.isUnresolved()) {
      return;
    }
    final Map<OrderRootType, Set<String>> toRemove = ContainerUtilRt.newHashMap();
    final Map<OrderRootType, Set<String>> toAdd = ContainerUtilRt.newHashMap();
    for (LibraryPathType pathType : LibraryPathType.values()) {
      OrderRootType ideType = myLibraryPathTypeMapper.map(pathType);
      HashSet<String> toAddPerType = ContainerUtilRt.newHashSet(externalLibrary.getPaths(pathType));
      toAdd.put(ideType, toAddPerType);

      HashSet<String> toRemovePerType = ContainerUtilRt.newHashSet();
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
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
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
            Map<OrderRootType, Collection<File>> roots = ContainerUtilRt.newHashMap();
            roots.put(entry.getKey(), ContainerUtil.map(entry.getValue(), File::new));
            registerPaths(roots, model, externalLibrary.getInternalName());
          }
        }
        finally {
          model.commit();
        }
      }
    });
  }
}

package com.intellij.openapi.externalSystem.service.project.manage;

import consulo.logging.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.project.ProjectStructureHelper;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtilRt;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.*;
import consulo.roots.impl.property.GeneratedContentFolderPropertyProvider;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2/7/12 3:20 PM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
public class ContentRootDataService implements ProjectDataService<ContentRootData, ContentEntry> {

  private static final Logger LOG = Logger.getInstance(ContentRootDataService.class);

  @Nonnull
  @Override
  public Key<ContentRootData> getTargetDataKey() {
    return ProjectKeys.CONTENT_ROOT;
  }

  @Override
  public void importData(@Nonnull final Collection<DataNode<ContentRootData>> toImport, @Nonnull final Project project, boolean synchronous) {
    if (toImport.isEmpty()) {
      return;
    }

    Map<DataNode<ModuleData>, List<DataNode<ContentRootData>>> byModule = ExternalSystemApiUtil.groupBy(toImport, ProjectKeys.MODULE);
    for (Map.Entry<DataNode<ModuleData>, List<DataNode<ContentRootData>>> entry : byModule.entrySet()) {
      final Module module = ProjectStructureHelper.findIdeModule(entry.getKey().getData(), project);
      if (module == null) {
        LOG.warn(String.format("Can't import content roots. Reason: target module (%s) is not found at the ide. Content roots: %s", entry.getKey(),
                               entry.getValue()));
        continue;
      }
      importData(entry.getValue(), module, synchronous);
    }
  }

  private static void importData(@Nonnull final Collection<DataNode<ContentRootData>> datas, @Nonnull final Module module, boolean synchronous) {
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(module) {
      @RequiredUIAccess
      @Override
      public void execute() {
        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        final ModifiableRootModel model = moduleRootManager.getModifiableModel();
        final ContentEntry[] contentEntries = model.getContentEntries();
        final Map<String, ContentEntry> contentEntriesMap = ContainerUtilRt.newHashMap();
        for (ContentEntry contentEntry : contentEntries) {
          contentEntriesMap.put(contentEntry.getUrl(), contentEntry);
        }

        boolean createEmptyContentRootDirectories = false;
        if (!datas.isEmpty()) {
          ProjectSystemId projectSystemId = datas.iterator().next().getData().getOwner();
          AbstractExternalSystemSettings externalSystemSettings = ExternalSystemApiUtil.getSettings(module.getProject(), projectSystemId);

          String path = ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.ROOT_PROJECT_PATH_KEY);
          if (path != null) {
            ExternalProjectSettings projectSettings = externalSystemSettings.getLinkedProjectSettings(path);
            createEmptyContentRootDirectories = projectSettings != null && projectSettings.isCreateEmptyContentRootDirectories();
          }
        }

        try {
          for (final DataNode<ContentRootData> data : datas) {
            final ContentRootData contentRoot = data.getData();

            final ContentEntry contentEntry = findOrCreateContentRoot(model, contentRoot.getRootPath());

            for (ContentFolder contentFolder : contentEntry.getFolders(ContentFolderScopes.all())) {
              if (contentFolder.isSynthetic()) {
                continue;
              }
              contentEntry.removeFolder(contentFolder);
            }
            LOG.info(String.format("Importing content root '%s' for module '%s'", contentRoot.getRootPath(), module.getName()));
            for (ContentRootData.SourceRoot path : contentRoot.getPaths(ExternalSystemSourceType.SOURCE)) {
              createSourceRootIfAbsent(contentEntry, path, module.getName(), ProductionContentFolderTypeProvider.getInstance(), false,
                                       createEmptyContentRootDirectories);
            }
            for (ContentRootData.SourceRoot path : contentRoot.getPaths(ExternalSystemSourceType.TEST)) {
              createSourceRootIfAbsent(contentEntry, path, module.getName(), TestContentFolderTypeProvider.getInstance(), false,
                                       createEmptyContentRootDirectories);
            }
            for (ContentRootData.SourceRoot path : contentRoot.getPaths(ExternalSystemSourceType.RESOURCE)) {
              createSourceRootIfAbsent(contentEntry, path, module.getName(), ProductionResourceContentFolderTypeProvider.getInstance(), false,
                                       createEmptyContentRootDirectories);
            }
            for (ContentRootData.SourceRoot path : contentRoot.getPaths(ExternalSystemSourceType.TEST_RESOURCE)) {
              createSourceRootIfAbsent(contentEntry, path, module.getName(), TestResourceContentFolderTypeProvider.getInstance(), false,
                                       createEmptyContentRootDirectories);
            }
            for (ContentRootData.SourceRoot path : contentRoot.getPaths(ExternalSystemSourceType.SOURCE_GENERATED)) {
              createSourceRootIfAbsent(contentEntry, path, module.getName(), ProductionContentFolderTypeProvider.getInstance(), true,
                                       createEmptyContentRootDirectories);
            }
            for (ContentRootData.SourceRoot path : contentRoot.getPaths(ExternalSystemSourceType.TEST_GENERATED)) {
              createSourceRootIfAbsent(contentEntry, path, module.getName(), TestContentFolderTypeProvider.getInstance(), true,
                                       createEmptyContentRootDirectories);
            }
            for (ContentRootData.SourceRoot path : contentRoot.getPaths(ExternalSystemSourceType.EXCLUDED)) {
              createExcludedRootIfAbsent(contentEntry, path, module.getName(), module.getProject());
            }
            contentEntriesMap.remove(contentEntry.getUrl());
          }
          for (ContentEntry contentEntry : contentEntriesMap.values()) {
            model.removeContentEntry(contentEntry);
          }
        }
        finally {
          model.commit();
        }
      }
    });
  }

  @Nonnull
  private static ContentEntry findOrCreateContentRoot(@Nonnull ModifiableRootModel model, @Nonnull String path) {
    ContentEntry[] entries = model.getContentEntries();

    for (ContentEntry entry : entries) {
      VirtualFile file = entry.getFile();
      if (file == null) {
        continue;
      }
      if (ExternalSystemApiUtil.getLocalFileSystemPath(file).equals(path)) {
        return entry;
      }
    }
    return model.addContentEntry(toVfsUrl(path));
  }

  private static void createSourceRootIfAbsent(@Nonnull ContentEntry entry,
                                               @Nonnull ContentRootData.SourceRoot root,
                                               @Nonnull String moduleName,
                                               @Nonnull ContentFolderTypeProvider folderTypeProvider,
                                               boolean generated,
                                               boolean createEmptyContentRootDirectories) {
    ContentFolder[] folders = entry.getFolders(ContentFolderScopes.of(folderTypeProvider));
    for (ContentFolder folder : folders) {
      VirtualFile file = folder.getFile();
      if (file == null) {
        continue;
      }
      if (ExternalSystemApiUtil.getLocalFileSystemPath(file).equals(root.getPath())) {
        return;
      }
    }
    LOG.info(String.format("Importing %s for content root '%s' of module '%s'", root, entry.getUrl(), moduleName));
    ContentFolder contentFolder = entry.addFolder(toVfsUrl(root.getPath()), folderTypeProvider);
    /*if (!StringUtil.isEmpty(root.getPackagePrefix())) {
      sourceFolder.setPackagePrefix(root.getPackagePrefix());
    } */
    if (generated) {
      contentFolder.setPropertyValue(GeneratedContentFolderPropertyProvider.IS_GENERATED, Boolean.TRUE);
    }
    if (createEmptyContentRootDirectories) {
      try {
        VfsUtil.createDirectoryIfMissing(root.getPath());
      }
      catch (IOException e) {
        LOG.warn(String.format("Unable to create directory for the path: %s", root.getPath()), e);
      }
    }
  }

  private static void createExcludedRootIfAbsent(@Nonnull ContentEntry entry,
                                                 @Nonnull ContentRootData.SourceRoot root,
                                                 @Nonnull String moduleName,
                                                 @Nonnull Project project) {
    String rootPath = root.getPath();
    for (VirtualFile file : entry.getFolderFiles(ContentFolderScopes.excluded())) {
      if (ExternalSystemApiUtil.getLocalFileSystemPath(file).equals(rootPath)) {
        return;
      }
    }
    LOG.info(String.format("Importing excluded root '%s' for content root '%s' of module '%s'", root, entry.getUrl(), moduleName));
    entry.addFolder(toVfsUrl(rootPath), ExcludedContentFolderTypeProvider.getInstance());
    ChangeListManager.getInstance(project).addDirectoryToIgnoreImplicitly(rootPath);
  }

  @Override
  public void removeData(@Nonnull Collection<? extends ContentEntry> toRemove, @Nonnull Project project, boolean synchronous) {
  }

  private static String toVfsUrl(@Nonnull String path) {
    return LocalFileSystem.PROTOCOL_PREFIX + path;
  }
}
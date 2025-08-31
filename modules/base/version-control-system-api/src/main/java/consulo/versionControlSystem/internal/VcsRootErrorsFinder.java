package consulo.versionControlSystem.internal;

import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.VcsRootChecker;
import consulo.versionControlSystem.VcsRootError;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.root.VcsRootDetector;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Nadya Zabrodina
 */
public class VcsRootErrorsFinder {
  private final Project myProject;
  private final ProjectLevelVcsManager myVcsManager;

  public VcsRootErrorsFinder(@Nonnull Project project) {
    myProject = project;
    myVcsManager = ProjectLevelVcsManager.getInstance(project);
  }

  @Nonnull
  public Collection<VcsRootError> find() {
    List<VcsDirectoryMapping> mappings = myVcsManager.getDirectoryMappings();
    Collection<VcsRoot> vcsRoots = myProject.getInstance(VcsRootDetector.class).detect();

    Collection<VcsRootError> errors = new ArrayList<>();
    errors.addAll(findExtraMappings(mappings));
    errors.addAll(findUnregisteredRoots(mappings, vcsRoots));
    return errors;
  }

  @Nonnull
  private Collection<VcsRootError> findUnregisteredRoots(@Nonnull List<VcsDirectoryMapping> mappings, @Nonnull Collection<VcsRoot> vcsRoots) {
    Collection<VcsRootError> errors = new ArrayList<>();
    List<String> mappedPaths = mappingsToPathsWithSelectedVcs(mappings);
    for (VcsRoot root : vcsRoots) {
      VirtualFile virtualFileFromRoot = root.getPath();
      if (virtualFileFromRoot == null) {
        continue;
      }
      String vcsPath = virtualFileFromRoot.getPath();
      if (!mappedPaths.contains(vcsPath) && root.getVcs() != null) {
        errors.add(new VcsRootErrorImpl(VcsRootError.Type.UNREGISTERED_ROOT, new VcsDirectoryMapping(vcsPath, root.getVcs().getName())));
      }
    }
    return errors;
  }

  @Nonnull
  private Collection<VcsRootError> findExtraMappings(@Nonnull List<VcsDirectoryMapping> mappings) {
    Collection<VcsRootError> errors = new ArrayList<>();
    for (VcsDirectoryMapping mapping : mappings) {
      if (!hasVcsChecker(mapping.getVcs())) {
        continue;
      }
      if (mapping.isDefaultMapping()) {
        if (!isRoot(mapping)) {
          errors.add(new VcsRootErrorImpl(VcsRootError.Type.EXTRA_MAPPING, new VcsDirectoryMapping(VcsDirectoryMapping.PROJECT_CONSTANT, mapping.getVcs())));
        }
      }
      else {
        String mappedPath = mapping.systemIndependentPath();
        if (!isRoot(mapping)) {
          errors.add(new VcsRootErrorImpl(VcsRootError.Type.EXTRA_MAPPING, new VcsDirectoryMapping(mappedPath, mapping.getVcs())));
        }
      }
    }
    return errors;
  }

  private static boolean hasVcsChecker(String vcs) {
    if (StringUtil.isEmptyOrSpaces(vcs)) {
      return false;
    }
    for (VcsRootChecker checker : VcsRootChecker.EXTENSION_POINT_NAME.getExtensionList()) {
      if (vcs.equalsIgnoreCase(checker.getSupportedVcs().getName())) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  public static Collection<VirtualFile> vcsRootsToVirtualFiles(@Nonnull Collection<VcsRoot> vcsRoots) {
    return ContainerUtil.map(vcsRoots, VcsRoot::getPath);
  }

  private List<String> mappingsToPathsWithSelectedVcs(@Nonnull List<VcsDirectoryMapping> mappings) {
    List<String> paths = new ArrayList<>();
    for (VcsDirectoryMapping mapping : mappings) {
      if (StringUtil.isEmptyOrSpaces(mapping.getVcs())) {
        continue;
      }
      if (!mapping.isDefaultMapping()) {
        paths.add(mapping.systemIndependentPath());
      }
      else {
        String basePath = myProject.getBasePath();
        if (basePath != null) {
          paths.add(FileUtil.toSystemIndependentName(basePath));
        }
      }
    }
    return paths;
  }

  public static VcsRootErrorsFinder getInstance(Project project) {
    return new VcsRootErrorsFinder(project);
  }

  private boolean isRoot(@Nonnull VcsDirectoryMapping mapping) {
    List<VcsRootChecker> checkers = VcsRootChecker.EXTENSION_POINT_NAME.getExtensionList();
    String pathToCheck = mapping.isDefaultMapping() ? myProject.getBasePath() : mapping.getDirectory();
    return ContainerUtil.find(checkers, checker -> checker.getSupportedVcs().getName().equalsIgnoreCase(mapping.getVcs()) && checker.isRoot(pathToCheck)) != null;
  }
}

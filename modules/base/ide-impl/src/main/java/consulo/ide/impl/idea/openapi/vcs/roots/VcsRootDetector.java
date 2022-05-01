package consulo.ide.impl.idea.openapi.vcs.roots;

import consulo.ide.impl.idea.openapi.vcs.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * Interface for detecting VCS roots in the project.
 *
 * @author Nadya Zabrodina
 */
public interface VcsRootDetector {

  /**
   * Detect vcs roots for whole project
   */
  @Nonnull
  Collection<VcsRoot> detect();

  /**
   * Detect vcs roots for startDir
   */
  @Nonnull
  Collection<VcsRoot> detect(@Nullable VirtualFile startDir);
}

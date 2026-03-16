package consulo.versionControlSystem.root;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

/**
 * Interface for detecting VCS roots in the project.
 *
 * @author Nadya Zabrodina
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface VcsRootDetector {

  /**
   * Detect vcs roots for whole project
   */
  
  Collection<VcsRoot> detect();

  /**
   * Detect vcs roots for startDir
   */
  
  Collection<VcsRoot> detect(@Nullable VirtualFile startDir);
}

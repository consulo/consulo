package consulo.ide.impl.idea.openapi.vcs.roots;

import consulo.versionControlSystem.root.VcsRoot;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Nadya Zabrodina
 */
public class VcsRootDetectInfo {

  private final @Nonnull
  Collection<VcsRoot> myRoots;
  private final boolean myFull;
  private final boolean myBelow;

  /**
   * @param roots Vcs roots important for the project.
   * @param full  Pass true to indicate that the project is fully under Vcs.
   * @param below Pass true to indicate that the project dir is below Vcs dir,
   *              i.e. .git is above the project dir, and there is no DOT dir directly under the project dir.
   */
  public VcsRootDetectInfo(@Nonnull Collection<VcsRoot> roots, boolean full, boolean below) {
    myRoots = new ArrayList<VcsRoot>(roots);
    myFull = full;
    myBelow = below;
  }

  /**
   * @return True if the project is fully under Vcs.
   * It is true if f.e. .git is directly inside or above the project dir.
   */
  public boolean totallyUnderVcs() {
    return myFull;
  }

  public boolean empty() {
    return myRoots.isEmpty();
  }

  @Nonnull
  public Collection<VcsRoot> getRoots() {
    return new ArrayList<VcsRoot>(myRoots);
  }

  /**
   * Below implies totally under Vcs.
   *
   * @return true if the uppermost interesting Vcs root is above the project dir,
   * false if all vcs internal directories are immediately under the project dir or deeper.
   */
  public boolean projectIsBelowVcs() {
    return myBelow;
  }
}

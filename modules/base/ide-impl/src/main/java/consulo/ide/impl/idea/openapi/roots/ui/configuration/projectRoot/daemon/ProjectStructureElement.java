package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.project.Project;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author nik
 */
public abstract class ProjectStructureElement {
  public abstract String getPresentableName();

  public @Nullable String getDescription() {
    return null;
  }

  public abstract String getTypeName();

  public abstract String getId();

  public abstract void check(Project project, ProjectStructureProblemsHolder problemsHolder);

  public abstract List<ProjectStructureElementUsage> getUsagesInElement();


  public boolean shouldShowWarningIfUnused() {
    return false;
  }

  public @Nullable ProjectStructureProblemDescription createUnusedElementWarning(Project project) {
    return null;
  }

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  @Override
  public String toString() {
    return getId();
  }
}

package consulo.ide.impl.idea.analysis;

import consulo.application.progress.PerformInBackgroundOption;
import consulo.language.editor.ui.scope.AnalysisUIOptions;
import consulo.project.Project;

/**
 * @author anna
 * @since 2007-01-22
 */
public class PerformAnalysisInBackgroundOption implements PerformInBackgroundOption {
  private final AnalysisUIOptions myUIOptions;

  public PerformAnalysisInBackgroundOption(Project project) {
    myUIOptions = AnalysisUIOptions.getInstance(project);
  }

  @Override
  public boolean shouldStartInBackground() {
    return myUIOptions.ANALYSIS_IN_BACKGROUND;
  }

  @Override
  public void processSentToBackground() {
    myUIOptions.ANALYSIS_IN_BACKGROUND = true;
  }

}

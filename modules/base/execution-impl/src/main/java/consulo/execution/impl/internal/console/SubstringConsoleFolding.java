package consulo.execution.impl.internal.console;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.ConsoleFolding;
import consulo.project.Project;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author peter
 */
@ExtensionImpl
public class SubstringConsoleFolding extends ConsoleFolding {
  private final ConsoleFoldingSettings mySettings;

  @Inject
  public SubstringConsoleFolding(ConsoleFoldingSettings settings) {
    mySettings = settings;
  }

  @Override
  public boolean shouldFoldLine(Project project, String line) {
    return mySettings.shouldFoldLine(line);
  }

  @Override
  public String getPlaceholderText(Project project, List<String> lines) {
    return " <" + lines.size() + " internal calls>";
  }
}

package consulo.ide.impl.idea.execution.console;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.annotation.component.ServiceImpl;
import consulo.execution.ConsoleFolding;

import java.util.List;

/**
 * @author peter
 */
public class SubstringConsoleFolding extends ConsoleFolding {
  private final ConsoleFoldingSettings mySettings;

  public SubstringConsoleFolding(ConsoleFoldingSettings settings) {
    mySettings = settings;
  }

  @Override
  public boolean shouldFoldLine(String line) {
    return mySettings.shouldFoldLine(line);
  }

  @Override
  public String getPlaceholderText(List<String> lines) {
    return " <" + lines.size() + " internal calls>";
  }
}

package consulo.remoteServer.impl.internal.runtime.log;

import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.process.ProcessHandler;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.project.Project;
import consulo.remoteServer.runtime.log.LoggingHandler;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class LoggingHandlerImpl implements LoggingHandler {
  private final ConsoleView myConsole;

  public LoggingHandlerImpl(Project project) {
    myConsole = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
  }

  public ConsoleView getConsole() {
    return myConsole;
  }

  @Override
  public void print(@Nonnull String s) {
    myConsole.print(s, ConsoleViewContentType.NORMAL_OUTPUT);
  }

  public void printlnSystemMessage(@Nonnull String s) {
    myConsole.print(s + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
  }

  @Override
  public void attachToProcess(@Nonnull ProcessHandler handler) {
    myConsole.attachToProcess(handler);
  }
}

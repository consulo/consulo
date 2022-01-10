package com.intellij.remoteServer.impl.runtime.log;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import javax.annotation.Nonnull;

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

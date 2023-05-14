package consulo.execution.debug.ui;

import consulo.application.AllIcons;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.layout.PlaceInGrid;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.ui.ex.content.Content;

import jakarta.annotation.Nonnull;

/**
 * Allows to customize xdebug layout for 'Debug' tool window.
 *
 * @author Sergey Simonchik
 * @see XDebugProcess#createTabLayouter()
 */
public class XDebugTabLayouter {

  /**
   * Registers tab for the given {@code console}.
   *
   * @param console {@code ExecutionConsole} instance
   * @param ui {@code RunnerLayoutUi} instance
   * @return registered {@code Content} instance
   */
  @Nonnull
  public Content registerConsoleContent(@Nonnull RunnerLayoutUi ui, @Nonnull ExecutionConsole console) {
    Content content = ui.createContent(DebuggerContentInfo.CONSOLE_CONTENT, console.getComponent(),
                                       XDebuggerBundle.message("debugger.session.tab.console.content.name"),
                                       AllIcons.Debugger.Console,
                                       console.getPreferredFocusableComponent());
    content.setCloseable(false);
    ui.addContent(content, 1, PlaceInGrid.bottom, false);
    return content;
  }

  /**
   * Registers additional tabs for 'Debug' tool window.
   * @param ui {@code RunnerLayoutUi} instance
   */
  public void registerAdditionalContent(@Nonnull RunnerLayoutUi ui) {
  }

}

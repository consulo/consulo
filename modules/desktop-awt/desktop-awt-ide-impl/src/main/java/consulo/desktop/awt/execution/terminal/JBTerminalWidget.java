package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.TerminalStarter;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.model.JediTerminal;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.TerminalAction;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ui.terminal.TerminalConsole;
import consulo.execution.ui.terminal.TerminalConsoleSettings;
import consulo.ui.Component;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.JBScrollBar;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.function.Predicate;

public class JBTerminalWidget extends JediTermWidget implements Disposable, TerminalConsole {
  private final TerminalConsoleSettings myTerminalConsoleSettings;

  public JBTerminalWidget(JBTerminalSystemSettingsProvider settingsProvider, TerminalConsoleSettings settings) {
    super(settingsProvider);
    myTerminalConsoleSettings = settings;

    convertActions(this, getActions());
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    return TargetAWT.wrap(getComponent());
  }

  @Override
  protected JBTerminalPanel createTerminalPanel(@Nonnull SettingsProvider settingsProvider,
                                                @Nonnull StyleState styleState,
                                                @Nonnull TerminalTextBuffer textBuffer) {
    JBTerminalPanel panel =
      new JBTerminalPanel((JBTerminalSystemSettingsProvider)settingsProvider, textBuffer, styleState, myTerminalConsoleSettings);
    Disposer.register(this, panel);
    return panel;
  }

  public static void convertActions(@Nonnull JComponent component, @Nonnull List<TerminalAction> actions) {
    convertActions(component, actions, null);
  }

  public static void convertActions(@Nonnull JComponent component,
                                    @Nonnull List<TerminalAction> actions,
                                    @Nullable final Predicate<java.awt.event.KeyEvent> elseAction) {
    for (final TerminalAction action : actions) {
      AnAction a = new DumbAwareAction() {
        @Override
        public void actionPerformed(AnActionEvent e) {
          java.awt.event.KeyEvent event =
            e.getInputEvent() instanceof java.awt.event.KeyEvent ? (java.awt.event.KeyEvent)e.getInputEvent() : null;
          if (!action.perform(event)) {
            if (elseAction != null) {
              elseAction.test(event);
            }
          }
        }
      };
      a.registerCustomShortcutSet(action.getKeyCode(), action.getModifiers(), component);
    }
  }


  @Override
  protected TerminalStarter createTerminalStarter(JediTerminal terminal, TtyConnector connector) {
    return new JBTerminalStarter(terminal, connector);
  }

  @Override
  protected JScrollBar createScrollBar() {
    return new JBScrollBar();
  }

  @Override
  public void dispose() {
  }
}

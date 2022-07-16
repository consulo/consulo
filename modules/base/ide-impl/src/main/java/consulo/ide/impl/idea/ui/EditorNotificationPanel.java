/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.ui;

import consulo.application.AllIcons;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorBundle;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.component.util.Iconable;
import consulo.dataContext.DataManager;
import consulo.language.editor.intention.AbstractEmptyIntentionAction;
import consulo.ide.impl.idea.codeInsight.intention.IntentionActionProvider;
import consulo.ide.impl.idea.codeInsight.intention.IntentionActionWithOptions;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.HyperlinkAdapter;
import consulo.ui.ex.awt.HyperlinkLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class EditorNotificationPanel extends JPanel implements IntentionActionProvider {
  protected final JLabel myLabel = new JLabel();
  protected final JLabel myGearLabel = new JLabel();
  protected final JPanel myLinksPanel;
  private final Color myBackgroundColor;

  public EditorNotificationPanel() {
    this(null);
  }

  public EditorNotificationPanel(@Nullable Color backgroundColor) {
    super(new BorderLayout());
    myBackgroundColor = backgroundColor;
    setBorder(JBUI.Borders.empty(1, 10, 1, 10));

    setPreferredSize(JBUI.size(-1, 24));

    add(myLabel, BorderLayout.CENTER);

    myLinksPanel = new JPanel(new FlowLayout());
    myLinksPanel.setBackground(getBackground());

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(getBackground());
    myGearLabel.setBorder(JBUI.Borders.empty(0, 3, 0, 0));
    panel.add(myLinksPanel, BorderLayout.WEST);
    panel.add(myGearLabel, BorderLayout.EAST);
    add(panel, BorderLayout.EAST);
  }

  public void setText(String text) {
    myLabel.setText(text);
  }

  public EditorNotificationPanel text(@Nonnull String text) {
    myLabel.setText(text);
    return this;
  }

  public EditorNotificationPanel icon(@Nonnull Icon icon) {
    myLabel.setIcon(icon);
    return this;
  }

  @Override
  public final Color getBackground() {
    if (myBackgroundColor != null) {
      return myBackgroundColor;
    }
    ColorValue color = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.NOTIFICATION_BACKGROUND);
    return color == null ? UIUtil.getToolTipBackground() : TargetAWT.to(color);
  }

  public HyperlinkLabel createActionLabel(final String text, final String actionId) {
    return createActionLabel(text, () -> executeAction(actionId));
  }

  public HyperlinkLabel createActionLabel(final String text, final Runnable action) {
    HyperlinkLabel label = new HyperlinkLabel(text, JBColor.BLUE, getBackground(), JBColor.BLUE);
    label.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        action.run();
      }
    });
    myLinksPanel.add(label);
    return label;
  }

  @RequiredUIAccess
  protected void executeAction(final String actionId) {
    final AnAction action = ActionManager.getInstance().getAction(actionId);
    final AnActionEvent event = new AnActionEvent(null, DataManager.getInstance().getDataContext(this), ActionPlaces.UNKNOWN, action.getTemplatePresentation(),
                                                  ActionManager.getInstance(), 0);
    action.beforeActionPerformedUpdate(event);
    action.update(event);

    if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
      action.actionPerformed(event);
    }
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(0, 0);
  }

  @Nullable
  @Override
  public IntentionActionWithOptions getIntentionAction() {
    MyIntentionAction action = new MyIntentionAction();
    return action.getOptions().isEmpty() ? null : action;
  }

  private class MyIntentionAction extends AbstractEmptyIntentionAction implements IntentionActionWithOptions, Iconable, SyntheticIntentionAction {
    private final List<IntentionAction> myOptions = new ArrayList<>();

    private MyIntentionAction() {
      for (Component component : myLinksPanel.getComponents()) {
        if (component instanceof HyperlinkLabel) {
          myOptions.add(new MyLinkOption(((HyperlinkLabel)component)));
        }
      }
      if (myGearLabel.getIcon() != null) {
        myOptions.add(new MySettingsOption(myGearLabel));
      }
    }

    @Nonnull
    @Override
    public List<IntentionAction> getOptions() {
      return myOptions;
    }

    @Nls
    @Nonnull
    @Override
    public String getText() {
      String text = myLabel.getText();
      return StringUtil.isEmpty(text) ? EditorBundle.message("editor.notification.default.action.name") : StringUtil.shortenTextWithEllipsis(text, 50, 0);
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return "Editor notification";
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public Image getIcon(@IconFlags int flags) {
      return AllIcons.Actions.IntentionBulb;
    }
  }

  private static class MyLinkOption implements IntentionAction {
    private final HyperlinkLabel myLabel;

    private MyLinkOption(HyperlinkLabel label) {
      myLabel = label;
    }

    @Nls
    @Nonnull
    @Override
    public String getText() {
      return myLabel.getText();
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return "Editor notification option";
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      myLabel.doClick();
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }
  }

  private static class MySettingsOption implements IntentionAction, Iconable, LowPriorityAction {
    private final JLabel myLabel;

    private MySettingsOption(JLabel label) {
      myLabel = label;
    }

    @Nls
    @Nonnull
    @Override
    public String getText() {
      return EditorBundle.message("editor.notification.settings.option.name");
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return "Editor notification settings";
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      myLabel.dispatchEvent(new MouseEvent(myLabel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 0, 0, 1, false));
      myLabel.dispatchEvent(new MouseEvent(myLabel, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 0, 0, 1, false));
      myLabel.dispatchEvent(new MouseEvent(myLabel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false));
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }

    @Override
    public Image getIcon(@IconFlags int flags) {
      return TargetAWT.from(myLabel.getIcon());
    }
  }
}

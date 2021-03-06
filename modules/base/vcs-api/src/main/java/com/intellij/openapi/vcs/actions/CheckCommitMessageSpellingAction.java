package com.intellij.openapi.vcs.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.Refreshable;

/**
 * Allows to toggle <code>'check commit message spelling errors'</code> processing.
 * 
 * @author Denis Zhdanov
 * @since 8/22/11 3:27 PM
 */
public class CheckCommitMessageSpellingAction extends ToggleAction implements DumbAware {

  public CheckCommitMessageSpellingAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    CommitMessageI checkinPanel = getCheckinPanel(e);
    return checkinPanel != null && checkinPanel.isCheckSpelling();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    CommitMessageI checkinPanel = getCheckinPanel(e);
    if (checkinPanel != null) {
      checkinPanel.setCheckSpelling(state);
    }
  }

  @javax.annotation.Nullable
  private static CommitMessageI getCheckinPanel(@javax.annotation.Nullable AnActionEvent e) {
    if (e == null) {
      return null;
    }
    Refreshable data = e.getData(Refreshable.PANEL_KEY);
    if (data instanceof CommitMessageI) {
      return (CommitMessageI)data;
    }
    CommitMessageI commitMessageI = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
    if (commitMessageI != null) {
      return commitMessageI;
    }
    return null;
  }
}

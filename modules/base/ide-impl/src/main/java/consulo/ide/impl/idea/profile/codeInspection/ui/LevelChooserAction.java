/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.profile.codeInspection.ui;

import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.ide.impl.idea.codeInsight.daemon.impl.SeverityRegistrarImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.SeverityUtil;
import consulo.ide.impl.idea.codeInspection.ex.InspectionProfileImpl;
import consulo.ide.impl.idea.codeInspection.ex.SeverityEditorDialog;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Dmitry Batkovich
 */
public abstract class LevelChooserAction extends ComboBoxAction implements DumbAware {

  private final SeverityRegistrarImpl mySeverityRegistrar;
  private HighlightSeverity myChosen = null;

  public LevelChooserAction(final InspectionProfileImpl profile) {
    this((SeverityRegistrarImpl)((SeverityProvider)profile.getProfileManager()).getOwnSeverityRegistrar());
  }

  public LevelChooserAction(final SeverityRegistrarImpl severityRegistrar) {
    mySeverityRegistrar = severityRegistrar;
  }

  @Nonnull
  @Override
  public DefaultActionGroup createPopupActionGroup(JComponent component) {
    final DefaultActionGroup group = new DefaultActionGroup();
    for (final HighlightSeverity severity : getSeverities(mySeverityRegistrar)) {
      final HighlightSeverityAction action = new HighlightSeverityAction(severity);
      if (myChosen == null) {
        setChosen(action.getSeverity());
      }
      group.add(action);
    }
    group.addSeparator();
    group.add(new DumbAwareAction("Edit severities...") {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull final AnActionEvent e) {
        final SeverityEditorDialog dlg = new SeverityEditorDialog(e.getData(CommonDataKeys.PROJECT), myChosen, mySeverityRegistrar);
        if (dlg.showAndGet()) {
          final HighlightInfoType type = dlg.getSelectedType();
          if (type != null) {
            final HighlightSeverity severity = type.getSeverity(null);
            setChosen(severity);
            onChosen(severity);
          }
        }
      }
    });
    return group;
  }

  public static SortedSet<HighlightSeverity> getSeverities(final SeverityRegistrarImpl severityRegistrar) {
    final SortedSet<HighlightSeverity> severities = new TreeSet<HighlightSeverity>(severityRegistrar);
    for (final SeverityRegistrarImpl.SeverityBasedTextAttributes type : SeverityUtil.getRegisteredHighlightingInfoTypes(severityRegistrar)) {
      severities.add(type.getSeverity());
    }
    severities.add(HighlightSeverity.ERROR);
    severities.add(HighlightSeverity.WARNING);
    severities.add(HighlightSeverity.WEAK_WARNING);
    severities.add(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING);
    return severities;
  }

  protected abstract void onChosen(final HighlightSeverity severity);

  public void setChosen(final HighlightSeverity severity) {
    myChosen = severity;
    final Presentation templatePresentation = getTemplatePresentation();
    templatePresentation.setText(SingleInspectionProfilePanel.renderSeverity(severity));
    templatePresentation.setIcon(HighlightDisplayLevel.find(severity).getIcon());
  }

  private class HighlightSeverityAction extends DumbAwareAction {
    private final HighlightSeverity mySeverity;

    public HighlightSeverity getSeverity() {
      return mySeverity;
    }

    private HighlightSeverityAction(final HighlightSeverity severity) {
      mySeverity = severity;
      final Presentation presentation = getTemplatePresentation();
      presentation.setText(SingleInspectionProfilePanel.renderSeverity(severity));
      presentation.setIcon(HighlightDisplayLevel.find(severity).getIcon());
    }

    @Override
    public void actionPerformed(@Nonnull final AnActionEvent e) {
      final HighlightSeverity severity = getSeverity();
      setChosen(severity);
      onChosen(severity);
    }
  }
}

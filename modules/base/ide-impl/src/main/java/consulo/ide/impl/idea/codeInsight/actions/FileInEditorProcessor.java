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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.LightweightHint;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.HyperlinkAdapter;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import static consulo.ide.impl.idea.codeInsight.actions.TextRangeType.SELECTED_TEXT;
import static consulo.ide.impl.idea.codeInsight.actions.TextRangeType.VCS_CHANGED_TEXT;

class FileInEditorProcessor {
  private static final Logger LOG = Logger.getInstance(FileInEditorProcessor.class);

  private final Editor myEditor;

  private boolean myNoChangesDetected = false;
  private final boolean myProcessChangesTextOnly;

  private final boolean myShouldOptimizeImports;
  private final boolean myShouldRearrangeCode;
  private final boolean myProcessSelectedText;

  private final Project myProject;

  private final PsiFile myFile;
  private AbstractLayoutCodeProcessor myProcessor;

  public FileInEditorProcessor(PsiFile file,
                               Editor editor,
                               LayoutCodeOptions runOptions)
  {
    myFile = file;
    myProject = file.getProject();
    myEditor = editor;

    myShouldOptimizeImports = runOptions.isOptimizeImports();
    myShouldRearrangeCode = runOptions.isRearrangeCode();
    myProcessSelectedText = myEditor != null && runOptions.getTextRangeType() == SELECTED_TEXT;
    myProcessChangesTextOnly = runOptions.getTextRangeType() == VCS_CHANGED_TEXT;
  }

  public void processCode() {
    if (myShouldOptimizeImports) {
      myProcessor = new OptimizeImportsProcessor(myProject, myFile);
    }

    if (myProcessChangesTextOnly && !FormatChangedTextUtil.hasChanges(myFile)) {
      myNoChangesDetected = true;
    }

    myProcessor = mixWithReformatProcessor(myProcessor);
    if (myShouldRearrangeCode) {
      myProcessor = mixWithRearrangeProcessor(myProcessor);
    }

    if (shouldNotify()) {
      myProcessor.setCollectInfo(true);
      myProcessor.setPostRunnable(()-> {
          String message = prepareMessage();
          if (!myEditor.isDisposed() && myEditor.getComponent().isShowing()) {
            HyperlinkListener hyperlinkListener = new HyperlinkAdapter() {
              @Override
              protected void hyperlinkActivated(HyperlinkEvent e) {
                AnAction action = ActionManager.getInstance().getAction("ShowReformatFileDialog");
                DataManager manager = DataManager.getInstance();
                if (manager != null) {
                  DataContext context = manager.getDataContext(myEditor.getContentComponent());
                  action.actionPerformed(AnActionEvent.createFromAnAction(action, null, "", context));
                }
              }
            };
            showHint(myEditor, message, hyperlinkListener);
          }
        });
    }

    myProcessor.run();
  }

  private AbstractLayoutCodeProcessor mixWithRearrangeProcessor(@Nonnull AbstractLayoutCodeProcessor processor) {
    if (myProcessSelectedText) {
      processor = new RearrangeCodeProcessor(processor, myEditor.getSelectionModel());
    }
    else {
      processor = new RearrangeCodeProcessor(processor);
    }
    return processor;
  }

  @Nonnull
  private AbstractLayoutCodeProcessor mixWithReformatProcessor(@Nullable AbstractLayoutCodeProcessor processor) {
    if (processor != null) {
      if (myProcessSelectedText) {
        processor = new ReformatCodeProcessor(processor, myEditor.getSelectionModel());
      }
      else {
        processor = new ReformatCodeProcessor(processor, myProcessChangesTextOnly);
      }
    }
    else {
      if (myProcessSelectedText) {
        processor = new ReformatCodeProcessor(myFile, myEditor.getSelectionModel());
      }
      else {
        processor = new ReformatCodeProcessor(myFile, myProcessChangesTextOnly);
      }
    }
    return processor;
  }

  @Nonnull
  private String prepareMessage() {
    StringBuilder builder = new StringBuilder("<html>");
    LayoutCodeInfoCollector notifications = myProcessor.getInfoCollector();
    LOG.assertTrue(notifications != null);

    if (notifications.isEmpty() && !myNoChangesDetected) {
      if (myProcessChangesTextOnly) {
        builder.append("No lines changed: changes since last revision are already properly formatted").append("<br>");
      }
      else {
        builder.append("No lines changed: code is already properly formatted").append("<br>");
      }
    }
    else {
      if (notifications.hasReformatOrRearrangeNotification()) {
        String reformatInfo = notifications.getReformatCodeNotification();
        String rearrangeInfo = notifications.getRearrangeCodeNotification();

        builder.append(joinWithCommaAndCapitalize(reformatInfo, rearrangeInfo));

        if (myProcessChangesTextOnly) {
          builder.append(" in changes since last revision");
        }

        builder.append("<br>");
      }
      else if (myNoChangesDetected) {
        builder.append("No lines changed: no changes since last revision").append("<br>");
      }

      String optimizeImportsNotification = notifications.getOptimizeImportsNotification();
      if (optimizeImportsNotification != null) {
        builder.append(StringUtil.capitalize(optimizeImportsNotification)).append("<br>");
      }
    }

    String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction("ShowReformatFileDialog"));
    String color = ColorUtil.toHex(JBColor.gray);

    builder.append("<span style='color:#").append(color).append("'>")
            .append("<a href=''>Show</a> reformat dialog: ").append(shortcutText).append("</span>")
            .append("</html>");

    return builder.toString();
  }

  @Nonnull
  private static String joinWithCommaAndCapitalize(String reformatNotification, String rearrangeNotification) {
    String firstNotificationLine = reformatNotification != null ? reformatNotification : rearrangeNotification;
    if (reformatNotification != null && rearrangeNotification != null) {
      firstNotificationLine += ", " + rearrangeNotification;
    }
    firstNotificationLine = StringUtil.capitalize(firstNotificationLine);
    return firstNotificationLine;
  }

  public static void showHint(@Nonnull Editor editor, @Nonnull String info, @Nullable HyperlinkListener hyperlinkListener) {
    JComponent component = HintUtil.createInformationLabel(info, hyperlinkListener, null, null);
    LightweightHint hint = new LightweightHint(component);
    HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, HintManager.UNDER,
                                                     HintManager.HIDE_BY_ANY_KEY |
                                                     HintManager.HIDE_BY_TEXT_CHANGE |
                                                     HintManager.HIDE_BY_SCROLLING,
                                                     0, false);
  }

  private boolean shouldNotify() {
    Application application = Application.get();
    if (application.isUnitTestMode() || application.isHeadlessEnvironment()) {
      return false;
    }
    EditorSettingsExternalizable.OptionSet editorOptions = EditorSettingsExternalizable.getInstance().getOptions();
    return editorOptions.SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION && myEditor != null && !myProcessSelectedText;
  }
}

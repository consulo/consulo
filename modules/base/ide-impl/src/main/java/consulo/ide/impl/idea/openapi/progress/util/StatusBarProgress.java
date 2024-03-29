// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.progress.util;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.lang.Pair;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.localize.LocalizeValue;
import consulo.util.lang.ObjectUtil;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static consulo.util.lang.Pair.pair;

public class StatusBarProgress extends ProgressIndicatorBase {
  // statusBar -> [textToRestore, MyPreviousText]
  private final Map<StatusBar, Pair<String, String>> myStatusBar2SavedText = new HashMap<>();
  private boolean myScheduledStatusBarTextSave;

  public StatusBarProgress() {
    super(true);
  }

  @Override
  public void start() {
    myScheduledStatusBarTextSave = false;
    super.start();
  }

  @Override
  public void stop() {
    super.stop();

    if (myScheduledStatusBarTextSave) {
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        for (StatusBar statusBar : myStatusBar2SavedText.keySet()) {
          String textToRestore = updateRestoreText(statusBar);
          statusBar.setInfo(textToRestore);
        }
        myStatusBar2SavedText.clear();
      });
    }
  }

  @Override
  public void setTextValue(LocalizeValue text) {
    super.setTextValue(text);
    update();
  }

  @Override
  public void setFraction(double fraction) {
    super.setFraction(fraction);
    update();
  }

  private void update() {
    String text;
    if (!isRunning()) {
      text = "";
    }
    else {
      text = getText();
      double fraction = getFraction();
      if (fraction > 0) {
        text += " " + (int)(fraction * 100 + 0.5) + "%";
      }
    }
    final String _text = text;
    if (!myScheduledStatusBarTextSave) {
      myScheduledStatusBarTextSave = true;
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        if (ApplicationManager.getApplication().isDisposed()) return;
        WindowManager windowManager = WindowManager.getInstance();
        if (windowManager == null) return;

        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) projects = new Project[]{null};

        for (Project project : projects) {
          StatusBar statusBar = windowManager.getStatusBar(project);
          if (statusBar != null) {
            String info = ObjectUtil.notNull(statusBar.getInfo(), "");
            myStatusBar2SavedText.put(statusBar, pair(info, info));  // initial value
          }
        }
      });
    }
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(() -> {
      for (StatusBar statusBarEx : myStatusBar2SavedText.keySet()) {
        setStatusBarText(statusBarEx, _text);
      }
    });
  }

  private void setStatusBarText(StatusBar statusBar, String text) {
    updateRestoreText(statusBar);
    Pair<String, String> textsPair = myStatusBar2SavedText.get(statusBar);
    myStatusBar2SavedText.put(statusBar, pair(textsPair.first, text));
    statusBar.setInfo(text);
  }

  private String updateRestoreText(StatusBar statusBar) {
    Pair<String, String> textsPair = myStatusBar2SavedText.get(statusBar);
    if (textsPair == null) {
      return "";
    }

    // if current status bar info doesn't match the value, that we set, use this value as a restore value
    String info = ObjectUtil.notNull(statusBar.getInfo(), "");
    if (!textsPair.getSecond().equals(info)) {
      myStatusBar2SavedText.put(statusBar, pair(info, textsPair.second));
    }
    return textsPair.getFirst();
  }
}

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
package consulo.ide.impl.idea.openapi.vcs.ex;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.vcs.actions.ShowNextChangeMarkerAction;
import consulo.ide.impl.idea.openapi.vcs.actions.ShowPrevChangeMarkerAction;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.*;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LineStatusTrackerDrawing {
  private LineStatusTrackerDrawing() {
  }

  public static void moveToRange(VcsRange range, Editor editor, LineStatusTracker tracker) {
    new MyLineStatusMarkerPopup(tracker, editor, range).scrollAndShow();
  }

  public static void showHint(VcsRange range, Editor editor, LineStatusTracker tracker) {
    new MyLineStatusMarkerPopup(tracker, editor, range).showAfterScroll();
  }

  public static class MyLineStatusMarkerPopup extends LineStatusMarkerPopup {
    @Nonnull
    private final LineStatusTracker myTracker;

    public MyLineStatusMarkerPopup(@Nonnull LineStatusTracker tracker,
                                   @Nonnull Editor editor,
                                   @Nonnull VcsRange range) {
      super(tracker, editor, range);
      myTracker = tracker;
    }

    @Override
    protected boolean isShowInnerDifferences() {
      return VcsApplicationSettings.getInstance().SHOW_LST_WORD_DIFFERENCES;
    }

    @Nonnull
    @Override
    protected ActionToolbar buildToolbar(@Nullable Point mousePosition, @Nonnull Disposable parentDisposable) {
      DefaultActionGroup group = new DefaultActionGroup();

      ShowPrevChangeMarkerAction localShowPrevAction =
        new ShowPrevChangeMarkerAction(myTracker.getPrevRange(myRange), myTracker, myEditor);
      ShowNextChangeMarkerAction localShowNextAction =
        new ShowNextChangeMarkerAction(myTracker.getNextRange(myRange), myTracker, myEditor);
      RollbackLineStatusRangeAction rollback = new RollbackLineStatusRangeAction(myTracker, myRange, myEditor);
      ShowLineStatusRangeDiffAction showDiff = new ShowLineStatusRangeDiffAction(myTracker, myRange, myEditor);
      CopyLineStatusRangeAction copyRange = new CopyLineStatusRangeAction(myTracker, myRange);
      ToggleByWordDiffAction toggleWordDiff = new ToggleByWordDiffAction(myRange, myEditor, myTracker, mousePosition);

      group.add(localShowPrevAction);
      group.add(localShowNextAction);
      group.add(rollback);
      group.add(showDiff);
      group.add(copyRange);
      group.add(toggleWordDiff);

      JComponent editorComponent = myEditor.getComponent();
      registerAction(localShowPrevAction, editorComponent);
      registerAction(localShowNextAction, editorComponent);
      registerAction(rollback, editorComponent);
      registerAction(showDiff, editorComponent);
      registerAction(copyRange, editorComponent);

      List<AnAction> actionList = ActionImplUtil.getActions(editorComponent);
      Disposer.register(parentDisposable, () -> {
        actionList.remove(localShowPrevAction);
        actionList.remove(localShowNextAction);
        actionList.remove(rollback);
        actionList.remove(showDiff);
        actionList.remove(copyRange);
      });

      return ActionManager.getInstance().createActionToolbar(ActionPlaces.FILEHISTORY_VIEW_TOOLBAR, group, true);
    }

    private static void registerAction(@Nonnull AnAction action, @Nonnull JComponent component) {
      action.registerCustomShortcutSet(action.getShortcutSet(), component);
    }

    @Nonnull
    @Override
    protected FileType getFileType() {
      return myTracker.getVirtualFile().getFileType();
    }
  }

  private static class ToggleByWordDiffAction extends ToggleAction implements DumbAware {
    @Nonnull
    private final VcsRange myRange;
    @Nonnull
    private final Editor myEditor;
    @Nonnull
    private final LineStatusTracker myTracker;
    @Nullable
    private final Point myMousePosition;

    public ToggleByWordDiffAction(@Nonnull VcsRange range,
                                  @Nonnull Editor editor,
                                  @Nonnull LineStatusTracker tracker,
                                  @Nullable Point mousePosition) {
      super("Highlight Words", null, PlatformIconGroup.generalHighlighting());
      myRange = range;
      myEditor = editor;
      myTracker = tracker;
      myMousePosition = mousePosition;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return VcsApplicationSettings.getInstance().SHOW_LST_WORD_DIFFERENCES;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      VcsApplicationSettings.getInstance().SHOW_LST_WORD_DIFFERENCES = state;
      new MyLineStatusMarkerPopup(myTracker, myEditor, myRange).showHintAt(myMousePosition);
    }
  }
}

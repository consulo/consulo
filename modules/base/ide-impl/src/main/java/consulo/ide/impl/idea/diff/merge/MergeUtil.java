/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diff.merge;

import consulo.ide.impl.idea.diff.DiffContext;
import consulo.diff.content.DiffContent;
import consulo.ide.impl.idea.diff.merge.MergeTool.MergeViewer;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.request.DiffRequest;
import consulo.ide.impl.idea.diff.util.DiffUserDataKeysEx;
import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.diff.util.ThreeSide;
import consulo.ide.impl.idea.openapi.diff.DiffBundle;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.Pair;
import consulo.util.lang.function.Condition;
import consulo.util.lang.Couple;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.merge.MergeData;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Function;

public class MergeUtil {
  @Nonnull
  public static Action createSimpleResolveAction(@Nonnull MergeResult result,
                                                 @Nonnull MergeRequest request,
                                                 @Nonnull MergeContext context,
                                                 @Nonnull MergeViewer viewer) {
    String caption = getResolveActionTitle(result, request, context);
    return new AbstractAction(caption) {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (result == MergeResult.CANCEL && !showExitWithoutApplyingChangesDialog(viewer, request, context)) {
          return;
        }
        context.finishMerge(result);
      }
    };
  }

  @Nonnull
  public static String getResolveActionTitle(@Nonnull MergeResult result, @Nonnull MergeRequest request, @Nonnull MergeContext context) {
    Function<MergeResult, String> getter = DiffUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_ACTION_CAPTIONS);
    String message = getter != null ? getter.apply(result) : null;
    if (message != null) return message;

    switch (result) {
      case CANCEL:
        return "Abort";
      case LEFT:
        return "Accept Left";
      case RIGHT:
        return "Accept Right";
      case RESOLVED:
        return "Apply";
      default:
        throw new IllegalArgumentException(result.toString());
    }
  }

  public static void reportProjectFileChangeIfNeeded(@Nullable Project project, @Nullable VirtualFile file) {
    //if (project != null && file != null && isProjectFile(file)) {
    //  ProjectManagerEx.getInstanceEx().saveChangedProjectFile(file, project);
    //}
  }

  @Nonnull
  public static List<String> notNullizeContentTitles(@Nonnull List<String> mergeContentTitles) {
    String left = StringUtil.notNullize(ThreeSide.LEFT.select(mergeContentTitles), "Your Version");
    String base = StringUtil.notNullize(ThreeSide.BASE.select(mergeContentTitles), "Base Version");
    String right = StringUtil.notNullize(ThreeSide.RIGHT.select(mergeContentTitles), "Server Version");
    return ContainerUtil.list(left, base, right);
  }

  public static class ProxyDiffContext extends DiffContext {
    @Nonnull
    private final MergeContext myMergeContext;

    public ProxyDiffContext(@Nonnull MergeContext mergeContext) {
      myMergeContext = mergeContext;
    }

    @jakarta.annotation.Nullable
    @Override
    public Project getProject() {
      return myMergeContext.getProject();
    }

    @Override
    public boolean isWindowFocused() {
      return true;
    }

    @Override
    public boolean isFocused() {
      return myMergeContext.isFocused();
    }

    @Override
    public void requestFocus() {
      myMergeContext.requestFocus();
    }

    @jakarta.annotation.Nullable
    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
      return myMergeContext.getUserData(key);
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, @jakarta.annotation.Nullable T value) {
      myMergeContext.putUserData(key, value);
    }
  }

  public static boolean showExitWithoutApplyingChangesDialog(@Nonnull MergeViewer viewer,
                                                             @Nonnull MergeRequest request,
                                                             @Nonnull MergeContext context) {
    Condition<MergeViewer> customHandler = DiffUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_CANCEL_HANDLER);
    if (customHandler != null) {
      return customHandler.value(viewer);
    }

    return showExitWithoutApplyingChangesDialog(viewer.getComponent(), request, context);
  }

  public static boolean showExitWithoutApplyingChangesDialog(@Nonnull JComponent component,
                                                             @Nonnull MergeRequest request,
                                                             @Nonnull MergeContext context) {
    String message = DiffBundle.message("merge.dialog.exit.without.applying.changes.confirmation.message");
    String title = DiffBundle.message("cancel.visual.merge.dialog.title");
    Couple<String> customMessage = DiffUtil.getUserData(request, context, DiffUserDataKeysEx.MERGE_CANCEL_MESSAGE);
    if (customMessage != null) {
      title = customMessage.first;
      message = customMessage.second;
    }

    return Messages.showYesNoDialog(component.getRootPane(), message, title, Messages.getQuestionIcon()) == Messages.YES;
  }

  public static void putRevisionInfos(@Nonnull MergeRequest request, @Nonnull MergeData data) {
    if (request instanceof ThreesideMergeRequest) {
      List<? extends DiffContent> contents = ((ThreesideMergeRequest)request).getContents();
      putRevisionInfo(contents, data);
    }
  }

  public static void putRevisionInfos(@Nonnull DiffRequest request, @Nonnull MergeData data) {
    if (request instanceof ContentDiffRequest) {
      List<? extends DiffContent> contents = ((ContentDiffRequest)request).getContents();
      if (contents.size() == 3) {
        putRevisionInfo(contents, data);
      }
    }
  }

  private static void putRevisionInfo(@Nonnull List<? extends DiffContent> contents, @Nonnull MergeData data) {
    for (ThreeSide side : ThreeSide.values()) {
      DiffContent content = side.select(contents);
      FilePath filePath = side.select(data.CURRENT_FILE_PATH, data.ORIGINAL_FILE_PATH, data.LAST_FILE_PATH);
      VcsRevisionNumber revision = side.select(data.CURRENT_REVISION_NUMBER, data.ORIGINAL_REVISION_NUMBER, data.LAST_REVISION_NUMBER);
      if (filePath != null && revision != null) {
        content.putUserData(DiffUserDataKeysEx.REVISION_INFO, Pair.create(filePath, revision));
      }
    }
  }
}

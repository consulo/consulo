/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.versionControlSystem.patch;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.desktop.awt.internal.diff.merge.MergeImplUtil;
import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.diff.DiffContext;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.merge.MergeContext;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.tool.ApplyPatchMergeRequest;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

@ExtensionImpl
public class ApplyPatchMergeTool implements MergeTool {
  @Nonnull
  @Override
  public MergeViewer createComponent(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
    return new MyApplyPatchViewer(context, (ApplyPatchMergeRequest)request);
  }

  @Override
  public boolean canShow(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
    return request instanceof ApplyPatchMergeRequest;
  }

  private static class MyApplyPatchViewer extends ApplyPatchViewer implements MergeViewer {
    @Nonnull
    private final MergeContext myMergeContext;
    @Nonnull
    private final ApplyPatchMergeRequest myMergeRequest;

    public MyApplyPatchViewer(@Nonnull MergeContext context, @Nonnull ApplyPatchMergeRequest request) {
      super(createWrapperDiffContext(context), request);
      myMergeContext = context;
      myMergeRequest = request;
    }

    @Nonnull
    private static DiffContext createWrapperDiffContext(@Nonnull MergeContext mergeContext) {
      return new MergeImplUtil.ProxyDiffContext(mergeContext);
    }

    @Nonnull
    @Override
    public ToolbarComponents init() {
      initPatchViewer();

      ToolbarComponents components = new ToolbarComponents();
      components.statusPanel = getStatusPanel();
      components.toolbarActions = createToolbarActions();

      components.closeHandler = () -> MergeImplUtil.showExitWithoutApplyingChangesDialog(this, myMergeRequest, myMergeContext);
      return components;
    }

    @Nullable
    @Override
    public Action getResolveAction(@Nonnull final MergeResult result) {
      if (result == MergeResult.LEFT || result == MergeResult.RIGHT) return null;

      String caption = MergeImplUtil.getResolveActionTitle(result, myMergeRequest, myMergeContext);
      return new AbstractAction(caption) {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (result == MergeResult.RESOLVED) {
            int unresolved = getUnresolvedCount();
            if (unresolved != 0 && Messages.showYesNoDialog(
              getComponent().getRootPane(),
              DiffLocalize.applyPatchPartiallyResolvedChangesConfirmationMessage(unresolved).get(),
              DiffLocalize.applyPartiallyResolvedMergeDialogTitle().get(),
              Messages.getQuestionIcon()
            ) != Messages.YES) {
              return;
            }
          }

          if (result == MergeResult.CANCEL &&
              !MergeImplUtil.showExitWithoutApplyingChangesDialog(MyApplyPatchViewer.this, myMergeRequest, myMergeContext)) {
            return;
          }

          myMergeContext.finishMerge(result);
        }
      };
    }

    private int getUnresolvedCount() {
      int count = 0;
      for (ApplyPatchChange change : getPatchChanges()) {
        if (change.isResolved()) continue;
        count++;
      }
      return count;
    }

    @Override
    protected void onChangeResolved() {
      super.onChangeResolved();

      if (!ContainerUtil.exists(getModelChanges(), (c) -> !c.isResolved())) {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (isDisposed()) return;

          JComponent component = getComponent();
          int yOffset = new RelativePoint(getResultEditor().getComponent(), new Point(0, JBUI.scale(5))).getPoint(component).y;
          RelativePoint point = new RelativePoint(component, new Point(component.getWidth() / 2, yOffset));

          LocalizeValue message = DiffLocalize.applyPatchAllChangesProcessedMessageText();
          AWTDiffUtil.showSuccessPopup(message.get(), point, this, () -> {
            if (isDisposed()) return;
            myMergeContext.finishMerge(MergeResult.RESOLVED);
          });
        });
      }
    }
  }
}

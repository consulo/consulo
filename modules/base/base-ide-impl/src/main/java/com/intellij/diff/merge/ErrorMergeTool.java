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
package com.intellij.diff.merge;

import com.intellij.diff.util.DiffUtil;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ErrorMergeTool implements MergeTool {
  public static final ErrorMergeTool INSTANCE = new ErrorMergeTool();

  @Nonnull
  @Override
  public MergeViewer createComponent(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
    return new MyViewer(context, request);
  }

  @Override
  public boolean canShow(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
    return true;
  }

  private static class MyViewer implements MergeViewer {
    @Nonnull
    private final MergeContext myMergeContext;
    @Nonnull
    private final MergeRequest myMergeRequest;

    @Nonnull
    private final JPanel myPanel;

    public MyViewer(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
      myMergeContext = context;
      myMergeRequest = request;

      myPanel = new JPanel(new BorderLayout());
      myPanel.add(createComponent(), BorderLayout.CENTER);
    }

    @Nonnull
    private JComponent createComponent() {
      return DiffUtil.createMessagePanel("Can't show diff");
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
      return myPanel;
    }

    @javax.annotation.Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
      return null;
    }

    @Nonnull
    @Override
    public ToolbarComponents init() {
      return new ToolbarComponents();
    }

    @javax.annotation.Nullable
    @Override
    public Action getResolveAction(@Nonnull final MergeResult result) {
      if (result == MergeResult.RESOLVED) return null;

      String caption = MergeUtil.getResolveActionTitle(result, myMergeRequest, myMergeContext);
      return new AbstractAction(caption) {
        @Override
        public void actionPerformed(ActionEvent e) {
          myMergeContext.finishMerge(result);
        }
      };
    }

    @Override
    public void dispose() {
    }
  }
}

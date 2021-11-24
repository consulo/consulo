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
package com.intellij.diff.tools.util.side;

import com.intellij.diff.DiffContext;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.tools.holders.EditorHolder;
import com.intellij.diff.tools.holders.EditorHolderFactory;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.FocusTrackerSupport;
import com.intellij.diff.tools.util.SimpleDiffPanel;
import com.intellij.diff.tools.util.base.ListenerDiffViewerBase;
import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.ThreeSide;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.DumbAwareAction;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import com.intellij.pom.Navigatable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public abstract class ThreesideDiffViewer<T extends EditorHolder> extends ListenerDiffViewerBase {
  @Nonnull
  protected final SimpleDiffPanel myPanel;
  @Nonnull
  protected final ThreesideContentPanel myContentPanel;

  @Nonnull
  private final List<T> myHolders;

  @Nonnull
  private final FocusTrackerSupport<ThreeSide> myFocusTrackerSupport;

  public ThreesideDiffViewer(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request, @Nonnull EditorHolderFactory<T> factory) {
    super(context, request);

    myHolders = createEditorHolders(factory);

    myFocusTrackerSupport = new FocusTrackerSupport.Threeside(myHolders);
    myContentPanel = new ThreesideContentPanel.Holders(myHolders);

    myPanel = new SimpleDiffPanel(myContentPanel, this, context);
  }

  @RequiredUIAccess
  @Override
  protected void onInit() {
    super.onInit();
    myPanel.setPersistentNotifications(DiffUtil.getCustomNotifications(myContext, myRequest));
    myContentPanel.setTitles(createTitles());
  }

  @Override
  @RequiredUIAccess
  protected void onDispose() {
    destroyEditorHolders();
    super.onDispose();
  }

  @Override
  @RequiredUIAccess
  protected void processContextHints() {
    super.processContextHints();
    myFocusTrackerSupport.processContextHints(myRequest, myContext);
  }

  @Override
  @RequiredUIAccess
  protected void updateContextHints() {
    super.updateContextHints();
    myFocusTrackerSupport.updateContextHints(myRequest, myContext);
  }

  @Nonnull
  protected List<T> createEditorHolders(@Nonnull EditorHolderFactory<T> factory) {
    List<DiffContent> contents = myRequest.getContents();

    List<T> holders = new ArrayList<>(3);
    for (int i = 0; i < 3; i++) {
      DiffContent content = contents.get(i);
      holders.add(factory.create(content, myContext));
    }
    return holders;
  }

  private void destroyEditorHolders() {
    for (T holder : myHolders) {
      Disposer.dispose(holder);
    }
  }

  @Nonnull
  protected List<JComponent> createTitles() {
    return DiffUtil.createSyncHeightComponents(DiffUtil.createSimpleTitles(myRequest));
  }

  //
  // Getters
  //

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    if (!myPanel.isGoodContent()) return null;
    return getCurrentEditorHolder().getPreferredFocusedComponent();
  }

  @Nonnull
  public ThreeSide getCurrentSide() {
    return myFocusTrackerSupport.getCurrentSide();
  }

  protected void setCurrentSide(@Nonnull ThreeSide side) {
    myFocusTrackerSupport.setCurrentSide(side);
  }

  @Nonnull
  protected List<T> getEditorHolders() {
    return myHolders;
  }

  @Nonnull
  protected T getCurrentEditorHolder() {
    return getCurrentSide().select(getEditorHolders());
  }

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (CommonDataKeys.VIRTUAL_FILE == dataId) {
      return DiffUtil.getVirtualFile(myRequest, getCurrentSide());
    }
    else if (DiffDataKeys.CURRENT_CONTENT == dataId) {
      return getCurrentSide().select(myRequest.getContents());
    }
    return super.getData(dataId);
  }

  //
  // Misc
  //

  @Nullable
  @Override
  protected Navigatable getNavigatable() {
    return getCurrentSide().select(getRequest().getContents()).getNavigatable();
  }

  public static <T extends EditorHolder> boolean canShowRequest(@Nonnull DiffContext context,
                                                                @Nonnull DiffRequest request,
                                                                @Nonnull EditorHolderFactory<T> factory) {
    if (!(request instanceof ContentDiffRequest)) return false;

    List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
    if (contents.size() != 3) return false;

    boolean canShow = true;
    boolean wantShow = false;
    for (DiffContent content : contents) {
      canShow &= factory.canShowContent(content, context);
      wantShow |= factory.wantShowContent(content, context);
    }
    return canShow && wantShow;
  }

  //
  // Actions
  //

  protected enum PartialDiffMode {LEFT_BASE, BASE_RIGHT, LEFT_RIGHT}
  protected class ShowPartialDiffAction extends DumbAwareAction {
    @Nonnull
    protected final ThreeSide mySide1;
    @Nonnull
    protected final ThreeSide mySide2;

    public ShowPartialDiffAction(@Nonnull PartialDiffMode mode) {
      String id;
      switch (mode) {
        case LEFT_BASE:
          mySide1 = ThreeSide.LEFT;
          mySide2 = ThreeSide.BASE;
          id = "Diff.ComparePartial.Base.Left";
          break;
        case BASE_RIGHT:
          mySide1 = ThreeSide.BASE;
          mySide2 = ThreeSide.RIGHT;
          id = "Diff.ComparePartial.Base.Right";
          break;
        case LEFT_RIGHT:
          mySide1 = ThreeSide.LEFT;
          mySide2 = ThreeSide.RIGHT;
          id = "Diff.ComparePartial.Left.Right";
          break;
        default:
          throw new IllegalArgumentException();
      }
      ActionUtil.copyFrom(this, id);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      DiffRequest request = createRequest();
      DiffManager.getInstance().showDiff(myProject, request, new DiffDialogHints(null, myPanel));
    }

    @Nonnull
    protected SimpleDiffRequest createRequest() {
      List<DiffContent> contents = myRequest.getContents();
      List<String> titles = myRequest.getContentTitles();
      return new SimpleDiffRequest(myRequest.getTitle(),
                                   mySide1.select(contents), mySide2.select(contents),
                                   mySide1.select(titles), mySide2.select(titles));
    }
  }
}

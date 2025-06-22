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
package consulo.desktop.awt.internal.diff.util;

import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.diff.content.DiffContent;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.diff.util.ThreeSide;
import consulo.disposer.Disposer;
import consulo.desktop.awt.internal.diff.EditorHolder;
import consulo.desktop.awt.internal.diff.EditorHolderFactory;
import consulo.desktop.awt.internal.diff.util.side.ThreesideContentPanel;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.navigation.Navigatable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

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
    myPanel.setPersistentNotifications(AWTDiffUtil.getCustomNotifications(myContext, myRequest));
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
    return AWTDiffUtil.createSyncHeightComponents(AWTDiffUtil.createSimpleTitles(myRequest));
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
    if (VirtualFile.KEY == dataId) {
      return DiffImplUtil.getVirtualFile(myRequest, getCurrentSide());
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
      ActionImplUtil.copyFrom(this, id);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      DiffRequest request = createRequest();
      DiffManager.getInstance().showDiff(myProject, request, new DiffDialogHints(null, myPanel));
    }

    @Nonnull
    protected SimpleDiffRequest createRequest() {
      List<DiffContent> contents = myRequest.getContents();
      List<String> titles = myRequest.getContentTitles();
      return new SimpleDiffRequest(
        myRequest.getTitle(),
        mySide1.select(contents),
        mySide2.select(contents),
        mySide1.select(titles),
        mySide2.select(titles)
      );
    }
  }
}

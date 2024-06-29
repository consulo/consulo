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
package consulo.ide.impl.idea.diff.tools.util.side;

import consulo.ide.impl.idea.diff.DiffContext;
import consulo.diff.content.DiffContent;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.ide.impl.idea.diff.tools.holders.EditorHolder;
import consulo.ide.impl.idea.diff.tools.holders.EditorHolderFactory;
import consulo.ide.impl.idea.diff.tools.util.DiffDataKeys;
import consulo.ide.impl.idea.diff.tools.util.FocusTrackerSupport;
import consulo.ide.impl.idea.diff.tools.util.SimpleDiffPanel;
import consulo.ide.impl.idea.diff.tools.util.base.ListenerDiffViewerBase;
import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.diff.util.Side;
import consulo.navigation.Navigatable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public abstract class TwosideDiffViewer<T extends EditorHolder> extends ListenerDiffViewerBase {
  @Nonnull
  protected final SimpleDiffPanel myPanel;
  @Nonnull
  protected final TwosideContentPanel myContentPanel;

  @Nonnull
  private final List<T> myHolders;

  @Nonnull
  private final FocusTrackerSupport<Side> myFocusTrackerSupport;

  public TwosideDiffViewer(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request, @Nonnull EditorHolderFactory<T> factory) {
    super(context, request);

    myHolders = createEditorHolders(factory);

    myFocusTrackerSupport = new FocusTrackerSupport.Twoside(myHolders);
    myContentPanel = TwosideContentPanel.createFromHolders(myHolders);

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

  //
  // Editors
  //

  @Nonnull
  protected List<T> createEditorHolders(@Nonnull EditorHolderFactory<T> factory) {
    List<DiffContent> contents = myRequest.getContents();

    List<T> holders = new ArrayList<>(2);
    for (int i = 0; i < 2; i++) {
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
  public Side getCurrentSide() {
    return myFocusTrackerSupport.getCurrentSide();
  }

  protected void setCurrentSide(@Nonnull Side side) {
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
    Navigatable navigatable1 = getCurrentSide().select(getRequest().getContents()).getNavigatable();
    if (navigatable1 != null) return navigatable1;
    return getCurrentSide().other().select(getRequest().getContents()).getNavigatable();
  }

  public static <T extends EditorHolder> boolean canShowRequest(@Nonnull DiffContext context,
                                                                @Nonnull DiffRequest request,
                                                                @Nonnull EditorHolderFactory<T> factory) {
    if (!(request instanceof ContentDiffRequest)) return false;

    List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
    if (contents.size() != 2) return false;

    boolean canShow = true;
    boolean wantShow = false;
    for (DiffContent content : contents) {
      canShow &= factory.canShowContent(content, context);
      wantShow |= factory.wantShowContent(content, context);
    }
    return canShow && wantShow;
  }
}

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
package consulo.diff.impl.internal.viewer.side;

import consulo.dataContext.DataSink;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.diff.content.DiffContent;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.impl.internal.viewer.EditorHolder;
import consulo.diff.impl.internal.viewer.EditorHolderFactory;
import consulo.diff.impl.internal.viewer.FocusTrackerSupport;
import consulo.diff.impl.internal.viewer.ListenerDiffViewerBase;
import consulo.diff.impl.internal.viewer.SimpleDiffPanel;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.diff.util.ThreeSide;
import consulo.disposer.Disposer;
import consulo.navigation.Navigatable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class ThreesideDiffViewer<T extends EditorHolder> extends ListenerDiffViewerBase {
  protected final SimpleDiffPanel myPanel;
  protected final ThreesideContentPanel myContentPanel;

  private final List<T> myHolders;

  private final FocusTrackerSupport<ThreeSide> myFocusTrackerSupport;

  @RequiredUIAccess
  public ThreesideDiffViewer(DiffContext context, ContentDiffRequest request, EditorHolderFactory<T> factory) {
    super(context, request);

    myHolders = createEditorHolders(factory);

    myFocusTrackerSupport = new FocusTrackerSupport.Threeside(myHolders);
    myContentPanel = ThreesideContentPanel.createFromHolders(myHolders, this);

    myPanel = new SimpleDiffPanel(myContentPanel.getComponent(), this, context);
  }

  @RequiredUIAccess
  @Override
  protected void onInit() {
    super.onInit();
    myPanel.setPersistentNotifications(UnifiedDiffUtil.getCustomNotifications(myContext, myRequest));
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

  protected List<T> createEditorHolders(EditorHolderFactory<T> factory) {
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

  @RequiredUIAccess
  protected List<? extends @Nullable Component> createTitles() {
    return UnifiedDiffUtil.createSimpleTitles(myRequest);
  }

  //
  // Getters
  //

  @Override
  public Component getComponent() {
    return myPanel.getComponent();
  }

  @Override
  public @Nullable Component getPreferredFocusedComponent() {
    if (!myPanel.isGoodContent()) return null;
    return getCurrentEditorHolder().getPreferredFocusedComponent();
  }

  public ThreeSide getCurrentSide() {
    return myFocusTrackerSupport.getCurrentSide();
  }

  protected void setCurrentSide(ThreeSide side) {
    myFocusTrackerSupport.setCurrentSide(side);
  }

  protected List<T> getEditorHolders() {
    return myHolders;
  }

  protected T getCurrentEditorHolder() {
    return getCurrentSide().select(getEditorHolders());
  }

  @Override
  public void uiDataSnapshot(DataSink sink) {
    super.uiDataSnapshot(sink);
    sink.lazy(VirtualFile.KEY, () -> DiffImplUtil.getVirtualFile(myRequest, getCurrentSide()));
    sink.lazy(DiffDataKeys.CURRENT_CONTENT, () -> getCurrentSide().select(myRequest.getContents()));
  }

  //
  // Misc
  //

  @Override
  protected @Nullable Navigatable getNavigatable() {
    return getCurrentSide().select(getRequest().getContents()).getNavigatable();
  }

  public static <T extends EditorHolder> boolean canShowRequest(DiffContext context,
                                                                DiffRequest request,
                                                                EditorHolderFactory<T> factory) {
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
    protected final ThreeSide mySide1;
    protected final ThreeSide mySide2;

    public ShowPartialDiffAction(PartialDiffMode mode) {
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

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
      DiffRequest request = createRequest();
      DiffManager.getInstance().showDiff(myProject, request, new DiffDialogHints(null, myPanel.getComponent()));
    }

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

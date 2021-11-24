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
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.EmptyContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.holders.EditorHolder;
import com.intellij.diff.tools.holders.EditorHolderFactory;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.SimpleDiffPanel;
import com.intellij.diff.tools.util.base.ListenerDiffViewerBase;
import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.Side;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.pom.Navigatable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

public abstract class OnesideDiffViewer<T extends EditorHolder> extends ListenerDiffViewerBase {
  @Nonnull
  protected final SimpleDiffPanel myPanel;
  @Nonnull
  protected final OnesideContentPanel myContentPanel;

  @Nonnull
  private final Side mySide;
  @Nonnull
  private final T myHolder;

  public OnesideDiffViewer(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request, @Nonnull EditorHolderFactory<T> factory) {
    super(context, request);

    mySide = Side.fromRight(myRequest.getContents().get(0) instanceof EmptyContent);
    myHolder = createEditorHolder(factory);

    myContentPanel = OnesideContentPanel.createFromHolder(myHolder);

    myPanel = new SimpleDiffPanel(myContentPanel, this, context);
  }

  @Override
  protected void onInit() {
    super.onInit();
    myPanel.setPersistentNotifications(DiffUtil.getCustomNotifications(myContext, myRequest));
    myContentPanel.setTitle(createTitle());
  }

  @Override
  @RequiredUIAccess
  protected void onDispose() {
    destroyEditorHolder();
    super.onDispose();
  }

  //
  // Editors
  //

  @Nonnull
  protected T createEditorHolder(@Nonnull EditorHolderFactory<T> factory) {
    DiffContent content = mySide.select(myRequest.getContents());
    return factory.create(content, myContext);
  }

  private void destroyEditorHolder() {
    Disposer.dispose(myHolder);
  }

  @Nullable
  protected JComponent createTitle() {
    List<JComponent> simpleTitles = DiffUtil.createSimpleTitles(myRequest);
    return mySide.select(simpleTitles);
  }

  //
  // Getters
  //

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @javax.annotation.Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    if (!myPanel.isGoodContent()) return null;
    return getEditorHolder().getPreferredFocusedComponent();
  }

  @Nonnull
  public Side getSide() {
    return mySide;
  }

  @Nonnull
  protected DiffContent getContent() {
    return mySide.select(myRequest.getContents());
  }

  @Nonnull
  protected T getEditorHolder() {
    return myHolder;
  }

  @javax.annotation.Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (CommonDataKeys.VIRTUAL_FILE == dataId) {
      return DiffUtil.getVirtualFile(myRequest, mySide);
    }
    else if (DiffDataKeys.CURRENT_CONTENT == dataId) {
      return getContent();
    }
    return super.getData(dataId);
  }

  //
  // Misc
  //

  @javax.annotation.Nullable
  @Override
  protected Navigatable getNavigatable() {
    return getContent().getNavigatable();
  }

  public static <T extends EditorHolder> boolean canShowRequest(@Nonnull DiffContext context,
                                                                @Nonnull DiffRequest request,
                                                                @Nonnull EditorHolderFactory<T> factory) {
    if (!(request instanceof ContentDiffRequest)) return false;

    List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
    if (contents.size() != 2) return false;

    DiffContent content1 = contents.get(0);
    DiffContent content2 = contents.get(1);

    if (content1 instanceof EmptyContent) {
      return factory.canShowContent(content2, context) && factory.wantShowContent(content2, context);
    }
    if (content2 instanceof EmptyContent) {
      return factory.canShowContent(content1, context) && factory.wantShowContent(content1, context);
    }
    return false;
  }
}

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
import com.intellij.diff.actions.impl.OpenInEditorWithMouseAction;
import com.intellij.diff.actions.impl.SetEditorSettingsAction;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.holders.EditorHolderFactory;
import com.intellij.diff.tools.holders.TextEditorHolder;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.base.InitialScrollPositionSupport;
import com.intellij.diff.tools.util.base.TextDiffSettingsHolder;
import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.LineCol;
import com.intellij.diff.util.Side;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import consulo.util.dataholder.Key;
import com.intellij.pom.Navigatable;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public abstract class OnesideTextDiffViewer extends OnesideDiffViewer<TextEditorHolder> {
  public static final Logger LOG = Logger.getInstance(OnesideTextDiffViewer.class);

  @Nonnull
  private final List<? extends EditorEx> myEditableEditors;

  @Nonnull
  protected final SetEditorSettingsAction myEditorSettingsAction;

  public OnesideTextDiffViewer(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request) {
    super(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);

    myEditableEditors = TextDiffViewerUtil.getEditableEditors(getEditors());

    myEditorSettingsAction = new SetEditorSettingsAction(getTextSettings(), getEditors());
    myEditorSettingsAction.applyDefaults();

    new MyOpenInEditorWithMouseAction().install(getEditors());
  }

  @Override
  @RequiredUIAccess
  protected void onInit() {
    super.onInit();
    installEditorListeners();
  }

  @Override
  @RequiredUIAccess
  protected void onDispose() {
    destroyEditorListeners();
    super.onDispose();
  }

  @Nonnull
  @Override
  protected TextEditorHolder createEditorHolder(@Nonnull EditorHolderFactory<TextEditorHolder> factory) {
    TextEditorHolder holder = super.createEditorHolder(factory);

    boolean[] forceReadOnly = TextDiffViewerUtil.checkForceReadOnly(myContext, myRequest);
    if (forceReadOnly[0]) holder.getEditor().setViewer(true);

    return holder;
  }

  @Nullable
  @Override
  protected JComponent createTitle() {
    List<JComponent> textTitles = DiffUtil.createTextTitles(myRequest, ContainerUtil.list(getEditor(), getEditor()));
    return getSide().select(textTitles);
  }

  //
  // Diff
  //

  @Nonnull
  public TextDiffSettingsHolder.TextDiffSettings getTextSettings() {
    return TextDiffViewerUtil.getTextSettings(myContext);
  }

  @Nonnull
  protected List<AnAction> createEditorPopupActions() {
    return TextDiffViewerUtil.createEditorPopupActions();
  }

  //
  // Listeners
  //

  @RequiredUIAccess
  protected void installEditorListeners() {
    new TextDiffViewerUtil.EditorActionsPopup(createEditorPopupActions()).install(getEditors(), myPanel);
  }

  @RequiredUIAccess
  protected void destroyEditorListeners() {
  }

  //
  // Getters
  //

  @Nonnull
  public List<? extends EditorEx> getEditors() {
    return Collections.singletonList(getEditor());
  }

  @Nonnull
  protected List<? extends EditorEx> getEditableEditors() {
    return myEditableEditors;
  }

  @Nonnull
  public EditorEx getEditor() {
    return getEditorHolder().getEditor();
  }

  @Nonnull
  @Override
  public DocumentContent getContent() {
    //noinspection unchecked
    return (DocumentContent)super.getContent();
  }

  //
  // Abstract
  //

  @RequiredUIAccess
  protected void scrollToLine(int line) {
    DiffUtil.scrollEditor(getEditor(), line, false);
  }

  //
  // Misc
  //

  @Nullable
  @Override
  protected Navigatable getNavigatable() {
    return getContent().getNavigatable(LineCol.fromCaret(getEditor()));
  }

  public static boolean canShowRequest(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return OnesideDiffViewer.canShowRequest(context, request, TextEditorHolder.TextEditorHolderFactory.INSTANCE);
  }

  //
  // Actions
  //

  private class MyOpenInEditorWithMouseAction extends OpenInEditorWithMouseAction {
    @Override
    protected Navigatable getNavigatable(@Nonnull Editor editor, int line) {
      if (editor != getEditor()) return null;
      return getContent().getNavigatable(new LineCol(line));
    }
  }

  //
  // Helpers
  //

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (DiffDataKeys.CURRENT_EDITOR == dataId) {
      return getEditor();
    }
    return super.getData(dataId);
  }

  protected abstract class MyInitialScrollPositionHelper extends InitialScrollPositionSupport.TwosideInitialScrollHelper {
    @Nonnull
    @Override
    protected List<? extends Editor> getEditors() {
      return OnesideTextDiffViewer.this.getEditors();
    }

    @Override
    protected void disableSyncScroll(boolean value) {
    }

    @Override
    protected boolean doScrollToLine() {
      if (myScrollToLine == null) return false;
      Side side = myScrollToLine.first;
      if (side != getSide()) return false;

      scrollToLine(myScrollToLine.second);
      return true;
    }
  }
}

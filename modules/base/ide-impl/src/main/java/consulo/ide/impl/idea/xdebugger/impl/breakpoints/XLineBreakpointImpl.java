/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl.breakpoints;

import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.LazyRangeMarkerFactory;
import consulo.document.RangeMarker;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.execution.debug.*;
import consulo.execution.debug.breakpoint.XBreakpointProperties;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.execution.debug.breakpoint.XLineBreakpointType;
import consulo.execution.debug.breakpoint.XLineBreakpointTypeResolver;
import consulo.execution.debug.ui.DebuggerColors;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.io.File;
import java.util.List;

/**
 * @author nik
 */
public class XLineBreakpointImpl<P extends XBreakpointProperties> extends XBreakpointBase<XLineBreakpoint<P>, P, LineBreakpointState<P>> implements XLineBreakpoint<P> {
  @Nullable
  private RangeMarker myHighlighter;
  private final XLineBreakpointType<P> myType;
  private XSourcePosition mySourcePosition;

  public XLineBreakpointImpl(final XLineBreakpointType<P> type, XBreakpointManagerImpl breakpointManager, @Nullable final P properties, LineBreakpointState<P> state) {
    super(type, breakpointManager, properties, state);
    myType = type;
  }

  XLineBreakpointImpl(final XLineBreakpointType<P> type, XBreakpointManagerImpl breakpointManager, final LineBreakpointState<P> breakpointState) {
    super(type, breakpointManager, breakpointState);
    myType = type;
  }

  public void updateUI() {
    if (isDisposed() || ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    VirtualFile file = getFile();
    if (file == null) {
      return;
    }

    // do not decompile files here
    Document document = FileDocumentManager.getInstance().getCachedDocument(file);
    if (document == null) {
      // currently LazyRangeMarkerFactory creates document for non binary files
      if (file.getFileType().isBinary()) {
        if (myHighlighter == null) {
          myHighlighter = LazyRangeMarkerFactory.getInstance(getProject()).createRangeMarker(file, getLine(), 0, true);
        }
        return;
      }
      document = FileDocumentManager.getInstance().getDocument(file);
      if (document == null) {
        return;
      }
    }

    if (myHighlighter != null && !(myHighlighter instanceof RangeHighlighter)) {
      removeHighlighter();
      myHighlighter = null;
    }

    if (myType instanceof XBreakpointTypeWithDocumentDelegation) {
      document = ((XBreakpointTypeWithDocumentDelegation)myType).getDocumentForHighlighting(document);
    }

    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    TextAttributes attributes = scheme.getAttributes(DebuggerColors.BREAKPOINT_ATTRIBUTES);

    if (!isEnabled()) {
      attributes = attributes.clone();
      attributes.setBackgroundColor(null);
    }

    RangeHighlighter highlighter = (RangeHighlighter)myHighlighter;
    if (highlighter != null && (!highlighter.isValid() || !DocumentUtil.isValidOffset(highlighter.getStartOffset(), document) || !Comparing.equal(highlighter.getTextAttributes(), attributes)
                                // it seems that this check is not needed - we always update line number from the highlighter
                                // and highlighter is removed on line and file change anyway
            /*|| document.getLineNumber(highlighter.getStartOffset()) != getLine()*/)) {
      removeHighlighter();
      highlighter = null;
    }

    MarkupModelEx markupModel;
    if (highlighter == null) {
      markupModel = (MarkupModelEx)DocumentMarkupModel.forDocument(document, getProject(), true);
      TextRange range = myType.getHighlightRange(this);
      if (range != null && !range.isEmpty()) {
        TextRange lineRange = DocumentUtil.getLineTextRange(document, getLine());
        if (range.intersects(lineRange)) {
          highlighter = markupModel.addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), DebuggerColors.BREAKPOINT_HIGHLIGHTER_LAYER, attributes, HighlighterTargetArea.EXACT_RANGE);
        }
      }
      if (highlighter == null) {
        highlighter = markupModel.addPersistentLineHighlighter(getLine(), DebuggerColors.BREAKPOINT_HIGHLIGHTER_LAYER, attributes);
      }
      if (highlighter == null) {
        return;
      }

      highlighter.setGutterIconRenderer(createGutterIconRenderer());
      highlighter.putUserData(DebuggerColors.BREAKPOINT_HIGHLIGHTER_KEY, Boolean.TRUE);
      myHighlighter = highlighter;
    }
    else {
      markupModel = null;
    }

    updateIcon();

    if (markupModel == null) {
      markupModel = (MarkupModelEx)DocumentMarkupModel.forDocument(document, getProject(), false);
      if (markupModel != null) {
        // renderersChanged false - we don't change gutter size
        markupModel.fireAttributesChanged((RangeHighlighterEx)highlighter, false, false);
      }
    }
  }

  @Nullable
  public Document getDocument() {
    VirtualFile file = getFile();
    if (file == null) return null;
    return FileDocumentManager.getInstance().getDocument(file);
  }

  @Nullable
  public VirtualFile getFile() {
    return VirtualFileManager.getInstance().findFileByUrl(getFileUrl());
  }

  @Override
  @Nonnull
  public XLineBreakpointType<P> getType() {
    return myType;
  }

  @Override
  public int getLine() {
    return myState.getLine();
  }

  @Override
  public String getFileUrl() {
    return myState.getFileUrl();
  }

  @Override
  public String getPresentableFilePath() {
    String url = getFileUrl();
    if (url != null && LocalFileSystem.PROTOCOL.equals(VirtualFileManager.extractProtocol(url))) {
      return FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(url));
    }
    return url != null ? url : "";
  }

  @Override
  public String getShortFilePath() {
    final String path = getPresentableFilePath();
    if (path.isEmpty()) return "";
    return new File(path).getName();
  }

  @Nullable
  public RangeHighlighter getHighlighter() {
    return myHighlighter instanceof RangeHighlighter ? (RangeHighlighter)myHighlighter : null;
  }

  @Override
  public XSourcePosition getSourcePosition() {
    if (mySourcePosition != null) {
      return mySourcePosition;
    }
    mySourcePosition = super.getSourcePosition();
    if (mySourcePosition == null) {
      mySourcePosition = XDebuggerUtil.getInstance().createPosition(getFile(), getLine());
    }
    return mySourcePosition;
  }

  @Override
  public boolean isValid() {
    return myHighlighter != null && myHighlighter.isValid();
  }

  @Override
  protected void doDispose() {
    removeHighlighter();
  }

  private void removeHighlighter() {
    if (myHighlighter != null) {
      myHighlighter.dispose();
      myHighlighter = null;
    }
  }

  @Override
  protected GutterDraggableObject createBreakpointDraggableObject() {
    return new GutterDraggableObject() {
      @Override
      public boolean copy(int line, VirtualFile file, int actionId) {
        if (canMoveTo(line, file)) {
          final XBreakpointManager breakpointManager = XDebuggerManager.getInstance(getProject()).getBreakpointManager();
          if (isCopyAction(actionId)) {
            WriteAction.run(() -> ((XBreakpointManagerImpl)breakpointManager).copyLineBreakpoint(XLineBreakpointImpl.this, file.getUrl(), line));
          }
          else {
            setFileUrl(file.getUrl());
            setLine(line, true);
          }
          return true;
        }
        return false;
      }

      @Override
      public void remove() {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(getProject()).getBreakpointManager();
        WriteAction.run(() -> breakpointManager.removeBreakpoint(XLineBreakpointImpl.this));
      }

      @Override
      public Cursor getCursor(int line, int actionId) {
        if (canMoveTo(line, getFile())) {
          return isCopyAction(actionId) ? DragSource.DefaultCopyDrop : DragSource.DefaultMoveDrop;
        }

        return DragSource.DefaultMoveNoDrop;
      }

      private boolean isCopyAction(int actionId) {
        return (actionId & DnDConstants.ACTION_COPY) == DnDConstants.ACTION_COPY;
      }
    };
  }

  private boolean canMoveTo(int line, VirtualFile file) {
    return file != null && XLineBreakpointTypeResolver.forFile(getProject(), file, line) != null && getBreakpointManager().findBreakpointAtLine(myType, file, line) == null;
  }

  public void updatePosition() {
    if (myHighlighter != null && myHighlighter.isValid()) {
      setLine(myHighlighter.getDocument().getLineNumber(myHighlighter.getStartOffset()), false);
      mySourcePosition = null; // need to clear this no matter what as the offset may be cached inside
    }
  }

  public void setFileUrl(final String newUrl) {
    if (!Comparing.equal(getFileUrl(), newUrl)) {
      myState.setFileUrl(newUrl);
      mySourcePosition = null;
      removeHighlighter();
      fireBreakpointChanged();
    }
  }

  private void setLine(final int line, boolean removeHighlighter) {
    if (getLine() != line) {
      myState.setLine(line);
      mySourcePosition = null;
      if (removeHighlighter) {
        removeHighlighter();
      }
      fireBreakpointChanged();
    }
  }

  @Override
  public boolean isTemporary() {
    return myState.isTemporary();
  }

  @Override
  public void setTemporary(boolean temporary) {
    if (isTemporary() != temporary) {
      myState.setTemporary(temporary);
      fireBreakpointChanged();
    }
  }

  @Override
  protected List<? extends AnAction> getAdditionalPopupMenuActions(final XDebugSession session) {
    return getType().getAdditionalPopupMenuActions(this, session);
  }

  @Override
  protected void updateIcon() {
    Image icon = calculateSpecialIcon();
    if (icon == null) {
      icon = isTemporary() ? myType.getTemporaryIcon() : myType.getEnabledIcon();
    }
    setIcon(icon);
  }

  @Override
  public String toString() {
    return "XLineBreakpointImpl(" + myType.getId() + " at " + getShortFilePath() + ":" + getLine() + ")";
  }
}

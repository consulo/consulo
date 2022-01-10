/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.xdebugger.impl.breakpoints;

import com.intellij.codeInsight.folding.impl.FoldingUtil;
import com.intellij.codeInsight.folding.impl.actions.ExpandRegionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.*;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.XDebuggerUtilImpl;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.intellij.xdebugger.impl.breakpoints.ui.BreakpointItem;
import com.intellij.xdebugger.impl.breakpoints.ui.BreakpointPanelProvider;
import consulo.xdebugger.breakpoints.XLineBreakpointResolverTypeExtension;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author nik
 */
public class XBreakpointUtil {
  private XBreakpointUtil() {
  }

  public static <B extends XBreakpoint<?>> String getShortText(B breakpoint) {
    return getType(breakpoint).getShortText(breakpoint);
  }

  public static <B extends XBreakpoint<?>> String getDisplayText(@Nonnull B breakpoint) {
    return getType(breakpoint).getDisplayText(breakpoint);
  }

  public static <B extends XBreakpoint<?>> XBreakpointType<B, ?> getType(@Nonnull B breakpoint) {
    //noinspection unchecked
    return (XBreakpointType<B,?>)breakpoint.getType();
  }

  @Nullable
  public static XBreakpointType<?,?> findType(@Nonnull @NonNls String id) {
    for (XBreakpointType breakpointType : getBreakpointTypes()) {
      if (id.equals(breakpointType.getId())) {
        return breakpointType;
      }
    }
    return null;
  }

  public static List<XBreakpointType> getBreakpointTypes() {
    return XBreakpointType.EXTENSION_POINT_NAME.getExtensionList();
  }

  @Nonnull
  public static Pair<GutterIconRenderer, Object> findSelectedBreakpoint(@Nonnull final Project project, @Nonnull final Editor editor) {
    int offset = editor.getCaretModel().getOffset();
    Document editorDocument = editor.getDocument();

    List<DebuggerSupport> debuggerSupports = DebuggerSupport.getDebuggerSupports();
    for (DebuggerSupport debuggerSupport : debuggerSupports) {
      final BreakpointPanelProvider<?> provider = debuggerSupport.getBreakpointPanelProvider();

      final int textLength = editor.getDocument().getTextLength();
      if (offset > textLength) {
        offset = textLength;
      }

      Object breakpoint = provider.findBreakpoint(project, editorDocument, offset);
      if (breakpoint != null) {
        final GutterIconRenderer iconRenderer = provider.getBreakpointGutterIconRenderer(breakpoint);
        return Pair.create(iconRenderer, breakpoint);
      }
    }
    return Pair.create(null, null);
  }

  public static List<BreakpointPanelProvider> collectPanelProviders() {
    List<BreakpointPanelProvider> panelProviders = new ArrayList<BreakpointPanelProvider>();
    for (DebuggerSupport debuggerSupport : DebuggerSupport.getDebuggerSupports()) {
      panelProviders.add(debuggerSupport.getBreakpointPanelProvider());
    }
    Collections.sort(panelProviders, new Comparator<BreakpointPanelProvider>() {
      @Override
      public int compare(BreakpointPanelProvider o1, BreakpointPanelProvider o2) {
        return o2.getPriority() - o1.getPriority();
      }
    });
    return panelProviders;
  }

  @Nullable
  public static DebuggerSupport getDebuggerSupport(Project project, BreakpointItem breakpointItem) {
    List<BreakpointItem> items = new ArrayList<BreakpointItem>();
    for (DebuggerSupport support : DebuggerSupport.getDebuggerSupports()) {
      support.getBreakpointPanelProvider().provideBreakpointItems(project, items);
      if (items.contains(breakpointItem))
        return support;
      items.clear();
    }
    return null;
  }

  /**
   * Toggle line breakpoint with editor support:
   * - unfolds folded block on the line
   * - if folded, checks if line breakpoints could be toggled inside folded text
   */
  @Nonnull
  public static AsyncResult<XLineBreakpoint> toggleLineBreakpoint(@Nonnull Project project,
                                                                  @Nonnull XSourcePosition position,
                                                                  @Nullable Editor editor,
                                                                  boolean temporary,
                                                                  boolean moveCaret) {
    int lineStart = position.getLine();
    VirtualFile file = position.getFile();
    // for folded text check each line and find out type with the biggest priority
    int linesEnd = lineStart;
    if (editor != null) {
      FoldRegion region = FoldingUtil.findFoldRegionStartingAtLine(editor, lineStart);
      if (region != null && !region.isExpanded()) {
        linesEnd = region.getDocument().getLineNumber(region.getEndOffset());
      }
    }

    final XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
    XLineBreakpointType<?>[] lineTypes = XDebuggerUtil.getInstance().getLineBreakpointTypes();
    XLineBreakpointType<?> typeWinner = null;
    int lineWinner = -1;
    for (int line = lineStart; line <= linesEnd; line++) {
      for (XLineBreakpointType<?> type : lineTypes) {
        final XLineBreakpoint<? extends XBreakpointProperties> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
        if (breakpoint != null && temporary && !breakpoint.isTemporary()) {
          breakpoint.setTemporary(true);
        }
        else if(breakpoint != null) {
          typeWinner = type;
          lineWinner = line;
          break;
        }
      }

      XLineBreakpointType<?> breakpointType = XLineBreakpointResolverTypeExtension.INSTANCE.resolveBreakpointType(project, file, line);
      if(breakpointType != null) {
        typeWinner = breakpointType;
        lineWinner = line;
      }

      // already found max priority type - stop
      if (typeWinner != null) {
        break;
      }
    }

    if (typeWinner != null) {
      XSourcePosition winPosition = (lineStart == lineWinner) ? position : XSourcePositionImpl.create(file, lineWinner);
      if (winPosition != null) {
        AsyncResult<XLineBreakpoint> res =
                XDebuggerUtilImpl.toggleAndReturnLineBreakpoint(project, typeWinner, winPosition, temporary, editor);

        if (editor != null && lineStart != lineWinner) {
          int offset = editor.getDocument().getLineStartOffset(lineWinner);
          ExpandRegionAction.expandRegionAtOffset(project, editor, offset);
          if (moveCaret) {
            editor.getCaretModel().moveToOffset(offset);
          }
        }
        return res;
      }
    }

    return AsyncResult.rejected();
  }
}

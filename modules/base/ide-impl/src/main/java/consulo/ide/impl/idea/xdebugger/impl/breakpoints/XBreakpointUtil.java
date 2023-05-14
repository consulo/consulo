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
package consulo.ide.impl.idea.xdebugger.impl.breakpoints;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.document.Document;
import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.*;
import consulo.ide.impl.idea.codeInsight.folding.impl.FoldingUtil;
import consulo.ide.impl.idea.codeInsight.folding.impl.actions.ExpandRegionAction;
import consulo.ide.impl.idea.xdebugger.impl.DebuggerSupport;
import consulo.ide.impl.idea.xdebugger.impl.XDebuggerUtilImpl;
import consulo.ide.impl.idea.xdebugger.impl.XSourcePositionImpl;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.ui.BreakpointItem;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.ui.BreakpointPanelProvider;
import consulo.project.Project;
import consulo.util.collection.SmartList;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

  @RequiredReadAction
  public static List<XLineBreakpointType> getAvailableLineBreakpointTypes(@NotNull Project project,
                                                                          @NotNull XSourcePosition position,
                                                                          @Nullable Editor editor) {
    return getAvailableLineBreakpointInfo(project, position, editor).first;
  }

  @RequiredReadAction
  private static Pair<List<XLineBreakpointType>, Integer> getAvailableLineBreakpointInfo(@NotNull Project project,
                                                                                         @NotNull XSourcePosition position,
                                                                                         @Nullable Editor editor) {
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
    List<XLineBreakpointType> typeWinner = new SmartList<>();
    int lineWinner = -1;
    if (linesEnd != lineStart) { // folding mode
      for (int line = lineStart; line <= linesEnd; line++) {
        int maxPriority = 0;
        for (XLineBreakpointType<?> type : lineTypes) {
          maxPriority = Math.max(maxPriority, type.getPriority());
          XLineBreakpoint<? extends XBreakpointProperties> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
          if ((canPutAt(type, file, line, project) || breakpoint != null) &&
            (typeWinner.isEmpty() || type.getPriority() > typeWinner.get(0).getPriority())) {
            typeWinner.clear();
            typeWinner.add(type);
            lineWinner = line;
          }
        }
        // already found max priority type - stop
        if (!typeWinner.isEmpty() && typeWinner.get(0).getPriority() == maxPriority) {
          break;
        }
      }
    }
    else {
      for (XLineBreakpointType<?> type : lineTypes) {
        XLineBreakpoint<? extends XBreakpointProperties> breakpoint = breakpointManager.findBreakpointAtLine(type, file, lineStart);
        if ((canPutAt(type, file, lineStart, project) || breakpoint != null)) {
          typeWinner.add(type);
          lineWinner = lineStart;
        }
      }
    }
    return Pair.create(typeWinner, lineWinner);
  }

  @RequiredReadAction
  private static boolean canPutAt(XLineBreakpointType lineBreakpoint, VirtualFile file, int line, Project project) {
    XLineBreakpointType<?> type = XLineBreakpointTypeResolver.forFile(project, file, line);
    return type == lineBreakpoint;
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

      XLineBreakpointType<?> breakpointType = XLineBreakpointTypeResolver.forFile(project, file, line);
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

// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.openapi.diff.impl.settings;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.colorScheme.EditorColorsScheme;
import consulo.diff.content.DiffContent;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.util.ThreeSide;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontSettingsListener;
import consulo.ide.impl.idea.application.options.colors.PreviewPanel;
import consulo.ide.impl.idea.diff.DiffContext;
import consulo.ide.impl.idea.diff.tools.simple.SimpleThreesideDiffChange;
import consulo.ide.impl.idea.diff.tools.simple.SimpleThreesideDiffViewer;
import consulo.ide.impl.idea.diff.tools.util.base.HighlightPolicy;
import consulo.ide.impl.idea.diff.tools.util.base.IgnorePolicy;
import consulo.ide.impl.idea.diff.util.DiffLineSeparatorRenderer;
import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.ide.impl.idea.diff.util.TextDiffTypeFactory.TextDiffTypeImpl;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.project.Project;
import consulo.ui.ex.awt.JBUI;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static consulo.ide.impl.idea.diff.tools.util.base.TextDiffSettingsHolder.TextDiffSettings;

/**
 * The panel from the Settings, that allows to see changes to diff/merge coloring scheme right away.
 */
class DiffPreviewPanel implements PreviewPanel {
  private final JPanel myPanel;
  private final MyViewer myViewer;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  DiffPreviewPanel() {
    myViewer = new MyViewer();
    myViewer.init();

    for (ThreeSide side : ThreeSide.values()) {
      final EditorMouseListener motionListener = new EditorMouseListener(side);
      final EditorClickListener clickListener = new EditorClickListener(side);
      Editor editor = myViewer.getEditor(side);
      editor.addEditorMouseMotionListener(motionListener);
      editor.addEditorMouseListener(clickListener);
      editor.getCaretModel().addCaretListener(clickListener);
    }

    myPanel = JBUI.Panels.simplePanel(myViewer.getComponent());
  }

  @Override
  public Component getPanel() {
    return myPanel;
  }

  @Override
  public void updateView() {
    List<SimpleThreesideDiffChange> changes = myViewer.getChanges();
    for (SimpleThreesideDiffChange change : changes) {
      change.reinstallHighlighters();
    }
    myViewer.repaint();
  }

  public void setColorScheme(final EditorColorsScheme highlighterSettings) {
    for (EditorEx editorEx : myViewer.getEditors()) {
      editorEx.setColorsScheme(editorEx.createBoundColorSchemeDelegate(highlighterSettings));
      editorEx.getColorsScheme().setAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES, null);
      editorEx.reinitSettings();
    }
  }

  private static class SampleRequest extends ContentDiffRequest {
    private final List<DiffContent> myContents;

    SampleRequest() {
      myContents = Arrays.asList(DiffPreviewProvider.getContents());
    }

    @Nonnull
    @Override
    public List<DiffContent> getContents() {
      return myContents;
    }

    @Nonnull
    @Override
    public List<String> getContentTitles() {
      return Arrays.asList(null, null, null);
    }

    @Nullable
    @Override
    public String getTitle() {
      return DiffLocalize.mergeColorOptionsDialogTitle().get();
    }
  }

  private static class SampleContext extends DiffContext {
    SampleContext() {
      TextDiffSettings settings = new TextDiffSettings();
      settings.setHighlightPolicy(HighlightPolicy.BY_WORD);
      settings.setIgnorePolicy(IgnorePolicy.IGNORE_WHITESPACES);
      settings.setContextRange(2);
      settings.setExpandByDefault(false);
      putUserData(TextDiffSettings.KEY, settings);
    }

    @Nullable
    @Override
    public Project getProject() {
      return null;
    }

    @Override
    public boolean isWindowFocused() {
      return false;
    }

    @Override
    public boolean isFocused() {
      return false;
    }

    @Override
    public void requestFocus() {

    }
  }

  @Override
  public void addListener(@Nonnull final ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  private final class EditorMouseListener implements EditorMouseMotionListener {
    @Nonnull
    private final ThreeSide mySide;

    private EditorMouseListener(@Nonnull ThreeSide side) {
      mySide = side;
    }

    @Override
    public void mouseMoved(@Nonnull EditorMouseEvent e) {
      int line = e.getLogicalPosition().line;
      Cursor cursor = getChange(mySide, line) != null || getFoldRegion(mySide, line) != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null;
      ((EditorEx)e.getEditor()).setCustomCursor(DiffPreviewPanel.class, cursor);
    }
  }

  private final class EditorClickListener implements CaretListener, consulo.codeEditor.event.EditorMouseListener {
    @Nonnull
    private final ThreeSide mySide;

    private EditorClickListener(@Nonnull ThreeSide side) {
      mySide = side;
    }

    @Override
    public void mouseClicked(@Nonnull EditorMouseEvent e) {
      selectColorForLine(mySide, e.getLogicalPosition().line);
    }

    @Override
    public void caretPositionChanged(@Nonnull CaretEvent e) {
      selectColorForLine(mySide, e.getNewPosition().line);
    }
  }

  private void selectColorForLine(@Nonnull ThreeSide side, int line) {
    SimpleThreesideDiffChange change = getChange(side, line);
    if (change != null) {
      TextDiffTypeImpl diffType = ObjectUtil.tryCast(change.getDiffType(), TextDiffTypeImpl.class);
      if (diffType != null) {
        myDispatcher.getMulticaster().selectionInPreviewChanged(diffType.getKey().getExternalName());
      }
      return;
    }

    FoldRegion region = getFoldRegion(side, line);
    if (region != null) {
      myDispatcher.getMulticaster().selectionInPreviewChanged(DiffLineSeparatorRenderer.BACKGROUND.getExternalName());
    }
  }

  @Nullable
  private SimpleThreesideDiffChange getChange(@Nonnull ThreeSide side, int line) {
    for (SimpleThreesideDiffChange change : myViewer.getChanges()) {
      int startLine = change.getStartLine(side);
      int endLine = change.getEndLine(side);
      if (DiffUtil.isSelectedByLine(line, startLine, endLine) && change.isChange(side)) {
        return change;
      }
    }
    return null;
  }

  @Nullable
  private FoldRegion getFoldRegion(@Nonnull ThreeSide side, int line) {
    EditorEx editor = myViewer.getEditor(side);
    Document document = editor.getDocument();
    for (FoldRegion region : editor.getFoldingModel().getAllFoldRegions()) {
      if (region.isExpanded()) continue;
      int line1 = document.getLineNumber(region.getStartOffset());
      int line2 = document.getLineNumber(region.getEndOffset());
      if (line1 <= line && line <= line2) return region;
    }
    return null;
  }

  @Override
  public void blinkSelectedHighlightType(final Object selected) {
  }

  @Override
  public void disposeUIResources() {
    Disposer.dispose(myViewer);
  }

  @Nonnull
  @TestOnly
  public SimpleThreesideDiffViewer testGetViewer() {
    return myViewer;
  }

  private static class MyViewer extends SimpleThreesideDiffViewer {
    MyViewer() {
      super(new SampleContext(), new SampleRequest());
    }

    @Override
    protected boolean forceRediffSynchronously() {
      return true;
    }

    public void repaint() {
      myPanel.repaint();
    }
  }
}

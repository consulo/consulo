// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.breadcrumbs;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.ui.UISettings;
import consulo.application.impl.internal.concurent.NonUrgentExecutor;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.impl.ComplementaryFontsRegistry;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.breadcrumbs.FileBreadcrumbsCollector;
import consulo.ide.impl.idea.ui.components.breadcrumbs.Crumb;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.highlight.HighlightManager;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.MouseEventAdapter;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import kava.beans.PropertyChangeEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static consulo.ui.ex.awt.RelativeFont.SMALL;
import static consulo.ui.ex.awt.UIUtil.getLabelFont;

/**
 * @author spleaner
 */
public class BreadcrumbsWrapper extends JComponent implements Disposable {
  final PsiBreadcrumbs breadcrumbs = new PsiBreadcrumbs();

  private final Project myProject;
  private Editor myEditor;
  private Collection<RangeHighlighter> myHighlighed;
  private final VirtualFile myFile;
  private boolean myUserCaretChange = true;
  private final MergingUpdateQueue myQueue = new MergingUpdateQueue("Breadcrumbs.Queue", 200, true, breadcrumbs);

  private final List<BreadcrumbListener> myBreadcrumbListeners = new ArrayList<>();

  private final Update myUpdate = new Update(this) {
    @Override
    public void run() {
      updateCrumbs();
    }

    @Override
    public boolean canEat(final Update update) {
      return true;
    }
  };

  private final FileBreadcrumbsCollector myBreadcrumbsCollector;

  public static final Key<BreadcrumbsWrapper> BREADCRUMBS_COMPONENT_KEY = new Key<>("BREADCRUMBS_KEY");
  private static final Iterable<? extends Crumb> EMPTY_BREADCRUMBS = ContainerUtil.emptyIterable();

  public BreadcrumbsWrapper(@Nonnull final Editor editor) {
    myEditor = editor;
    myEditor.putUserData(BREADCRUMBS_COMPONENT_KEY, this);
    if (editor instanceof EditorEx) {
      ((EditorEx)editor).addPropertyChangeListener(this::updateEditorFont, this);
    }

    final Project project = editor.getProject();
    assert project != null;
    myProject = project;

    myFile = FileDocumentManager.getInstance().getFile(myEditor.getDocument());

    final FileStatusManager manager = FileStatusManager.getInstance(project);
    manager.addFileStatusListener(new FileStatusListener() {
      @Override
      public void fileStatusesChanged() {
        queueUpdate();
      }
    }, this);

    final CaretListener caretListener = new CaretListener() {
      @Override
      public void caretPositionChanged(@Nonnull final CaretEvent e) {
        if (myUserCaretChange) {
          queueUpdate();
        }

        myUserCaretChange = true;
      }
    };

    editor.getCaretModel().addCaretListener(caretListener, this);

    myBreadcrumbsCollector = FileBreadcrumbsCollector.findBreadcrumbsCollector(myProject, myFile);
    if (myFile != null) {
      myBreadcrumbsCollector.watchForChanges(myFile, editor, this, () -> queueUpdate());
    }

    breadcrumbs.onHover(this::itemHovered);
    breadcrumbs.onSelect(this::itemSelected);
    breadcrumbs.setFont(getNewFont(myEditor));

    JScrollPane pane = ScrollPaneFactory.createScrollPane(breadcrumbs, true);
    pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    pane.getHorizontalScrollBar().setEnabled(false);
    setLayout(new BorderLayout());
    add(BorderLayout.CENTER, pane);

    EditorGutter gutter = editor.getGutter();
    if (gutter instanceof EditorGutterComponentEx) {
      EditorGutterComponentEx gutterComponent = (EditorGutterComponentEx)gutter;
      MouseEventAdapter mouseListener = new MouseEventAdapter<EditorGutterComponentEx>(gutterComponent) {
        @Nonnull
        @Override
        protected MouseEvent convert(@Nonnull MouseEvent event) {
          return convert(event, gutterComponent.getComponent());
        }
      };
      ComponentAdapter resizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent event) {
          breadcrumbs.updateBorder(gutterComponent.getWhitespaceSeparatorOffset());
          breadcrumbs.setFont(getNewFont(myEditor));
        }
      };

      addComponentListener(resizeListener);
      gutterComponent.getComponent().addComponentListener(resizeListener);
      breadcrumbs.addMouseListener(mouseListener);
      Disposer.register(this, () -> {
        removeComponentListener(resizeListener);
        gutterComponent.getComponent().removeComponentListener(resizeListener);
        breadcrumbs.removeMouseListener(mouseListener);
      });
      breadcrumbs.updateBorder(gutterComponent.getWhitespaceSeparatorOffset());
    }
    else {
      breadcrumbs.updateBorder(0);
    }
    Disposer.register(this, new UiNotifyConnector(breadcrumbs, myQueue));
    Disposer.register(this, myQueue);

    if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
      myQueue.setPassThrough(true);
    }

    queueUpdate();
  }

  private void updateCrumbs() {
    if (myEditor == null || myFile == null || myEditor.isDisposed()) return;

    Document document = myEditor.getDocument();
    int offset = myEditor.getCaretModel().getOffset();
    Boolean forcedShown = BreadcrumbsForceShownSettings.getForcedShown(myEditor);
    ReadAction.nonBlocking(() -> myBreadcrumbsCollector.computeCrumbs(myFile, document, offset, forcedShown)).withDocumentsCommitted(myProject).expireWith(this).coalesceBy(this)
            .finishOnUiThread(Application::getAnyModalityState, (_crumbs) -> {
              Iterable<? extends Crumb> crumbs = breadcrumbs.isShowing() || ApplicationManager.getApplication().isHeadlessEnvironment() ? _crumbs : EMPTY_BREADCRUMBS;
              breadcrumbs.setFont(getNewFont(myEditor));
              breadcrumbs.setCrumbs(crumbs);
              notifyListeners(crumbs);
            }).submit(NonUrgentExecutor.getInstance());
  }

  public void queueUpdate() {
    myQueue.cancelAllUpdates();
    myQueue.queue(myUpdate);
  }

  public void addBreadcrumbListener(BreadcrumbListener listener, Disposable parentDisposable) {
    myBreadcrumbListeners.add(listener);
    Disposer.register(parentDisposable, () -> myBreadcrumbListeners.remove(listener));
  }

  private void notifyListeners(@Nonnull Iterable<? extends Crumb> breadcrumbs) {
    for (BreadcrumbListener listener : myBreadcrumbListeners) {
      listener.breadcrumbsChanged(breadcrumbs);
    }
  }

  @Deprecated
  public JComponent getComponent() {
    return this;
  }

  private void itemSelected(Crumb crumb, InputEvent event) {
    if (event == null || !(crumb instanceof NavigatableCrumb)) return;
    NavigatableCrumb navigatableCrumb = (NavigatableCrumb)crumb;
    navigate(navigatableCrumb, event.isShiftDown() || event.isMetaDown());
  }

  public void navigate(NavigatableCrumb crumb, boolean withSelection) {
    myUserCaretChange = false;
    crumb.navigate(myEditor, withSelection);
  }

  private void itemHovered(Crumb crumb, @SuppressWarnings("unused") InputEvent event) {
    if (!Registry.is("editor.breadcrumbs.highlight.on.hover")) {
      return;
    }

    HighlightManager hm = HighlightManager.getInstance(myProject);
    if (myHighlighed != null) {
      for (RangeHighlighter highlighter : myHighlighed) {
        hm.removeSegmentHighlighter(myEditor, highlighter);
      }
      myHighlighed = null;
    }
    if (crumb instanceof NavigatableCrumb) {
      final TextRange range = ((NavigatableCrumb)crumb).getHighlightRange();
      if (range == null) return;
      final TextAttributes attributes = new TextAttributes();
      final CrumbPresentation p = PsiCrumb.getPresentation(crumb);
      ColorValue color = p == null ? null : p.getBackgroundColor(false, false, false);
      if (color == null) color = BreadcrumbsComponent.ButtonSettings.getBackgroundColor(false, false, false, false);
      if (color == null) color = TargetAWT.from(UIUtil.getLabelBackground());
      final ColorValue background = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.CARET_ROW_COLOR);
      attributes.setBackgroundColor(makeTransparent(color, background != null ? background : new RGBColor(200, 200, 200), 0.3));
      myHighlighed = new ArrayList<>(1);
      int flags = HighlightManager.HIDE_BY_ESCAPE | HighlightManager.HIDE_BY_TEXT_CHANGE | HighlightManager.HIDE_BY_ANY_KEY;
      hm.addOccurrenceHighlight(myEditor, range.getStartOffset(), range.getEndOffset(), attributes, flags, myHighlighed, null);
    }
  }

  private static RGBColor makeTransparent(@Nonnull ColorValue c, @Nonnull ColorValue cb, double transparency) {
    RGBColor color = c.toRGB();
    RGBColor backgroundColor = cb.toRGB();
    
    int r = makeTransparent(transparency, color.getRed(), backgroundColor.getRed());
    int g = makeTransparent(transparency, color.getGreen(), backgroundColor.getGreen());
    int b = makeTransparent(transparency, color.getBlue(), backgroundColor.getBlue());

    return new RGBColor(r, g, b);
  }

  private static int makeTransparent(double transparency, int channel, int backgroundChannel) {
    final int result = (int)(backgroundChannel * (1 - transparency) + channel * transparency);
    if (result < 0) {
      return 0;
    }
    if (result > 255) {
      return 255;
    }
    return result;
  }

  @Nullable
  public static BreadcrumbsWrapper getBreadcrumbsComponent(@Nonnull Editor editor) {
    return editor.getUserData(BREADCRUMBS_COMPONENT_KEY);
  }

  @Override
  public void dispose() {
    if (myEditor != null) {
      myEditor.putUserData(BREADCRUMBS_COMPONENT_KEY, null);
    }
    myEditor = null;
    breadcrumbs.setCrumbs(EMPTY_BREADCRUMBS);
    notifyListeners(EMPTY_BREADCRUMBS);
  }

  private void updateEditorFont(PropertyChangeEvent event) {
    if (EditorEx.PROP_FONT_SIZE.equals(event.getPropertyName())) queueUpdate();
  }

  private static Font getNewFont(Editor editor) {
    Font font = editor == null || Registry.is("editor.breadcrumbs.system.font") ? getLabelFont() : getEditorFont(editor);
    return UISettings.getInstance().getUseSmallLabelsOnTabs() ? SMALL.derive(font) : font;
  }

  private static Font getEditorFont(Editor editor) {
    return ComplementaryFontsRegistry.getFontAbleToDisplay('a', Font.PLAIN, editor.getColorsScheme().getFontPreferences(), null).getFont();
  }
}

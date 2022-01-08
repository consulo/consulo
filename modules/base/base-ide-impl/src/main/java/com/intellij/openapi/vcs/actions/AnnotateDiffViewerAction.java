/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.actions;

import com.intellij.diff.DiffContext;
import com.intellij.diff.DiffContextEx;
import com.intellij.diff.DiffExtension;
import com.intellij.diff.FrameDiffTool.DiffViewer;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.FileContent;
import com.intellij.diff.merge.TextMergeViewer;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer;
import com.intellij.diff.tools.simple.SimpleThreesideDiffViewer;
import com.intellij.diff.tools.simple.ThreesideTextDiffViewerEx;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.base.DiffViewerBase;
import com.intellij.diff.tools.util.base.DiffViewerListener;
import com.intellij.diff.tools.util.side.OnesideTextDiffViewer;
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer;
import com.intellij.diff.util.DiffUserDataKeysEx;
import com.intellij.diff.util.Side;
import com.intellij.diff.util.ThreeSide;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationsManagerImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.localVcs.UpToDateLineNumberProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProducer;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.impl.BackgroundableActionLock;
import com.intellij.openapi.vcs.impl.UpToDateLineNumberProviderImpl;
import com.intellij.openapi.vcs.impl.VcsBackgroundableActions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.AnnotationProviderEx;
import com.intellij.vcsUtil.VcsUtil;
import consulo.awt.TargetAWT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class AnnotateDiffViewerAction extends ToggleAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(AnnotateDiffViewerAction.class);

  private static final Key<boolean[]> ANNOTATIONS_SHOWN_KEY = Key.create("Diff.AnnotateAction.AnnotationShown");

  private static final ViewerAnnotatorFactory[] ANNOTATORS =
          new ViewerAnnotatorFactory[]{new TwosideAnnotatorFactory(), new OnesideAnnotatorFactory(), new UnifiedAnnotatorFactory(), new ThreesideAnnotatorFactory(), new TextMergeAnnotatorFactory()};

  public AnnotateDiffViewerAction() {
    ActionUtil.copyFrom(this, "Annotate");
    setEnabledInModalContext(true);
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    boolean enabled = isEnabled(e);
    e.getPresentation().setVisible(enabled);
    e.getPresentation().setEnabled(enabled && !isSuspended(e));
  }

  @javax.annotation.Nullable
  @SuppressWarnings("unchecked")
  private static ViewerAnnotator getAnnotator(@Nonnull DiffViewerBase viewer, @Nonnull Editor editor) {
    for (ViewerAnnotatorFactory annotator : ANNOTATORS) {
      if (annotator.getViewerClass().isInstance(viewer)) return annotator.createAnnotator(viewer, editor);
    }
    return null;
  }

  @javax.annotation.Nullable
  private static EventData collectEventData(AnActionEvent e) {
    DiffViewerBase viewer = getViewer(e);
    if (viewer == null) return null;
    if (viewer.getProject() == null) return null;
    if (viewer.isDisposed()) return null;

    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (editor == null) return null;

    ViewerAnnotator annotator = getAnnotator(viewer, editor);
    if (annotator == null) return null;

    return new EventData(viewer, annotator);
  }

  @javax.annotation.Nullable
  private static DiffViewerBase getViewer(AnActionEvent e) {
    DiffViewerBase diffViewer = ObjectUtils.tryCast(e.getData(DiffDataKeys.DIFF_VIEWER), DiffViewerBase.class);
    if (diffViewer != null) return diffViewer;

    TextMergeViewer mergeViewer = ObjectUtils.tryCast(e.getData(DiffDataKeys.MERGE_VIEWER), TextMergeViewer.class);
    if (mergeViewer != null) return mergeViewer.getViewer();

    return null;
  }

  private static boolean isEnabled(AnActionEvent e) {
    EventData data = collectEventData(e);
    if (data == null) return false;

    if (data.annotator.isAnnotationShown()) return true;
    return data.annotator.createAnnotationsLoader() != null;
  }

  private static boolean isSuspended(AnActionEvent e) {
    EventData data = collectEventData(e);
    return data != null && data.annotator.getBackgroundableLock().isLocked();
  }

  private static boolean isAnnotated(AnActionEvent e) {
    EventData data = collectEventData(e);
    assert data != null;
    return data.annotator.isAnnotationShown();
  }

  private static void perform(AnActionEvent e, boolean selected) {
    EventData data = collectEventData(e);
    assert data != null;

    boolean annotationShown = data.annotator.isAnnotationShown();
    if (annotationShown) {
      data.annotator.hideAnnotation();
    }
    else {
      doAnnotate(data.annotator);
    }
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    EventData data = collectEventData(e);
    return data != null && data.annotator.isAnnotationShown();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    perform(e, state);
  }

  private static void doAnnotate(@Nonnull final ViewerAnnotator annotator) {
    final DiffViewerBase viewer = annotator.getViewer();
    final Project project = viewer.getProject();
    if (project == null) return;

    final FileAnnotationLoader loader = annotator.createAnnotationsLoader();
    if (loader == null) return;

    final DiffContextEx diffContext = ObjectUtils.tryCast(viewer.getContext(), DiffContextEx.class);

    annotator.getBackgroundableLock().lock();
    if (diffContext != null) diffContext.showProgressBar(true);

    BackgroundTaskUtil.executeOnPooledThread(viewer, () -> {
      try {
        loader.run();
      }
      finally {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (diffContext != null) diffContext.showProgressBar(false);
          annotator.getBackgroundableLock().unlock();

          VcsException exception = loader.getException();
          if (exception != null) {
            Notification notification = VcsNotifier.IMPORTANT_ERROR_NOTIFICATION.createNotification("Can't Load Annotations", exception.getMessage(), NotificationType.ERROR, null);
            showNotification(viewer, notification);
            LOG.warn(exception);
            return;
          }

          if (loader.getResult() == null) return;
          if (viewer.isDisposed()) return;

          annotator.showAnnotation(loader.getResult());
        }, ProgressManager.getGlobalProgressIndicator().getModalityState());
      }
    });
  }

  @javax.annotation.Nullable
  private static FileAnnotationLoader createThreesideAnnotationsLoader(@Nonnull Project project, @Nonnull DiffRequest request, @Nonnull ThreeSide side) {
    if (request instanceof ContentDiffRequest) {
      ContentDiffRequest requestEx = (ContentDiffRequest)request;
      if (requestEx.getContents().size() == 3) {
        DiffContent content = side.select(requestEx.getContents());
        FileAnnotationLoader loader = createAnnotationsLoader(project, content);
        if (loader != null) return loader;
      }
    }

    return null;
  }

  @javax.annotation.Nullable
  private static FileAnnotationLoader createTwosideAnnotationsLoader(@Nonnull Project project, @Nonnull DiffRequest request, @Nonnull Side side) {
    Change change = request.getUserData(ChangeDiffRequestProducer.CHANGE_KEY);
    if (change != null) {
      ContentRevision revision = side.select(change.getBeforeRevision(), change.getAfterRevision());
      if (revision != null) {
        AbstractVcs vcs = ChangesUtil.getVcsForChange(change, project);

        if (revision instanceof CurrentContentRevision) {
          VirtualFile file = ((CurrentContentRevision)revision).getVirtualFile();
          FileAnnotationLoader loader = doCreateAnnotationsLoader(project, vcs, file);
          if (loader != null) return loader;
        }
        else {
          FileAnnotationLoader loader = doCreateAnnotationsLoader(vcs, revision.getFile(), revision.getRevisionNumber());
          if (loader != null) return loader;
        }
      }
    }

    if (request instanceof ContentDiffRequest) {
      ContentDiffRequest requestEx = (ContentDiffRequest)request;
      if (requestEx.getContents().size() == 2) {
        DiffContent content = side.select(requestEx.getContents());
        return createAnnotationsLoader(project, content);
      }
    }

    return null;
  }

  @javax.annotation.Nullable
  private static FileAnnotationLoader createAnnotationsLoader(@Nonnull Project project, @Nonnull DiffContent content) {
    if (content instanceof FileContent) {
      VirtualFile file = ((FileContent)content).getFile();
      AbstractVcs vcs = VcsUtil.getVcsFor(project, file);
      FileAnnotationLoader loader = doCreateAnnotationsLoader(project, vcs, file);
      if (loader != null) return loader;
    }

    Pair<FilePath, VcsRevisionNumber> info = content.getUserData(DiffUserDataKeysEx.REVISION_INFO);
    if (info != null) {
      FilePath filePath = info.first;
      AbstractVcs vcs = VcsUtil.getVcsFor(project, filePath);
      FileAnnotationLoader loader = doCreateAnnotationsLoader(vcs, filePath, info.second);
      if (loader != null) return loader;
    }
    return null;
  }

  @javax.annotation.Nullable
  private static FileAnnotationLoader doCreateAnnotationsLoader(@Nonnull Project project, @javax.annotation.Nullable AbstractVcs vcs, @javax.annotation.Nullable final VirtualFile file) {
    if (vcs == null || file == null) return null;
    final AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
    if (annotationProvider == null) return null;

    FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
    if (fileStatus == FileStatus.UNKNOWN || fileStatus == FileStatus.ADDED || fileStatus == FileStatus.IGNORED) {
      return null;
    }

    return new FileAnnotationLoader(vcs) {
      @Override
      public FileAnnotation compute() throws VcsException {
        return annotationProvider.annotate(file);
      }
    };
  }

  @javax.annotation.Nullable
  private static FileAnnotationLoader doCreateAnnotationsLoader(@javax.annotation.Nullable AbstractVcs vcs,
                                                                @javax.annotation.Nullable final FilePath path,
                                                                @javax.annotation.Nullable final VcsRevisionNumber revisionNumber) {
    if (vcs == null || path == null || revisionNumber == null) return null;
    if (revisionNumber instanceof TextRevisionNumber) return null;
    final AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
    if (!(annotationProvider instanceof AnnotationProviderEx)) return null;

    return new FileAnnotationLoader(vcs) {
      @Override
      public FileAnnotation compute() throws VcsException {
        return ((AnnotationProviderEx)annotationProvider).annotate(path, revisionNumber);
      }
    };
  }

  public static class MyDiffExtension extends DiffExtension {
    @Override
    public void onViewerCreated(@Nonnull DiffViewer diffViewer, @Nonnull DiffContext context, @Nonnull DiffRequest request) {
      if (diffViewer instanceof DiffViewerBase) {
        DiffViewerBase viewer = (DiffViewerBase)diffViewer;
        viewer.addListener(new MyDiffViewerListener(viewer));
      }
    }
  }

  private static class MyDiffViewerListener extends DiffViewerListener {
    @Nonnull
    private final DiffViewerBase myViewer;

    public MyDiffViewerListener(@Nonnull DiffViewerBase viewer) {
      myViewer = viewer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onInit() {
      if (myViewer.getProject() == null) return;

      for (ViewerAnnotatorFactory annotator : ANNOTATORS) {
        if (annotator.getViewerClass().isInstance(myViewer)) annotator.showRememberedAnnotations(myViewer);
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onDispose() {
      if (myViewer.getProject() == null) return;

      for (ViewerAnnotatorFactory annotator : ANNOTATORS) {
        if (annotator.getViewerClass().isInstance(myViewer)) annotator.rememberShownAnnotations(myViewer);
      }
    }
  }

  private static void showNotification(@Nonnull DiffViewerBase viewer, @Nonnull Notification notification) {
    JComponent component = viewer.getComponent();

    Window awtWindow = UIUtil.getWindow(component);

    if (awtWindow != null) {
      consulo.ui.Window uiWindow = TargetAWT.from(awtWindow);

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);

      if (ideFrame != null && NotificationsManagerImpl.findWindowForBalloon(viewer.getProject()) == awtWindow) {
        notification.notify(viewer.getProject());
        return;
      }
    }

    Balloon balloon = NotificationsManagerImpl.createBalloon(component, notification, false, true, null, viewer);

    Dimension componentSize = component.getSize();
    Dimension balloonSize = balloon.getPreferredSize();

    int width = Math.min(balloonSize.width, componentSize.width);
    int height = Math.min(balloonSize.height, componentSize.height);

    // top-right corner, 20px to the edges
    RelativePoint point = new RelativePoint(component, new Point(componentSize.width - 20 - width / 2, 20 + height / 2));
    balloon.show(point, Balloon.Position.above);
  }

  private static class TwosideAnnotatorFactory extends TwosideViewerAnnotatorFactory<TwosideTextDiffViewer> {
    @Override
    @Nonnull
    public Class<TwosideTextDiffViewer> getViewerClass() {
      return TwosideTextDiffViewer.class;
    }

    @Override
    @javax.annotation.Nullable
    public Side getCurrentSide(@Nonnull TwosideTextDiffViewer viewer, @Nonnull Editor editor) {
      Side side = null; // we can't just use getCurrentSide() here, popup can be called on unfocused editor
      if (viewer.getEditor(Side.LEFT) == editor) side = Side.LEFT;
      if (viewer.getEditor(Side.RIGHT) == editor) side = Side.RIGHT;
      return side;
    }

    @Override
    public boolean isAnnotationShown(@Nonnull TwosideTextDiffViewer viewer, @Nonnull Side side) {
      return viewer.getEditor(side).getGutter().isAnnotationsShown();
    }

    @Override
    public void showAnnotation(@Nonnull TwosideTextDiffViewer viewer, @Nonnull Side side, @Nonnull AnnotationData data) {
      Project project = ObjectUtils.assertNotNull(viewer.getProject());
      AnnotateToggleAction.doAnnotate(viewer.getEditor(side), project, null, data.annotation, data.vcs);
    }

    @Override
    public void hideAnnotation(@Nonnull TwosideTextDiffViewer viewer, @Nonnull Side side) {
      viewer.getEditor(side).getGutter().closeAllAnnotations();
    }
  }

  private static class OnesideAnnotatorFactory extends TwosideViewerAnnotatorFactory<OnesideTextDiffViewer> {
    @Override
    @Nonnull
    public Class<OnesideTextDiffViewer> getViewerClass() {
      return OnesideTextDiffViewer.class;
    }

    @Override
    @javax.annotation.Nullable
    public Side getCurrentSide(@Nonnull OnesideTextDiffViewer viewer, @Nonnull Editor editor) {
      if (viewer.getEditor() != editor) return null;
      return viewer.getSide();
    }

    @Override
    public boolean isAnnotationShown(@Nonnull OnesideTextDiffViewer viewer, @Nonnull Side side) {
      if (side != viewer.getSide()) return false;
      return viewer.getEditor().getGutter().isAnnotationsShown();
    }

    @Override
    public void showAnnotation(@Nonnull OnesideTextDiffViewer viewer, @Nonnull Side side, @Nonnull AnnotationData data) {
      if (side != viewer.getSide()) return;
      Project project = ObjectUtils.assertNotNull(viewer.getProject());
      AnnotateToggleAction.doAnnotate(viewer.getEditor(), project, null, data.annotation, data.vcs);
    }

    @Override
    public void hideAnnotation(@Nonnull OnesideTextDiffViewer viewer, @Nonnull Side side) {
      viewer.getEditor().getGutter().closeAllAnnotations();
    }
  }

  private static class UnifiedAnnotatorFactory extends TwosideViewerAnnotatorFactory<UnifiedDiffViewer> {
    @Override
    @Nonnull
    public Class<UnifiedDiffViewer> getViewerClass() {
      return UnifiedDiffViewer.class;
    }

    @Override
    @javax.annotation.Nullable
    public Side getCurrentSide(@Nonnull UnifiedDiffViewer viewer, @Nonnull Editor editor) {
      if (viewer.getEditor() != editor) return null;
      return viewer.getMasterSide();
    }

    @Override
    public boolean isAnnotationShown(@Nonnull UnifiedDiffViewer viewer, @Nonnull Side side) {
      if (side != viewer.getMasterSide()) return false;
      return viewer.getEditor().getGutter().isAnnotationsShown();
    }

    @Override
    public void showAnnotation(@Nonnull UnifiedDiffViewer viewer, @Nonnull Side side, @Nonnull AnnotationData data) {
      if (side != viewer.getMasterSide()) return;
      Project project = ObjectUtils.assertNotNull(viewer.getProject());
      UnifiedUpToDateLineNumberProvider lineNumberProvider = new UnifiedUpToDateLineNumberProvider(viewer, side);
      AnnotateToggleAction.doAnnotate(viewer.getEditor(), project, null, data.annotation, data.vcs, lineNumberProvider);
    }

    @Override
    public void hideAnnotation(@Nonnull UnifiedDiffViewer viewer, @Nonnull Side side) {
      viewer.getEditor().getGutter().closeAllAnnotations();
    }
  }

  private static class UnifiedUpToDateLineNumberProvider implements UpToDateLineNumberProvider {
    @Nonnull
    private final UnifiedDiffViewer myViewer;
    @Nonnull
    private final Side mySide;
    @Nonnull
    private final UpToDateLineNumberProvider myLocalChangesProvider;

    public UnifiedUpToDateLineNumberProvider(@Nonnull UnifiedDiffViewer viewer, @Nonnull Side side) {
      myViewer = viewer;
      mySide = side;
      myLocalChangesProvider = new UpToDateLineNumberProviderImpl(myViewer.getDocument(mySide), viewer.getProject());
    }

    @Override
    public int getLineNumber(int currentNumber) {
      int number = myViewer.transferLineFromOnesideStrict(mySide, currentNumber);
      return number != -1 ? myLocalChangesProvider.getLineNumber(number) : FAKE_LINE_NUMBER;
    }

    @Override
    public boolean isLineChanged(int currentNumber) {
      return getLineNumber(currentNumber) == ABSENT_LINE_NUMBER;
    }

    @Override
    public boolean isRangeChanged(int start, int end) {
      int line1 = myViewer.transferLineFromOnesideStrict(mySide, start);
      int line2 = myViewer.transferLineFromOnesideStrict(mySide, end);
      if (line2 - line1 != end - start) return true;

      for (int i = start; i <= end; i++) {
        if (isLineChanged(i)) return true; // TODO: a single request to LineNumberConvertor
      }
      return myLocalChangesProvider.isRangeChanged(line1, line2);
    }

    @Override
    public int getLineCount() {
      return myLocalChangesProvider.getLineCount();
    }
  }

  private static class ThreesideAnnotatorFactory extends ThreesideViewerAnnotatorFactory<ThreesideTextDiffViewerEx> {
    @Override
    @Nonnull
    public Class<? extends ThreesideTextDiffViewerEx> getViewerClass() {
      return SimpleThreesideDiffViewer.class;
    }

    @Override
    @javax.annotation.Nullable
    public ThreeSide getCurrentSide(@Nonnull ThreesideTextDiffViewerEx viewer, @Nonnull Editor editor) {
      ThreeSide side = null; // we can't just use getCurrentSide() here, popup can be called on unfocused editor
      if (viewer.getEditor(ThreeSide.LEFT) == editor) side = ThreeSide.LEFT;
      if (viewer.getEditor(ThreeSide.BASE) == editor) side = ThreeSide.BASE;
      if (viewer.getEditor(ThreeSide.RIGHT) == editor) side = ThreeSide.RIGHT;
      return side;
    }

    @Override
    public boolean isAnnotationShown(@Nonnull ThreesideTextDiffViewerEx viewer, @Nonnull ThreeSide side) {
      return viewer.getEditor(side).getGutter().isAnnotationsShown();
    }

    @Override
    public void showAnnotation(@Nonnull ThreesideTextDiffViewerEx viewer, @Nonnull ThreeSide side, @Nonnull AnnotationData data) {
      Project project = ObjectUtils.assertNotNull(viewer.getProject());
      AnnotateToggleAction.doAnnotate(viewer.getEditor(side), project, null, data.annotation, data.vcs);
    }

    @Override
    public void hideAnnotation(@Nonnull ThreesideTextDiffViewerEx viewer, @Nonnull ThreeSide side) {
      viewer.getEditor(side).getGutter().closeAllAnnotations();
    }
  }

  private static class TextMergeAnnotatorFactory extends ThreesideAnnotatorFactory {
    @Override
    @Nonnull
    public Class<? extends ThreesideTextDiffViewerEx> getViewerClass() {
      return TextMergeViewer.MyThreesideViewer.class;
    }

    @javax.annotation.Nullable
    @Override
    public ViewerAnnotator createAnnotator(@Nonnull ThreesideTextDiffViewerEx viewer, @Nonnull ThreeSide side) {
      if (side == ThreeSide.BASE) return null; // middle content is local Document, not the BASE one
      return super.createAnnotator(viewer, side);
    }
  }

  private static abstract class TwosideViewerAnnotatorFactory<T extends DiffViewerBase> extends ViewerAnnotatorFactory<T> {
    @javax.annotation.Nullable
    public abstract Side getCurrentSide(@Nonnull T viewer, @Nonnull Editor editor);

    public abstract boolean isAnnotationShown(@Nonnull T viewer, @Nonnull Side side);

    public abstract void showAnnotation(@Nonnull T viewer, @Nonnull Side side, @Nonnull AnnotationData data);

    public abstract void hideAnnotation(@Nonnull T viewer, @Nonnull Side side);

    @Override
    @javax.annotation.Nullable
    public ViewerAnnotator createAnnotator(@Nonnull T viewer, @Nonnull Editor editor) {
      Side side = getCurrentSide(viewer, editor);
      if (side == null) return null;
      return createAnnotator(viewer, side);
    }

    @Override
    public void showRememberedAnnotations(@Nonnull T viewer) {
      boolean[] annotationsShown = viewer.getRequest().getUserData(ANNOTATIONS_SHOWN_KEY);
      if (annotationsShown == null || annotationsShown.length != 2) return;
      if (annotationsShown[0]) {
        ViewerAnnotator annotator = createAnnotator(viewer, Side.LEFT);
        if (annotator != null) doAnnotate(annotator);
      }
      if (annotationsShown[1]) {
        ViewerAnnotator annotator = createAnnotator(viewer, Side.RIGHT);
        if (annotator != null) doAnnotate(annotator);
      }
    }

    @Override
    public void rememberShownAnnotations(@Nonnull T viewer) {
      boolean[] annotationsShown = new boolean[2];
      annotationsShown[0] = isAnnotationShown(viewer, Side.LEFT);
      annotationsShown[1] = isAnnotationShown(viewer, Side.RIGHT);

      viewer.getRequest().putUserData(ANNOTATIONS_SHOWN_KEY, annotationsShown);
    }

    @javax.annotation.Nullable
    public ViewerAnnotator createAnnotator(@Nonnull T viewer, @Nonnull Side side) {
      TwosideViewerAnnotatorFactory<T> factory = this;
      Project project = viewer.getProject();
      assert project != null;

      return new ViewerAnnotator() {
        @Nonnull
        @Override
        public T getViewer() {
          return viewer;
        }

        @Override
        public boolean isAnnotationShown() {
          return factory.isAnnotationShown(viewer, side);
        }

        @Override
        public void showAnnotation(@Nonnull AnnotationData data) {
          factory.showAnnotation(viewer, side, data);
        }

        @Override
        public void hideAnnotation() {
          factory.hideAnnotation(viewer, side);
        }

        @javax.annotation.Nullable
        @Override
        public FileAnnotationLoader createAnnotationsLoader() {
          return createTwosideAnnotationsLoader(project, viewer.getRequest(), side);
        }

        @Nonnull
        public BackgroundableActionLock getBackgroundableLock() {
          return BackgroundableActionLock.getLock(viewer.getProject(), VcsBackgroundableActions.ANNOTATE, viewer, side);
        }
      };
    }
  }

  private static abstract class ThreesideViewerAnnotatorFactory<T extends DiffViewerBase> extends ViewerAnnotatorFactory<T> {
    @javax.annotation.Nullable
    public abstract ThreeSide getCurrentSide(@Nonnull T viewer, @Nonnull Editor editor);

    public abstract boolean isAnnotationShown(@Nonnull T viewer, @Nonnull ThreeSide side);

    public abstract void showAnnotation(@Nonnull T viewer, @Nonnull ThreeSide side, @Nonnull AnnotationData data);

    public abstract void hideAnnotation(@Nonnull T viewer, @Nonnull ThreeSide side);

    @Override
    @javax.annotation.Nullable
    public ViewerAnnotator createAnnotator(@Nonnull T viewer, @Nonnull Editor editor) {
      ThreeSide side = getCurrentSide(viewer, editor);
      if (side == null) return null;
      return createAnnotator(viewer, side);
    }

    @Override
    public void showRememberedAnnotations(@Nonnull T viewer) {
      boolean[] annotationsShown = viewer.getRequest().getUserData(ANNOTATIONS_SHOWN_KEY);
      if (annotationsShown == null || annotationsShown.length != 3) return;
      if (annotationsShown[0]) {
        ViewerAnnotator annotator = createAnnotator(viewer, ThreeSide.LEFT);
        if (annotator != null) doAnnotate(annotator);
      }
      if (annotationsShown[1]) {
        ViewerAnnotator annotator = createAnnotator(viewer, ThreeSide.BASE);
        if (annotator != null) doAnnotate(annotator);
      }
      if (annotationsShown[2]) {
        ViewerAnnotator annotator = createAnnotator(viewer, ThreeSide.RIGHT);
        if (annotator != null) doAnnotate(annotator);
      }
    }

    @Override
    public void rememberShownAnnotations(@Nonnull T viewer) {
      boolean[] annotationsShown = new boolean[3];
      annotationsShown[0] = isAnnotationShown(viewer, ThreeSide.LEFT);
      annotationsShown[1] = isAnnotationShown(viewer, ThreeSide.BASE);
      annotationsShown[2] = isAnnotationShown(viewer, ThreeSide.RIGHT);

      viewer.getRequest().putUserData(ANNOTATIONS_SHOWN_KEY, annotationsShown);
    }

    @javax.annotation.Nullable
    public ViewerAnnotator createAnnotator(@Nonnull T viewer, @Nonnull ThreeSide side) {
      ThreesideViewerAnnotatorFactory<T> factory = this;
      Project project = viewer.getProject();
      assert project != null;

      return new ViewerAnnotator() {
        @Nonnull
        @Override
        public T getViewer() {
          return viewer;
        }

        @Override
        public boolean isAnnotationShown() {
          return factory.isAnnotationShown(viewer, side);
        }

        @Override
        public void showAnnotation(@Nonnull AnnotationData data) {
          factory.showAnnotation(viewer, side, data);
        }

        @Override
        public void hideAnnotation() {
          factory.hideAnnotation(viewer, side);
        }

        @javax.annotation.Nullable
        @Override
        public FileAnnotationLoader createAnnotationsLoader() {
          return createThreesideAnnotationsLoader(project, viewer.getRequest(), side);
        }

        @Nonnull
        public BackgroundableActionLock getBackgroundableLock() {
          return BackgroundableActionLock.getLock(viewer.getProject(), VcsBackgroundableActions.ANNOTATE, viewer, side);
        }
      };
    }
  }

  private static abstract class ViewerAnnotatorFactory<T extends DiffViewerBase> {
    @Nonnull
    public abstract Class<? extends T> getViewerClass();

    @javax.annotation.Nullable
    public abstract ViewerAnnotator createAnnotator(@Nonnull T viewer, @Nonnull Editor editor);

    public abstract void showRememberedAnnotations(@Nonnull T viewer);

    public abstract void rememberShownAnnotations(@Nonnull T viewer);
  }

  private static abstract class ViewerAnnotator {
    @Nonnull
    public abstract DiffViewerBase getViewer();

    public abstract boolean isAnnotationShown();

    public abstract void showAnnotation(@Nonnull AnnotationData data);

    public abstract void hideAnnotation();

    @javax.annotation.Nullable
    public abstract FileAnnotationLoader createAnnotationsLoader();

    @Nonnull
    public abstract BackgroundableActionLock getBackgroundableLock();
  }

  private abstract static class FileAnnotationLoader {
    @Nonnull
    private final AbstractVcs myVcs;

    @javax.annotation.Nullable
    private VcsException myException;
    @javax.annotation.Nullable
    private FileAnnotation myResult;

    public FileAnnotationLoader(@Nonnull AbstractVcs vcs) {
      myVcs = vcs;
    }

    @Nullable
    public VcsException getException() {
      return myException;
    }

    @javax.annotation.Nullable
    public AnnotationData getResult() {
      return myResult != null ? new AnnotationData(myVcs, myResult) : null;
    }

    public void run() {
      try {
        myResult = compute();
      }
      catch (VcsException e) {
        myException = e;
      }
    }

    protected abstract FileAnnotation compute() throws VcsException;
  }

  private static class AnnotationData {
    @Nonnull
    public final AbstractVcs vcs;
    @Nonnull
    public final FileAnnotation annotation;

    public AnnotationData(@Nonnull AbstractVcs vcs, @Nonnull FileAnnotation annotation) {
      this.vcs = vcs;
      this.annotation = annotation;
    }
  }

  private static class EventData {
    @Nonnull
    public final DiffViewerBase viewer;
    @Nonnull
    public final ViewerAnnotator annotator;

    public EventData(@Nonnull DiffViewerBase viewer, @Nonnull ViewerAnnotator annotator) {
      this.viewer = viewer;
      this.annotator = annotator;
    }
  }

  public static class Provider implements AnnotateToggleAction.Provider {
    @Override
    public boolean isEnabled(AnActionEvent e) {
      return AnnotateDiffViewerAction.isEnabled(e);
    }

    @Override
    public boolean isSuspended(AnActionEvent e) {
      return AnnotateDiffViewerAction.isSuspended(e);
    }

    @Override
    public boolean isAnnotated(AnActionEvent e) {
      return AnnotateDiffViewerAction.isAnnotated(e);
    }

    @Override
    public void perform(AnActionEvent e, boolean selected) {
      AnnotateDiffViewerAction.perform(e, selected);
    }
  }
}

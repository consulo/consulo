// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.unscramble;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.DateFormatUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutterAction;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.TextAnnotationGutterProvider;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorFontType;
import consulo.disposer.Disposer;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.FileHyperlinkInfo;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.ide.impl.idea.execution.impl.EditorHyperlinkSupport;
import consulo.ide.impl.idea.openapi.vcs.annotate.ShowAllAffectedGenericAction;
import consulo.ide.impl.idea.vcs.history.VcsHistoryProviderEx;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.logging.Logger;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.history.VcsHistorySession;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;

public class AnnotateStackTraceAction extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(AnnotateStackTraceAction.class);

  private final EditorHyperlinkSupport myHyperlinks;
  private final Editor myEditor;

  private boolean myIsLoading = false;

  public AnnotateStackTraceAction(@Nonnull ConsoleView consoleView) {
    super("Show files modification info", null, AllIcons.Actions.Annotate);
    myHyperlinks = ((ConsoleViewImpl)consoleView).getHyperlinks();
    myEditor = consoleView.getEditor();
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    boolean isShown = myEditor.getGutter().isAnnotationsShown();
    e.getPresentation().setEnabled(!isShown && !myIsLoading);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    myIsLoading = true;

    ProgressManager.getInstance().run(new Task.Backgroundable(myEditor.getProject(), "Getting File History", true) {
      private final Object LOCK = new Object();
      private final MergingUpdateQueue myUpdateQueue = new MergingUpdateQueue("AnnotateStackTraceAction", 200, true, null);

      private MyActiveAnnotationGutter myGutter;

      @RequiredUIAccess
      @Override
      public void onCancel() {
        myEditor.getGutter().closeAllAnnotations();
      }

      @RequiredUIAccess
      @Override
      public void onFinished() {
        myIsLoading = false;
        Disposer.dispose(myUpdateQueue);
      }

      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        MultiMap<VirtualFile, Integer> files2lines = new MultiMap<>();
        Map<Integer, LastRevision> revisions = new HashMap<>();

        ApplicationManager.getApplication().runReadAction(() -> {
          for (int line = 0; line < myEditor.getDocument().getLineCount(); line++) {
            indicator.checkCanceled();
            VirtualFile file = getHyperlinkVirtualFile(myHyperlinks.findAllHyperlinksOnLine(line));
            if (file == null) continue;

            files2lines.putValue(file, line);
          }
        });

        files2lines.entrySet().forEach(entry -> {
          indicator.checkCanceled();
          VirtualFile file = entry.getKey();
          Collection<Integer> lines = entry.getValue();

          LastRevision revision = getLastRevision(file);
          if (revision == null) return;

          synchronized (LOCK) {
            for (Integer line : lines) {
              revisions.put(line, revision);
            }
          }

          myUpdateQueue.queue(new Update("update") {
            @Override
            public void run() {
              updateGutter(indicator, revisions);
            }
          });
        });

        // myUpdateQueue can be disposed before the last revisions are passed to the gutter
        ApplicationManager.getApplication().invokeLater(() -> updateGutter(indicator, revisions));
      }

      @RequiredUIAccess
      private void updateGutter(@Nonnull ProgressIndicator indicator, @Nonnull Map<Integer, LastRevision> revisions) {
        if (indicator.isCanceled()) return;

        if (myGutter == null) {
          myGutter = new MyActiveAnnotationGutter((Project)getProject(), myHyperlinks, indicator);
          myEditor.getGutter().registerTextAnnotation(myGutter, myGutter);
        }

        Map<Integer, LastRevision> revisionsCopy;
        synchronized (LOCK) {
          revisionsCopy = new HashMap<>(revisions);
        }

        myGutter.updateData(revisionsCopy);
        ((EditorGutterComponentEx)myEditor.getGutter()).revalidateMarkup();
      }

      @Nullable
      private LastRevision getLastRevision(@Nonnull VirtualFile file) {
        try {
          AbstractVcs vcs = VcsUtil.getVcsFor(myEditor.getProject(), file);
          if (vcs == null) return null;

          VcsHistoryProvider historyProvider = vcs.getVcsHistoryProvider();
          if (historyProvider == null) return null;

          FilePath filePath = VcsContextFactory.getInstance().createFilePathOn(file);

          if (historyProvider instanceof VcsHistoryProviderEx) {
            VcsFileRevision revision = ((VcsHistoryProviderEx)historyProvider).getLastRevision(filePath);
            if (revision == null) return null;
            return LastRevision.create(revision);
          }
          else {
            VcsHistorySession session = historyProvider.createSessionFor(filePath);
            if (session == null) return null;

            List<VcsFileRevision> list = session.getRevisionList();
            if (list == null || list.isEmpty()) return null;

            return LastRevision.create(list.get(0));
          }
        }
        catch (VcsException ignored) {
          LOG.warn(ignored);
          return null;
        }
      }
    });
  }

  @Nullable
  @RequiredReadAction
  private static VirtualFile getHyperlinkVirtualFile(@Nonnull List<RangeHighlighter> links) {
    RangeHighlighter key = ContainerUtil.getLastItem(links);
    if (key == null) return null;
    HyperlinkInfo info = EditorHyperlinkSupport.getHyperlinkInfo(key);
    if (!(info instanceof FileHyperlinkInfo)) return null;
    OpenFileDescriptor descriptor = ((FileHyperlinkInfo)info).getDescriptor();
    return descriptor != null ? descriptor.getFile() : null;
  }

  private static class LastRevision {
    @Nonnull
    private final VcsRevisionNumber myNumber;
    @Nonnull
    private final String myAuthor;
    @Nonnull
    private final Date myDate;
    @Nonnull
    private final String myMessage;

    LastRevision(@Nonnull VcsRevisionNumber number, @Nonnull String author, @Nonnull Date date, @Nonnull String message) {
      myNumber = number;
      myAuthor = author;
      myDate = date;
      myMessage = message;
    }

    @Nonnull
    public static LastRevision create(@Nonnull VcsFileRevision revision) {
      VcsRevisionNumber number = revision.getRevisionNumber();
      String author = StringUtil.notNullize(revision.getAuthor(), "Unknown");
      Date date = revision.getRevisionDate();
      String message = StringUtil.notNullize(revision.getCommitMessage());
      return new LastRevision(number, author, date, message);
    }

    @Nonnull
    public VcsRevisionNumber getNumber() {
      return myNumber;
    }

    @Nonnull
    public String getAuthor() {
      return myAuthor;
    }

    @Nonnull
    public Date getDate() {
      return myDate;
    }

    @Nonnull
    public String getMessage() {
      return myMessage;
    }
  }

  private static class MyActiveAnnotationGutter implements TextAnnotationGutterProvider, EditorGutterAction {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final EditorHyperlinkSupport myHyperlinks;
    @Nonnull
    private final ProgressIndicator myIndicator;

    @Nonnull
    private Map<Integer, LastRevision> myRevisions = Collections.emptyMap();
    private Date myNewestDate = null;
    private int myMaxDateLength = 0;

    MyActiveAnnotationGutter(@Nonnull Project project, @Nonnull EditorHyperlinkSupport hyperlinks, @Nonnull ProgressIndicator indicator) {
      myProject = project;
      myHyperlinks = hyperlinks;
      myIndicator = indicator;
    }

    @Override
    public void doAction(int lineNum) {
      LastRevision revision = myRevisions.get(lineNum);
      if (revision == null) return;

      VirtualFile file = getHyperlinkVirtualFile(myHyperlinks.findAllHyperlinksOnLine(lineNum));
      if (file == null) return;

      AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(file);
      if (vcs != null) {
        VcsRevisionNumber number = revision.getNumber();
        VcsKey vcsKey = vcs.getKeyInstanceMethod();
        ShowAllAffectedGenericAction.showSubmittedFiles(myProject, number, file, vcsKey);
      }
    }

    @Override
    public Cursor getCursor(int lineNum) {
      return myRevisions.containsKey(lineNum) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
    }

    @Override
    public String getLineText(int line, Editor editor) {
      LastRevision revision = myRevisions.get(line);
      if (revision != null) {
        return String.format("%" + myMaxDateLength + "s", FileAnnotation.formatDate(revision.getDate())) + " " + revision.getAuthor();
      }
      return "";
    }

    @Override
    public String getToolTip(int line, Editor editor) {
      LastRevision revision = myRevisions.get(line);
      if (revision != null) {
        return XmlStringUtil.escapeString(revision.getAuthor() + " " + DateFormatUtil.formatDateTime(revision.getDate()) + "\n" + VcsUtil.trimCommitMessageToSaneSize(revision.getMessage()));
      }
      return null;
    }

    @Override
    public EditorFontType getStyle(int line, Editor editor) {
      LastRevision revision = myRevisions.get(line);
      return revision != null && revision.getDate().equals(myNewestDate) ? EditorFontType.BOLD : EditorFontType.PLAIN;
    }

    @Override
    public EditorColorKey getColor(int line, Editor editor) {
      return AnnotationSource.LOCAL.getColor();
    }

    @Override
    public ColorValue getBgColor(int line, Editor editor) {
      return null;
    }

    @Override
    public List<AnAction> getPopupActions(int line, Editor editor) {
      return Collections.emptyList();
    }

    @Override
    public void gutterClosed() {
      myIndicator.cancel();
    }

    @RequiredUIAccess
    public void updateData(@Nonnull Map<Integer, LastRevision> revisions) {
      myRevisions = revisions;

      Date newestDate = null;
      int maxDateLength = 0;

      for (LastRevision revision : myRevisions.values()) {
        Date date = revision.getDate();
        if (newestDate == null || date.after(newestDate)) {
          newestDate = date;
        }
        int length = DateFormatUtil.formatPrettyDate(date).length();
        if (length > maxDateLength) {
          maxDateLength = length;
        }
      }

      myNewestDate = newestDate;
      myMaxDateLength = maxDateLength;
    }
  }
}

// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.unscramble;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
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
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.OpenFileDescriptor;
import consulo.platform.base.icon.PlatformIconGroup;
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
import consulo.util.lang.xml.XmlStringUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.*;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public class AnnotateStackTraceAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(AnnotateStackTraceAction.class);

    private final EditorHyperlinkSupport myHyperlinks;
    private final Editor myEditor;

    private boolean myIsLoading = false;

    public AnnotateStackTraceAction(ConsoleView consoleView) {
        super("Show files modification info", null, PlatformIconGroup.actionsAnnotate());
        myHyperlinks = ((ConsoleViewImpl) consoleView).getHyperlinks();
        myEditor = consoleView.getEditor();
    }

    @Override
    public void update(AnActionEvent e) {
        boolean isShown = myEditor.getGutter().isAnnotationsShown();
        e.getPresentation().setEnabled(!isShown && !myIsLoading);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
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
            public void run(ProgressIndicator indicator) {
                MultiMap<VirtualFile, Integer> files2lines = new MultiMap<>();
                Map<Integer, LastRevision> revisions = new HashMap<>();

                Application.get().runReadAction(() -> {
                    for (int line = 0; line < myEditor.getDocument().getLineCount(); line++) {
                        indicator.checkCanceled();
                        VirtualFile file = getHyperlinkVirtualFile(myHyperlinks.findAllHyperlinksOnLine(line));
                        if (file == null) {
                            continue;
                        }

                        files2lines.putValue(file, line);
                    }
                });

                files2lines.entrySet().forEach(entry -> {
                    indicator.checkCanceled();
                    VirtualFile file = entry.getKey();
                    Collection<Integer> lines = entry.getValue();

                    LastRevision revision = getLastRevision(file);
                    if (revision == null) {
                        return;
                    }

                    synchronized (LOCK) {
                        for (Integer line : lines) {
                            revisions.put(line, revision);
                        }
                    }

                    myUpdateQueue.queue(new Update("update") {
                        @Override
                        @RequiredUIAccess
                        public void run() {
                            updateGutter(indicator, revisions);
                        }
                    });
                });

                // myUpdateQueue can be disposed before the last revisions are passed to the gutter
                Application.get().invokeLater(() -> updateGutter(indicator, revisions));
            }

            @RequiredUIAccess
            private void updateGutter(ProgressIndicator indicator, Map<Integer, LastRevision> revisions) {
                if (indicator.isCanceled()) {
                    return;
                }

                if (myGutter == null) {
                    myGutter = new MyActiveAnnotationGutter((Project) getProject(), myHyperlinks, indicator);
                    myEditor.getGutter().registerTextAnnotation(myGutter, myGutter);
                }

                Map<Integer, LastRevision> revisionsCopy;
                synchronized (LOCK) {
                    revisionsCopy = new HashMap<>(revisions);
                }

                myGutter.updateData(revisionsCopy);
                ((EditorGutterComponentEx) myEditor.getGutter()).revalidateMarkup();
            }

            private @Nullable LastRevision getLastRevision(VirtualFile file) {
                try {
                    AbstractVcs vcs = VcsUtil.getVcsFor(myEditor.getProject(), file);
                    if (vcs == null) {
                        return null;
                    }

                    VcsHistoryProvider historyProvider = vcs.getVcsHistoryProvider();
                    if (historyProvider == null) {
                        return null;
                    }

                    FilePath filePath = VcsContextFactory.getInstance().createFilePathOn(file);

                    if (historyProvider instanceof VcsHistoryProviderEx vcsHistoryProviderEx) {
                        VcsFileRevision revision = vcsHistoryProviderEx.getLastRevision(filePath);
                        if (revision == null) {
                            return null;
                        }
                        return LastRevision.create(revision);
                    }
                    else {
                        VcsHistorySession session = historyProvider.createSessionFor(filePath);
                        if (session == null) {
                            return null;
                        }

                        List<VcsFileRevision> list = session.getRevisionList();
                        if (list == null || list.isEmpty()) {
                            return null;
                        }

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
    private static VirtualFile getHyperlinkVirtualFile(List<RangeHighlighter> links) {
        RangeHighlighter key = ContainerUtil.getLastItem(links);
        if (key == null) {
            return null;
        }
        HyperlinkInfo info = EditorHyperlinkSupport.getHyperlinkInfo(key);
        if (info instanceof FileHyperlinkInfo fileHyperlinkInfo) {
            OpenFileDescriptor descriptor = fileHyperlinkInfo.getDescriptor();
            return descriptor != null ? descriptor.getFile() : null;
        }
        return null;
    }

    private static class LastRevision {
        
        private final VcsRevisionNumber myNumber;
        
        private final String myAuthor;
        
        private final Date myDate;
        
        private final String myMessage;

        LastRevision(VcsRevisionNumber number, String author, Date date, String message) {
            myNumber = number;
            myAuthor = author;
            myDate = date;
            myMessage = message;
        }

        
        public static LastRevision create(VcsFileRevision revision) {
            VcsRevisionNumber number = revision.getRevisionNumber();
            String author = StringUtil.notNullize(revision.getAuthor(), "Unknown");
            Date date = revision.getRevisionDate();
            String message = StringUtil.notNullize(revision.getCommitMessage());
            return new LastRevision(number, author, date, message);
        }

        
        public VcsRevisionNumber getNumber() {
            return myNumber;
        }

        
        public String getAuthor() {
            return myAuthor;
        }

        
        public Date getDate() {
            return myDate;
        }

        
        public String getMessage() {
            return myMessage;
        }
    }

    private static class MyActiveAnnotationGutter implements TextAnnotationGutterProvider, EditorGutterAction {
        
        private final Project myProject;
        
        private final EditorHyperlinkSupport myHyperlinks;
        
        private final ProgressIndicator myIndicator;

        
        private Map<Integer, LastRevision> myRevisions = Collections.emptyMap();
        private Date myNewestDate = null;
        private int myMaxDateLength = 0;

        MyActiveAnnotationGutter(
            Project project,
            EditorHyperlinkSupport hyperlinks,
            ProgressIndicator indicator
        ) {
            myProject = project;
            myHyperlinks = hyperlinks;
            myIndicator = indicator;
        }

        @Override
        public void doAction(int lineNum) {
            LastRevision revision = myRevisions.get(lineNum);
            if (revision == null) {
                return;
            }

            VirtualFile file = getHyperlinkVirtualFile(myHyperlinks.findAllHyperlinksOnLine(lineNum));
            if (file == null) {
                return;
            }

            AbstractVcs vcs = ProjectLevelVcsManager.getInstance(myProject).getVcsFor(file);
            if (vcs != null) {
                VcsRevisionNumber number = revision.getNumber();
                VcsKey vcsKey = vcs.getKeyInstanceMethod();
                AbstractVcsHelper.getInstance(myProject).showSubmittedFiles(number, file, vcsKey);
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
                return String.format(
                    "%" + myMaxDateLength + "s",
                    FileAnnotation.formatDate(revision.getDate())
                ) + " " + revision.getAuthor();
            }
            return "";
        }

        
        @Override
        public LocalizeValue getToolTipValue(int line, Editor editor) {
            LastRevision revision = myRevisions.get(line);
            if (revision != null) {
                return LocalizeValue.of(XmlStringUtil.escapeText(
                    revision.getAuthor() + " " + DateFormatUtil.formatDateTime(revision.getDate()) + "\n" +
                        VcsUtil.trimCommitMessageToSaneSize(revision.getMessage())
                ));
            }
            return LocalizeValue.empty();
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
        public void updateData(Map<Integer, LastRevision> revisions) {
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

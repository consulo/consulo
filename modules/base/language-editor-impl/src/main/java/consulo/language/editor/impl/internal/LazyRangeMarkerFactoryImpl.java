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
package consulo.language.editor.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.LazyRangeMarkerFactory;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.language.codeStyle.CodeStyle;
import consulo.project.Project;
import consulo.util.collection.WeakList;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.function.Supplier;

@Singleton
@ServiceImpl
public class LazyRangeMarkerFactoryImpl extends LazyRangeMarkerFactory {
    private final Project myProject;
    private static final Key<WeakList<LazyMarker>> LAZY_MARKERS_KEY = Key.create("LAZY_MARKERS_KEY");

    @Inject
    public LazyRangeMarkerFactoryImpl(@Nonnull Project project,
                                      @Nonnull FileDocumentManager fileDocumentManager,
                                      @Nonnull EditorFactory editorFactory) {
        myProject = project;

        editorFactory.getEventMulticaster().addDocumentListener(new DocumentAdapter() {
            @Override
            public void beforeDocumentChange(DocumentEvent e) {
                transformRangeMarkers(e);
            }

            @Override
            public void documentChanged(DocumentEvent e) {
                transformRangeMarkers(e);
            }

            private void transformRangeMarkers(@Nonnull DocumentEvent e) {
                Document document = e.getDocument();
                VirtualFile file = fileDocumentManager.getFile(document);
                if (file == null || myProject.isDisposed()) {
                    return;
                }

                WeakList<LazyMarker> lazyMarkers = getMarkers(file);
                if (lazyMarkers == null) {
                    return;
                }

                List<LazyMarker> markers = lazyMarkers.toStrongList();
                for (LazyMarker marker : markers) {
                    if (file.equals(marker.getFile())) {
                        marker.getOrCreateDelegate();
                    }
                }
            }
        }, project);
    }

    static WeakList<LazyMarker> getMarkers(@Nonnull VirtualFile file) {
        return file.getUserData(LazyRangeMarkerFactoryImpl.LAZY_MARKERS_KEY);
    }

    private static void addToLazyMarkersList(@Nonnull LazyMarker marker, @Nonnull VirtualFile file) {
        WeakList<LazyMarker> markers = getMarkers(file);

        if (markers == null) {
            markers = file.putUserDataIfAbsent(LAZY_MARKERS_KEY, new WeakList<>());
        }
        markers.add(marker);
    }

    private static void removeFromLazyMarkersList(@Nonnull LazyMarker marker, @Nonnull VirtualFile file) {
        WeakList<LazyMarker> markers = getMarkers(file);

        if (markers != null) {
            markers.remove(marker);
        }
    }

    @Override
    @Nonnull
    public RangeMarker createRangeMarker(@Nonnull VirtualFile file, int offset) {
        return ApplicationManager.getApplication().runReadAction((Supplier<RangeMarker>) () -> {
            // even for already loaded document do not create range marker yet - wait until it really needed when e.g. user clicked to jump to OpenFileDescriptor
            LazyMarker marker = new OffsetLazyMarker(file, offset);
            addToLazyMarkersList(marker, file);
            return marker;
        });
    }

    @Override
    @Nonnull
    public RangeMarker createRangeMarker(@Nonnull VirtualFile file, int line, int column, boolean persistent) {
        return ApplicationManager.getApplication().runReadAction((Supplier<RangeMarker>) () -> {
            Document document = FileDocumentManager.getInstance().getCachedDocument(file);
            if (document != null) {
                int myTabSize = CodeStyle.getProjectOrDefaultSettings(myProject).getTabSize(file.getFileType());
                int offset = calculateOffset(document, line, column, myTabSize);
                return document.createRangeMarker(offset, offset, persistent);
            }

            LazyMarker marker = new LineColumnLazyMarker(myProject, file, line, column);
            addToLazyMarkersList(marker, file);
            return marker;
        });
    }

    abstract static class LazyMarker extends UserDataHolderBase implements RangeMarker {
        protected RangeMarker myDelegate; // the real range marker which is created only when document is opened, or (this) which means it's disposed
        protected final VirtualFile myFile;
        protected final int myInitialOffset;

        private LazyMarker(@Nonnull VirtualFile file, int offset) {
            myFile = file;
            myInitialOffset = offset;
        }

        boolean isDelegated() {
            return myDelegate != null;
        }

        @Nonnull
        public VirtualFile getFile() {
            return myFile;
        }

        @Nullable
        final RangeMarker getOrCreateDelegate() {
            if (myDelegate == null) {
                Document document = FileDocumentManager.getInstance().getDocument(myFile);
                if (document == null) {
                    return null;
                }
                myDelegate = createDelegate(myFile, document);
                removeFromLazyMarkersList(this, myFile);
            }
            return isDisposed() ? null : myDelegate;
        }

        @Nullable
        protected abstract RangeMarker createDelegate(@Nonnull VirtualFile file, @Nonnull Document document);

        @Override
        @Nonnull
        public Document getDocument() {
            RangeMarker delegate = getOrCreateDelegate();
            if (delegate == null) {
                //noinspection ConstantConditions
                return FileDocumentManager.getInstance().getDocument(myFile);
            }
            return delegate.getDocument();
        }

        @Override
        public int getStartOffset() {
            return myDelegate == null || isDisposed() ? myInitialOffset : myDelegate.getStartOffset();
        }

        public boolean isDisposed() {
            return myDelegate == this;
        }


        @Override
        public int getEndOffset() {
            return myDelegate == null || isDisposed() ? myInitialOffset : myDelegate.getEndOffset();
        }

        @Override
        public boolean isValid() {
            RangeMarker delegate = getOrCreateDelegate();
            return delegate != null && !isDisposed() && delegate.isValid();
        }

        @Override
        public void setGreedyToLeft(boolean greedy) {
            getOrCreateDelegate().setGreedyToLeft(greedy);
        }

        @Override
        public void setGreedyToRight(boolean greedy) {
            getOrCreateDelegate().setGreedyToRight(greedy);
        }

        @Override
        public boolean isGreedyToRight() {
            return getOrCreateDelegate().isGreedyToRight();
        }

        @Override
        public boolean isGreedyToLeft() {
            return getOrCreateDelegate().isGreedyToLeft();
        }

        @Override
        public void dispose() {
            assert !isDisposed();
            RangeMarker delegate = myDelegate;
            if (delegate == null) {
                removeFromLazyMarkersList(this, myFile);
                myDelegate = this; // mark of disposed marker
            }
            else {
                delegate.dispose();
            }
        }
    }

    private static class OffsetLazyMarker extends LazyMarker {
        private OffsetLazyMarker(@Nonnull VirtualFile file, int offset) {
            super(file, offset);
        }

        @Override
        public long getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValid() {
            RangeMarker delegate = myDelegate;
            if (delegate == null) {
                Document document = FileDocumentManager.getInstance().getDocument(myFile);
                return document != null;
            }

            return super.isValid();
        }

        @Override
        @Nonnull
        public RangeMarker createDelegate(@Nonnull VirtualFile file, @Nonnull Document document) {
            int offset = Math.min(myInitialOffset, document.getTextLength());
            return document.createRangeMarker(offset, offset);
        }
    }

    private static class LineColumnLazyMarker extends LazyMarker {
        private final int myLine;
        private final int myColumn;
        private final int myTabSize;

        private LineColumnLazyMarker(@Nonnull Project project, @Nonnull VirtualFile file, int line, int column) {
            super(file, -1);
            myLine = line;
            myColumn = column;
            myTabSize = CodeStyle.getProjectOrDefaultSettings(project).getTabSize(file.getFileType());
        }

        @Override
        public long getId() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Nullable
        public RangeMarker createDelegate(@Nonnull VirtualFile file, @Nonnull Document document) {
            if (document.getTextLength() == 0 && !(myLine == 0 && myColumn == 0)) {
                return null;
            }

            int offset = calculateOffset(document, myLine, myColumn, myTabSize);
            return document.createRangeMarker(offset, offset);
        }

        @Override
        public boolean isValid() {
            RangeMarker delegate = myDelegate;
            if (delegate == null) {
                Document document = FileDocumentManager.getInstance().getDocument(myFile);
                return document != null && (document.getTextLength() != 0 || myLine == 0 && myColumn == 0);
            }

            return super.isValid();
        }

        @Override
        public int getStartOffset() {
            getOrCreateDelegate();
            return super.getStartOffset();
        }

        @Override
        public int getEndOffset() {
            getOrCreateDelegate();
            return super.getEndOffset();
        }
    }

    private static int calculateOffset(@Nonnull Document document,
                                       int line,
                                       int column,
                                       int tabSize) {
        int offset;
        if (0 <= line && line < document.getLineCount()) {
            int lineStart = document.getLineStartOffset(line);
            int lineEnd = document.getLineEndOffset(line);
            CharSequence docText = document.getCharsSequence();

            offset = lineStart;
            int col = 0;
            while (offset < lineEnd && col < column) {
                col += docText.charAt(offset) == '\t' ? tabSize : 1;
                offset++;
            }
        }
        else {
            offset = document.getTextLength();
        }
        return offset;
    }
}

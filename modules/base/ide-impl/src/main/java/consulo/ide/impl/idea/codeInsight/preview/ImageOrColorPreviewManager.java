/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.preview;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class ImageOrColorPreviewManager implements Disposable, EditorMouseMotionListener {
    private static final Logger LOG = Logger.getInstance(ImageOrColorPreviewManager.class);

    private static final Key<KeyListener> EDITOR_LISTENER_ADDED = Key.create("previewManagerListenerAdded");

    private Future<?> executeFuture = CompletableFuture.completedFuture(null);

    /**
     * this collection should not keep strong references to the elements
     *
     * @link getPsiElementsAt()
     */
    @Nullable
    private Collection<PsiElement> myElements;

    @Inject
    public ImageOrColorPreviewManager(Application application, EditorFactory editorFactory) {
        if (!application.isSwingApplication()) {
            return;
        }

        // we don't use multicaster because we don't want to serve all editors - only supported
        editorFactory.addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorCreated(@Nonnull EditorFactoryEvent event) {
                registerListeners(event.getEditor());
            }

            @Override
            public void editorReleased(@Nonnull EditorFactoryEvent event) {
                Editor editor = event.getEditor();
                if (editor.isOneLineMode()) {
                    return;
                }

                KeyListener keyListener = EDITOR_LISTENER_ADDED.get(editor);
                if (keyListener != null) {
                    EDITOR_LISTENER_ADDED.set(editor, null);
                    editor.getContentComponent().removeKeyListener(keyListener);
                    editor.removeEditorMouseMotionListener(ImageOrColorPreviewManager.this);
                }
            }
        }, this);
    }

    private void registerListeners(final Editor editor) {
        if (editor.isOneLineMode()) {
            return;
        }

        Project project = editor.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null || psiFile instanceof PsiCompiledElement || !isSupportedFile(psiFile)) {
            return;
        }

        editor.addEditorMouseMotionListener(this);

        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SHIFT && !editor.isOneLineMode()) {
                    PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                    if (pointerInfo != null) {
                        Point location = pointerInfo.getLocation();
                        SwingUtilities.convertPointFromScreen(location, editor.getContentComponent());

                        executeFuture.cancel(false);
                        executeFuture =
                            project.getUIAccess()
                                .getScheduler()
                                .schedule(new PreviewRequest(location, editor, true), 100, TimeUnit.MILLISECONDS);
                    }
                }
            }
        };
        editor.getContentComponent().addKeyListener(keyListener);

        EDITOR_LISTENER_ADDED.set(editor, keyListener);
    }

    private static boolean isSupportedFile(PsiFile psiFile) {
        for (PsiFile file : psiFile.getViewProvider().getAllFiles()) {
            for (ElementPreviewProvider provider : ElementPreviewProvider.EP_NAME.getExtensionList()) {
                if (provider.isSupportedFile(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nonnull
    private static Collection<PsiElement> getPsiElementsAt(Point point, Editor editor) {
        if (editor.isDisposed()) {
            return Collections.emptySet();
        }

        Project project = editor.getProject();
        if (project == null || project.isDisposed()) {
            return Collections.emptySet();
        }

        final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        final Document document = editor.getDocument();
        PsiFile psiFile = documentManager.getPsiFile(document);
        if (psiFile == null || psiFile instanceof PsiCompiledElement || !psiFile.isValid()) {
            return Collections.emptySet();
        }

        final Set<PsiElement> elements = Collections.newSetFromMap(ContainerUtil.createWeakMap());
        final int offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(point));
        if (documentManager.isCommitted(document)) {
            ContainerUtil.addIfNotNull(elements, InjectedLanguageUtil.findElementAtNoCommit(psiFile, offset));
        }
        for (PsiFile file : psiFile.getViewProvider().getAllFiles()) {
            ContainerUtil.addIfNotNull(elements, file.findElementAt(offset));
        }

        return elements;
    }

    @Override
    public void dispose() {
        executeFuture.cancel(false);
        myElements = null;
    }

    @Override
    public void mouseMoved(@Nonnull EditorMouseEvent event) {
        Editor editor = event.getEditor();
        if (editor.isOneLineMode()) {
            return;
        }

        Project project = editor.getProject();
        if (project == null) {
            return;
        }

        executeFuture.cancel(false);
        Point point = event.getMouseEvent().getPoint();
        if (myElements == null && event.getMouseEvent().isShiftDown()) {
            executeFuture =
                project.getUIAccess().getScheduler().schedule(new PreviewRequest(point, editor, false), 100, TimeUnit.MILLISECONDS);
        }
        else {
            Collection<PsiElement> elements = myElements;
            if (!getPsiElementsAt(point, editor).equals(elements)) {
                myElements = null;
                for (ElementPreviewProvider provider : ElementPreviewProvider.EP_NAME.getExtensionList()) {
                    try {
                        if (elements != null) {
                            for (PsiElement element : elements) {
                                provider.hide(element, editor);
                            }
                        }
                        else {
                            provider.hide(null, editor);
                        }
                    }
                    catch (Exception e) {
                        LOG.error(e);
                    }
                }
            }
        }
    }

    private final class PreviewRequest implements Runnable {
        private final Point point;
        private final Editor editor;
        private final boolean keyTriggered;

        public PreviewRequest(Point point, Editor editor, boolean keyTriggered) {
            this.point = point;
            this.editor = editor;
            this.keyTriggered = keyTriggered;
        }

        @Override
        public void run() {
            Collection<PsiElement> elements = getPsiElementsAt(point, editor);
            if (elements.equals(myElements)) {
                return;
            }
            for (PsiElement element : elements) {
                if (element == null || !element.isValid()) {
                    return;
                }
                if (PsiDocumentManager.getInstance(element.getProject()).isUncommited(editor.getDocument()) ||
                    DumbService.getInstance(element.getProject()).isDumb()) {
                    return;
                }

                for (ElementPreviewProvider provider : ElementPreviewProvider.EP_NAME.getExtensions()) {
                    if (!provider.isSupportedFile(element.getContainingFile())) {
                        continue;
                    }

                    try {
                        provider.show(element, editor, point, keyTriggered);
                    }
                    catch (Exception e) {
                        LOG.error(e);
                    }
                }
            }
            myElements = elements;
        }
    }
}
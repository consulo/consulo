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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
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
@ServiceAPI(value = ComponentScope.APPLICATION)
@ServiceImpl
public class ImageOrColorPreviewManager implements Disposable, EditorMouseMotionListener {
  private static final Key<KeyListener> EDITOR_LISTENER_ADDED = Key.create("previewManagerListenerAdded");
  private static final Logger LOG = Logger.getInstance(ImageOrColorPreviewManager.class);

  @Nonnull
  private final Application myApplication;
  private final ApplicationConcurrency myApplicationConcurrency;

  private Future<?> myExecuteFuture = CompletableFuture.completedFuture(null);

  /**
   * this collection should not keep strong references to the elements
   *
   * @link getPsiElementsAt()
   */
  @Nullable
  private Collection<PsiElement> myElements;

  @Inject
  public ImageOrColorPreviewManager(@Nonnull Application application, ApplicationConcurrency applicationConcurrency) {
    myApplication = application;
    myApplicationConcurrency = applicationConcurrency;
  }

  public void registerListeners(final Editor editor) {
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

            myExecuteFuture.cancel(false);
            myExecuteFuture = myApplicationConcurrency.getScheduledExecutorService()
                                                      .schedule(new PreviewRequest(project, location, editor, true),
                                                                100,
                                                                TimeUnit.MILLISECONDS);
          }
        }
      }
    };
    editor.getContentComponent().addKeyListener(keyListener);

    EDITOR_LISTENER_ADDED.set(editor, keyListener);
  }

  public void unregisterListeners(Editor editor) {
    if (editor.isOneLineMode()) {
      return;
    }

    KeyListener keyListener = EDITOR_LISTENER_ADDED.get(editor);
    if (keyListener != null) {
      EDITOR_LISTENER_ADDED.set(editor, null);
      editor.getContentComponent().removeKeyListener(keyListener);
      editor.removeEditorMouseMotionListener(this);
    }
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
  @RequiredReadAction
  private Collection<PsiElement> getPsiElementsAt(Point point, Editor editor) {
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

    final Set<PsiElement> elements = Collections.newSetFromMap(Maps.newWeakHashMap());
    final int offset = editor.logicalPositionToOffset(editor.xyToLogicalPosition(point));
    if (documentManager.isCommitted(document)) {
      ContainerUtil.addIfNotNull(elements, InjectedLanguageManager.getInstance(project).findElementAtNoCommit(psiFile, offset));
    }
    for (PsiFile file : psiFile.getViewProvider().getAllFiles()) {
      ContainerUtil.addIfNotNull(elements, file.findElementAt(offset));
    }

    return elements;
  }

  @Override
  public void dispose() {
    myExecuteFuture.cancel(false);
    myElements = null;
  }

  @RequiredUIAccess
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

    myExecuteFuture.cancel(false);
    Point point = event.getMouseEvent().getPoint();
    if (myElements == null && event.getMouseEvent().isShiftDown()) {
      myExecuteFuture = myApplicationConcurrency.getScheduledExecutorService()
                                                .schedule(new PreviewRequest(project, point, editor, false), 100, TimeUnit.MILLISECONDS);
    }
    else {
      Collection<PsiElement> elements = myElements;
      myApplication.getLock().readAsync(() -> getPsiElementsAt(point, editor).equals(elements))
                   .whenComplete((result, throwable) -> {
                     // psi elements changed
                     if (result == Boolean.FALSE) {
                       myElements = null;
                     }

                     project.getUIAccess().giveAsync(() -> {
                       myApplication.getExtensionPoint(ElementPreviewProvider.class).forEachExtensionSafe(provider -> {
                         if (elements != null) {
                           for (PsiElement element : elements) {
                             provider.hide(element, editor);
                           }
                         }
                         else {
                           provider.hide(null, editor);
                         }
                       });
                     });
                   });
    }
  }

  private final class PreviewRequest implements Runnable {
    private final Point point;
    private final Editor editor;
    private final boolean keyTriggered;

    public PreviewRequest(Project project, Point point, Editor editor, boolean keyTriggered) {
      this.point = point;
      this.editor = editor;
      this.keyTriggered = keyTriggered;
    }

    @Override
    public void run() {
      Collection<PsiElement> elements = getPsiElementsAt(point, editor);
      if (elements.equals(myElements)) return;
      for (PsiElement element : elements) {
        if (element == null || !element.isValid()) {
          return;
        }
        if (PsiDocumentManager.getInstance(element.getProject()).isUncommited(editor.getDocument()) ||
          DumbService.getInstance(element.getProject()).isDumb()) {
          return;
        }

        myApplication.getExtensionPoint(ElementPreviewProvider.class).forEachExtensionSafe(provider -> {
          if (provider.isSupportedFile(element.getContainingFile())) {
            provider.show(element, editor, point, keyTriggered);
          }
        });
      }
      myElements = elements;
    }
  }
}
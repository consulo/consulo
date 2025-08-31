// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.language.editor.parameterInfo.CreateParameterInfoContext;
import consulo.language.editor.parameterInfo.ParameterInfoHandler;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.editor.EditorActivityManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class ShowParameterInfoContext implements CreateParameterInfoContext {
  private final Editor myEditor;
  private final PsiFile myFile;
  private final Project myProject;
  private final int myOffset;
  private final int myParameterListStart;
  private final boolean mySingleParameterInfo;
  private Object myHighlightedElement;
  private Object[] myItems;
  private boolean myRequestFocus;

  public ShowParameterInfoContext(Editor editor, Project project, PsiFile file, int offset, int parameterListStart) {
    this(editor, project, file, offset, parameterListStart, false);
  }

  public ShowParameterInfoContext(Editor editor, Project project, PsiFile file, int offset, int parameterListStart, boolean requestFocus) {
    this(editor, project, file, offset, parameterListStart, requestFocus, false);
  }

  public ShowParameterInfoContext(Editor editor, Project project, PsiFile file, int offset, int parameterListStart, boolean requestFocus, boolean singleParameterInfo) {
    myEditor = editor;
    myProject = project;
    myFile = file;
    myParameterListStart = parameterListStart;
    myOffset = offset;
    myRequestFocus = requestFocus;
    mySingleParameterInfo = singleParameterInfo;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public PsiFile getFile() {
    return myFile;
  }

  @Override
  public int getOffset() {
    return myOffset;
  }

  @Override
  public int getParameterListStart() {
    return myParameterListStart;
  }

  @Override
  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }

  @Override
  public Object getHighlightedElement() {
    return myHighlightedElement;
  }

  @Override
  public void setHighlightedElement(Object element) {
    myHighlightedElement = element;
  }

  @Override
  public void setItemsToShow(Object[] items) {
    myItems = items;
  }

  @Override
  public Object[] getItemsToShow() {
    return myItems;
  }

  @Override
  public void showHint(PsiElement element, int offset, ParameterInfoHandler handler) {
    Object[] itemsToShow = getItemsToShow();
    if (itemsToShow == null || itemsToShow.length == 0) return;
    showParameterHint(element, getEditor(), itemsToShow, getProject(), itemsToShow.length > 1 ? getHighlightedElement() : null, offset, handler, myRequestFocus, mySingleParameterInfo);
  }

  private static void showParameterHint(PsiElement element,
                                        Editor editor,
                                        Object[] descriptors,
                                        Project project,
                                        @Nullable Object highlighted,
                                        int elementStart,
                                        ParameterInfoHandler handler,
                                        boolean requestFocus,
                                        boolean singleParameterInfo) {
    if (editor.isDisposed() || !editor.getComponent().isVisible()) return;

    PsiDocumentManager.getInstance(project).performLaterWhenAllCommitted(() -> {
      if (editor.isDisposed() || DumbService.isDumb(project) || !element.isValid() || (!ApplicationManager.getApplication().isUnitTestMode() && !EditorActivityManager.getInstance().isVisible(editor)))
        return;

      Document document = editor.getDocument();
      if (document.getTextLength() < elementStart) return;

      ParameterInfoController controller = ParameterInfoController.findControllerAtOffset(editor, elementStart);
      if (controller == null) {
        new ParameterInfoController(project, editor, elementStart, descriptors, highlighted, element, handler, true, requestFocus);
      }
      else {
        controller.setDescriptors(descriptors);
        controller.showHint(requestFocus, singleParameterInfo);
      }
    });
  }

  public void setRequestFocus(boolean requestFocus) {
    myRequestFocus = requestFocus;
  }

  public boolean isRequestFocus() {
    return myRequestFocus;
  }
}

/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.language.editor.markup;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.daemon.impl.DaemonTooltipRendererProvider;
import consulo.ide.impl.idea.codeInsight.daemon.impl.TrafficLightRenderer;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.DaemonCodeAnalyzerSettings;
import consulo.language.editor.impl.internal.daemon.DaemonEditorPopup;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.ErrorStripTooltipRendererProvider;
import consulo.codeEditor.internal.ErrorStripeRenderer;
import consulo.language.editor.impl.internal.markup.ErrorStripeUpdateManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 20-Sep-22
 */
@Singleton
@ServiceImpl
public class ErrorStripeUpdateManagerImpl extends ErrorStripeUpdateManager {
  private final Project myProject;
  private final PsiDocumentManager myPsiDocumentManager;

  @Inject
  public ErrorStripeUpdateManagerImpl(Project project) {
    myProject = project;
    myPsiDocumentManager = PsiDocumentManager.getInstance(myProject);
  }

  @Override
  public void repaintErrorStripePanel(Editor editor) {
    if (!myProject.isInitialized()) return;
    // Read PsiFile on background thread to avoid blocking EDT with read lock
    ReadAction.nonBlocking(() -> myPsiDocumentManager.getPsiFile(editor.getDocument()))
        .expireWith(myProject)
        .expireWhen(() -> editor.isDisposed())
        .finishOnUiThread(psiFile -> {
            if (!editor.isDisposed() && !myProject.isDisposed()) {
                repaintErrorStripePanel(editor, psiFile);
            }
        })
        .submit(AppExecutorUtil.getAppExecutorService());
  }

  @Override
  @RequiredUIAccess
  public void repaintErrorStripePanel(Editor editor, @Nullable PsiFile psiFile) {
    UIAccess.assertIsUIThread();
    if (!myProject.isInitialized()) return;
    EditorMarkupModel markup = (EditorMarkupModel)editor.getMarkupModel();
    markup.setErrorPanelPopupHandler(new DaemonEditorPopup(myProject, editor));
    markup.setErrorStripTooltipRendererProvider(createTooltipRenderer(editor));
    markup.setMinMarkHeight(DaemonCodeAnalyzerSettings.getInstance().getErrorStripeMarkMinHeight());
    setOrRefreshErrorStripeRenderer(markup, psiFile);
  }

  @Override
  @RequiredUIAccess
  public void setOrRefreshErrorStripeRenderer(EditorMarkupModel editorMarkupModel, @Nullable PsiFile file) {
    UIAccess.assertIsUIThread();
    if (!editorMarkupModel.isErrorStripeVisible() || !DaemonCodeAnalyzer.getInstance(myProject).isHighlightingAvailable(file)) {
      return;
    }
    ErrorStripeRenderer renderer = editorMarkupModel.getErrorStripeRenderer();
    if (renderer instanceof TrafficLightRenderer) {
      TrafficLightRenderer tlr = (TrafficLightRenderer)renderer;
      tlr.refresh(editorMarkupModel);
      editorMarkupModel.repaintTrafficLightIcon();
      if (tlr.isValid()) return;
    }
    Editor editor = editorMarkupModel.getEditor();
    if (editor.isDisposed()) return;

    editorMarkupModel.setErrorStripeRenderer(createRenderer(editor, file));
  }

  private ErrorStripTooltipRendererProvider createTooltipRenderer(Editor editor) {
    return new DaemonTooltipRendererProvider(myProject, editor);
  }

  protected @Nullable ErrorStripeRenderer createRenderer(Editor editor, @Nullable PsiFile file) {
    return new TrafficLightRenderer(myProject, editor.getDocument());
  }
}

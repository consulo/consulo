// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.ide.impl.idea.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import consulo.application.ApplicationManager;
import consulo.ide.ServiceManager;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.editor.ex.EditorMarkupModel;
import consulo.ide.impl.idea.openapi.editor.ex.ErrorStripTooltipRendererProvider;
import consulo.ide.impl.idea.openapi.editor.impl.DesktopEditorMarkupModelImpl;
import consulo.ide.impl.idea.openapi.editor.markup.ErrorStripeRenderer;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ErrorStripeUpdateManager {
  public static ErrorStripeUpdateManager getInstance(Project project) {
    return ServiceManager.getService(project, ErrorStripeUpdateManager.class);
  }

  private final Project myProject;
  private final PsiDocumentManager myPsiDocumentManager;

  public ErrorStripeUpdateManager(Project project) {
    myProject = project;
    myPsiDocumentManager = PsiDocumentManager.getInstance(myProject);
  }

  @SuppressWarnings("WeakerAccess") // Used in Rider
  public void repaintErrorStripePanel(@Nonnull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!myProject.isInitialized()) return;

    PsiFile file = myPsiDocumentManager.getPsiFile(editor.getDocument());
    final EditorMarkupModel markup = (EditorMarkupModel)editor.getMarkupModel();
    markup.setErrorPanelPopupHandler(new DaemonEditorPopup(myProject, editor));
    markup.setErrorStripTooltipRendererProvider(createTooltipRenderer(editor));
    markup.setMinMarkHeight(DaemonCodeAnalyzerSettings.getInstance().getErrorStripeMarkMinHeight());
    setOrRefreshErrorStripeRenderer(markup, file);
  }

  @SuppressWarnings("WeakerAccess") // Used in Rider
  protected void setOrRefreshErrorStripeRenderer(@Nonnull EditorMarkupModel editorMarkupModel, @Nullable PsiFile file) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!editorMarkupModel.isErrorStripeVisible() || !DaemonCodeAnalyzer.getInstance(myProject).isHighlightingAvailable(file)) {
      return;
    }
    ErrorStripeRenderer renderer = editorMarkupModel.getErrorStripeRenderer();
    if (renderer instanceof TrafficLightRenderer) {
      TrafficLightRenderer tlr = (TrafficLightRenderer)renderer;
      DesktopEditorMarkupModelImpl markupModelImpl = (DesktopEditorMarkupModelImpl)editorMarkupModel;
      tlr.refresh(markupModelImpl);
      markupModelImpl.repaintTrafficLightIcon();
      if (tlr.isValid()) return;
    }
    Editor editor = editorMarkupModel.getEditor();
    if (editor.isDisposed()) return;

    editorMarkupModel.setErrorStripeRenderer(createRenderer(editor, file));
  }

  @Nonnull
  private ErrorStripTooltipRendererProvider createTooltipRenderer(Editor editor) {
    return new DaemonTooltipRendererProvider(myProject, editor);
  }

  @Nullable
  protected TrafficLightRenderer createRenderer(@Nonnull Editor editor, @Nullable PsiFile file) {
    //for (TrafficLightRendererContributor contributor : TrafficLightRendererContributor.EP_NAME.getExtensionList()) {
    //  TrafficLightRenderer renderer = contributor.createRenderer(editor, file);
    //  if (renderer != null) {
    //    return renderer;
    //  }
    //}
    return new TrafficLightRenderer(myProject, editor.getDocument());
  }
}

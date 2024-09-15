// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.template;

import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.SelectionModel;
import consulo.document.Document;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.editor.template.event.TemplateEditingListener;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.util.AttachmentFactoryUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
public class CustomTemplateCallback {
  private static final Logger LOGGER = Logger.getInstance(CustomTemplateCallback.class);
  private final TemplateManager myTemplateManager;
  @Nonnull
  private final Editor myEditor;
  @Nonnull
  private final PsiFile myFile;
  private final int myOffset;
  @Nonnull
  private final Project myProject;
  private final boolean myInInjectedFragment;
  protected Set<TemplateContextType> myApplicableContextTypes;

  public CustomTemplateCallback(@Nonnull Editor editor, @Nonnull PsiFile file) {
    myProject = file.getProject();
    myTemplateManager = TemplateManager.getInstance(myProject);

    int parentEditorOffset = getOffset(editor);
    PsiElement element = InjectedLanguageManager.getInstance(file.getProject()).findInjectedElementAt(file, parentEditorOffset);
    myFile = element != null ? element.getContainingFile() : file;

    myInInjectedFragment = InjectedLanguageManager.getInstance(myProject).isInjectedFragment(myFile);
    myEditor = myInInjectedFragment ? LanguageEditorInternalHelper.getInstance().getEditorForInjectedLanguageNoCommit(editor, file, parentEditorOffset) : editor;
    myOffset = myInInjectedFragment ? getOffset(myEditor) : parentEditorOffset;
  }

  public TemplateManager getTemplateManager() {
    return myTemplateManager;
  }

  @Nonnull
  public PsiFile getFile() {
    return myFile;
  }

  @Nonnull
  public PsiElement getContext() {
    return getContext(myFile, getOffset(), myInInjectedFragment);
  }

  public int getOffset() {
    return myOffset;
  }

  public static int getOffset(@Nonnull Editor editor) {
    SelectionModel selectionModel = editor.getSelectionModel();
    return selectionModel.hasSelection() ? selectionModel.getSelectionStart() : Math.max(editor.getCaretModel().getOffset() - 1, 0);
  }

  @Nullable
  public Template findApplicableTemplate(@Nonnull String key) {
    return ContainerUtil.getFirstItem(findApplicableTemplates(key));
  }

  @Nonnull
  public List<Template> findApplicableTemplates(@Nonnull String key) {
    List<Template> result = new ArrayList<>();
    for (Template candidate : getMatchingTemplates(key)) {
      if (isAvailableTemplate(candidate)) {
        result.add(candidate);
      }
    }
    return result;
  }

  private boolean isAvailableTemplate(@Nonnull Template template) {
    if (myApplicableContextTypes == null) {
      myApplicableContextTypes = TemplateManager.getInstance(myProject).getApplicableContextTypes(TemplateActionContext.create(myFile, myEditor, myOffset, myOffset, false));
    }
    return !template.isDeactivated() && TemplateManager.getInstance(myProject).isApplicable(template, myApplicableContextTypes);
  }

  public void startTemplate(@Nonnull Template template, Map<String, String> predefinedValues, TemplateEditingListener listener) {
    if (myInInjectedFragment) {
      template.setToReformat(false);
    }
    myTemplateManager.startTemplate(myEditor, template, false, predefinedValues, listener);
  }

  @Nonnull
  private static List<Template> getMatchingTemplates(@Nonnull String templateKey) {
    TemplateSettings settings = TemplateSettings.getInstance();
    List<Template> candidates = new ArrayList<>();
    for (Template template : settings.getTemplates(templateKey)) {
      if (!template.isDeactivated()) {
        candidates.add(template);
      }
    }
    return candidates;
  }

  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }

  @Nonnull
  public FileType getFileType() {
    return myFile.getFileType();
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  public void deleteTemplateKey(@Nonnull String key) {
    int caretAt = myEditor.getCaretModel().getOffset();
    int templateStart = caretAt - key.length();
    myEditor.getDocument().deleteString(templateStart, caretAt);
    myEditor.getCaretModel().moveToOffset(templateStart);
    myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    myEditor.getSelectionModel().removeSelection();
  }

  @Nonnull
  public static PsiElement getContext(@Nonnull PsiFile file, int offset) {
    return getContext(file, offset, true);
  }

  @Nonnull
  public static PsiElement getContext(@Nonnull PsiFile file, int offset, boolean searchInInjectedFragment) {
    PsiElement element = null;
    if (searchInInjectedFragment && !InjectedLanguageManager.getInstance(file.getProject()).isInjectedFragment(file)) {
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
      Document document = documentManager.getDocument(file);
      if (document != null && !documentManager.isCommitted(document)) {
        LOGGER.error("Trying to access to injected template context on uncommited document, offset = " + offset, AttachmentFactoryUtil.createAttachment(file.getVirtualFile()));
      }
      else {
        element = InjectedLanguageManager.getInstance(file.getProject()).findInjectedElementAt(file, offset);
      }
    }
    if (element == null) {
      element = PsiUtilCore.getElementAtOffset(file, offset);
    }
    return element;
  }

  public boolean isInInjectedFragment() {
    return myInInjectedFragment;
  }
}

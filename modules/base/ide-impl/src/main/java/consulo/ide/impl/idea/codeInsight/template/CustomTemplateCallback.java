// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.template;

import consulo.ide.impl.idea.codeInsight.template.impl.TemplateImpl;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateManagerImpl;
import consulo.ide.impl.idea.codeInsight.template.impl.TemplateSettings;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.event.TemplateEditingListener;
import consulo.language.util.AttachmentFactoryUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.ide.impl.idea.openapi.diagnostic.Logger;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.SelectionModel;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiUtilCore;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
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
    myEditor = myInInjectedFragment ? InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file, parentEditorOffset) : editor;
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
  public TemplateImpl findApplicableTemplate(@Nonnull String key) {
    return ContainerUtil.getFirstItem(findApplicableTemplates(key));
  }

  @Nonnull
  public List<TemplateImpl> findApplicableTemplates(@Nonnull String key) {
    List<TemplateImpl> result = new ArrayList<>();
    for (TemplateImpl candidate : getMatchingTemplates(key)) {
      if (isAvailableTemplate(candidate)) {
        result.add(candidate);
      }
    }
    return result;
  }

  private boolean isAvailableTemplate(@Nonnull TemplateImpl template) {
    if (myApplicableContextTypes == null) {
      myApplicableContextTypes = TemplateManagerImpl.getApplicableContextTypes(TemplateActionContext.create(myFile, myEditor, myOffset, myOffset, false));
    }
    return !template.isDeactivated() && TemplateManagerImpl.isApplicable(template, myApplicableContextTypes);
  }

  public void startTemplate(@Nonnull Template template, Map<String, String> predefinedValues, TemplateEditingListener listener) {
    if (myInInjectedFragment) {
      template.setToReformat(false);
    }
    myTemplateManager.startTemplate(myEditor, template, false, predefinedValues, listener);
  }

  @Nonnull
  private static List<TemplateImpl> getMatchingTemplates(@Nonnull String templateKey) {
    TemplateSettings settings = TemplateSettings.getInstance();
    List<TemplateImpl> candidates = new ArrayList<>();
    for (TemplateImpl template : settings.getTemplates(templateKey)) {
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

/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.daemon.impl.quickfix;

import consulo.codeEditor.Editor;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.language.editor.refactoring.rename.RenameProcessor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author ven
 */
public class RenameElementFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private static final Logger LOG = Logger.getInstance(RenameElementFix.class);

  private final String myNewName;
  private final LocalizeValue myText;

  public RenameElementFix(@Nonnull PsiNamedElement element) {
    super(element);
    VirtualFile vFile = element.getContainingFile().getVirtualFile();
    assert vFile != null : element;
    myNewName = vFile.getNameWithoutExtension();
    myText = CodeInsightLocalize.renamePublicClassText(element.getName(), myNewName);
  }

  public RenameElementFix(@Nonnull PsiNamedElement element, @Nonnull String newName) {
    super(element);
    myNewName = newName;
    myText = CodeInsightLocalize.renameNamedElementText(element.getName(), myNewName);
  }

  @Override
  @Nonnull
  public LocalizeValue getText() {
    return myText;
  }

  @Override
  public void invoke(@Nonnull Project project,
                     @Nonnull PsiFile file,
                     @Nullable Editor editor,
                     @Nonnull PsiElement startElement,
                     @Nonnull PsiElement endElement) {
    if (isAvailable(project, null, file)) {
      LOG.assertTrue(file == startElement.getContainingFile());
      if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;
      RenameProcessor processor = new RenameProcessor(project, startElement, myNewName, false, false);
      processor.run();
    }
  }

  @Override
  public boolean isAvailable(@Nonnull Project project,
                             @Nonnull PsiFile file,
                             @Nonnull PsiElement startElement,
                             @Nonnull PsiElement endElement) {
    if (!startElement.isValid()) {
      return false;
    }
    NamesValidator namesValidator = NamesValidator.forLanguage(file.getLanguage());
    return namesValidator != null && namesValidator.isIdentifier(myNewName, project);
  }


  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.move;

import consulo.document.util.TextRange;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.path.FileReferenceOwner;
import consulo.language.psi.path.PsiFileReference;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileReferenceContextUtil {
  private static final Logger LOG = Logger.getInstance(FileReferenceContextUtil.class);
  private static final Key<PsiFileSystemItem> REF_FILE_SYSTEM_ITEM_KEY = Key.create("REF_FILE_SYSTEM_ITEM_KEY");

  private FileReferenceContextUtil() {
  }

  public static Map<String, PsiFileSystemItem> encodeFileReferences(PsiElement element) {
    final Map<String, PsiFileSystemItem> map = new HashMap<>();
    if (element == null || element instanceof PsiCompiledElement || isBinary(element)) return map;
    element.accept(new PsiRecursiveElementWalkingVisitor(true) {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof PsiLanguageInjectionHost) {
          InjectedLanguageManager.getInstance(element.getProject()).enumerate(element, new PsiLanguageInjectionHost.InjectedPsiVisitor() {
            @Override
            public void visit(@Nonnull PsiFile injectedPsi, @Nonnull List<PsiLanguageInjectionHost.Shred> places) {
              encodeFileReferences(injectedPsi);
            }
          });
        }

        PsiReference[] refs = element.getReferences();
        if (refs.length > 0 && refs[0] instanceof FileReferenceOwner) {
          PsiFileReference ref = ((FileReferenceOwner)refs[0]).getLastFileReference();
          if (ref != null) {
            ResolveResult[] results = ref.multiResolve(false);
            for (ResolveResult result : results) {
              if (result.getElement() instanceof PsiFileSystemItem) {
                PsiFileSystemItem fileSystemItem = (PsiFileSystemItem)result.getElement();
                element.putCopyableUserData(REF_FILE_SYSTEM_ITEM_KEY, fileSystemItem);
                map.put(element.getText(), fileSystemItem);
                break;
              }
            }
          }
        }
        super.visitElement(element);
      }
    });
    return map;
  }

  private static boolean isBinary(PsiElement element) {
    PsiFile containingFile = element.getContainingFile();
    if (containingFile == null || containingFile.getFileType().isBinary()) return true;
    return false;
  }

  public static void decodeFileReferences(PsiElement element) {
    if (element == null || element instanceof PsiCompiledElement || isBinary(element)) return;
    element.accept(new PsiRecursiveElementVisitor(true) {
      @Override
      public void visitElement(PsiElement element) {
        PsiFileSystemItem item = element.getCopyableUserData(REF_FILE_SYSTEM_ITEM_KEY);
        element.putCopyableUserData(REF_FILE_SYSTEM_ITEM_KEY, null);
        element = bindElement(element, item);
        if (element != null) {
          element.acceptChildren(this);
        }

        if (element instanceof PsiLanguageInjectionHost) {
          InjectedLanguageManager.getInstance(element.getProject()).enumerate(element, new PsiLanguageInjectionHost.InjectedPsiVisitor() {
            @Override
            public void visit(@Nonnull PsiFile injectedPsi, @Nonnull List<PsiLanguageInjectionHost.Shred> places) {
              decodeFileReferences(injectedPsi);
            }
          });
        }
      }
    });
  }

  public static void decodeFileReferences(PsiElement element, final Map<String, PsiFileSystemItem> map, final TextRange range) {
    if (element == null || element instanceof PsiCompiledElement || isBinary(element)) return;
    element.accept(new PsiRecursiveElementVisitor(true) {
      @Override
      public void visitElement(PsiElement element) {
        if (!range.intersects(element.getTextRange())) return;
        String text = element.getText();
        PsiFileSystemItem item = map.get(text);
        element.putCopyableUserData(REF_FILE_SYSTEM_ITEM_KEY, item);
        element.acceptChildren(this);
      }
    });
    decodeFileReferences(element);
  }

  private static PsiElement bindElement(PsiElement element, PsiFileSystemItem item) {
    if (item != null && item.isValid() && item.getVirtualFile() != null) {
      PsiReference[] refs = element.getReferences();
      for (PsiReference ref : refs) {
        if (ref instanceof FileReferenceOwner) {
          PsiFileReference fileReference = ((FileReferenceOwner)refs[0]).getLastFileReference();
          if (fileReference != null) {
            try {
              PsiElement newElement = fileReference.bindToElement(item);
              if (newElement != null) {
                // assertion for troubles like IDEA-59540
                LOG.assertTrue(element.getClass() == newElement.getClass(), "Reference " + ref + " violated contract of bindToElement()");
              }
              return newElement;
            }
            catch (IncorrectOperationException e) {
              LOG.error(e);
            }
          }
          break;
        }
      }
    }
    return element;
  }
}

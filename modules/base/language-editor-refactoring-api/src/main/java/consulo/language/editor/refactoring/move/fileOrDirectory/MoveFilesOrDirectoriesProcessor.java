/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.move.fileOrDirectory;

import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.event.RefactoringEventData;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.move.FileReferenceContextUtil;
import consulo.language.editor.refactoring.move.MoveCallback;
import consulo.language.editor.refactoring.move.MoveFileHandler;
import consulo.language.editor.refactoring.rename.RenameUtil;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.util.EditorHelper;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.path.PsiDynaReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.usage.NonCodeUsageInfo;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveFilesOrDirectoriesProcessor extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(MoveFilesOrDirectoriesProcessor.class);

  protected final PsiElement[] myElementsToMove;
  protected final boolean mySearchForReferences;
  protected final boolean mySearchInComments;
  protected final boolean mySearchInNonJavaFiles;
  private final PsiDirectory myNewParent;
  private final MoveCallback myMoveCallback;
  private NonCodeUsageInfo[] myNonCodeUsages;
  private final Map<PsiFile, List<UsageInfo>> myFoundUsages = new HashMap<>();

  public MoveFilesOrDirectoriesProcessor(Project project,
                                         PsiElement[] elements,
                                         PsiDirectory newParent,
                                         boolean searchInComments,
                                         boolean searchInNonJavaFiles,
                                         MoveCallback moveCallback,
                                         Runnable prepareSuccessfulCallback) {
    this(project, elements, newParent, true, searchInComments, searchInNonJavaFiles, moveCallback, prepareSuccessfulCallback);
  }

  public MoveFilesOrDirectoriesProcessor(Project project,
                                         PsiElement[] elements,
                                         PsiDirectory newParent,
                                         boolean searchForReferences,
                                         boolean searchInComments,
                                         boolean searchInNonJavaFiles,
                                         MoveCallback moveCallback,
                                         Runnable prepareSuccessfulCallback) {
    super(project, prepareSuccessfulCallback);
    myElementsToMove = elements;
    myNewParent = newParent;
    mySearchForReferences = searchForReferences;
    mySearchInComments = searchInComments;
    mySearchInNonJavaFiles = searchInNonJavaFiles;
    myMoveCallback = moveCallback;
  }

  @Override
  @Nonnull
  protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
    return new MoveFilesOrDirectoriesViewDescriptor(myElementsToMove, myNewParent);
  }

  @Override
  @Nonnull
  protected UsageInfo[] findUsages() {
    ArrayList<UsageInfo> result = new ArrayList<>();
    for (int i = 0; i < myElementsToMove.length; i++) {
      PsiElement element = myElementsToMove[i];
      if (mySearchForReferences) {
        for (PsiReference reference : ReferencesSearch.search(element, GlobalSearchScope.projectScope(myProject))) {
          result.add(new MyUsageInfo(reference.getElement(), i, reference));
        }
      }
      findElementUsages(result, element);
    }

    return result.toArray(new UsageInfo[result.size()]);
  }

  private void findElementUsages(ArrayList<UsageInfo> result, PsiElement element) {
    if (!mySearchForReferences) {
      return;
    }
    if (element instanceof PsiFile file) {
      List<UsageInfo> usages =
        MoveFileHandler.forElement(file).findUsages(file, myNewParent, mySearchInComments, mySearchInNonJavaFiles);
      if (usages != null) {
        result.addAll(usages);
        myFoundUsages.put((PsiFile)element, usages);
      }
    }
    else if (element instanceof PsiDirectory) {
      for (PsiElement childElement : element.getChildren()) {
        findElementUsages(result, childElement);
      }
    }
  }

  @Override
  protected void refreshElements(@Nonnull PsiElement[] elements) {
    LOG.assertTrue(elements.length == myElementsToMove.length);
    System.arraycopy(elements, 0, myElementsToMove, 0, elements.length);
  }

  @Override
  protected void performPsiSpoilingRefactoring() {
    if (myNonCodeUsages != null) {
      RenameUtil.renameNonCodeUsages(myProject, myNonCodeUsages);
    }
  }

  @Override
  protected void performRefactoring(@Nonnull UsageInfo[] usages) {
    // If files are being moved then I need to collect some information to delete these
    // files from CVS. I need to know all common parents of the moved files and relative
    // paths.

    // Move files with correction of references.

    try {

      List<PsiFile> movedFiles = new ArrayList<>();
      Map<PsiElement, PsiElement> oldToNewMap = new HashMap<>();
      for (PsiElement element : myElementsToMove) {
        RefactoringElementListener elementListener = getTransaction().getElementListener(element);

        if (element instanceof PsiDirectory directory) {
          if (mySearchForReferences) {
            encodeDirectoryFiles(directory);
          }
          MoveFilesOrDirectoriesUtil.doMoveDirectory(directory, myNewParent);
          for (PsiElement psiElement : directory.getChildren()) {
            processDirectoryFiles(movedFiles, oldToNewMap, psiElement);
          }
        }
        else if (element instanceof PsiFile movedFile) {
          if (mySearchForReferences) {
            FileReferenceContextUtil.encodeFileReferences(element);
          }
          MoveFileHandler.forElement(movedFile).prepareMovedFile(movedFile, myNewParent, oldToNewMap);

          PsiFile moving = myNewParent.findFile(movedFile.getName());
          if (moving == null) {
            MoveFilesOrDirectoriesUtil.doMoveFile(movedFile, myNewParent);
          }
          moving = myNewParent.findFile(movedFile.getName());
          movedFiles.add(moving);
        }

        elementListener.elementMoved(element);
      }
      // sort by offset descending to process correctly several usages in one PsiElement [IDEADEV-33013]
      CommonRefactoringUtil.sortDepthFirstRightLeftOrder(usages);

      DumbService.getInstance(myProject).completeJustSubmittedTasks();

      // fix references in moved files to outer files
      for (PsiFile movedFile : movedFiles) {
        MoveFileHandler.forElement(movedFile).updateMovedFile(movedFile);
        if (mySearchForReferences) FileReferenceContextUtil.decodeFileReferences(movedFile);
      }

      retargetUsages(usages, oldToNewMap);

      if (MoveFilesOrDirectoriesDialog.isOpenInEditor()) {
        EditorHelper.openFilesInEditor(movedFiles.toArray(new PsiFile[movedFiles.size()]));
      }

      // Perform CVS "add", "remove" commands on moved files.

      if (myMoveCallback != null) {
        myMoveCallback.refactoringCompleted();
      }

    }
    catch (IncorrectOperationException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        LOG.info(e);
        myProject.getApplication().invokeLater(
          () -> Messages.showMessageDialog(myProject, cause.getMessage(), RefactoringLocalize.errorTitle().get(), UIUtil.getErrorIcon())
        );
      }
      else {
        LOG.error(e);
      }
    }
  }

  @Nullable
  @Override
  protected String getRefactoringId() {
    return "refactoring.move";
  }

  @Nullable
  @Override
  protected RefactoringEventData getBeforeData() {
    RefactoringEventData data = new RefactoringEventData();
    data.addElements(myElementsToMove);
    return data;
  }

  @Nullable
  @Override
  protected RefactoringEventData getAfterData(@Nonnull UsageInfo[] usages) {
    RefactoringEventData data = new RefactoringEventData();
    data.addElement(myNewParent);
    return data;
  }

  private static void encodeDirectoryFiles(PsiElement psiElement) {
    if (psiElement instanceof PsiFile) {
      FileReferenceContextUtil.encodeFileReferences(psiElement);
    }
    else if (psiElement instanceof PsiDirectory) {
      for (PsiElement element : psiElement.getChildren()) {
        encodeDirectoryFiles(element);
      }
    }
  }

  private static void processDirectoryFiles(List<PsiFile> movedFiles, Map<PsiElement, PsiElement> oldToNewMap, PsiElement psiElement) {
    if (psiElement instanceof PsiFile movedFile) {
      MoveFileHandler.forElement(movedFile).prepareMovedFile(movedFile, movedFile.getParent(), oldToNewMap);
      movedFiles.add(movedFile);
    }
    else if (psiElement instanceof PsiDirectory) {
      for (PsiElement element : psiElement.getChildren()) {
        processDirectoryFiles(movedFiles, oldToNewMap, element);
      }
    }
  }

  protected void retargetUsages(UsageInfo[] usages, Map<PsiElement, PsiElement> oldToNewMap) {
    List<NonCodeUsageInfo> nonCodeUsages = new ArrayList<>();
    for (UsageInfo usageInfo : usages) {
      if (usageInfo instanceof MyUsageInfo info) {
        PsiElement element = myElementsToMove[info.myIndex];

        if (info.getReference() instanceof FileReference || info.getReference() instanceof PsiDynaReference) {
          PsiElement usageElement = info.getElement();
          if (usageElement != null) {
            PsiFile usageFile = usageElement.getContainingFile();
            PsiFile psiFile = usageFile.getViewProvider().getPsi(usageFile.getViewProvider().getBaseLanguage());
            if (psiFile != null && psiFile.equals(element)) {
              continue;  // already processed in MoveFilesOrDirectoriesUtil.doMoveFile
            }
          }
        }
        PsiElement refElement = info.myReference.getElement();
        if (refElement != null && refElement.isValid()) {
          info.myReference.bindToElement(element);
        }
      } else if (usageInfo instanceof NonCodeUsageInfo nonCodeUsageInfo) {
        nonCodeUsages.add(nonCodeUsageInfo);
      }
    }

    for (PsiFile movedFile : myFoundUsages.keySet()) {
      MoveFileHandler.forElement(movedFile).retargetUsages(myFoundUsages.get(movedFile), oldToNewMap);
    }

    myNonCodeUsages = nonCodeUsages.toArray(new NonCodeUsageInfo[nonCodeUsages.size()]);
  }

  @Override
  protected String getCommandName() {
    return RefactoringLocalize.moveTitle().get();
  }

  static class MyUsageInfo extends UsageInfo {
    int myIndex;
    PsiReference myReference;

    public MyUsageInfo(PsiElement element, int index, PsiReference reference) {
      super(element);
      myIndex = index;
      myReference = reference;
    }
  }
}

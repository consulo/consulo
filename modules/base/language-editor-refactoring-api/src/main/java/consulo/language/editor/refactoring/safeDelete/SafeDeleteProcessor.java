/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.safeDelete;

import consulo.application.ApplicationManager;
import consulo.application.util.function.Processor;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.editor.refactoring.event.RefactoringEventData;
import consulo.language.editor.refactoring.event.RefactoringEventListener;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteCustomUsageInfo;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteReferenceUsageInfo;
import consulo.language.editor.refactoring.safeDelete.usageInfo.SafeDeleteUsageInfo;
import consulo.language.editor.refactoring.ui.RefactoringUIUtil;
import consulo.language.editor.refactoring.util.NonCodeSearchDescriptionLocation;
import consulo.language.editor.refactoring.util.TextOccurrencesUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.usage.*;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.function.Condition;
import consulo.util.lang.ref.Ref;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author dsl
 */
public class SafeDeleteProcessor extends BaseRefactoringProcessor {
  private static final Logger LOG = Logger.getInstance(SafeDeleteProcessor.class);
  private final PsiElement[] myElements;
  private boolean mySearchInCommentsAndStrings;
  private boolean mySearchNonJava;
  private boolean myPreviewNonCodeUsages = true;

  private SafeDeleteProcessor(Project project,
                              @Nullable Runnable prepareSuccessfulCallback,
                              PsiElement[] elementsToDelete,
                              boolean isSearchInComments,
                              boolean isSearchNonJava) {
    super(project, prepareSuccessfulCallback);
    myElements = elementsToDelete;
    mySearchInCommentsAndStrings = isSearchInComments;
    mySearchNonJava = isSearchNonJava;
  }

  @Override
  @Nonnull
  protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
    return new SafeDeleteUsageViewDescriptor(myElements);
  }

  private static boolean isInside(PsiElement place, PsiElement[] ancestors) {
    return isInside(place, Arrays.asList(ancestors));
  }

  private static boolean isInside(PsiElement place, Collection<? extends PsiElement> ancestors) {
    for (PsiElement element : ancestors) {
      if (isInside(place, element)) return true;
    }
    return false;
  }

  public static boolean isInside(PsiElement place, PsiElement ancestor) {
    if (ancestor instanceof PsiDirectoryContainer directoryContainer) {
      final PsiDirectory[] directories = directoryContainer.getDirectories(place.getResolveScope());
      for (PsiDirectory directory : directories) {
        if (isInside(place, directory)) return true;
      }
    }

    if (ancestor instanceof PsiFile ancestorFile) {
      for (PsiFile file : ancestorFile.getViewProvider().getAllFiles()) {
        if (PsiTreeUtil.isAncestor(file, place, false)) return true;
      }
    }

    boolean isAncestor = PsiTreeUtil.isAncestor(ancestor, place, false);
    if (!isAncestor && ancestor instanceof PsiNameIdentifierOwner nameIdentifierOwner) {
      final PsiElement nameIdentifier = nameIdentifierOwner.getNameIdentifier();
      if (nameIdentifier != null && !PsiTreeUtil.isAncestor(ancestor, nameIdentifier, true)) {
        isAncestor = PsiTreeUtil.isAncestor(nameIdentifier.getParent(), place, false);
      }
    }

    if (!isAncestor) {
      final InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(place.getProject());
      PsiLanguageInjectionHost host = injectedLanguageManager.getInjectionHost(place);
      while (host != null) {
        if (PsiTreeUtil.isAncestor(ancestor, host, false)) {
          isAncestor = true;
          break;
        }
        host = injectedLanguageManager.getInjectionHost(host);
      }
    }
    return isAncestor;
  }

  @Override
  @Nonnull
  protected UsageInfo[] findUsages() {
    List<UsageInfo> usages = Collections.synchronizedList(new ArrayList<UsageInfo>());
    for (PsiElement element : myElements) {
      boolean handled = false;
      for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
        if (delegate.handlesElement(element)) {
          final NonCodeUsageSearchInfo filter = delegate.findUsages(element, myElements, usages);
          if (filter != null) {
            for (PsiElement nonCodeUsageElement : filter.getElementsToSearch()) {
              addNonCodeUsages(nonCodeUsageElement, usages, filter.getInsideDeletedCondition(), mySearchNonJava, mySearchInCommentsAndStrings);
            }
          }
          handled = true;
          break;
        }
      }
      if (!handled && element instanceof PsiNamedElement) {
        findGenericElementUsages(element, usages, myElements);
        addNonCodeUsages(element, usages, getDefaultInsideDeletedCondition(myElements), mySearchNonJava, mySearchInCommentsAndStrings);
      }
    }
    final UsageInfo[] result = usages.toArray(new UsageInfo[usages.size()]);
    return UsageViewUtil.removeDuplicatedUsages(result);
  }

  public static Condition<PsiElement> getDefaultInsideDeletedCondition(final PsiElement[] elements) {
    return new Condition<PsiElement>() {
      @Override
      public boolean value(final PsiElement usage) {
        return !(usage instanceof PsiFile) && isInside(usage, elements);
      }
    };
  }

  public static void findGenericElementUsages(final PsiElement element, final List<UsageInfo> usages, final PsiElement[] allElementsToDelete) {
    ReferencesSearch.search(element).forEach(new Processor<PsiReference>() {
      @Override
      public boolean process(final PsiReference reference) {
        final PsiElement refElement = reference.getElement();
        if (!isInside(refElement, allElementsToDelete)) {
          usages.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(refElement, element, false));
        }
        return true;
      }
    });
  }

  @Override
  @RequiredUIAccess
  protected boolean preprocessUsages(@Nonnull SimpleReference<UsageInfo[]> refUsages) {
    UsageInfo[] usages = refUsages.get();
    ArrayList<String> conflicts = new ArrayList<String>();

    for (PsiElement element : myElements) {
      for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
        if (delegate.handlesElement(element)) {
          Collection<String> foundConflicts = delegate.findConflicts(element, myElements);
          if (foundConflicts != null) {
            conflicts.addAll(foundConflicts);
          }
          break;
        }
      }
    }

    final HashMap<PsiElement, UsageHolder> elementsToUsageHolders = sortUsages(usages);
    final Collection<UsageHolder> usageHolders = elementsToUsageHolders.values();
    for (UsageHolder usageHolder : usageHolders) {
      if (usageHolder.hasUnsafeUsagesInCode()) {
        conflicts.add(usageHolder.getDescription());
      }
    }

    if (!conflicts.isEmpty()) {
      final RefactoringEventData conflictData = new RefactoringEventData();
      conflictData.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts);
      myProject.getMessageBus().syncPublisher(RefactoringEventListener.class).conflictsDetected("refactoring.safeDelete", conflictData);
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        if (!ConflictsInTestsException.isTestIgnore()) throw new ConflictsInTestsException(conflicts);
      }
      else {
        UnsafeUsagesDialog dialog = new UnsafeUsagesDialog(ArrayUtil.toStringArray(conflicts), myProject);
        if (!dialog.showAndGet()) {
          final int exitCode = dialog.getExitCode();
          prepareSuccessful(); // dialog is always dismissed;
          if (exitCode == UnsafeUsagesDialog.VIEW_USAGES_EXIT_CODE) {
            showUsages(usages);
          }
          return false;
        }
        else {
          myPreviewNonCodeUsages = false;
        }
      }
    }

    UsageInfo[] preprocessedUsages = usages;
    for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
      preprocessedUsages = delegate.preprocessUsages(myProject, preprocessedUsages);
      if (preprocessedUsages == null) return false;
    }
    final UsageInfo[] filteredUsages = UsageViewUtil.removeDuplicatedUsages(preprocessedUsages);
    prepareSuccessful(); // dialog is always dismissed
    if (filteredUsages == null) {
      return false;
    }
    refUsages.set(filteredUsages);
    return true;
  }

  private void showUsages(final UsageInfo[] usages) {
    UsageViewPresentation presentation = new UsageViewPresentation();
    presentation.setTabText(RefactoringLocalize.safeDeleteTitle().get());
    presentation.setTargetsNodeText(RefactoringLocalize.attemptingToDeleteTargetsNodeText().get());
    presentation.setShowReadOnlyStatusAsRed(true);
    presentation.setShowCancelButton(true);
    presentation.setCodeUsagesString(RefactoringLocalize.referencesFoundInCode().get());
    presentation.setUsagesInGeneratedCodeString(RefactoringLocalize.referencesFoundInGeneratedCode().get());
    presentation.setNonCodeUsagesString(RefactoringLocalize.occurrencesFoundInCommentsStringsAndNonJavaFiles().get());
    presentation.setUsagesString(RefactoringLocalize.usageviewUsagestext().get());

    UsageViewManager manager = UsageViewManager.getInstance(myProject);
    final UsageView usageView = showUsages(usages, presentation, manager);
    usageView.addPerformOperationAction(
      new RerunSafeDelete(myProject, myElements, usageView),
      RefactoringLocalize.retryCommand().get(),
      null,
      RefactoringLocalize.rerunSafeDelete().get()
    );
    usageView.addPerformOperationAction(
      () -> {
        UsageInfo[] preprocessedUsages = usages;
        for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
          preprocessedUsages = delegate.preprocessUsages(myProject, preprocessedUsages);
          if (preprocessedUsages == null) return;
        }
        final UsageInfo[] filteredUsages = UsageViewUtil.removeDuplicatedUsages(preprocessedUsages);
        execute(filteredUsages);
      },
      "Delete Anyway",
      RefactoringLocalize.usageviewNeedRerun().get(),
      RefactoringLocalize.usageviewDoaction().get()
    );
  }

  private UsageView showUsages(UsageInfo[] usages, UsageViewPresentation presentation, UsageViewManager manager) {
    for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
      if (delegate instanceof SafeDeleteProcessorDelegateBase safeDeleteProcessorDelegateBase) {
        final UsageView view = safeDeleteProcessorDelegateBase.showUsages(usages, presentation, manager, myElements);
        if (view != null) return view;
      }
    }
    UsageTarget[] targets = new UsageTarget[myElements.length];
    for (int i = 0; i < targets.length; i++) {
      targets[i] = RefactoringInternalHelper.getInstance().createPsiElement2UsageTargetAdapter(myElements[i]);
    }

    return manager.showUsages(targets, UsageInfoToUsageConverter.convert(myElements, usages), presentation);
  }

  public PsiElement[] getElements() {
    return myElements;
  }

  private static class RerunSafeDelete implements Runnable {
    final SmartPsiElementPointer[] myPointers;
    private final Project myProject;
    private final UsageView myUsageView;

    RerunSafeDelete(Project project, PsiElement[] elements, UsageView usageView) {
      myProject = project;
      myUsageView = usageView;
      myPointers = new SmartPsiElementPointer[elements.length];
      for (int i = 0; i < elements.length; i++) {
        PsiElement element = elements[i];
        myPointers[i] = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(element);
      }
    }

    @Override
    public void run() {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          PsiDocumentManager.getInstance(myProject).commitAllDocuments();
          myUsageView.close();
          ArrayList<PsiElement> elements = new ArrayList<PsiElement>();
          for (SmartPsiElementPointer pointer : myPointers) {
            final PsiElement element = pointer.getElement();
            if (element != null) {
              elements.add(element);
            }
          }
          if (!elements.isEmpty()) {
            SafeDeleteHandler.invoke(myProject, PsiUtilCore.toPsiElementArray(elements), true);
          }
        }
      });
    }
  }

  /**
   * @param usages
   * @return Map from elements to UsageHolders
   */
  private static HashMap<PsiElement, UsageHolder> sortUsages(@Nonnull UsageInfo[] usages) {
    HashMap<PsiElement, UsageHolder> result = new HashMap<PsiElement, UsageHolder>();

    for (final UsageInfo usage : usages) {
      if (usage instanceof SafeDeleteUsageInfo safeDeleteUsageInfo) {
        final PsiElement referencedElement = safeDeleteUsageInfo.getReferencedElement();
        if (!result.containsKey(referencedElement)) {
          result.put(referencedElement, new UsageHolder(referencedElement, usages));
        }
      }
    }
    return result;
  }


  @Override
  protected void refreshElements(@Nonnull PsiElement[] elements) {
    LOG.assertTrue(elements.length == myElements.length);
    System.arraycopy(elements, 0, myElements, 0, elements.length);
  }

  @Override
  protected boolean isPreviewUsages(@Nonnull UsageInfo[] usages) {
    if (myPreviewNonCodeUsages && UsageViewUtil.reportNonRegularUsages(usages, myProject)) {
      return true;
    }

    return super.isPreviewUsages(filterToBeDeleted(usages));
  }

  private static UsageInfo[] filterToBeDeleted(UsageInfo[] infos) {
    ArrayList<UsageInfo> list = new ArrayList<UsageInfo>();
    for (UsageInfo info : infos) {
      if (!(info instanceof SafeDeleteReferenceUsageInfo safeDeleteReferenceUsageInfo) || safeDeleteReferenceUsageInfo.isSafeDelete()) {
        list.add(info);
      }
    }
    return list.toArray(new UsageInfo[list.size()]);
  }

  @Nullable
  @Override
  protected RefactoringEventData getBeforeData() {
    final RefactoringEventData beforeData = new RefactoringEventData();
    beforeData.addElements(myElements);
    return beforeData;
  }

  @Nullable
  @Override
  protected String getRefactoringId() {
    return "refactoring.safeDelete";
  }

  @Override
  protected void performRefactoring(@Nonnull UsageInfo[] usages) {
    try {
      for (UsageInfo usage : usages) {
        if (usage instanceof SafeDeleteCustomUsageInfo safeDeleteCustomUsageInfo) {
          safeDeleteCustomUsageInfo.performRefactoring();
        }
      }

      for (PsiElement element : myElements) {
        for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
          if (delegate.handlesElement(element)) {
            delegate.prepareForDeletion(element);
          }
        }

        element.delete();
      }
    }
    catch (IncorrectOperationException e) {
      RefactoringUIUtil.processIncorrectOperation(myProject, e);
    }
  }

  private String calcCommandName() {
    return RefactoringLocalize.safeDeleteCommand(RefactoringUIUtil.calculatePsiElementDescriptionList(myElements)).get();
  }

  private String myCachedCommandName = null;

  @Override
  protected String getCommandName() {
    if (myCachedCommandName == null) {
      myCachedCommandName = calcCommandName();
    }
    return myCachedCommandName;
  }


  public static void addNonCodeUsages(
    final PsiElement element,
    List<UsageInfo> usages,
    @Nullable final Condition<PsiElement> insideElements,
    boolean searchNonJava,
    boolean searchInCommentsAndStrings
  ) {
    UsageInfoFactory nonCodeUsageFactory = (usage,  startOffset, endOffset) -> {
      if (insideElements != null && insideElements.value(usage)) {
        return null;
      }
      return new SafeDeleteReferenceSimpleDeleteUsageInfo(usage, element, startOffset, endOffset, true, false);
    };
    if (searchInCommentsAndStrings) {
      String stringToSearch = ElementDescriptionUtil.getElementDescription(element, NonCodeSearchDescriptionLocation.STRINGS_AND_COMMENTS);
      TextOccurrencesUtil.addUsagesInStringsAndComments(element, stringToSearch, usages, nonCodeUsageFactory);
    }
    if (searchNonJava) {
      String stringToSearch = ElementDescriptionUtil.getElementDescription(element, NonCodeSearchDescriptionLocation.NON_JAVA);
      TextOccurrencesUtil.addTextOccurences(element, stringToSearch, GlobalSearchScope.projectScope(element.getProject()), usages, nonCodeUsageFactory);
    }
  }

  @Override
  protected boolean isToBeChanged(@Nonnull UsageInfo usageInfo) {
    if (usageInfo instanceof SafeDeleteReferenceUsageInfo safeDeleteReferenceUsageInfo) {
      return safeDeleteReferenceUsageInfo.isSafeDelete() && super.isToBeChanged(usageInfo);
    }
    return super.isToBeChanged(usageInfo);
  }

  public static boolean validElement(@Nonnull PsiElement element) {
    if (element instanceof PsiFile) return true;
    if (!element.isPhysical()) return false;
    final RefactoringSupportProvider provider = RefactoringSupportProvider.forLanguage(element.getLanguage());
    return provider.isSafeDeleteAvailable(element);
  }

  public static SafeDeleteProcessor createInstance(
    Project project,
    @Nullable Runnable prepareSuccessfulCallback,
    PsiElement[] elementsToDelete,
    boolean isSearchInComments,
    boolean isSearchNonJava
  ) {
    return new SafeDeleteProcessor(project, prepareSuccessfulCallback, elementsToDelete, isSearchInComments, isSearchNonJava);
  }

  public static SafeDeleteProcessor createInstance(
    Project project,
    @Nullable Runnable prepareSuccessfulCallBack,
    PsiElement[] elementsToDelete,
    boolean isSearchInComments,
    boolean isSearchNonJava,
    boolean askForAccessors
  ) {
    ArrayList<PsiElement> elements = new ArrayList<>(Arrays.asList(elementsToDelete));
    HashSet<PsiElement> elementsToDeleteSet = new HashSet<>(Arrays.asList(elementsToDelete));

    for (PsiElement psiElement : elementsToDelete) {
      for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
        if (delegate.handlesElement(psiElement)) {
          Collection<PsiElement> addedElements = delegate.getAdditionalElementsToDelete(psiElement, elementsToDeleteSet, askForAccessors);
          if (addedElements != null) {
            elements.addAll(addedElements);
          }
          break;
        }
      }
    }

    return new SafeDeleteProcessor(project, prepareSuccessfulCallBack, PsiUtilCore.toPsiElementArray(elements), isSearchInComments, isSearchNonJava);
  }

  public boolean isSearchInCommentsAndStrings() {
    return mySearchInCommentsAndStrings;
  }

  public void setSearchInCommentsAndStrings(boolean searchInCommentsAndStrings) {
    mySearchInCommentsAndStrings = searchInCommentsAndStrings;
  }

  public boolean isSearchNonJava() {
    return mySearchNonJava;
  }

  public void setSearchNonJava(boolean searchNonJava) {
    mySearchNonJava = searchNonJava;
  }

  @Override
  protected boolean skipNonCodeUsages() {
    return true;
  }
}

/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.find.findUsages;

import consulo.application.AccessRule;
import consulo.application.ApplicationManager;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processor;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.Editor;
import consulo.component.ProcessCanceledException;
import consulo.content.scope.SearchScope;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorLocation;
import consulo.fileEditor.TextEditor;
import consulo.find.*;
import consulo.find.ui.AbstractFindUsagesDialog;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.progress.impl.ProgressManagerImpl;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.ui.LightweightHint;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.language.psi.IdePsiManagerImpl;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.search.SearchRequestCollector;
import consulo.language.psi.search.SearchSession;
import consulo.logging.Logger;
import consulo.module.content.ProjectFileIndex;
import consulo.navigation.NavigationItem;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.content.Content;
import consulo.usage.*;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * see {@link consulo.ide.impl.idea.find.impl.FindManagerImpl#getFindUsagesManager()}
 */
public class FindUsagesManager {
  private static final Logger LOG = Logger.getInstance(FindUsagesManager.class);

  private enum FileSearchScope {
    FROM_START,
    FROM_END,
    AFTER_CARET,
    BEFORE_CARET
  }

  private static final Key<String> KEY_START_USAGE_AGAIN = Key.create("KEY_START_USAGE_AGAIN");
  private static final String VALUE_START_USAGE_AGAIN = "START_AGAIN";
  private final Project myProject;
  private final UsageViewManager myAnotherManager;

  private PsiElement2UsageTargetComposite myLastSearchInFileData; // EDT only
  private final UsageHistory myHistory = new UsageHistory();

  public FindUsagesManager(@Nonnull Project project, @Nonnull UsageViewManager anotherManager) {
    myProject = project;
    myAnotherManager = anotherManager;
  }

  public boolean canFindUsages(@Nonnull final PsiElement element) {
    for (FindUsagesHandlerFactory factory : FindUsagesHandlerFactory.EP_NAME.getExtensionList(myProject)) {
      try {
        if (factory.canFindUsages(element)) {
          return true;
        }
      }
      catch (IndexNotReadyException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
    return false;
  }

  public void clearFindingNextUsageInFile() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myLastSearchInFileData = null;
  }

  public boolean findNextUsageInFile(@Nonnull FileEditor editor) {
    return findUsageInFile(editor, FileSearchScope.AFTER_CARET);
  }

  public boolean findPreviousUsageInFile(@Nonnull FileEditor editor) {
    return findUsageInFile(editor, FileSearchScope.BEFORE_CARET);
  }

  private boolean findUsageInFile(@Nonnull FileEditor editor, @Nonnull FileSearchScope direction) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (myLastSearchInFileData == null) return false;
    PsiElement[] primaryElements = myLastSearchInFileData.getPrimaryElements();
    PsiElement[] secondaryElements = myLastSearchInFileData.getSecondaryElements();
    if (primaryElements.length == 0) {//all elements have been invalidated
      Messages.showMessageDialog(
        myProject,
        FindBundle.message("find.searched.elements.have.been.changed.error"),
        FindBundle.message("cannot.search.for.usages.title"),
        UIUtil.getInformationIcon()
      );
      // SCR #10022
      //clearFindingNextUsageInFile();
      return false;
    }

    //todo
    TextEditor textEditor = (TextEditor)editor;
    Document document = textEditor.getEditor().getDocument();
    PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (psiFile == null) return false;

    final FindUsagesHandler handler = getFindUsagesHandler(primaryElements[0], false);
    if (handler == null) return false;
    findUsagesInEditor(primaryElements, secondaryElements, handler, psiFile, direction, myLastSearchInFileData.getOptions(), textEditor);
    return true;
  }


  private void initLastSearchElement(@Nonnull FindUsagesOptions findUsagesOptions,
                                     @Nonnull PsiElement[] primaryElements,
                                     @Nonnull PsiElement[] secondaryElements) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    myLastSearchInFileData = new PsiElement2UsageTargetComposite(primaryElements, secondaryElements, findUsagesOptions);
  }

  @Nullable
  public FindUsagesHandler getFindUsagesHandler(@Nonnull PsiElement element, final boolean forHighlightUsages) {
    for (FindUsagesHandlerFactory factory : FindUsagesHandlerFactory.EP_NAME.getExtensionList(myProject)) {
      if (factory.canFindUsages(element)) {
        final FindUsagesHandler handler = factory.createFindUsagesHandler(element, forHighlightUsages);
        if (handler == FindUsagesHandler.NULL_HANDLER) return null;
        if (handler != null) {
          return handler;
        }
      }
    }
    return null;
  }

  @Nullable
  public FindUsagesHandler getNewFindUsagesHandler(@Nonnull PsiElement element, final boolean forHighlightUsages) {
    for (FindUsagesHandlerFactory factory : FindUsagesHandlerFactory.EP_NAME.getExtensionList(myProject)) {
      if (factory.canFindUsages(element)) {
        Class<? extends FindUsagesHandlerFactory> aClass = factory.getClass();
        FindUsagesHandlerFactory copy = myProject.getInjectingContainer().getUnbindedInstance(aClass);
        final FindUsagesHandler handler = copy.createFindUsagesHandler(element, forHighlightUsages);
        if (handler == FindUsagesHandler.NULL_HANDLER) return null;
        if (handler != null) {
          return handler;
        }
      }
    }
    return null;
  }

  /**
   *
   * @param psiElement
   * @param scopeFile
   * @param editor
   * @param showDialog
   * @param searchScope null means default (stored in options)
   */
  public void findUsages(@Nonnull PsiElement psiElement,
                         final PsiFile scopeFile,
                         final FileEditor editor,
                         boolean showDialog,
                         @Nullable SearchScope searchScope) {
    FindUsagesHandler handler = getFindUsagesHandler(psiElement, false);
    if (handler == null) return;

    boolean singleFile = scopeFile != null;
    AbstractFindUsagesDialog dialog = handler.getFindUsagesDialog(singleFile, shouldOpenInNewTab(), mustOpenInNewTab());
    if (showDialog) {
      if (!dialog.showAndGet()) {
        return;
      }
    }
    else {
      dialog.close(DialogWrapper.OK_EXIT_CODE);
    }

    setOpenInNewTab(dialog.isShowInSeparateWindow());

    FindUsagesOptions findUsagesOptions = dialog.calcFindUsagesOptions();
    if (searchScope != null) {
      findUsagesOptions.searchScope = searchScope;
    }

    clearFindingNextUsageInFile();

    startFindUsages(findUsagesOptions, handler, scopeFile, editor);
  }

  public void startFindUsages(@Nonnull PsiElement psiElement, @Nonnull FindUsagesOptions findUsagesOptions, PsiFile scopeFile, FileEditor editor) {
    FindUsagesHandler handler = getFindUsagesHandler(psiElement, false);
    if (handler == null) return;
    startFindUsages(findUsagesOptions, handler, scopeFile, editor);
  }

  private void startFindUsages(@Nonnull FindUsagesOptions findUsagesOptions, @Nonnull FindUsagesHandler handler, PsiFile scopeFile, FileEditor editor) {
    boolean singleFile = scopeFile != null;

    clearFindingNextUsageInFile();
    LOG.assertTrue(handler.getPsiElement().isValid());
    PsiElement[] primaryElements = handler.getPrimaryElements();
    checkNotNull(primaryElements, handler, "getPrimaryElements()");
    PsiElement[] secondaryElements = handler.getSecondaryElements();
    checkNotNull(secondaryElements, handler, "getSecondaryElements()");
    if (singleFile) {
      editor.putUserData(KEY_START_USAGE_AGAIN, null);
      findUsagesInEditor(primaryElements, secondaryElements, handler, scopeFile, FileSearchScope.FROM_START, findUsagesOptions.clone(), editor);
    }
    else {
      boolean skipResultsWithOneUsage = FindSettings.getInstance().isSkipResultsWithOneUsage();
      findUsages(primaryElements, secondaryElements, handler, findUsagesOptions, skipResultsWithOneUsage);
    }
  }

  public static void showSettingsAndFindUsages(@Nonnull NavigationItem[] targets) {
    if (targets.length == 0) return;
    NavigationItem target = targets[0];
    if (!(target instanceof ConfigurableUsageTarget)) return;
    ((ConfigurableUsageTarget)target).showSettings();
  }

  private static void checkNotNull(@Nonnull PsiElement[] elements, @Nonnull FindUsagesHandler handler, @NonNls @Nonnull String methodName) {
    for (PsiElement element : elements) {
      if (element == null) {
        LOG.error(handler + "." + methodName + " has returned array with null elements: " + Arrays.asList(elements));
      }
    }
  }

  @Nonnull
  public static ProgressIndicator startProcessUsages(@Nonnull final FindUsagesHandler handler,
                                                     @Nonnull final PsiElement[] primaryElements,
                                                     @Nonnull final PsiElement[] secondaryElements,
                                                     @Nonnull final Processor<Usage> processor,
                                                     @Nonnull final FindUsagesOptions findUsagesOptions,
                                                     @Nonnull final Runnable onComplete) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final ProgressIndicatorBase indicator = new ProgressIndicatorBase();
    Task.Backgroundable task = new Task.Backgroundable(handler.getProject(), "Finding Usages") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        ThrowableComputable<UsageSearcher, RuntimeException> action = () -> {
          PsiElement2UsageTargetAdapter[] primaryTargets = PsiElement2UsageTargetAdapter.convert(primaryElements);
          PsiElement2UsageTargetAdapter[] secondaryTargets = PsiElement2UsageTargetAdapter.convert(secondaryElements);
          return createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, null);
        };
        UsageSearcher usageSearcher = AccessRule.read(action);
        usageSearcher.generate(processor);
      }
    };

    ((ProgressManagerImpl)ProgressManager.getInstance()).runProcessWithProgressAsynchronously(task, indicator, onComplete);

    return indicator;
  }

  @Nonnull
  public UsageViewPresentation createPresentation(@Nonnull FindUsagesHandler handler, @Nonnull FindUsagesOptions findUsagesOptions) {
    PsiElement element = handler.getPsiElement();
    LOG.assertTrue(element.isValid());
    return createPresentation(element, findUsagesOptions, FindSettings.getInstance().isShowResultsInSeparateView());
  }

  private void setOpenInNewTab(final boolean toOpenInNewTab) {
    if (!mustOpenInNewTab()) {
      FindSettings.getInstance().setShowResultsInSeparateView(toOpenInNewTab);
    }
  }

  private boolean shouldOpenInNewTab() {
    return mustOpenInNewTab() || FindSettings.getInstance().isShowResultsInSeparateView();
  }

  private boolean mustOpenInNewTab() {
    Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
    return selectedContent != null && selectedContent.isPinned();
  }


  /**
   * @throws PsiInvalidElementAccessException when the searcher can't be created (i.e. because element was invalidated)
   */
  @Nonnull
  private static UsageSearcher createUsageSearcher(@Nonnull final PsiElement2UsageTargetAdapter[] primaryTargets,
                                                   @Nonnull final PsiElement2UsageTargetAdapter[] secondaryTargets,
                                                   @Nonnull final FindUsagesHandler handler,
                                                   @Nonnull FindUsagesOptions options,
                                                   final PsiFile scopeFile) throws PsiInvalidElementAccessException {
    AccessRule.read(() -> {
      PsiElement[] primaryElements = PsiElement2UsageTargetAdapter.convertToPsiElements(primaryTargets);
      PsiElement[] secondaryElements = PsiElement2UsageTargetAdapter.convertToPsiElements(secondaryTargets);

      ContainerUtil.concat(primaryElements, secondaryElements).forEach(psi -> {
        if (psi == null || !psi.isValid()) throw new PsiInvalidElementAccessException(psi);
      });
    });

    FindUsagesOptions optionsClone = options.clone();
    return processor -> {
      ThrowableComputable<PsiElement[], RuntimeException> action3 = () -> PsiElement2UsageTargetAdapter.convertToPsiElements(primaryTargets);
      PsiElement[] primaryElements = AccessRule.read(action3);
      ThrowableComputable<PsiElement[], RuntimeException> action2 = () -> PsiElement2UsageTargetAdapter.convertToPsiElements(secondaryTargets);
      PsiElement[] secondaryElements = AccessRule.read(action2);

      ThrowableComputable<Project, RuntimeException> action1 = () -> scopeFile != null ? scopeFile.getProject() : primaryElements[0].getProject();
      Project project = AccessRule.read(action1);

      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      LOG.assertTrue(indicator != null, "Must run under progress. see ProgressManager.run*");

      ((IdePsiManagerImpl)PsiManager.getInstance(project)).dropResolveCacheRegularly(indicator);

      if (scopeFile != null) {
        optionsClone.searchScope = new LocalSearchScope(scopeFile);
      }
      final Processor<UsageInfo> usageInfoProcessor = new CommonProcessors.UniqueProcessor<>(usageInfo -> {
        ThrowableComputable<Usage, RuntimeException> action = () -> UsageInfoToUsageConverter.convert(primaryElements, usageInfo);
        Usage usage = AccessRule.read(action);
        return processor.process(usage);
      });
      final Iterable<PsiElement> elements = ContainerUtil.concat(primaryElements, secondaryElements);

      optionsClone.fastTrack = new SearchRequestCollector(new SearchSession());
      if (optionsClone.searchScope instanceof GlobalSearchScope) {
        // we will search in project scope always but warn if some usage is out of scope
        optionsClone.searchScope = optionsClone.searchScope.union(GlobalSearchScope.projectScope(project));
      }
      try {
        for (final PsiElement element : elements) {
          handler.processElementUsages(element, usageInfoProcessor, optionsClone);
          for (CustomUsageSearcher searcher : CustomUsageSearcher.EP_NAME.getExtensionList()) {
            try {
              searcher.processElementUsages(element, processor, optionsClone);
            }
            catch (IndexNotReadyException e) {
              DumbService.getInstance(element.getProject()).showDumbModeNotification("Find usages is not available during indexing");
            }
            catch (ProcessCanceledException e) {
              throw e;
            }
            catch (Exception e) {
              LOG.error(e);
            }
          }
        }

        PsiSearchHelper.getInstance(project).processRequests(optionsClone.fastTrack, ref -> {
          ThrowableComputable<UsageInfo,RuntimeException> action = () -> {
            if (!ref.getElement().isValid()) return null;
            return new UsageInfo(ref);
          };
          UsageInfo info = AccessRule.read(action);
          return info == null || usageInfoProcessor.process(info);
        });
      }
      finally {
        optionsClone.fastTrack = null;
      }
    };
  }

  @Nonnull
  private static PsiElement2UsageTargetAdapter[] convertToUsageTargets(@Nonnull Iterable<PsiElement> elementsToSearch,
                                                                       @Nonnull final FindUsagesOptions findUsagesOptions) {
    final List<PsiElement2UsageTargetAdapter> targets = ContainerUtil.map(elementsToSearch, element -> convertToUsageTarget(element, findUsagesOptions));
    return targets.toArray(new PsiElement2UsageTargetAdapter[targets.size()]);
  }

  public void findUsages(@Nonnull final PsiElement[] primaryElements,
                         @Nonnull final PsiElement[] secondaryElements,
                         @Nonnull final FindUsagesHandler handler,
                         @Nonnull final FindUsagesOptions findUsagesOptions,
                         final boolean toSkipUsagePanelWhenOneUsage) {
    doFindUsages(primaryElements, secondaryElements, handler, findUsagesOptions, toSkipUsagePanelWhenOneUsage);
  }

  public UsageView doFindUsages(@Nonnull final PsiElement[] primaryElements,
                                @Nonnull final PsiElement[] secondaryElements,
                                @Nonnull final FindUsagesHandler handler,
                                @Nonnull final FindUsagesOptions findUsagesOptions,
                                final boolean toSkipUsagePanelWhenOneUsage) {
    if (primaryElements.length == 0) {
      throw new AssertionError(handler + " " + findUsagesOptions);
    }
    PsiElement2UsageTargetAdapter[] primaryTargets = convertToUsageTargets(Arrays.asList(primaryElements), findUsagesOptions);
    PsiElement2UsageTargetAdapter[] secondaryTargets = convertToUsageTargets(Arrays.asList(secondaryElements), findUsagesOptions);
    PsiElement2UsageTargetAdapter[] targets = ArrayUtil.mergeArrays(primaryTargets, secondaryTargets);
    Supplier<UsageSearcher> factory = () -> createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, null);
    UsageView usageView = myAnotherManager.searchAndShowUsages(targets, factory, !toSkipUsagePanelWhenOneUsage, true,
                                                               createPresentation(primaryElements[0], findUsagesOptions, shouldOpenInNewTab()), null);
    myHistory.add(targets[0]);
    return usageView;
  }

  @Nonnull
  private static UsageViewPresentation createPresentation(@Nonnull PsiElement psiElement, @Nonnull FindUsagesOptions options, boolean toOpenInNewTab) {
    UsageViewPresentation presentation = new UsageViewPresentation();
    String scopeString = options.searchScope.getDisplayName();
    presentation.setScopeText(scopeString);
    String usagesString = generateUsagesString(options);
    presentation.setUsagesString(usagesString);
    String title = FindBundle.message("find.usages.of.element.in.scope.panel.title", usagesString, UsageViewUtil.getLongName(psiElement), scopeString);
    presentation.setTabText(title);
    presentation.setTabName(FindBundle.message("find.usages.of.element.tab.name", usagesString, UsageViewUtil.getShortName(psiElement)));
    presentation.setTargetsNodeText(StringUtil.capitalize(UsageViewUtil.getType(psiElement)));
    presentation.setOpenInNewTab(toOpenInNewTab);
    return presentation;
  }

  private void findUsagesInEditor(@Nonnull final PsiElement[] primaryElements,
                                  @Nonnull final PsiElement[] secondaryElements,
                                  @Nonnull FindUsagesHandler handler,
                                  @Nonnull PsiFile scopeFile,
                                  @Nonnull FileSearchScope direction,
                                  @Nonnull final FindUsagesOptions findUsagesOptions,
                                  @Nonnull FileEditor fileEditor) {
    initLastSearchElement(findUsagesOptions, primaryElements, secondaryElements);

    clearStatusBar();

    final FileEditorLocation currentLocation = fileEditor.getCurrentLocation();

    PsiElement2UsageTargetAdapter[] primaryTargets = PsiElement2UsageTargetAdapter.convert(primaryElements);
    PsiElement2UsageTargetAdapter[] secondaryTargets = PsiElement2UsageTargetAdapter.convert(primaryElements);
    final UsageSearcher usageSearcher = createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, scopeFile);
    AtomicBoolean usagesWereFound = new AtomicBoolean();

    Usage fUsage = findSiblingUsage(usageSearcher, direction, currentLocation, usagesWereFound, fileEditor);

    if (fUsage != null) {
      fUsage.navigate(true);
      fUsage.selectInEditor();
    }
    else if (!usagesWereFound.get()) {
      String message = getNoUsagesFoundMessage(primaryElements[0]) + " in " + scopeFile.getName();
      showHintOrStatusBarMessage(message, fileEditor);
    }
    else {
      fileEditor.putUserData(KEY_START_USAGE_AGAIN, VALUE_START_USAGE_AGAIN);
      showHintOrStatusBarMessage(getSearchAgainMessage(primaryElements[0], direction), fileEditor);
    }
  }

  private static String getNoUsagesFoundMessage(PsiElement psiElement) {
    String elementType = UsageViewUtil.getType(psiElement);
    String elementName = UsageViewUtil.getShortName(psiElement);
    return FindBundle.message("find.usages.of.element_type.element_name.not.found.message", elementType, elementName);
  }

  private void clearStatusBar() {
    StatusBar.Info.set("", myProject);
  }

  private static String getSearchAgainMessage(PsiElement element, final FileSearchScope direction) {
    String message = getNoUsagesFoundMessage(element);
    if (direction == FileSearchScope.AFTER_CARET) {
      AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_NEXT);
      String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(action);
      if (shortcutsText.isEmpty()) {
        message = FindBundle.message("find.search.again.from.top.action.message", message);
      }
      else {
        message = FindBundle.message("find.search.again.from.top.hotkey.message", message, shortcutsText);
      }
    }
    else {
      String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_PREVIOUS));
      if (shortcutsText.isEmpty()) {
        message = FindBundle.message("find.search.again.from.bottom.action.message", message);
      }
      else {
        message = FindBundle.message("find.search.again.from.bottom.hotkey.message", message, shortcutsText);
      }
    }
    return message;
  }

  private void showHintOrStatusBarMessage(String message, FileEditor fileEditor) {
    if (fileEditor instanceof TextEditor) {
      TextEditor textEditor = (TextEditor)fileEditor;
      showEditorHint(message, textEditor.getEditor());
    }
    else {
      StatusBar.Info.set(message, myProject);
    }
  }

  private static Usage findSiblingUsage(@Nonnull final UsageSearcher usageSearcher,
                                        @Nonnull FileSearchScope dir,
                                        final FileEditorLocation currentLocation,
                                        @Nonnull final AtomicBoolean usagesWereFound,
                                        @Nonnull FileEditor fileEditor) {
    if (fileEditor.getUserData(KEY_START_USAGE_AGAIN) != null) {
      dir = dir == FileSearchScope.AFTER_CARET ? FileSearchScope.FROM_START : FileSearchScope.FROM_END;
    }

    final FileSearchScope direction = dir;

    final AtomicReference<Usage> foundUsage = new AtomicReference<>();
    usageSearcher.generate(usage -> {
      usagesWereFound.set(true);
      if (direction == FileSearchScope.FROM_START) {
        foundUsage.compareAndSet(null, usage);
        return false;
      }
      if (direction == FileSearchScope.FROM_END) {
        foundUsage.set(usage);
      }
      else if (direction == FileSearchScope.AFTER_CARET) {
        if (Comparing.compare(usage.getLocation(), currentLocation) > 0) {
          foundUsage.set(usage);
          return false;
        }
      }
      else if (direction == FileSearchScope.BEFORE_CARET) {
        if (Comparing.compare(usage.getLocation(), currentLocation) >= 0) {
          return false;
        }
        while (true) {
          Usage found = foundUsage.get();
          if (found == null) {
            if (foundUsage.compareAndSet(null, usage)) break;
          }
          else {
            if (Comparing.compare(found.getLocation(), usage.getLocation()) < 0 && foundUsage.compareAndSet(found, usage)) break;
          }
        }
      }

      return true;
    });

    fileEditor.putUserData(KEY_START_USAGE_AGAIN, null);

    return foundUsage.get();
  }

  private static PsiElement2UsageTargetAdapter convertToUsageTarget(@Nonnull PsiElement elementToSearch, @Nonnull FindUsagesOptions findUsagesOptions) {
    if (elementToSearch instanceof NavigationItem) {
      return new PsiElement2UsageTargetAdapter(elementToSearch, findUsagesOptions);
    }
    throw new IllegalArgumentException("Wrong usage target:" + elementToSearch + "; " + elementToSearch.getClass());
  }

  @Nonnull
  private static String generateUsagesString(@Nonnull FindUsagesOptions selectedOptions) {
    return selectedOptions.generateUsagesString();
  }

  private static void showEditorHint(String message, final Editor editor) {
    JComponent component = HintUtil.createInformationLabel(message);
    final LightweightHint hint = new LightweightHint(component);
    HintManagerImpl.getInstanceImpl()
            .showEditorHint(hint, editor, HintManager.UNDER, HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0,
                            false);
  }

  public void rerunAndRecallFromHistory(@Nonnull ConfigurableUsageTarget usageTarget) {
    usageTarget.findUsages();
    addToHistory(usageTarget);
  }

  public void addToHistory(@Nonnull ConfigurableUsageTarget usageTarget) {
    myHistory.add(usageTarget);
  }

  @Nonnull
  public UsageHistory getHistory() {
    return myHistory;
  }


  @Nonnull
  public static GlobalSearchScope getMaximalScope(@Nonnull FindUsagesHandler handler) {
    PsiElement element = handler.getPsiElement();
    Project project = element.getProject();
    PsiFile file = element.getContainingFile();
    if (file != null && ProjectFileIndex.SERVICE.getInstance(project).isInContent(file.getViewProvider().getVirtualFile())) {
      return GlobalSearchScope.projectScope(project);
    }
    return GlobalSearchScope.allScope(project);
  }
}

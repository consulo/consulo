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
package com.intellij.usages.impl;

import com.intellij.find.FindManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.util.TooManyUsagesStatus;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.HyperlinkAdapter;
import consulo.usage.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageLimitUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.RangeBlinker;
import com.intellij.xml.util.XmlStringUtil;
import consulo.application.AccessRule;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.TransactionGuard;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.performance.PerformanceWatcher;
import consulo.application.impl.internal.progress.ProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.Processor;
import consulo.application.util.function.Processors;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposer;
import consulo.document.util.Segment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.NotificationType;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.Balloon;
import consulo.usage.*;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class SearchForUsagesRunnable implements Runnable {
  private static final String FIND_OPTIONS_HREF_TARGET = "FindOptions";
  private static final String SEARCH_IN_PROJECT_HREF_TARGET = "SearchInProject";
  private static final String LARGE_FILES_HREF_TARGET = "LargeFiles";
  private static final String SHOW_PROJECT_FILE_OCCURRENCES_HREF_TARGET = "SHOW_PROJECT_FILE_OCCURRENCES";

  private final AtomicInteger myUsageCountWithoutDefinition = new AtomicInteger(0);
  private final AtomicReference<Usage> myFirstUsage = new AtomicReference<>();
  @Nonnull
  private final Project myProject;
  private final AtomicReference<UsageViewImpl> myUsageViewRef;
  private final UsageViewPresentation myPresentation;
  private final UsageTarget[] mySearchFor;
  private final Supplier<UsageSearcher> mySearcherFactory;
  private final FindUsagesProcessPresentation myProcessPresentation;
  @Nonnull
  private final SearchScope mySearchScopeToWarnOfFallingOutOf;
  private final UsageViewManager.UsageViewStateListener myListener;
  private final UsageViewManagerImpl myUsageViewManager;
  private final AtomicInteger myOutOfScopeUsages = new AtomicInteger();

  SearchForUsagesRunnable(@Nonnull UsageViewManagerImpl usageViewManager,
                          @Nonnull Project project,
                          @Nonnull AtomicReference<UsageViewImpl> usageViewRef,
                          @Nonnull UsageViewPresentation presentation,
                          @Nonnull UsageTarget[] searchFor,
                          @Nonnull Supplier<UsageSearcher> searcherFactory,
                          @Nonnull FindUsagesProcessPresentation processPresentation,
                          @Nonnull SearchScope searchScopeToWarnOfFallingOutOf,
                          @Nullable UsageViewManager.UsageViewStateListener listener) {
    myProject = project;
    myUsageViewRef = usageViewRef;
    myPresentation = presentation;
    mySearchFor = searchFor;
    mySearcherFactory = searcherFactory;
    myProcessPresentation = processPresentation;
    mySearchScopeToWarnOfFallingOutOf = searchScopeToWarnOfFallingOutOf;
    myListener = listener;
    myUsageViewManager = usageViewManager;
  }

  @Nonnull
  private static String createOptionsHtml(UsageTarget[] searchFor) {
    KeyboardShortcut shortcut = UsageViewImpl.getShowUsagesWithSettingsShortcut(searchFor);
    String shortcutText = "";
    if (shortcut != null) {
      shortcutText = "&nbsp;(" + KeymapUtil.getShortcutText(shortcut) + ")";
    }
    return "<a href='" + FIND_OPTIONS_HREF_TARGET + "'>Find Options...</a>" + shortcutText;
  }

  @Nonnull
  private static String createSearchInProjectHtml() {
    return "<a href='" + SEARCH_IN_PROJECT_HREF_TARGET + "'>Search in Project</a>";
  }

  private static void notifyByFindBalloon(@Nullable final HyperlinkListener listener,
                                          @Nonnull final NotificationType info,
                                          @Nonnull FindUsagesProcessPresentation processPresentation,
                                          @Nonnull final Project project,
                                          @Nonnull final List<String> lines) {
    UsageViewContentManager.getInstance(project); // in case tool window not registered

    final Collection<VirtualFile> largeFiles = processPresentation.getLargeFiles();
    List<String> resultLines = new ArrayList<>(lines);
    HyperlinkListener resultListener = listener;
    if (!largeFiles.isEmpty()) {
      String shortMessage = "(<a href='" + LARGE_FILES_HREF_TARGET + "'>" + UsageViewBundle.message("large.files.were.ignored", largeFiles.size()) + "</a>)";

      resultLines.add(shortMessage);
      resultListener = addHrefHandling(resultListener, LARGE_FILES_HREF_TARGET, () -> {
        String detailedMessage = detailedLargeFilesMessage(largeFiles);
        List<String> strings = new ArrayList<>(lines);
        strings.add(detailedMessage);
        //noinspection SSBasedInspection
        ToolWindowManager.getInstance(project).notifyByBalloon(ToolWindowId.FIND, info, wrapInHtml(strings), AllIcons.Actions.Find, listener);
      });
    }

    Runnable searchIncludingProjectFileUsages = processPresentation.searchIncludingProjectFileUsages();
    if (searchIncludingProjectFileUsages != null) {
      resultLines
              .add("Occurrences in project configuration files are skipped. " + "<a href='" + SHOW_PROJECT_FILE_OCCURRENCES_HREF_TARGET + "'>Include them</a>");
      resultListener = addHrefHandling(resultListener, SHOW_PROJECT_FILE_OCCURRENCES_HREF_TARGET, searchIncludingProjectFileUsages);
    }

    //noinspection SSBasedInspection
    ToolWindowManager.getInstance(project).notifyByBalloon(ToolWindowId.FIND, info, wrapInHtml(resultLines), AllIcons.Actions.Find, resultListener);
  }

  private static HyperlinkListener addHrefHandling(@Nullable final HyperlinkListener listener,
                                                   @Nonnull final String hrefTarget,
                                                   @Nonnull final Runnable handler) {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        if (e.getDescription().equals(hrefTarget)) {
          handler.run();
        }
        else if (listener != null) {
          listener.hyperlinkUpdate(e);
        }
      }
    };
  }

  @Nonnull
  private static String wrapInHtml(@Nonnull List<String> strings) {
    return XmlStringUtil.wrapInHtml(StringUtil.join(strings, "<br>"));
  }

  @Nonnull
  private static String detailedLargeFilesMessage(@Nonnull Collection<VirtualFile> largeFiles) {
    String message = "";
    if (largeFiles.size() == 1) {
      final VirtualFile vFile = largeFiles.iterator().next();
      message += "File " + presentableFileInfo(vFile) + " is ";
    }
    else {
      message += "Files<br> ";

      int counter = 0;
      for (VirtualFile vFile : largeFiles) {
        message += presentableFileInfo(vFile) + "<br> ";
        if (counter++ > 10) break;
      }

      message += "are ";
    }

    message += "too large and cannot be scanned";
    return message;
  }

  @Nonnull
  private static String presentableFileInfo(@Nonnull VirtualFile vFile) {
    return getPresentablePath(vFile) + "&nbsp;(" + UsageViewManagerImpl.presentableSize(UsageViewManagerImpl.getFileLength(vFile)) + ")";
  }

  @Nonnull
  private static String getPresentablePath(@Nonnull final VirtualFile virtualFile) {
    return "'" + AccessRule.read(virtualFile::getPresentableUrl) + "'";
  }

  @Nonnull
  private HyperlinkListener createGotToOptionsListener(@Nonnull final UsageTarget[] targets) {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        if (e.getDescription().equals(FIND_OPTIONS_HREF_TARGET)) {
          TransactionGuard.getInstance().submitTransactionAndWait(() -> FindManager.getInstance(myProject).showSettingsAndFindUsages(targets));
        }
      }
    };
  }

  @Nonnull
  private HyperlinkListener createSearchInProjectListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        if (e.getDescription().equals(SEARCH_IN_PROJECT_HREF_TARGET)) {
          PsiElement psiElement = getPsiElement(mySearchFor);
          if (psiElement != null) {
            TransactionGuard.getInstance().submitTransactionAndWait(
                    () -> FindManager.getInstance(myProject).findUsagesInScope(psiElement, GlobalSearchScope.projectScope(myProject)));
          }
        }
      }
    };
  }

  private static PsiElement getPsiElement(@Nonnull UsageTarget[] searchFor) {
    final UsageTarget target = searchFor[0];
    if (!(target instanceof PsiElementUsageTarget)) return null;
    return AccessRule.read(((PsiElementUsageTarget)target)::getElement);
  }

  private static void flashUsageScriptaculously(@Nonnull final Usage usage) {
    if (!(usage instanceof UsageInfo2UsageAdapter)) {
      return;
    }
    UsageInfo2UsageAdapter usageInfo = (UsageInfo2UsageAdapter)usage;

    Editor editor = usageInfo.openTextEditor(true);
    if (editor == null) return;
    TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES);

    RangeBlinker rangeBlinker = new RangeBlinker(editor, attributes, 6);
    List<Segment> segments = new ArrayList<>();
    Processor<Segment> processor = Processors.cancelableCollectProcessor(segments);
    usageInfo.processRangeMarkers(processor);
    rangeBlinker.resetMarkers(segments);
    rangeBlinker.startBlinking();
  }

  private UsageViewImpl getUsageView(@Nonnull ProgressIndicator indicator) {
    UsageViewImpl usageView = myUsageViewRef.get();
    if (usageView != null) return usageView;
    int usageCount = myUsageCountWithoutDefinition.get();
    if (usageCount >= 2 || usageCount == 1 && myProcessPresentation.isShowPanelIfOnlyOneUsage()) {
      usageView = new UsageViewImpl(myProject, myPresentation, mySearchFor, mySearcherFactory);
      usageView.associateProgress(indicator);
      if (myUsageViewRef.compareAndSet(null, usageView)) {
        openView(usageView);
        final Usage firstUsage = myFirstUsage.get();
        if (firstUsage != null) {
          final UsageViewImpl finalUsageView = usageView;
          ApplicationManager.getApplication().runReadAction(() -> finalUsageView.appendUsage(firstUsage));
        }
      }
      else {
        UsageViewImpl finalUsageView = usageView;
        // later because dispose does some sort of swing magic e.g. AnAction.unregisterCustomShortcutSet()
        UIUtil.invokeLaterIfNeeded(() -> Disposer.dispose(finalUsageView));
      }
      return myUsageViewRef.get();
    }
    return null;
  }

  private void openView(@Nonnull final UsageViewImpl usageView) {
    SwingUtilities.invokeLater(() -> {
      if (myProject.isDisposed()) return;
      myUsageViewManager.addContent(usageView, myPresentation);
      if (myListener != null) {
        myListener.usageViewCreated(usageView);
      }
      myUsageViewManager.showToolWindow(false);
    });
  }

  @Override
  public void run() {
    PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();

    AtomicBoolean findUsagesStartedShown = new AtomicBoolean();
    searchUsages(findUsagesStartedShown);
    endSearchForUsages(findUsagesStartedShown);

    snapshot.logResponsivenessSinceCreation("Find Usages");
  }

  private void searchUsages(@Nonnull final AtomicBoolean findStartedBalloonShown) {
    ProgressIndicator indicator = ProgressWrapper.unwrap(ProgressManager.getInstance().getProgressIndicator());
    assert indicator != null : "must run find usages under progress";
    TooManyUsagesStatus.createFor(indicator);
    Alarm findUsagesStartedBalloon = new Alarm();
    findUsagesStartedBalloon.addRequest(() -> {
      notifyByFindBalloon(null, NotificationType.WARNING, myProcessPresentation, myProject,
                          Collections.singletonList(StringUtil.escapeXml(UsageViewManagerImpl.getProgressTitle(myPresentation))));
      findStartedBalloonShown.set(true);
    }, 300, IdeaModalityState.NON_MODAL);
    UsageSearcher usageSearcher = mySearcherFactory.get();

    usageSearcher.generate(usage -> {
      ProgressIndicator indicator1 = ProgressWrapper.unwrap(ProgressManager.getInstance().getProgressIndicator());
      assert indicator1 != null : "must run find usages under progress";
      if (indicator1.isCanceled()) return false;

      if (!UsageViewManagerImpl.isInScope(usage, mySearchScopeToWarnOfFallingOutOf)) {
        myOutOfScopeUsages.incrementAndGet();
        return true;
      }

      boolean incrementCounter = !UsageViewManager.isSelfUsage(usage, mySearchFor);

      if (incrementCounter) {
        final int usageCount = myUsageCountWithoutDefinition.incrementAndGet();
        if (usageCount == 1 && !myProcessPresentation.isShowPanelIfOnlyOneUsage()) {
          myFirstUsage.compareAndSet(null, usage);
        }

        final UsageViewImpl usageView = getUsageView(indicator1);

        TooManyUsagesStatus tooManyUsagesStatus = TooManyUsagesStatus.getFrom(indicator1);
        if (usageCount > UsageLimitUtil.USAGES_LIMIT && tooManyUsagesStatus.switchTooManyUsagesStatus()) {
          UsageViewManagerImpl.showTooManyUsagesWarningLater(myProject, tooManyUsagesStatus, indicator1, myPresentation, usageCount, usageView);
        }
        tooManyUsagesStatus.pauseProcessingIfTooManyUsages();
        if (usageView != null) {
          ApplicationManager.getApplication().runReadAction(() -> usageView.appendUsage(usage));
        }
      }
      return !indicator1.isCanceled();
    });
    if (getUsageView(indicator) != null) {
      ApplicationManager.getApplication().invokeLater(() -> myUsageViewManager.showToolWindow(true), myProject.getDisposed());
    }
    Disposer.dispose(findUsagesStartedBalloon);
    ApplicationManager.getApplication().invokeLater(() -> {
      if (findStartedBalloonShown.get()) {
        Balloon balloon = ToolWindowManager.getInstance(myProject).getToolWindowBalloon(ToolWindowId.FIND);
        if (balloon != null) {
          balloon.hide();
        }
      }
    }, myProject.getDisposed());
  }

  private void endSearchForUsages(@Nonnull final AtomicBoolean findStartedBalloonShown) {
    assert !ApplicationManager.getApplication().isDispatchThread() : Thread.currentThread();
    int usageCount = myUsageCountWithoutDefinition.get();
    if (usageCount == 0 && myProcessPresentation.isShowNotFoundMessage()) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          if (myProcessPresentation.isCanceled()) {
            notifyByFindBalloon(null, NotificationType.WARNING, myProcessPresentation, myProject, Collections.singletonList("Usage search was canceled"));
            findStartedBalloonShown.set(false);
            return;
          }

          final List<Action> notFoundActions = myProcessPresentation.getNotFoundActions();
          final String message = UsageViewBundle
                  .message("dialog.no.usages.found.in", StringUtil.decapitalize(myPresentation.getUsagesString()), myPresentation.getScopeText(),
                           myPresentation.getContextText());

          if (notFoundActions.isEmpty()) {
            List<String> lines = new ArrayList<>();
            lines.add(StringUtil.escapeXml(message));
            if (myOutOfScopeUsages.get() != 0) {
              lines.add(UsageViewManagerImpl.outOfScopeMessage(myOutOfScopeUsages.get(), mySearchScopeToWarnOfFallingOutOf));
            }
            if (myProcessPresentation.isShowFindOptionsPrompt()) {
              lines.add(createOptionsHtml(mySearchFor));
            }
            NotificationType type = myOutOfScopeUsages.get() == 0 ? NotificationType.INFO : NotificationType.WARNING;
            notifyByFindBalloon(createGotToOptionsListener(mySearchFor), type, myProcessPresentation, myProject, lines);
            findStartedBalloonShown.set(false);
          }
          else {
            List<String> titles = new ArrayList<>(notFoundActions.size() + 1);
            titles.add(UsageViewBundle.message("dialog.button.ok"));
            for (Action action : notFoundActions) {
              Object value = action.getValue(FindUsagesProcessPresentation.NAME_WITH_MNEMONIC_KEY);
              if (value == null) value = action.getValue(Action.NAME);

              titles.add((String)value);
            }

            int option = Messages.showDialog(myProject, message, UsageViewBundle.message("dialog.title.information"), ArrayUtil.toStringArray(titles), 0,
                                             Messages.getInformationIcon());

            if (option > 0) {
              notFoundActions.get(option - 1).actionPerformed(new ActionEvent(this, 0, titles.get(option)));
            }
          }
        }
      }, IdeaModalityState.NON_MODAL, myProject.getDisposed());
    }
    else if (usageCount == 1 && !myProcessPresentation.isShowPanelIfOnlyOneUsage()) {
      ApplicationManager.getApplication().invokeLater(() -> {
        Usage usage = myFirstUsage.get();
        if (usage.canNavigate()) {
          usage.navigate(true);
          flashUsageScriptaculously(usage);
        }
        List<String> lines = new ArrayList<>();

        lines.add("Only one usage found.");
        if (myOutOfScopeUsages.get() != 0) {
          lines.add(UsageViewManagerImpl.outOfScopeMessage(myOutOfScopeUsages.get(), mySearchScopeToWarnOfFallingOutOf));
        }
        lines.add(createOptionsHtml(mySearchFor));
        NotificationType type = myOutOfScopeUsages.get() == 0 ? NotificationType.INFO : NotificationType.WARNING;
        notifyByFindBalloon(createGotToOptionsListener(mySearchFor), type, myProcessPresentation, myProject, lines);
      }, IdeaModalityState.NON_MODAL, myProject.getDisposed());
    }
    else {
      final UsageViewImpl usageView = myUsageViewRef.get();
      if (usageView != null) {
        usageView.drainQueuedUsageNodes();
        usageView.setSearchInProgress(false);
      }

      final List<String> lines;
      final HyperlinkListener hyperlinkListener;
      if (myOutOfScopeUsages.get() == 0 || getPsiElement(mySearchFor) == null) {
        lines = Collections.emptyList();
        hyperlinkListener = null;
      }
      else {
        lines = Arrays.asList(UsageViewManagerImpl.outOfScopeMessage(myOutOfScopeUsages.get(), mySearchScopeToWarnOfFallingOutOf), createSearchInProjectHtml());
        hyperlinkListener = createSearchInProjectListener();
      }

      if (!myProcessPresentation.getLargeFiles().isEmpty() ||
          myOutOfScopeUsages.get() != 0 ||
          myProcessPresentation.searchIncludingProjectFileUsages() != null) {
        ApplicationManager.getApplication().invokeLater(() -> {
          NotificationType type = myOutOfScopeUsages.get() == 0 ? NotificationType.INFO : NotificationType.WARNING;
          notifyByFindBalloon(hyperlinkListener, type, myProcessPresentation, myProject, lines);
        }, IdeaModalityState.NON_MODAL, myProject.getDisposed());
      }
    }

    if (myListener != null) {
      myListener.findingUsagesFinished(myUsageViewRef.get());
    }
  }
}

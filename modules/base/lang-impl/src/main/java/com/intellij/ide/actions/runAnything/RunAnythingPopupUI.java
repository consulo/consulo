// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.BigPopupUI;
import com.intellij.ide.actions.bigPopup.ShowFilterAction;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.groups.RunAnythingCompletionGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGeneralGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingRecentGroup;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.ide.actions.runAnything.ui.RunAnythingScrollingUtil;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.Application;
import consulo.logging.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.ActionCallback;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.event.FocusEvent;
import consulo.ui.event.FocusListener;
import consulo.ui.event.KeyListener;
import consulo.ui.image.Image;

import javax.accessibility.Accessible;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.ide.actions.runAnything.RunAnythingAction.ALT_IS_PRESSED;
import static com.intellij.ide.actions.runAnything.RunAnythingAction.SHIFT_IS_PRESSED;
import static com.intellij.openapi.wm.IdeFocusManager.getGlobalInstance;
import static java.awt.FlowLayout.RIGHT;

public class RunAnythingPopupUI extends BigPopupUI {
  public static final int SEARCH_FIELD_COLUMNS = 25;
  public static final Key<Executor> EXECUTOR_KEY = RunAnythingAction.EXECUTOR_KEY;
  static final String RUN_ANYTHING = "RunAnything";
  public static final KeyStroke DOWN_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
  public static final KeyStroke UP_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);

  private static final Logger LOG = Logger.getInstance(RunAnythingPopupUI.class);
  private static final Border RENDERER_BORDER = JBUI.Borders.empty(1, 0);
  private static final String HELP_PLACEHOLDER = "?";
  private static final int LIST_REBUILD_DELAY = 100;
  private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, Application.get());
  private final AnActionEvent myActionEvent;
  private boolean myIsUsedTrigger;
  private CalcThread myCalcThread;
  private volatile ActionCallback myCurrentWorker;
  private int myCalcThreadRestartRequestId = 0;
  private final Object myWorkerRestartRequestLock = new Object();
  private boolean mySkipFocusGain = false;
  @Nullable
  private VirtualFile myVirtualFile;
  private JLabel myTextFieldTitle;
  private boolean myIsItemSelected;
  private String myLastInputText = null;
  private RunAnythingSearchListModel.RunAnythingMainListModel myListModel;
  private Project myProject;
  private Module myModule;

  private RunAnythingContext mySelectedExecutingContext;
  private final List<RunAnythingContext> myAvailableExecutingContexts = new ArrayList<>();
  private RunAnythingChooseContextAction myChooseContextAction;

  private void onMouseClicked(@Nonnull MouseEvent event) {
    int clickCount = event.getClickCount();
    if (clickCount > 1 && clickCount % 2 == 0) {
      event.consume();
      final int i = myResultsList.locationToIndex(event.getPoint());
      if (i != -1) {
        getGlobalInstance().doWhenFocusSettlesDown(() -> getGlobalInstance().requestFocus(getField(), true));
        Application.get().invokeLater(() -> {
          myResultsList.setSelectedIndex(i);
          executeCommand();
        });
      }
    }
  }

  private void initSearchField() {
    updateContextCombobox();
    mySearchField.addValueListener(event -> {
      myIsUsedTrigger = true;

      final String pattern = mySearchField.getValueOrError();
      if (mySearchField.hasFocus()) {
        Application.get().invokeLater(() -> myIsItemSelected = false);

        if (!myIsItemSelected) {
          myLastInputText = null;
          clearSelection();

          rebuildList();
        }

        if (!isHelpMode(pattern)) {
          updateContextCombobox();
          adjustMainListEmptyText(mySearchField);
          return;
        }

        adjustEmptyText(mySearchField, field -> true, "", IdeBundle.message("run.anything.help.list.empty.secondary.text"));
      }
    });

    mySearchField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        if (mySkipFocusGain) {
          mySkipFocusGain = false;
          return;
        }
        //mySearchField.setForeground(UIUtil.getLabelForeground());
        mySearchField.setVisibleLength(SEARCH_FIELD_COLUMNS);
        Application.get().invokeLater(() -> {
          final JComponent parent = (JComponent)TargetAWT.to(mySearchField).getParent();
          parent.revalidate();
          parent.repaint();
        });
        rebuildList();
      }

      @Override
      public void focusLost(FocusEvent e) {
        final ActionCallback result = new ActionCallback();
        UIUtil.invokeLaterIfNeeded(() -> {
          try {
            if (myCalcThread != null) {
              myCalcThread.cancel();
            }
            myAlarm.cancelAllRequests();

            Application.get().invokeLater(() -> ActionToolbarImpl.updateAllToolbarsImmediately());

            searchFinishedHandler.run();
          }
          finally {
            result.setDone();
          }
        });
      }
    });
  }

  private static void adjustMainListEmptyText(@Nonnull TextBoxWithExtensions editor) {
    adjustEmptyText(editor, field -> field.getText().isEmpty(), IdeBundle.message("run.anything.main.list.empty.primary.text"), IdeBundle.message("run.anything.main.list.empty.secondary.text"));
  }

  private static boolean isHelpMode(@Nonnull String pattern) {
    return pattern.startsWith(HELP_PLACEHOLDER);
  }

  private void clearSelection() {
    myResultsList.getSelectionModel().clearSelection();
  }

  private TextBoxWithExtensions getField() {
    return mySearchField;
  }

  private void executeCommand() {
    final String pattern = getField().getValueOrError();
    int index = myResultsList.getSelectedIndex();

    //do nothing on attempt to execute empty command
    if (pattern.isEmpty() && index == -1) return;

    final Project project = getProject();

    final RunAnythingSearchListModel model = getSearchingModel(myResultsList);
    if (index != -1 && model != null && isMoreItem(index)) {
      RunAnythingGroup group = model.findGroupByMoreIndex(index);

      if (group != null) {
        myCurrentWorker.doWhenProcessed(() -> {
          myCalcThread = new CalcThread(project, pattern, true);
          //RunAnythingUsageCollector.Companion.triggerMoreStatistics(project, group, model.getClass());
          myCurrentWorker = myCalcThread.insert(index, group);
        });

        return;
      }
    }

    if (model != null) {
      //RunAnythingUsageCollector.Companion.triggerExecCategoryStatistics(project, model.getGroups(), model.getClass(), index, SHIFT_IS_PRESSED.get(), ALT_IS_PRESSED.get());
    }
    RunAnythingUtil.executeMatched(getDataContext(), pattern);

    mySearchField.setValue("");
    searchFinishedHandler.run();
    triggerUsed();
  }

  @Nonnull
  private Project getProject() {
    return myProject;
  }

  @Nullable
  private Module getModule() {
    if (myModule != null) {
      return myModule;
    }

    Project project = getProject();
    if (myVirtualFile != null) {
      Module moduleForFile = ModuleUtilCore.findModuleForFile(myVirtualFile, project);
      if (moduleForFile != null) {
        return moduleForFile;
      }
    }

    VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
    if (selectedFiles.length != 0) {
      Module moduleForFile = ModuleUtilCore.findModuleForFile(selectedFiles[0], project);
      if (moduleForFile != null) {
        return moduleForFile;
      }
    }

    return null;
  }

  @Nonnull
  private VirtualFile getWorkDirectory() {
    if (ALT_IS_PRESSED.get()) {
      if (myVirtualFile != null) {
        VirtualFile file = myVirtualFile.isDirectory() ? myVirtualFile : myVirtualFile.getParent();
        if (file != null) {
          return file;
        }
      }

      VirtualFile[] selectedFiles = FileEditorManager.getInstance(getProject()).getSelectedFiles();
      if (selectedFiles.length > 0) {
        VirtualFile file = selectedFiles[0].getParent();
        if (file != null) {
          return file;
        }
      }
    }

    return getBaseDirectory(getModule());
  }

  @Nonnull
  private VirtualFile getBaseDirectory(@Nullable Module module) {
    VirtualFile projectBaseDir = getProject().getBaseDir();
    if (module == null) {
      return projectBaseDir;
    }

    VirtualFile firstContentRoot = getFirstContentRoot(module);
    if (firstContentRoot == null) {
      return projectBaseDir;
    }

    return firstContentRoot;
  }

  @Nullable
  public VirtualFile getFirstContentRoot(@Nonnull final Module module) {
    if (module.isDisposed()) return null;
    return ArrayUtil.getFirstElement(ModuleRootManager.getInstance(module).getContentRoots());
  }

  private boolean isMoreItem(int index) {
    RunAnythingSearchListModel model = getSearchingModel(myResultsList);
    return model != null && model.isMoreIndex(index);
  }

  @Nullable
  public static RunAnythingSearchListModel getSearchingModel(@Nonnull JBList list) {
    ListModel model = list.getModel();
    return model instanceof RunAnythingSearchListModel ? (RunAnythingSearchListModel)model : null;
  }

  private void rebuildList() {
    String pattern = getSearchPattern();
    assert EventQueue.isDispatchThread() : "Must be EDT";
    if (myCalcThread != null && !myCurrentWorker.isProcessed()) {
      myCurrentWorker = myCalcThread.cancel();
    }
    if (myCalcThread != null && !myCalcThread.isCanceled()) {
      myCalcThread.cancel();
    }
    synchronized (myWorkerRestartRequestLock) { // this lock together with RestartRequestId should be enough to prevent two CalcThreads running at the same time
      final int currentRestartRequest = ++myCalcThreadRestartRequestId;
      myCurrentWorker.doWhenProcessed(() -> {
        synchronized (myWorkerRestartRequestLock) {
          if (currentRestartRequest != myCalcThreadRestartRequestId) {
            return;
          }
          myCalcThread = new CalcThread(getProject(), pattern, false);
          myCurrentWorker = myCalcThread.start();
        }
      });
    }
  }

  public void initResultsList() {
    myResultsList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        updateAdText(getDataContext());

        Object selectedValue = myResultsList.getSelectedValue();
        if (selectedValue == null) return;

        String lastInput = mySearchField.getValueOrError();
        myIsItemSelected = true;

        if (isMoreItem(myResultsList.getSelectedIndex())) {
          if (myLastInputText != null) {
            mySearchField.setValue(myLastInputText);
          }
          return;
        }

        mySearchField.setValue(selectedValue instanceof RunAnythingItem ? ((RunAnythingItem)selectedValue).getCommand() : myLastInputText);

        if (myLastInputText == null) myLastInputText = lastInput;
      }
    });
  }

  private void updateContextCombobox() {
    DataContext dataContext = getDataContext();
    Object value = myResultsList.getSelectedValue();
    String text = value instanceof RunAnythingItem ? ((RunAnythingItem)value).getCommand() : getSearchPattern();
    RunAnythingProvider provider = RunAnythingProvider.findMatchedProvider(dataContext, text);
    if (provider != null) {
      myChooseContextAction.setAvailableContexts(provider.getExecutionContexts(dataContext));
    }

    AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext);
    ActionUtil.performDumbAwareUpdate(false, myChooseContextAction, event, false);
  }

  @Override
  @Nonnull
  public JPanel createTopLeftPanel() {
    myTextFieldTitle = new JLabel(IdeBundle.message("run.anything.run.anything.title"));
    JPanel topPanel = new NonOpaquePanel(new BorderLayout());
    Color foregroundColor = UIUtil.getLabelForeground();


    myTextFieldTitle.setForeground(foregroundColor);
    myTextFieldTitle.setBorder(BorderFactory.createEmptyBorder(3, 5, 5, 0));
    if (SystemInfo.isMac) {
      myTextFieldTitle.setFont(myTextFieldTitle.getFont().deriveFont(Font.BOLD, myTextFieldTitle.getFont().getSize() - 1f));
    }
    else {
      myTextFieldTitle.setFont(myTextFieldTitle.getFont().deriveFont(Font.BOLD));
    }

    topPanel.add(myTextFieldTitle);

    return topPanel;
  }

  @Nonnull
  private DataContext getDataContext() {
    HashMap<Key, Object> dataMap = new HashMap<>();
    dataMap.put(CommonDataKeys.PROJECT, getProject());
    dataMap.put(LangDataKeys.MODULE, getModule());
    dataMap.put(CommonDataKeys.VIRTUAL_FILE, getWorkDirectory());
    dataMap.put(EXECUTOR_KEY, getExecutor());
    dataMap.put(RunAnythingProvider.EXECUTING_CONTEXT, myChooseContextAction.getSelectedContext());
    return SimpleDataContext.getSimpleContext(dataMap, myActionEvent.getDataContext());
  }

  public void initMySearchField() {
    updateExtensions(null);

    setHandleMatchedConfiguration();

    adjustMainListEmptyText(mySearchField);

    // todo
    mySearchField.addKeyListener(new KeyListener() {
      @Override
      public void keyPressed(consulo.ui.event.KeyEvent e) {
        updateByModifierKeysEvent(e);
      }

      @Override
      public void keyReleased(consulo.ui.event.KeyEvent e) {
        updateByModifierKeysEvent(e);
      }

      private void updateByModifierKeysEvent(@Nonnull consulo.ui.event.KeyEvent e) {
        String message;
        if (e.withShift() && e.withAlt()) {
          message = IdeBundle.message("run.anything.run.in.context.debug.title");
        }
        else if (e.withShift()) {
          message = IdeBundle.message("run.anything.run.debug.title");
        }
        else if (e.withAlt()) {
          message = IdeBundle.message("run.anything.run.in.context.title");
        }
        else {
          message = IdeBundle.message("run.anything.run.anything.title");
        }
        myTextFieldTitle.setText(message);
        updateMatchedRunConfigurationStuff(e.withAlt());
      }
    });

    initSearchField();

    mySearchField.setVisibleLength(SEARCH_FIELD_COLUMNS);
  }

  public static void adjustEmptyText(@Nonnull TextBoxWithExtensions textEditor, @Nonnull BooleanFunction<JBTextField> function, @Nonnull String leftText, @Nonnull String rightText) {
    textEditor.setPlaceholder(leftText);

    //textEditor.putClientProperty("StatusVisibleFunction", function);
    //StatusText statusText = textEditor.getEmptyText();
    //statusText.setIsVerticalFlow(false);
    //statusText.setShowAboveCenter(false);
    //statusText.setText(leftText, SimpleTextAttributes.GRAY_ATTRIBUTES);
    //statusText.appendSecondaryText(rightText, SimpleTextAttributes.GRAY_ATTRIBUTES, null);
    //statusText.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
  }

  private void setHandleMatchedConfiguration() {
    mySearchField.addValueListener(event -> {
      updateMatchedRunConfigurationStuff(ALT_IS_PRESSED.get());
    });
  }

  private void updateMatchedRunConfigurationStuff(boolean isAltPressed) {
    TextBox textField = mySearchField;
    String pattern = textField.getValueOrError();

    DataContext dataContext = getDataContext();
    RunAnythingProvider provider = RunAnythingProvider.findMatchedProvider(dataContext, pattern);

    if (provider == null) {
      return;
    }

    Object value = provider.findMatchingValue(dataContext, pattern);

    if (value == null) {
      return;
    }
    //noinspection unchecked
    Image icon = provider.getIcon(value);
    if (icon == null) {
      return;
    }

    updateExtensions(icon);
  }

  private void updateExtensions(@Nullable Image l) {
    Image leftImage = ObjectUtil.notNull(l, AllIcons.Actions.Run_anything);

    mySearchField.setExtensions(new TextBoxWithExtensions.Extension(true, leftImage, null));
  }

  private void updateAdText(@Nonnull DataContext dataContext) {
    Object value = myResultsList.getSelectedValue();

    if (value instanceof RunAnythingItem) {
      RunAnythingProvider provider = RunAnythingProvider.findMatchedProvider(dataContext, ((RunAnythingItem)value).getCommand());
      if (provider != null) {
        String adText = provider.getAdText();
        if (adText != null) {
          setAdText(adText);
        }
      }
    }
  }

  private void triggerUsed() {
    if (myIsUsedTrigger) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed(RUN_ANYTHING);
    }
    myIsUsedTrigger = false;
  }


  public void setAdText(@Nonnull final String s) {
    myHintLabel.setText(s);
  }

  @Nonnull
  public static Executor getExecutor() {
    final Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();
    final Executor debugExecutor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);

    return !SHIFT_IS_PRESSED.get() ? runExecutor : debugExecutor;
  }

  private class MyListRenderer extends ColoredListCellRenderer<Object> {
    private final RunAnythingMyAccessibleComponent myMainPanel = new RunAnythingMyAccessibleComponent(new BorderLayout());

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
      Component cmp = null;
      if (isMoreItem(index)) {
        cmp = RunAnythingMore.get(isSelected);
      }

      if (cmp == null) {
        if (value instanceof RunAnythingItem) {
          cmp = ((RunAnythingItem)value).createComponent(myLastInputText, isSelected, hasFocus);
        }
        else {
          cmp = super.getListCellRendererComponent(list, value, index, isSelected, isSelected);
          final JPanel p = new JPanel(new BorderLayout());
          p.setBackground(UIUtil.getListBackground(isSelected, true));
          p.add(cmp, BorderLayout.CENTER);
          cmp = p;
        }
      }

      Color bg = cmp.getBackground();
      if (bg == null) {
        cmp.setBackground(UIUtil.getListBackground(isSelected));
        bg = cmp.getBackground();
      }

      Color foreground = cmp.getForeground();
      if (foreground == null) {
        cmp.setForeground(UIUtil.getListForeground(isSelected));
        foreground = cmp.getBackground();
      }
      myMainPanel.removeAll();
      RunAnythingSearchListModel model = getSearchingModel(myResultsList);
      if (model != null) {
        String title = model.getTitle(index);
        if (title != null) {
          myMainPanel.add(RunAnythingUtil.createTitle(" " + title, UIUtil.getListBackground(false, false)), BorderLayout.NORTH);
        }
      }
      JPanel wrapped = new JPanel(new BorderLayout());
      wrapped.setBackground(bg);
      wrapped.setForeground(foreground);
      wrapped.setBorder(RENDERER_BORDER);
      wrapped.add(cmp, BorderLayout.CENTER);
      myMainPanel.add(wrapped, BorderLayout.CENTER);
      if (cmp instanceof Accessible) {
        myMainPanel.setAccessible((Accessible)cmp);
      }

      return myMainPanel;
    }

    @Override
    protected void customizeCellRenderer(@Nonnull JList list, final Object value, int index, final boolean selected, boolean hasFocus) {
    }
  }

  private class CalcThread implements Runnable {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final String myPattern;
    private final ProgressIndicator myProgressIndicator = new ProgressIndicatorBase();
    private final ActionCallback myDone = new ActionCallback();
    @Nonnull
    private final RunAnythingSearchListModel myListModel;

    private CalcThread(@Nonnull Project project, @Nonnull String pattern, boolean reuseModel) {
      myProject = project;
      myPattern = pattern;
      RunAnythingSearchListModel model = getSearchingModel(myResultsList);

      myListModel = reuseModel && model != null ? model : isHelpMode(pattern) ? new RunAnythingSearchListModel.RunAnythingHelpListModel() : new RunAnythingSearchListModel.RunAnythingMainListModel();
    }

    @Override
    public void run() {
      try {
        check();

        Application.get().invokeLater(() -> {
          // this line must be called on EDT to avoid context switch at clear().append("text") Don't touch. Ask [kb]
          myResultsList.getEmptyText().setText("Searching...");

          if (getSearchingModel(myResultsList) != null) {
            myAlarm.cancelAllRequests();
            myAlarm.addRequest(() -> {
              if (DumbService.getInstance(myProject).isDumb()) {
                myResultsList.setEmptyText(IdeBundle.message("run.anything.indexing.mode.not.supported"));
                return;
              }

              if (!myDone.isRejected()) {
                myResultsList.setModel(myListModel);
              }
            }, LIST_REBUILD_DELAY);
          }
          else {
            myResultsList.setModel(myListModel);
          }
        });

        if (myPattern.trim().length() == 0) {
          buildGroups(true);
          return;
        }

        if (isHelpMode(mySearchField.getValue())) {
          buildHelpGroups(myListModel);
          updatePopup();
          return;
        }

        check();
        buildGroups(false);
      }
      catch (ProcessCanceledException ignore) {
        myDone.setRejected();
      }
      catch (Exception e) {
        LOG.error(e);
        myDone.setRejected();
      }
      finally {
        if (!isCanceled()) {
          Application.get().invokeLater(() -> myResultsList.getEmptyText().setText(IdeBundle.message("run.anything.command.empty.list.title")));
        }
        if (!myDone.isProcessed()) {
          myDone.setDone();
        }
      }
    }

    private void buildGroups(boolean isRecent) {
      buildAllGroups(getDataContext(), myPattern, () -> check(), isRecent);
      updatePopup();
    }

    private void buildHelpGroups(@Nonnull RunAnythingSearchListModel listModel) {
      listModel.getGroups().forEach(group -> {
        group.collectItems(getDataContext(), myListModel, trimHelpPattern(), () -> check());
        check();
      });
    }

    protected void check() {
      myProgressIndicator.checkCanceled();
      if (myDone.isRejected()) throw new ProcessCanceledException();
      assert myCalcThread == this : "There are two CalcThreads running before one of them was cancelled";
    }

    private void buildAllGroups(@Nonnull DataContext dataContext, @Nonnull String pattern, @Nonnull Runnable checkCancellation, boolean isRecent) {
      if (isRecent) {
        RunAnythingRecentGroup.INSTANCE.collectItems(dataContext, myListModel, pattern, checkCancellation);
      }
      else {
        buildCompletionGroups(dataContext, pattern, checkCancellation);
      }
    }

    private void buildCompletionGroups(@Nonnull DataContext dataContext, @Nonnull String pattern, @Nonnull Runnable checkCancellation) {
      LOG.assertTrue(myListModel instanceof RunAnythingSearchListModel.RunAnythingMainListModel);

      if (DumbService.getInstance(myProject).isDumb()) {
        return;
      }

      List<RunAnythingGroup> list = new ArrayList<>();
      list.add(RunAnythingRecentGroup.INSTANCE);
      list.addAll(myListModel.getGroups().stream().filter(group -> group instanceof RunAnythingCompletionGroup || group instanceof RunAnythingGeneralGroup)
                          .filter(group -> RunAnythingCache.getInstance(myProject).isGroupVisible(group.getTitle())).collect(Collectors.toList()));

      for (RunAnythingGroup group : list) {
        Application.get().runReadAction(() -> group.collectItems(dataContext, myListModel, pattern, checkCancellation));
        checkCancellation.run();
      }
    }

    private boolean isCanceled() {
      return myProgressIndicator.isCanceled() || myDone.isRejected();
    }

    void updatePopup() {
      Application.get().invokeLater(() -> {
        myListModel.update();
        myResultsList.revalidate();
        myResultsList.repaint();

        installScrollingActions();

        updateViewType(myListModel.getSize() == 0 ? ViewType.SHORT : ViewType.FULL);
      });
    }

    public ActionCallback cancel() {
      myProgressIndicator.cancel();
      return myDone;
    }

    public ActionCallback insert(final int index, @Nonnull RunAnythingGroup group) {
      Application.get().executeOnPooledThread(() -> Application.get().runReadAction(() -> {
        try {
          RunAnythingGroup.SearchResult result = group.getItems(getDataContext(), myListModel, trimHelpPattern(), true, this::check);

          check();
          Application.get().invokeLater(() -> {
            try {
              int shift = 0;
              int i = index + 1;
              for (Object o : result) {
                myListModel.add(i, o);
                shift++;
                i++;
              }

              myListModel.shiftIndexes(index, shift);
              if (!result.isNeedMore()) {
                group.resetMoreIndex();
              }

              clearSelection();
              ScrollingUtil.selectItem(myResultsList, index);
              myDone.setDone();
            }
            catch (Exception e) {
              myDone.setRejected();
            }
          });
        }
        catch (Exception e) {
          myDone.setRejected();
        }
      }));
      return myDone;
    }

    @Nonnull
    public String trimHelpPattern() {
      return isHelpMode(myPattern) ? myPattern.substring(HELP_PLACEHOLDER.length()) : myPattern;
    }

    public ActionCallback start() {
      Application.get().executeOnPooledThread(this);
      return myDone;
    }
  }

  @Override
  public void installScrollingActions() {
    RunAnythingScrollingUtil.installActions(myResultsList, (JTextField)TargetAWT.to(getField()), () -> {
      myIsItemSelected = true;
      mySearchField.setValue(myLastInputText);
      clearSelection();
    }, UISettings.getInstance().getCycleScrolling());

    super.installScrollingActions();
  }

  protected void resetFields() {
    myCurrentWorker.doWhenProcessed(() -> {
      final Object lock = myCalcThread;
      if (lock != null) {
        synchronized (lock) {
          myCurrentWorker = ActionCallback.DONE;
          myCalcThread = null;
          myVirtualFile = null;
          myProject = null;
          myModule = null;
        }
      }
    });
    mySkipFocusGain = false;
  }

  public RunAnythingPopupUI(@Nonnull AnActionEvent actionEvent) {
    super(actionEvent.getProject());

    myActionEvent = actionEvent;

    myCurrentWorker = ActionCallback.DONE;
    myVirtualFile = actionEvent.getData(CommonDataKeys.VIRTUAL_FILE);

    myProject = ObjectUtils.notNull(myActionEvent.getData(CommonDataKeys.PROJECT));
    myModule = myActionEvent.getData(LangDataKeys.MODULE);

    init();

    initSearchActions();

    initResultsList();

    initSearchField();

    initMySearchField();
  }

  @Nonnull
  @Override
  public JBList<Object> createList() {
    myListModel = new RunAnythingSearchListModel.RunAnythingMainListModel();
    addListDataListener(myListModel);

    return new JBList<>(myListModel);
  }

  private void initSearchActions() {
    myResultsList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        onMouseClicked(e);
      }
    });

    DumbAwareAction.create(e -> RunAnythingUtil.jumpNextGroup(true, myResultsList)).registerCustomShortcutSet(CustomShortcutSet.fromString("TAB"), (JComponent)TargetAWT.to(mySearchField), this);
    DumbAwareAction.create(e -> RunAnythingUtil.jumpNextGroup(false, myResultsList))
            .registerCustomShortcutSet(CustomShortcutSet.fromString("shift TAB"), (JComponent)TargetAWT.to(mySearchField), this);

    AnAction escape = ActionManager.getInstance().getAction("EditorEscape");
    DumbAwareAction.create(__ -> {
      triggerUsed();
      searchFinishedHandler.run();
    }).registerCustomShortcutSet(escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(), this);

    DumbAwareAction.create(e -> executeCommand())
            .registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER", "shift ENTER", "alt ENTER", "alt shift ENTER", "meta ENTER"), (JComponent)TargetAWT.to(mySearchField), this);

    DumbAwareAction.create(e -> {
      RunAnythingSearchListModel model = getSearchingModel(myResultsList);
      if (model == null) return;

      Object selectedValue = myResultsList.getSelectedValue();
      int index = myResultsList.getSelectedIndex();
      if (!(selectedValue instanceof RunAnythingItem) || isMoreItem(index)) return;

      RunAnythingCache.getInstance(getProject()).getState().getCommands().remove(((RunAnythingItem)selectedValue).getCommand());

      model.remove(index);
      model.shiftIndexes(index, -1);
      if (model.getSize() > 0) ScrollingUtil.selectItem(myResultsList, index < model.getSize() ? index : index - 1);

      Application.get().invokeLater(() -> {
        if (myCalcThread != null) {
          myCalcThread.updatePopup();
        }
      });
    }).registerCustomShortcutSet(CustomShortcutSet.fromString("shift BACK_SPACE"), (JComponent)TargetAWT.to(mySearchField), this);

    myProject.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      @Override
      public void exitDumbMode() {
        Application.get().invokeLater(() -> rebuildList());
      }
    });
  }

  @Nonnull
  @Override
  protected ListCellRenderer<Object> createCellRenderer() {
    return new MyListRenderer();
  }

  @Nonnull
  @Override
  protected JPanel createSettingsPanel() {
    JPanel res = new JPanel(new FlowLayout(RIGHT, 0, 0));
    res.setOpaque(false);

    DefaultActionGroup actionGroup = new DefaultActionGroup();
    myChooseContextAction = new RunAnythingChooseContextAction(res) {
      @Override
      public void setAvailableContexts(@Nonnull List<? extends RunAnythingContext> executionContexts) {
        myAvailableExecutingContexts.clear();
        myAvailableExecutingContexts.addAll(executionContexts);
      }

      @Nonnull
      @Override
      public List<RunAnythingContext> getAvailableContexts() {
        return myAvailableExecutingContexts;
      }

      @Override
      public void setSelectedContext(@Nullable RunAnythingContext context) {
        mySelectedExecutingContext = context;
      }

      @Nullable
      @Override
      public RunAnythingContext getSelectedContext() {
        return mySelectedExecutingContext;
      }
    };
    actionGroup.addAction(myChooseContextAction);
    actionGroup.addAction(new RunAnythingShowFilterAction());

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("run.anything.toolbar", actionGroup, true);
    toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    toolbar.setTargetComponent(this);
    toolbar.updateActionsImmediately();
    JComponent toolbarComponent = toolbar.getComponent();
    toolbarComponent.setOpaque(false);
    res.add(toolbarComponent);
    return res;
  }

  @Nonnull
  @Override
  protected String getInitialHint() {
    return IdeBundle.message("run.anything.hint.initial.text", KeymapUtil.getKeystrokeText(UP_KEYSTROKE), KeymapUtil.getKeystrokeText(DOWN_KEYSTROKE));
  }

  //@Nonnull
  //@Override
  //protected ExtendableTextField createSearchField() {
  //  ExtendableTextField searchField = super.createSearchField();
  //
  //  Consumer<? super ExtendableTextComponent.Extension> extensionConsumer = (extension) -> searchField.addExtension(extension);
  //  searchField.addPropertyChangeListener(new RunAnythingIconHandler(extensionConsumer, searchField));
  //
  //  return searchField;
  //}

  @Override
  public void dispose() {
    resetFields();
  }

  private class RunAnythingShowFilterAction extends ShowFilterAction {

    @Nonnull
    @Override
    public String getDimensionServiceKey() {
      return "RunAnythingAction_Filter_Popup";
    }

    @Override
    protected boolean isEnabled() {
      return true;
    }

    @Override
    protected boolean isActive() {
      return RunAnythingCompletionGroup.MAIN_GROUPS.size() != getVisibleGroups().size();
    }

    @Override
    protected ElementsChooser<?> createChooser() {
      ElementsChooser<RunAnythingGroup> res = new ElementsChooser<RunAnythingGroup>(new ArrayList<>(RunAnythingCompletionGroup.MAIN_GROUPS), false) {
        @Override
        protected String getItemText(@Nonnull RunAnythingGroup value) {
          return value.getTitle();
        }
      };

      res.markElements(getVisibleGroups());
      ElementsChooser.ElementsMarkListener<RunAnythingGroup> listener = (element, isMarked) -> {
        RunAnythingCache.getInstance(myProject).saveGroupVisibilityKey(element.getTitle(), isMarked);
        rebuildList();
      };
      res.addElementsMarkListener(listener);
      return res;
    }

    @Nonnull
    private List<RunAnythingGroup> getVisibleGroups() {
      Collection<RunAnythingGroup> groups = RunAnythingCompletionGroup.MAIN_GROUPS;
      return ContainerUtil.filter(groups, group -> RunAnythingCache.getInstance(myProject).isGroupVisible(group.getTitle()));
    }
  }
}

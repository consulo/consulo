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
package com.intellij.openapi.options.newEditor;

import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.ide.ui.search.ConfigurableHit;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.options.ex.GlassPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.AbstractPainter;
import com.intellij.openapi.ui.DetailsComponent;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.NullableComponent;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.EdtRunnable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeGlassPaneUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.LightColors;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.navigation.History;
import com.intellij.ui.navigation.Place;
import com.intellij.ui.speedSearch.ElementFilter;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.UiNotifyConnector;
import com.intellij.util.ui.update.Update;
import consulo.application.ApplicationProperties;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.options.ConfigurableUIMigrationUtil;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.decorator.SwingUIDecorator;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class OptionsEditor implements DataProvider, Place.Navigator, Disposable, AWTEventListener, UISettingsListener {
  private static class SearchableWrapper implements SearchableConfigurable {
    private final Configurable myConfigurable;

    private SearchableWrapper(final Configurable configurable) {
      myConfigurable = configurable;
    }

    @Override
    @Nonnull
    public String getId() {
      return myConfigurable.getClass().getName();
    }

    @Override
    @Nls
    public String getDisplayName() {
      return myConfigurable.getDisplayName();
    }

    @Override
    public String getHelpTopic() {
      return myConfigurable.getHelpTopic();
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent() {
      return myConfigurable.createComponent();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
      return myConfigurable.isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
      myConfigurable.apply();
    }

    @RequiredUIAccess
    @Override
    public void reset() {
      myConfigurable.reset();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
      myConfigurable.disposeUIResources();
    }
  }


  private static class ContentWrapper extends NonOpaquePanel {
    private final JLabel myErrorLabel;

    private JComponent mySimpleContent;
    private ConfigurationException myException;

    private ContentWrapper() {
      setLayout(new BorderLayout());
      myErrorLabel = new JLabel();
      myErrorLabel.setOpaque(true);
      myErrorLabel.setBackground(LightColors.RED);
    }

    void setContent(final JComponent component, ConfigurationException e, @Nonnull Configurable configurable) {
      if (component != null && mySimpleContent == component && myException == e) {
        return;
      }

      removeAll();

      if (component != null) {
        boolean noMargin = ConfigurableWrapper.isNoMargin(configurable);
        JComponent wrapComponent = component;
        if (!noMargin) {
          wrapComponent = JBUI.Panels.simplePanel().addToCenter(wrapComponent);
          wrapComponent.setBorder(new EmptyBorder(UIUtil.PANEL_SMALL_INSETS));
        }


        boolean noScroll = ConfigurableWrapper.isNoScroll(configurable);
        if (!noScroll) {
          JScrollPane scroll = ScrollPaneFactory.createScrollPane(wrapComponent, true);
          scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
          scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
          add(scroll, BorderLayout.CENTER);
        }
        else {
          add(wrapComponent, BorderLayout.CENTER);
        }
      }

      if (e != null) {
        myErrorLabel.setText(UIUtil.toHtml(e.getMessage()));
        add(myErrorLabel, BorderLayout.NORTH);
      }

      mySimpleContent = component;
      myException = e;
    }

    @Override
    public boolean isNull() {
      final boolean superNull = super.isNull();
      if (superNull) return superNull;
      return NullableComponent.Check.isNull(mySimpleContent);
    }
  }

  private class ConfigurableContext {
    JComponent myComponent;
    Configurable myConfigurable;

    @RequiredUIAccess
    ConfigurableContext(final Configurable configurable) {
      myConfigurable = configurable;
      myComponent = ConfigurableUIMigrationUtil.createComponent(configurable);

      if (myComponent != null) {
        final Object clientProperty = myComponent.getClientProperty(NOT_A_NEW_COMPONENT);
        if (clientProperty != null && ApplicationProperties.isInSandbox()) {
          LOG.warn(String.format("Settings component for '%s' MUST be recreated, please dispose it in disposeUIResources() and create a new instance in createComponent()!",
                                 configurable.getClass().getCanonicalName()));
        }
        else {
          myComponent.putClientProperty(NOT_A_NEW_COMPONENT, Boolean.TRUE);
        }
      }
    }

    void set(final ContentWrapper wrapper) {
      myOwnDetails.setDetailsModeEnabled(true);
      wrapper.setContent(myComponent, getContext().getErrors().get(myConfigurable), myConfigurable);
    }

    boolean isShowing() {
      return myComponent != null && myComponent.isShowing();
    }
  }

  private static class MySearchField extends SearchTextField {
    private boolean myDelegatingNow;

    private MySearchField() {
      super(false);
      addKeyListener(new KeyAdapter() {
      });
    }

    @Override
    protected boolean preprocessEventForTextField(final KeyEvent e) {
      final KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
      if (!myDelegatingNow) {
        if ("pressed ESCAPE".equals(stroke.toString()) && getText().length() > 0) {
          setText(""); // reset filter on ESC
          return true;
        }

        if (getTextEditor().isFocusOwner()) {
          try {
            myDelegatingNow = true;
            boolean treeNavigation = stroke.getModifiers() == 0 && (stroke.getKeyCode() == KeyEvent.VK_UP || stroke.getKeyCode() == KeyEvent.VK_DOWN);

            if ("pressed ENTER".equals(stroke.toString())) {
              return true; // avoid closing dialog on ENTER
            }

            final Object action = getTextEditor().getInputMap().get(stroke);
            if (action == null || treeNavigation) {
              onTextKeyEvent(e);
              return true;
            }
          }
          finally {
            myDelegatingNow = false;
          }
        }
      }
      return false;
    }

    protected void onTextKeyEvent(final KeyEvent e) {
    }
  }

  private class SpotlightPainter extends AbstractPainter {
    Map<Configurable, String> myConfigurableToLastOption = new HashMap<>();

    GlassPanel myGP = new GlassPanel(myOwnDetails.getContentGutter());
    boolean myVisible;

    @Override
    public void executePaint(final Component component, final Graphics2D g) {
      if (myVisible && myGP.isVisible()) {
        myGP.paintSpotlight(g, myOwnDetails.getContentGutter());
      }
    }

    public boolean updateForCurrentConfigurable() {
      final Configurable current = getContext().getCurrentConfigurable();

      if (current != null && !myConfigurable2Content.containsKey(current)) {
        return ApplicationManager.getApplication().isUnitTestMode();
      }

      String text = getFilterText();

      try {
        final boolean sameText = myConfigurableToLastOption.containsKey(current) && text.equals(myConfigurableToLastOption.get(current));


        if (current == null) {
          myVisible = false;
          myGP.clear();
          return true;
        }

        SearchableConfigurable searchable;
        if (current instanceof SearchableConfigurable) {
          searchable = (SearchableConfigurable)current;
        }
        else {
          searchable = new SearchableWrapper(current);
        }

        myGP.clear();

        final Runnable runnable = SearchUtil.lightOptions(searchable, myContentWrapper, text, myGP);
        if (runnable != null) {
          myVisible = true;//myContext.isHoldingFilter();
          runnable.run();

          boolean pushFilteringFurther = true;
          if (sameText) {
            pushFilteringFurther = false;
          }
          else {
            if (myFilter.myHits != null) {
              pushFilteringFurther = !myFilter.myHits.getNameHits().contains(current);
            }
          }

          final Runnable ownSearch = searchable.enableSearch(text);
          if (pushFilteringFurther && ownSearch != null) {
            ownSearch.run();
          }
          fireNeedsRepaint(myOwnDetails.getComponent());
        }
        else {
          myVisible = false;
        }
      }
      finally {
        myConfigurableToLastOption.put(current, text);
      }

      return true;
    }

    @Override
    public boolean needsRepaint() {
      return true;
    }
  }

  private class MyColleague implements OptionsEditorColleague {
    @Override
    public ActionCallback onSelected(final Configurable configurable, final Configurable oldConfigurable) {
      return processSelected(configurable, oldConfigurable);
    }

    @Override
    public ActionCallback onModifiedRemoved(final Configurable configurable) {
      return updateIfCurrent(configurable);
    }

    @Override
    public ActionCallback onModifiedAdded(final Configurable configurable) {
      return updateIfCurrent(configurable);
    }

    @Override
    public ActionCallback onErrorsChanged() {
      return updateIfCurrent(getContext().getCurrentConfigurable());
    }

    private ActionCallback updateIfCurrent(final Configurable configurable) {
      if (getContext().getCurrentConfigurable() == configurable && configurable != null) {
        updateContent();
        return new ActionCallback.Done();
      }
      else {
        return new ActionCallback.Rejected();
      }
    }
  }

  private class Filter extends ElementFilter.Active.Impl<SimpleNode> {
    SearchableOptionsRegistrar myIndex = SearchableOptionsRegistrar.getInstance();
    Set<Configurable> myFiltered = null;
    ConfigurableHit myHits;

    boolean myUpdateEnabled = true;
    private Configurable myLastSelected;

    @Override
    public boolean shouldBeShowing(final SimpleNode value) {
      if (myFiltered == null) return true;

      if (value instanceof OptionsTree.ConfigurableNode) {
        final OptionsTree.ConfigurableNode node = (OptionsTree.ConfigurableNode)value;
        return myFiltered.contains(node.getConfigurable()) || isChildOfNameHit(node);
      }

      return true;
    }

    private boolean isChildOfNameHit(OptionsTree.ConfigurableNode node) {
      if (myHits != null) {
        OptionsTree.Base eachParent = node;
        while (eachParent != null) {
          if (eachParent instanceof OptionsTree.ConfigurableNode) {
            final OptionsTree.ConfigurableNode eachConfigurableNode = (OptionsTree.ConfigurableNode)eachParent;
            if (myHits.getNameFullHits().contains(eachConfigurableNode.myConfigurable)) return true;
          }
          eachParent = (OptionsTree.Base)eachParent.getParent();
        }

        return false;
      }

      return false;
    }

    public Promise<?> refilterFor(String text, boolean adjustSelection, final boolean now) {
      try {
        myUpdateEnabled = false;
        mySearch.setText(text);
      }
      finally {
        myUpdateEnabled = true;
      }

      return update(DocumentEvent.EventType.CHANGE, adjustSelection, now);
    }

    public void clearTemporary() {
      myContext.setHoldingFilter(false);
      updateSpotlight(false);
    }

    public void reenable() {
      myContext.setHoldingFilter(true);
      updateSpotlight(false);
    }

    public Promise<?> update(DocumentEvent.EventType type, boolean adjustSelection, boolean now) {
      if (!myUpdateEnabled) return Promises.rejectedPromise();

      final String text = mySearch.getText();
      if (getFilterText().length() == 0) {
        myContext.setHoldingFilter(false);
        myFiltered = null;
      }
      else {
        myContext.setHoldingFilter(true);
        myHits = myIndex.getConfigurables(myConfigurables, type, myFiltered, text, myProject);
        myFiltered = myHits.getAll();
      }

      if (myFiltered != null && myFiltered.isEmpty()) {
        mySearch.getTextEditor().setBackground(LightColors.RED);
      }
      else {
        mySearch.getTextEditor().setBackground(UIUtil.getTextFieldBackground());
      }


      final Configurable current = getContext().getCurrentConfigurable();

      boolean shouldMoveSelection = true;

      if (myHits != null && (myHits.getNameFullHits().contains(current) || myHits.getContentHits().contains(current))) {
        shouldMoveSelection = false;
      }

      if (shouldMoveSelection && type != DocumentEvent.EventType.INSERT && (myFiltered == null || myFiltered.contains(current))) {
        shouldMoveSelection = false;
      }

      Configurable toSelect = adjustSelection ? current : null;
      if (shouldMoveSelection && myHits != null) {
        if (!myHits.getNameHits().isEmpty()) {
          toSelect = suggestToSelect(myHits.getNameHits(), myHits.getNameFullHits());
        }
        else if (!myHits.getContentHits().isEmpty()) {
          toSelect = suggestToSelect(myHits.getContentHits(), null);
        }
      }

      updateSpotlight(false);

      if ((myFiltered == null || !myFiltered.isEmpty()) && toSelect == null && myLastSelected != null) {
        toSelect = myLastSelected;
        myLastSelected = null;
      }

      if (toSelect == null && current != null) {
        myLastSelected = current;
      }

      final Promise<?> callback = fireUpdate(adjustSelection ? myTree.findNodeFor(toSelect) : null, adjustSelection, now);

      myFilterDocumentWasChanged = true;

      return callback;
    }

    private boolean isEmptyParent(Configurable configurable) {
      return configurable instanceof SearchableConfigurable.Parent && !((SearchableConfigurable.Parent)configurable).hasOwnContent();
    }

    @Nullable
    private Configurable suggestToSelect(Set<Configurable> set, Set<Configurable> fullHits) {
      Configurable candidate = null;
      for (Configurable each : set) {
        if (fullHits != null && fullHits.contains(each)) return each;
        if (!isEmptyParent(each) && candidate == null) {
          candidate = each;
        }
      }

      return candidate;
    }

  }

  public static Key<OptionsEditor> KEY = Key.create("options.editor");

  private static final Logger LOG = Logger.getInstance(OptionsEditor.class);

  @NonNls
  public static final String MAIN_SPLITTER_PROPORTION = "options.splitter.main.proportions";

  @NonNls
  private static final String NOT_A_NEW_COMPONENT = "component.was.already.instantiated";

  private final Project myProject;

  private final OptionsEditorContext myContext;

  private final OptionsTree myTree;
  private final MySearchField mySearch;

  private final DetailsComponent myOwnDetails = new DetailsComponent(false, false).setEmptyContentText("Select configuration element in the tree to edit its settings");
  private final ContentWrapper myContentWrapper = new ContentWrapper();

  private final Map<Configurable, ConfigurableContext> myConfigurable2Content = new HashMap<>();
  private final Map<Configurable, AsyncResult<Void>> myConfigurable2LoadCallback = new HashMap<>();

  private final MergingUpdateQueue myModificationChecker;
  private final Configurable[] myConfigurables;
  private JPanel myRootPanel;

  private final SpotlightPainter mySpotlightPainter = new SpotlightPainter();
  private final MergingUpdateQueue mySpotlightUpdate;
  private final LoadingDecorator myLoadingDecorator;
  private final Filter myFilter;

  private final JPanel mySearchWrapper = new JPanel(new BorderLayout());
  private final JPanel myLeftSide;

  private boolean myFilterDocumentWasChanged;
  private Window myWindow;
  private volatile boolean myDisposed;

  public OptionsEditor(Project project, Configurable[] configurables, Configurable preselectedConfigurable, final JPanel rootPanel) {
    myProject = project;
    myConfigurables = configurables;
    myRootPanel = rootPanel;

    myFilter = new Filter();
    myContext = new OptionsEditorContext(myFilter);

    mySearch = new MySearchField() {
      @Override
      protected void onTextKeyEvent(final KeyEvent e) {
        myTree.processTextEvent(e);
      }
    };

    mySearch.getTextEditor().addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        boolean hasText = mySearch.getText().length() > 0;
        if (!myContext.isHoldingFilter() && hasText) {
          myFilter.reenable();
        }

        if (!isSearchFieldFocused() && hasText) {
          mySearch.selectText();
        }
      }
    });

    myTree = new OptionsTree(configurables, getContext()) {
      @Override
      protected void onTreeKeyEvent(final KeyEvent e) {
        myFilterDocumentWasChanged = false;
        try {
          mySearch.keyEventToTextField(e);
        }
        finally {
          if (myFilterDocumentWasChanged && !isFilterFieldVisible()) {
            setFilterFieldVisible(true, false, false);
          }
        }
      }
    };

    getContext().addColleague(myTree);
    Disposer.register(this, myTree);
    mySearch.addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        myFilter.update(e.getType(), true, false);
      }
    });

    JComponent component = myTree.getComponent();

    myLeftSide = new JPanel(new BorderLayout());

    myLeftSide.add(mySearchWrapper, BorderLayout.NORTH);
    myLeftSide.add(component, BorderLayout.CENTER);

    myLoadingDecorator = new LoadingDecorator(myOwnDetails.getComponent(), this, 150);

    MyColleague colleague = new MyColleague();
    getContext().addColleague(colleague);

    mySpotlightUpdate = new MergingUpdateQueue("OptionsSpotlight", 200, false, rootPanel, this, rootPanel);

    if (preselectedConfigurable != null) {
      myTree.select(preselectedConfigurable);
    }
    else {
      myTree.selectFirst();
    }

    Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

    ActionManager.getInstance().addAnActionListener(new AnActionListener() {
      @Override
      public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
      }

      @Override
      public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        queueModificationCheck();
      }
    }, this);

    myModificationChecker = new MergingUpdateQueue("OptionsModificationChecker", 1000, false, rootPanel, this, rootPanel);

    IdeGlassPaneUtil.installPainter(myOwnDetails.getContentGutter(), mySpotlightPainter, this);

    setFilterFieldVisible(true, false, false);

    uiSettingsChanged(UISettings.getInstance());

    new UiNotifyConnector.Once(myRootPanel, new Activatable() {
      @Override
      public void showNotify() {
        myWindow = SwingUtilities.getWindowAncestor(rootPanel);
      }
    });
  }

  @Override
  public void uiSettingsChanged(UISettings source) {
    mySearchWrapper.setBorder(JBUI.Borders.empty(10, 5));

    mySearch.setBackground(SwingUIDecorator.get(SwingUIDecorator::getSidebarColor));
    mySearchWrapper.setBackground(SwingUIDecorator.get(SwingUIDecorator::getSidebarColor));
    myLeftSide.setBackground(SwingUIDecorator.get(SwingUIDecorator::getSidebarColor));
  }

  public JPanel getLeftSide() {
    return myLeftSide;
  }

  public JComponent getRightSide() {
    return myLoadingDecorator.getComponent();
  }

  @Nonnull
  public ActionCallback select(Class<? extends Configurable> configurableClass) {
    final Configurable configurable = findConfigurable(configurableClass);
    if (configurable == null) {
      return new ActionCallback.Rejected();
    }
    return select(configurable);
  }

  @Nullable
  public <T extends Configurable> T findConfigurable(Class<T> configurableClass) {
    return myTree.findConfigurable(configurableClass);
  }

  @Nullable
  public SearchableConfigurable findConfigurableById(@Nonnull String configurableId) {
    return myTree.findConfigurableById(configurableId);
  }

  public ActionCallback clearSearchAndSelect(Configurable configurable) {
    clearFilter();
    return select(configurable, "");
  }

  public ActionCallback select(Configurable configurable) {
    if (StringUtil.isEmpty(mySearch.getText())) {
      return select(configurable, "");
    }
    else {
      return Promises.toActionCallback(myFilter.refilterFor(mySearch.getText(), true, true));
    }
  }

  public ActionCallback select(Configurable configurable, final String text) {
    ActionCallback callback = new ActionCallback();
    Promises.toActionCallback(myFilter.refilterFor(text, false, true)).doWhenDone(() -> myTree.select(configurable).notify(callback));
    return callback;
  }

  private ActionCallback processSelected(final Configurable configurable, final Configurable oldConfigurable) {
    if (isShowing(configurable)) return new ActionCallback.Done();

    final ActionCallback result = new ActionCallback();

    if (configurable == null) {
      myOwnDetails.setContent(null);

      updateSpotlight(true);
      checkModified(oldConfigurable);

      result.setDone();

    }
    else {
      getUiFor(configurable).doWhenDone(new EdtRunnable() {
        @Override
        public void runEdt() {
          if (myDisposed) return;

          final Configurable current = getContext().getCurrentConfigurable();
          if (current != configurable) {
            result.setRejected();
            return;
          }

          updateContent();

          myOwnDetails.setContent(myContentWrapper);
          myOwnDetails.setBannerMinHeight(mySearchWrapper.getHeight());
          myOwnDetails.setText(getBannerText(configurable));

          myLoadingDecorator.stopLoading();

          updateSpotlight(false);

          checkModified(oldConfigurable);
          checkModified(configurable);

          if (myTree.myBuilder.getSelectedElements().size() == 0) {
            select(configurable).notify(result);
          }
          else {
            result.setDone();
          }
        }
      });
    }

    return result;
  }

  private static void assertIsDispatchThread() {
    ApplicationManager.getApplication().assertIsDispatchThread();
  }

  @RequiredUIAccess
  private AsyncResult<Void> getUiFor(final Configurable target) {
    assertIsDispatchThread();

    if (myDisposed) {
      return AsyncResult.rejected();
    }

    UIAccess uiAccess = UIAccess.current();
    if (!myConfigurable2Content.containsKey(target)) {

      return myConfigurable2LoadCallback.computeIfAbsent(target, configurable -> {
        AsyncResult<Void> result = AsyncResult.undefined();

        myLoadingDecorator.startLoading(false);

        uiAccess.give(() -> {
          if (myProject.isDisposed()) {
            result.setRejected();
            return;
          }

          initConfigurable(configurable, result);
        });

        return result;
      });
    }

    return AsyncResult.resolved();
  }

  @RequiredUIAccess
  private void initConfigurable(@Nonnull final Configurable configurable, AsyncResult<Void> result) {
    UIAccess.assertIsUIThread();
    
    if (myDisposed) {
      result.setRejected();
      return;
    }

    try {
      final ConfigurableContext content = new ConfigurableContext(configurable);
      if (!myConfigurable2Content.containsKey(configurable)) {
        configurable.reset();
      }
      myConfigurable2Content.put(configurable, content);
      result.setDone();
    }
    catch (Throwable e) {
      result.rejectWithThrowable(e);
    }
  }

  private void updateSpotlight(boolean now) {
    if (now) {
      final boolean success = mySpotlightPainter.updateForCurrentConfigurable();
      if (!success) {
        updateSpotlight(false);
      }
    }
    else {
      mySpotlightUpdate.queue(new Update(this) {
        @Override
        public void run() {
          final boolean success = mySpotlightPainter.updateForCurrentConfigurable();
          if (!success) {
            updateSpotlight(false);
          }
        }
      });
    }
  }

  private String[] getBannerText(Configurable configurable) {
    final List<Configurable> list = myTree.getPathToRoot(configurable);
    final String[] result = new String[list.size()];
    int add = 0;
    for (int i = list.size() - 1; i >= 0; i--) {
      result[add++] = list.get(i).getDisplayName().replace('\n', ' ');
    }
    return result;
  }

  private void checkModified(final Configurable configurable) {
    fireModification(configurable);
  }

  private void fireModification(final Configurable actual) {
    Collection<Configurable> toCheck = collectAllParentsAndSiblings(actual);

    for (Configurable configurable : toCheck) {
      fireModificationForItem(configurable);
    }
  }

  private Collection<Configurable> collectAllParentsAndSiblings(final Configurable actual) {
    ArrayList<Configurable> result = new ArrayList<>();
    Configurable nearestParent = getContext().getParentConfigurable(actual);

    if (nearestParent != null) {
      Configurable parent = nearestParent;
      while (parent != null) {
        result.add(parent);
        parent = getContext().getParentConfigurable(parent);
      }

      result.addAll(getContext().getChildren(nearestParent));
    }
    else {
      result.add(actual);
    }

    return result;
  }

  private void fireModificationForItem(final Configurable configurable) {
    if (configurable != null) {
      if (!myConfigurable2Content.containsKey(configurable) && ConfigurableWrapper.hasOwnContent(configurable)) {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (myDisposed) return;
          AsyncResult<Void> result = AsyncResult.undefined();
          initConfigurable(configurable, result);
          result.doWhenDone(() -> {
            if (myDisposed) return;
            fireModificationInt(configurable);
          });
        });
      }
      else if (myConfigurable2Content.containsKey(configurable)) {
        fireModificationInt(configurable);
      }
    }
  }

  @RequiredUIAccess
  private void fireModificationInt(final Configurable configurable) {
    if (configurable.isModified()) {
      getContext().fireModifiedAdded(configurable, null);
    }
    else if (!configurable.isModified() && !getContext().getErrors().containsKey(configurable)) {
      getContext().fireModifiedRemoved(configurable, null);
    }
  }

  private void updateContent() {
    final Configurable current = getContext().getCurrentConfigurable();

    assert current != null;

    final ConfigurableContext content = myConfigurable2Content.get(current);
    content.set(myContentWrapper);
  }

  private boolean isShowing(Configurable configurable) {
    final ConfigurableContext content = myConfigurable2Content.get(configurable);
    return content != null && content.isShowing();
  }

  @Nullable
  public String getHelpTopic() {
    Configurable current = getContext().getCurrentConfigurable();
    while (current != null) {
      String topic = current.getHelpTopic();
      if (topic != null) return topic;
      current = getContext().getParentConfigurable(current);
    }
    return null;
  }

  public boolean isFilterFieldVisible() {
    return mySearch.getParent() == mySearchWrapper;
  }

  public void setFilterFieldVisible(final boolean visible, boolean requestFocus, boolean checkFocus) {
    if (isFilterFieldVisible() && checkFocus && requestFocus && !isSearchFieldFocused()) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(mySearch, true));
      return;
    }

    mySearchWrapper.removeAll();
    mySearchWrapper.add(visible ? mySearch : null, BorderLayout.CENTER);

    myLeftSide.revalidate();
    myLeftSide.repaint();

    if (visible && requestFocus) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(mySearch, true));
    }
  }

  public boolean isSearchFieldFocused() {
    return mySearch.getTextEditor().isFocusOwner();
  }

  public void repaint() {
    myRootPanel.invalidate();
    myRootPanel.repaint();
  }

  public void reset(Configurable configurable, boolean notify) {
    configurable.reset();
    if (notify) {
      getContext().fireReset(configurable);
    }
  }

  public void apply() {
    Map<Configurable, ConfigurationException> errors = new LinkedHashMap<>();
    final Set<Configurable> modified = getContext().getModified();
    for (Configurable each : modified) {
      try {
        each.apply();
        if (!each.isModified()) {
          getContext().fireModifiedRemoved(each, null);
        }
      }
      catch (ConfigurationException e) {
        errors.put(each, e);
        LOG.debug(e);
      }
    }

    getContext().fireErrorsChanged(errors, null);

    if (!errors.isEmpty()) {
      myTree.select(errors.keySet().iterator().next());
    }
  }


  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (KEY == dataId) {
      return this;
    }
    return null;
  }

  public JTree getPreferredFocusedComponent() {
    return myTree.getTree();
  }

  @Override
  public AsyncResult<Void> navigateTo(@Nullable final Place place, final boolean requestFocus) {
    final Configurable config = (Configurable)place.getPath("configurable");
    final String filter = (String)place.getPath("filter");

    final AsyncResult<Void> result = AsyncResult.undefined();

    myFilter.refilterFor(filter, false, true).onSuccess((c) -> myTree.select(config).notifyWhenDone(result));

    return result;
  }

  @Override
  public void queryPlace(@Nonnull final Place place) {
    final Configurable current = getContext().getCurrentConfigurable();
    place.putPath("configurable", current);
    place.putPath("filter", getFilterText());

    if (current instanceof Place.Navigator) {
      ((Place.Navigator)current).queryPlace(place);
    }
  }

  @Override
  public void dispose() {
    assertIsDispatchThread();

    if (myDisposed) {
      return;
    }

    myDisposed = true;

    Toolkit.getDefaultToolkit().removeAWTEventListener(this);

    visitRecursive(myConfigurables, each -> {
      ActionCallback loadCb = myConfigurable2LoadCallback.get(each);
      if (loadCb != null) {
        loadCb.doWhenProcessed(() -> {
          assertIsDispatchThread();
          each.disposeUIResources();
        });
      }
      else {
        each.disposeUIResources();
      }
    });

    ReflectionUtil.clearOwnFields(this, Conditions.<Field>alwaysTrue());
  }

  private static void visitRecursive(Configurable[] configurables, Consumer<Configurable> consumer) {
    for (Configurable configurable : configurables) {
      consumer.accept(configurable);

      if (configurable instanceof Configurable.Composite) {
        visitRecursive(((Configurable.Composite)configurable).getConfigurables(), consumer);
      }
    }
  }

  public OptionsEditorContext getContext() {
    return myContext;
  }

  public void flushModifications() {
    fireModification(getContext().getCurrentConfigurable());
  }

  public boolean canApply() {
    return !getContext().getModified().isEmpty();
  }

  @Override
  public void eventDispatched(final AWTEvent event) {
    if (event.getID() == MouseEvent.MOUSE_PRESSED || event.getID() == MouseEvent.MOUSE_RELEASED || event.getID() == MouseEvent.MOUSE_DRAGGED) {
      final MouseEvent me = (MouseEvent)event;
      if (SwingUtilities.isDescendingFrom(me.getComponent(), SwingUtilities.getWindowAncestor(myContentWrapper)) || isPopupOverEditor(me.getComponent())) {
        queueModificationCheck();
        myFilter.clearTemporary();
      }
    }
    else if (event.getID() == KeyEvent.KEY_PRESSED || event.getID() == KeyEvent.KEY_RELEASED) {
      final KeyEvent ke = (KeyEvent)event;
      if (SwingUtilities.isDescendingFrom(ke.getComponent(), myContentWrapper)) {
        queueModificationCheck();
      }
    }
  }

  private void queueModificationCheck() {
    final Configurable configurable = getContext().getCurrentConfigurable();
    myModificationChecker.queue(new Update(this) {
      @Override
      public void run() {
        checkModified(configurable);
      }

      @Override
      public boolean isExpired() {
        return getContext().getCurrentConfigurable() != configurable;
      }
    });
  }

  private boolean isPopupOverEditor(Component c) {
    final Window wnd = SwingUtilities.getWindowAncestor(c);
    return (wnd instanceof JWindow || wnd instanceof JDialog && ((JDialog)wnd).getModalityType() == Dialog.ModalityType.MODELESS) && myWindow != null && wnd.getParent() == myWindow;
  }

  private String getFilterText() {
    return mySearch.getText() != null ? mySearch.getText().trim() : "";
  }

  public void clearFilter() {
    mySearch.setText("");
  }

  @Override
  public void setHistory(final History history) {
  }
}

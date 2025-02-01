// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.ui.DimensionService;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.fileEditor.impl.internal.IdeDocumentHistoryImpl;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.CaptionPanel;
import consulo.ide.impl.idea.ui.WindowMoveListener;
import consulo.ide.impl.idea.ui.speedSearch.ListWithFilter;
import consulo.ide.impl.idea.ui.speedSearch.NameFilteringListModel;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.Coordinate2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearch;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static consulo.ui.ex.awt.speedSearch.SpeedSearchSupply.ENTERED_PREFIX_PROPERTY_NAME;

public class RecentLocationsAction extends DumbAwareAction {
  public static final String RECENT_LOCATIONS_ACTION_ID = "RecentLocations";
  private static final String LOCATION_SETTINGS_KEY = "recent.locations.popup";

  private static final JBValue.UIInteger DEFAULT_WIDTH = new JBValue.UIInteger(RECENT_LOCATIONS_ACTION_ID + ".defaultWidth", 700);
  private static final JBValue.UIInteger DEFAULT_HEIGHT = new JBValue.UIInteger(RECENT_LOCATIONS_ACTION_ID + ".defaultHeight", 530);
  private static final JBValue.UIInteger MINIMUM_WIDTH = new JBValue.UIInteger(RECENT_LOCATIONS_ACTION_ID + ".minWidth", 600);
  private static final JBValue.UIInteger MINIMUM_HEIGHT = new JBValue.UIInteger(RECENT_LOCATIONS_ACTION_ID + ".minHeight", 450);

  public static String getShortcutHexColor() {
    Color color = UIUtil.getContextHelpForeground();
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed(RECENT_LOCATIONS_ACTION_ID);
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }

    showPopup(project, false);
  }

  public static void showPopup(@Nonnull Project project, boolean showChanged) {
    RecentLocationsDataModel model = new RecentLocationsDataModel(project, new ArrayList<>());
    JBList<RecentLocationItem> list = new JBList<>(JBList.createDefaultListModel(model.getPlaces(showChanged)));
    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(
      list,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    );

    scrollPane.setBorder(BorderFactory.createEmptyBorder());

    ShortcutSet showChangedOnlyShortcutSet = KeymapUtil.getActiveKeymapShortcuts(RECENT_LOCATIONS_ACTION_ID);
    JBCheckBox checkBox = createCheckbox(showChangedOnlyShortcutSet, showChanged);

    ListWithFilter<RecentLocationItem> listWithFilter =
      (ListWithFilter<RecentLocationItem>)ListWithFilter.wrap(list, scrollPane, getNamer(model, checkBox), true);
    listWithFilter.setAutoPackHeight(false);
    listWithFilter.setBorder(BorderFactory.createEmptyBorder());

    final SpeedSearch speedSearch = listWithFilter.getSpeedSearch();
    speedSearch.addChangeListener(evt -> {
      if (evt.getPropertyName().equals(ENTERED_PREFIX_PROPERTY_NAME)) {
        if (StringUtil.isEmpty(speedSearch.getFilter())) {
          model.getEditorsToRelease().forEach(editor -> clearSelectionInEditor(editor));
        }
      }
    });

    list.setCellRenderer(new RecentLocationsRenderer(project, speedSearch, model, checkBox));
    list.setEmptyText(IdeLocalize.recentLocationsPopupEmptyText().get());
    list.setBackground(TargetAWT.to(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground()));
    ScrollingUtil.installActions(list);
    ScrollingUtil.ensureSelectionExists(list);

    JLabel title = createTitle(showChanged);

    CaptionPanel topPanel = createHeaderPanel(title, checkBox);
    JPanel mainPanel = createMainPanel(listWithFilter, topPanel);

    Ref<Boolean> navigationRef = Ref.create(false);
    JBPopup popup = JBPopupFactory.getInstance()
      .createComponentPopupBuilder(mainPanel, list)
      .setProject(project)
      .setCancelOnClickOutside(true)
      .setRequestFocus(true)
      .setCancelCallback(() -> {
        if (speedSearch.isHoldingFilter() && !navigationRef.get()) {
          speedSearch.reset();
          return false;
        }
        return true;
      })
      .setResizable(true)
      .setMovable(true)
      .setDimensionServiceKey(project, LOCATION_SETTINGS_KEY, true)
      .setMinSize(new Dimension(MINIMUM_WIDTH.get(), MINIMUM_HEIGHT.get()))
      .setLocateWithinScreenBounds(false)
      .createPopup();

    DumbAwareAction.create(event -> {
      checkBox.setSelected(!checkBox.isSelected());
      updateItems(model, listWithFilter, title, checkBox, popup);
    }).registerCustomShortcutSet(showChangedOnlyShortcutSet, list, popup);

    checkBox.addActionListener(e -> updateItems(model, listWithFilter, title, checkBox, popup));

    if (DimensionService.getInstance().getSize(LOCATION_SETTINGS_KEY, project) == null) {
      popup.setSize(new Dimension(DEFAULT_WIDTH.get(), DEFAULT_HEIGHT.get()));
    }

    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        int clickCount = event.getClickCount();
        if (clickCount > 1 && clickCount % 2 == 0) {
          event.consume();
          final int i = list.locationToIndex(event.getPoint());
          if (i != -1) {
            list.setSelectedIndex(i);
            navigateToSelected(project, list, popup, navigationRef);
          }
        }
      }
    });

    popup.addListener(new JBPopupListener() {
      @Override
      public void onClosed(@Nonnull LightweightWindowEvent event) {
        model.getEditorsToRelease().forEach(editor -> EditorFactory.getInstance().releaseEditor(editor));
        model.getProjectConnection().disconnect();
      }
    });

    initSearchActions(project, model, listWithFilter, list, checkBox, popup, navigationRef);

    IdeEventQueueProxy.getInstance().closeAllPopups(false);

    list.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        if (!(e.getOppositeComponent() instanceof JCheckBox)) {
          popup.cancel();
        }
      }
    });

    showPopup(project, popup);
  }

  private static void updateItems(
    @Nonnull RecentLocationsDataModel data,
    @Nonnull ListWithFilter<RecentLocationItem> listWithFilter,
    @Nonnull JLabel title,
    @Nonnull JBCheckBox checkBox,
    @Nonnull JBPopup popup
  ) {
    boolean state = checkBox.isSelected();
    updateModel(listWithFilter, data, state);
    updateTitleText(title, state);

    IdeFocusManager.getGlobalInstance().requestFocus(listWithFilter, false);

    popup.pack(false, false);
  }

  @Nonnull
  public static JBCheckBox createCheckbox(@Nonnull ShortcutSet checkboxShortcutSet, boolean showChanged) {
    String text = "<html>" +
      IdeLocalize.recentLocationsTitleText().get() +
      " <font color=\"" +
      getShortcutHexColor() +
      "\">" +
      KeymapUtil.getShortcutsText(checkboxShortcutSet.getShortcuts()) +
      "</font>" +
      "</html>";
    JBCheckBox checkBox = new JBCheckBox(text);
    checkBox.setBorder(JBUI.Borders.empty());
    checkBox.setOpaque(false);
    checkBox.setSelected(showChanged);

    return checkBox;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled((e == null ? null : e.getData(Project.KEY)) != null);
  }

  static void clearSelectionInEditor(@Nonnull Editor editor) {
    editor.getSelectionModel().removeSelection(true);
  }

  private static void showPopup(@Nonnull Project project, @Nonnull JBPopup popup) {
    Coordinate2D savedLocation = DimensionService.getInstance().getLocation(LOCATION_SETTINGS_KEY, project);
    Window recentFocusedWindow = TargetAWT.to(WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow());
    if (savedLocation != null && recentFocusedWindow != null) {
      popup.showInScreenCoordinates(recentFocusedWindow, new Point(savedLocation.getX(), savedLocation.getY()));
    }
    else {
      popup.showCenteredInCurrentWindow(project);
    }
  }

  private static void updateModel(
    @Nonnull ListWithFilter<RecentLocationItem> listWithFilter,
    @Nonnull RecentLocationsDataModel data,
    boolean changed
  ) {
    NameFilteringListModel<RecentLocationItem> model =
      (NameFilteringListModel<RecentLocationItem>)listWithFilter.getList().getModel();
    DefaultListModel<RecentLocationItem> originalModel =
      (DefaultListModel<RecentLocationItem>)model.getOriginalModel();

    originalModel.removeAllElements();
    data.getPlaces(changed).forEach(item -> originalModel.addElement(item));

    listWithFilter.getSpeedSearch().reset();
  }

  @Nonnull
  private static JPanel createMainPanel(@Nonnull ListWithFilter listWithFilter, @Nonnull CaptionPanel topPanel) {
    JPanel mainPanel = new JPanel(new BorderLayout());
    UIUtil.putClientProperty(mainPanel, CaptionPanel.KEY, topPanel);
    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(listWithFilter, BorderLayout.CENTER);
    return mainPanel;
  }

  @Nonnull
  private static CaptionPanel createHeaderPanel(@Nonnull JLabel title, @Nonnull JComponent checkbox) {
    CaptionPanel topPanel = new CaptionPanel();
    topPanel.add(title, BorderLayout.WEST);
    topPanel.add(checkbox, BorderLayout.EAST);

    Dimension size = topPanel.getPreferredSize();
    size.height = JBUIScale.scale(29);
    topPanel.setPreferredSize(size);
    topPanel.setBorder(JBUI.Borders.empty(5, 8));

    WindowMoveListener moveListener = new WindowMoveListener(topPanel);
    topPanel.addMouseListener(moveListener);
    topPanel.addMouseMotionListener(moveListener);

    return topPanel;
  }

  @Nonnull
  private static JLabel createTitle(boolean showChanged) {
    JBLabel title = new JBLabel();
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    updateTitleText(title, showChanged);
    return title;
  }

  private static void updateTitleText(@Nonnull JLabel title, boolean showChanged) {
    title.setText(
      showChanged
        ? IdeLocalize.recentLocationsChangedLocations().get()
        : IdeLocalize.recentLocationsPopupTitle().get()
    );
  }

  @Nonnull
  private static Function<RecentLocationItem, String> getNamer(
    @Nonnull RecentLocationsDataModel data,
    @Nonnull JBCheckBox checkBox
  ) {
    return value -> {
      EditorEx editor = value.getEditor();
      return value.getInfo().getFile().getName() + " " + editor.getDocument().getText();
    };
  }

  private static void initSearchActions(
    @Nonnull Project project,
    @Nonnull RecentLocationsDataModel data,
    @Nonnull ListWithFilter<RecentLocationItem> listWithFilter,
    @Nonnull JBList<RecentLocationItem> list,
    @Nonnull JBCheckBox checkBox,
    @Nonnull JBPopup popup,
    @Nonnull Ref<? super Boolean> navigationRef
  ) {
    listWithFilter.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        int clickCount = event.getClickCount();
        if (clickCount > 1 && clickCount % 2 == 0) {
          event.consume();
          navigateToSelected(project, list, popup, navigationRef);
        }
      }
    });

    DumbAwareAction.create(e -> navigateToSelected(project, list, popup, navigationRef))
                   .registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), listWithFilter, popup);

    DumbAwareAction.create(e -> removePlaces(project, listWithFilter, list, data, checkBox.isSelected()))
                   .registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"), listWithFilter, popup);
  }

  private static void removePlaces(
    @Nonnull Project project,
    @Nonnull ListWithFilter<RecentLocationItem> listWithFilter,
    @Nonnull JBList<RecentLocationItem> list,
    @Nonnull RecentLocationsDataModel data,
    boolean showChanged
  ) {
    List<RecentLocationItem> selectedValue = list.getSelectedValuesList();
    if (selectedValue.isEmpty()) {
      return;
    }

    int index = list.getSelectedIndex();

    IdeDocumentHistory ideDocumentHistory = IdeDocumentHistory.getInstance(project);
    for (RecentLocationItem item : selectedValue) {
      if (showChanged) {
        ContainerUtil
          .filter(ideDocumentHistory.getChangePlaces(), info -> IdeDocumentHistoryImpl.isSame(info, item.getInfo()))
          .forEach(info -> ideDocumentHistory.removeChangePlace(info));
      }
      else {
        ContainerUtil
          .filter(ideDocumentHistory.getBackPlaces(), info -> IdeDocumentHistoryImpl.isSame(info, item.getInfo()))
          .forEach(info -> ideDocumentHistory.removeBackPlace(info));
      }
    }

    updateModel(listWithFilter, data, showChanged);

    if (list.getModel().getSize() > 0) {
      ScrollingUtil.selectItem(list, index < list.getModel().getSize() ? index : index - 1);
    }
  }

  private static void navigateToSelected(
    @Nonnull Project project,
    @Nonnull JBList<RecentLocationItem> list,
    @Nonnull JBPopup popup,
    @Nonnull Ref<? super Boolean> navigationRef
  ) {
    ContainerUtil.reverse(list.getSelectedValuesList())
                 .forEach(item -> IdeDocumentHistory.getInstance(project).gotoPlaceInfo(item.getInfo()));

    navigationRef.set(true);
    popup.closeOk(null);
  }
}

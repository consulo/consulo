/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.editor.impl;

import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ActivityTracker;
import com.intellij.ide.DataManager;
import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.editor.impl.DesktopEditorMarkupModelImpl;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.AncestorListenerAdapter;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.labels.DropDownLink;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.popup.util.PopupState;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.Function;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import com.intellij.xml.util.XmlStringUtil;
import consulo.actionSystem.impl.ActionButtonUI;
import consulo.awt.TargetAWT;
import consulo.desktop.editor.DesktopEditorFloatPanel;
import consulo.disposer.Disposable;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.LabelUI;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 2020-06-19
 */
public class DesktopEditorAnalyzeStatusPanel implements Disposable {
  private static class StatusComponentLayout implements LayoutManager {
    private JComponent statusComponent;
    private final java.util.List<JComponent> actionButtons = new ArrayList<>();

    @Override
    public void addLayoutComponent(String s, Component component) {
      JComponent jc = (JComponent)component;
      if (ActionToolbar.CUSTOM_COMPONENT_CONSTRAINT.equals(s) && jc instanceof StatusButton) {
        statusComponent = jc;
      }
      else if (ActionToolbar.ACTION_BUTTON_CONSTRAINT.equals(s) && jc instanceof ActionButton) {
        actionButtons.add(jc);
      }
    }

    @Override
    public void removeLayoutComponent(Component component) {
      JComponent jc = (JComponent)component;
      if (jc instanceof StatusButton) {
        statusComponent = null;
      }
      else if (jc instanceof ActionButton) {
        actionButtons.remove(jc);
      }
    }

    @Override
    public Dimension preferredLayoutSize(Container container) {
      Dimension size = statusComponent != null && statusComponent.isVisible() ? statusComponent.getPreferredSize() : JBUI.emptySize();

      for (JComponent jc : actionButtons) {
        if (jc.isVisible()) {
          Dimension prefSize = jc.getPreferredSize();
          size.height = Math.max(size.height, prefSize.height);
        }
      }

      for (JComponent jc : actionButtons) {
        if (jc.isVisible()) {
          Dimension prefSize = jc.getPreferredSize();
          Insets i = jc.getInsets();
          JBInsets.removeFrom(prefSize, i);

          int maxBareHeight = size.height - i.top - i.bottom;
          size.width += Math.max(prefSize.width, maxBareHeight) + i.left + i.right;
        }
      }

      if (size.width > 0 && size.height > 0) {
        JBInsets.addTo(size, container.getInsets());
      }
      return size;
    }

    @Override
    public Dimension minimumLayoutSize(Container container) {
      return preferredLayoutSize(container);
    }

    @Override
    public void layoutContainer(Container container) {
      Dimension prefSize = preferredLayoutSize(container);

      if (prefSize.width > 0 && prefSize.height > 0) {
        Insets i = container.getInsets();
        JBInsets.removeFrom(prefSize, i);
        int offset = i.left;

        if (statusComponent != null && statusComponent.isVisible()) {
          Dimension size = statusComponent.getPreferredSize();
          statusComponent.setBounds(offset, i.top, size.width, prefSize.height);
          offset += size.width;
        }

        for (JComponent jc : actionButtons) {
          if (jc.isVisible()) {
            Dimension jcPrefSize = jc.getPreferredSize();
            Insets jcInsets = jc.getInsets();
            JBInsets.removeFrom(jcPrefSize, jcInsets);

            int maxBareHeight = prefSize.height - jcInsets.top - jcInsets.bottom;
            int width = Math.max(jcPrefSize.width, maxBareHeight) + jcInsets.left + jcInsets.right;

            jc.setBounds(offset, i.top, width, prefSize.height);
            offset += width;
          }
        }
      }
    }
  }

  private static class StatusButton extends JPanel {
    private static final int LEFT_RIGHT_INDENT = 5;
    private static final int INTER_GROUP_OFFSET = 6;

    private boolean mousePressed;
    private boolean mouseHover;
    private final MouseListener mouseListener;
    private final PropertyChangeListener presentationPropertyListener;
    private final Presentation presentation;
    private final EditorColorsScheme colorsScheme;
    private boolean translucent;

    private final ActionButton myActionEmulator;
    private ActionButtonUI myActionButtonUI;

    private StatusButton(@Nonnull AnAction action, @Nonnull Presentation presentation, @Nonnull String place, @Nonnull EditorColorsScheme colorsScheme, @Nonnull BooleanSupplier hasNavButtons) {
      setLayout(new GridBagLayout());
      setOpaque(false);

      myActionEmulator = new ActionButton(action, presentation, place, new Size(16, 16));

      this.presentation = presentation;
      this.colorsScheme = colorsScheme;

      presentationPropertyListener = l -> {
        String propName = l.getPropertyName();
        if (propName.equals(EXPANDED_STATUS.toString()) && l.getNewValue() != null) {
          //noinspection unchecked
          java.util.List<StatusItem> newStatus = (java.util.List<StatusItem>)l.getNewValue();
          updateContents(newStatus);
          translucent = false;
          revalidate();
          repaint();
        }
        else if (propName.equals(TRANSLUCENT_STATE.toString())) {
          translucent = l.getNewValue() == Boolean.TRUE;
          repaint();
        }
      };

      mouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent me) {
          DataContext context = getDataContext();
          AnActionEvent event = AnActionEvent.createFromInputEvent(me, place, presentation, context, false, true);
          if (!ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
            return;
          }

          if (presentation.isEnabled()) {
            ActionManagerEx manager = ActionManagerEx.getInstanceEx();
            manager.fireBeforeActionPerformed(action, context, event);

            action.actionPerformed(event);

            manager.queueActionPerformedEvent(action, context, event);
            //ActionsCollector.getInstance().record(event.getProject(), action, event, null);

            ActionToolbar toolbar = getActionToolbar();
            if (toolbar != null) {
              toolbar.updateActionsImmediately();
            }
          }
        }

        @Override
        public void mousePressed(MouseEvent me) {
          mousePressed = true;
          repaint();
        }

        @Override
        public void mouseReleased(MouseEvent me) {
          mousePressed = false;
          repaint();
        }

        @Override
        public void mouseEntered(MouseEvent me) {
          mouseHover = true;
          repaint();
        }

        @Override
        public void mouseExited(MouseEvent me) {
          mouseHover = false;
          repaint();
        }
      };

      java.util.List<StatusItem> newStatus = presentation.getClientProperty(EXPANDED_STATUS);
      if (newStatus != null) {
        updateContents(newStatus);
      }

      Border border = new Border() {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        }

        @Override
        public boolean isBorderOpaque() {
          return false;
        }

        @Override
        public Insets getBorderInsets(Component c) {
          return hasNavButtons.getAsBoolean() ? JBUI.insets(2, 2, 2, 0) : JBUI.insets(2);
        }
      };

      setBorder(border);

      myActionEmulator.setBorder(border);

      updateUI();
    }

    @Override
    public void updateUI() {
      super.updateUI();
      // first call from parent constructor
      if (myActionEmulator != null) {
        myActionButtonUI = (ActionButtonUI)UIManager.getUI(myActionEmulator);
      }
    }

    @Override
    public void paint(Graphics g) {
      int state = mousePressed ? ActionButtonComponent.PUSHED : mouseHover ? ActionButtonComponent.POPPED : ActionButtonComponent.NORMAL;
      myActionEmulator.setBounds(getBounds());
      myActionButtonUI.paintBackground(myActionEmulator, g, getBounds().getSize(), state);

      super.paint(g);
    }

    @Override
    public void addNotify() {
      super.addNotify();
      presentation.addPropertyChangeListener(presentationPropertyListener);
      addMouseListener(mouseListener);
    }

    @Override
    public void removeNotify() {
      presentation.removePropertyChangeListener(presentationPropertyListener);
      removeMouseListener(mouseListener);
    }

    private DataContext getDataContext() {
      ActionToolbar actionToolbar = getActionToolbar();
      return actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext(this);
    }

    private ActionToolbar getActionToolbar() {
      return ComponentUtil.getParentOfType((Class<? extends ActionToolbar>)ActionToolbar.class, this);
    }

    private void updateContents(@Nonnull java.util.List<StatusItem> status) {
      removeAll();

      setEnabled(!status.isEmpty());
      setVisible(!status.isEmpty());

      GridBag gc = new GridBag().nextLine();
      if (status.size() == 1 && StringUtil.isEmpty(status.get(0).getText())) {
        add(createStyledLabel(null, status.get(0).getIcon(), SwingConstants.CENTER), gc.next().weightx(1).fillCellHorizontally());
      }
      else if (status.size() > 0) {
        int leftRightOffset = JBUIScale.scale(LEFT_RIGHT_INDENT);
        add(Box.createHorizontalStrut(leftRightOffset), gc.next());

        int counter = 0;
        for (StatusItem item : status) {
          add(createStyledLabel(item.getText(), item.getIcon(), SwingConstants.LEFT), gc.next().insetLeft(counter++ > 0 ? INTER_GROUP_OFFSET : 0));
        }

        add(Box.createHorizontalStrut(leftRightOffset), gc.next());
      }
    }

    private JLabel createStyledLabel(@Nullable String text, @Nullable Image icon, int alignment) {
      JLabel label = new JLabel(text, TargetAWT.to(icon), alignment) {
        @Override
        protected void paintComponent(Graphics graphics) {
          Graphics2D g2 = (Graphics2D)graphics.create();
          try {
            float alpha = translucent ? 0.5f : 1.0f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintComponent(g2);
          }
          finally {
            g2.dispose();
          }
        }

        @Override
        public void setUI(LabelUI ui) {
          super.setUI(ui);

          if (!SystemInfo.isWindows) {
            Font font = getFont();
            font = new FontUIResource(font.deriveFont(font.getStyle(), font.getSize() - JBUIScale.scale(2))); // Allow to reset the font by UI
            setFont(font);
          }
        }
      };

      label.setForeground(new JBColor(() -> ObjectUtil.notNull(TargetAWT.to(colorsScheme.getColor(ICON_TEXT_COLOR)), TargetAWT.to(ICON_TEXT_COLOR.getDefaultColorValue()))));
      label.setIconTextGap(JBUIScale.scale(1));

      return label;
    }

    @Override
    public Dimension getPreferredSize() {
      if (getComponentCount() == 0) {
        return JBUI.emptySize();
      }

      Dimension size = super.getPreferredSize();
      Insets i = getInsets();
      size.height = Math.max(getStatusIconSize() + i.top + i.bottom, size.height);
      size.width = Math.max(getStatusIconSize() + i.left + i.right, size.width);
      return size;
    }
  }

  private class StatusAction extends DumbAwareAction implements CustomComponentAction {
    @Override
    @Nonnull
    public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
      return new StatusButton(this, presentation, place, myEditor.getColorsScheme(), () -> showNavigation);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myPopupManager.showPopup(e.getInputEvent());
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
      Presentation presentation = e.getPresentation();

      if (analyzerStatus != null) {
        List<StatusItem> newStatus = analyzerStatus.getExpandedStatus();
        Image newIcon = analyzerStatus.getIcon();

        if (!hasAnalyzed || analyzerStatus.getAnalyzingType() != AnalyzingType.EMPTY) {
          if (newStatus.isEmpty()) {
            newStatus = Collections.singletonList(new StatusItem("", newIcon));
            presentation.putClientProperty(EXPANDED_STATUS, newStatus);
          }

          if (!Objects.equals(presentation.getClientProperty(EXPANDED_STATUS), newStatus)) {
            presentation.putClientProperty(EXPANDED_STATUS, newStatus);
          }

          presentation.putClientProperty(TRANSLUCENT_STATE, analyzerStatus.getAnalyzingType() != AnalyzingType.COMPLETE);
        }
        else {
          presentation.putClientProperty(TRANSLUCENT_STATE, true);
        }
      }
      else {
        presentation.putClientProperty(EXPANDED_STATUS, Collections.emptyList());
      }
    }
  }

  private class InspectionPopupManager {
    private final JPanel myContent = new JPanel(new GridBagLayout());
    private final ComponentPopupBuilder myPopupBuilder;
    private final Map<String, JProgressBar> myProgressBarMap = new HashMap<>();
    private final AncestorListener myAncestorListener;
    private final JBPopupListener myPopupListener;
    private final PopupState myPopupState = new PopupState();

    private JBPopup myPopup;

    private InspectionPopupManager() {
      myContent.setOpaque(true);
      myContent.setBackground(UIUtil.getToolTipBackground());

      myPopupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(myContent, null).
              setCancelOnClickOutside(true).
              setCancelCallback(() -> analyzerStatus == null || analyzerStatus.getController().canClosePopup());

      myAncestorListener = new AncestorListenerAdapter() {
        @Override
        public void ancestorMoved(AncestorEvent event) {
          hidePopup();
        }
      };

      myPopupListener = new JBPopupListener() {
        @Override
        public void onClosed(@Nonnull LightweightWindowEvent event) {
          if (analyzerStatus != null) {
            analyzerStatus.getController().onClosePopup();
          }
          myEditor.getComponent().removeAncestorListener(myAncestorListener);
        }
      };
    }

    private void updateUI() {
      IJSwingUtilities.updateComponentTreeUI(myContent);
    }

    private void showPopup(@Nonnull InputEvent event) {
      showPopup(event, (size) -> {
        JComponent owner = (JComponent)event.getComponent();
        return new RelativePoint(owner, new Point(owner.getWidth() - owner.getInsets().right + JBUIScale.scale(DELTA_X) - size.width, owner.getHeight() + JBUIScale.scale(DELTA_Y)));
      });
    }

    private void showPopup(@Nonnull InputEvent event, @Nonnull Function<Dimension, RelativePoint> pointFunction) {
      hidePopup();
      if (myPopupState.isRecentlyHidden()) return; // do not show new popup

      updateContentPanel(analyzerStatus.getController());

      myPopup = myPopupBuilder.createPopup();
      myPopup.addListener(myPopupListener);
      myPopup.addListener(myPopupState);
      myEditor.getComponent().addAncestorListener(myAncestorListener);

      Dimension size = myContent.getPreferredSize();
      size.width = Math.max(size.width, JBUIScale.scale(296));

      myPopup.setSize(size);

      myPopup.show(pointFunction.apply(size));
    }

    private void hidePopup() {
      if (myPopup != null && !myPopup.isDisposed()) {
        myPopup.cancel();
      }
      myPopup = null;
    }

    private void updateContentPanel(@Nonnull UIController controller) {
      java.util.List<PassWrapper> passes = analyzerStatus.getPasses();
      Set<String> presentableNames = ContainerUtil.map2Set(passes, p -> p.getPresentableName());

      if (!presentableNames.isEmpty() && myProgressBarMap.keySet().equals(presentableNames)) {
        for (PassWrapper pass : passes) {
          myProgressBarMap.get(pass.getPresentableName()).setValue(pass.toPercent());
        }
        return;
      }
      myContent.removeAll();

      GridBag gc = new GridBag().nextLine().next().
              anchor(GridBagConstraints.LINE_START).
              weightx(1).
              fillCellHorizontally().
              insets(10, 10, 10, 0);

      boolean hasTitle = StringUtil.isNotEmpty(analyzerStatus.getTitle());

      if (hasTitle) {
        myContent.add(new JLabel(XmlStringUtil.wrapInHtml(analyzerStatus.getTitle())), gc);
      }
      else if (StringUtil.isNotEmpty(analyzerStatus.getDetails())) {
        myContent.add(new JLabel(XmlStringUtil.wrapInHtml(analyzerStatus.getDetails())), gc);
      }
      else if (analyzerStatus.getExpandedStatus().size() > 0 && analyzerStatus.getAnalyzingType() != AnalyzingType.EMPTY) {
        myContent.add(createDetailsPanel(), gc);
      }

      Presentation presentation = new Presentation();
      presentation.setIcon(AllIcons.Actions.More);
      presentation.putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, Boolean.TRUE);

      java.util.List<AnAction> actions = controller.getActions();
      if (!actions.isEmpty()) {
        ActionButton menuButton = new ActionButton(new MenuAction(actions), presentation, ActionPlaces.EDITOR_POPUP, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);

        myContent.add(menuButton, gc.next().anchor(GridBagConstraints.LINE_END).weightx(0).insets(10, 6, 10, 6));
      }

      myProgressBarMap.clear();
      JPanel myProgressPanel = new NonOpaquePanel(new GridBagLayout());
      GridBag progressGC = new GridBag();
      for (PassWrapper pass : passes) {
        myProgressPanel.add(new JLabel(pass.getPresentableName() + ": "), progressGC.nextLine().next().anchor(GridBagConstraints.LINE_START).weightx(0).insets(0, 10, 0, 6));

        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(pass.toPercent());
        myProgressPanel.add(pb, progressGC.next().anchor(GridBagConstraints.LINE_START).weightx(1).fillCellHorizontally().insets(0, 0, 0, 6));
        myProgressBarMap.put(pass.getPresentableName(), pb);
      }

      myContent.add(myProgressPanel, gc.nextLine().next().anchor(GridBagConstraints.LINE_START).fillCellHorizontally().coverLine().weightx(1));

      if (hasTitle) {
        int topIndent = !myProgressBarMap.isEmpty() ? 10 : 0;
        gc.nextLine().next().anchor(GridBagConstraints.LINE_START).fillCellHorizontally().coverLine().weightx(1).insets(topIndent, 10, 10, 6);

        if (StringUtil.isNotEmpty(analyzerStatus.getDetails())) {
          myContent.add(new JLabel(XmlStringUtil.wrapInHtml(analyzerStatus.getDetails())), gc);
        }
        else if (analyzerStatus.getExpandedStatus().size() > 0 && analyzerStatus.getAnalyzingType() != AnalyzingType.EMPTY) {
          myContent.add(createDetailsPanel(), gc);
        }
      }

      //if (Experiments.getInstance().isFeatureEnabled("problems.view.enabled")) {
      //  JLabel openProblemsViewLabel = new TrackableLinkLabel(EditorBundle.message("iw.open.problems.view"), () -> {
      //    hidePopup();
      //    controller.openProblemsView();
      //  });
      //  myContent.add(openProblemsViewLabel, gc.nextLine().next().anchor(GridBagConstraints.LINE_START).fillCellHorizontally().coverLine().weightx(1).insets(10, 10, 10, 0));
      //}

      myContent.add(createLowerPanel(controller), gc.nextLine().next().anchor(GridBagConstraints.LINE_START).fillCellHorizontally().coverLine().weightx(1));
    }

    private void updateVisiblePopup() {
      if (myPopup != null && myPopup.isVisible()) {
        updateContentPanel(analyzerStatus.getController());

        Dimension size = myContent.getPreferredSize();
        size.width = Math.max(size.width, JBUIScale.scale(296));
        myPopup.setSize(size);
      }
    }

    @Nonnull
    private JComponent createDetailsPanel() {
      StringBuilder text = new StringBuilder();
      for (int i = 0; i < analyzerStatus.getExpandedStatus().size(); i++) {
        boolean last = i == analyzerStatus.getExpandedStatus().size() - 1;
        StatusItem item = analyzerStatus.getExpandedStatus().get(i);

        text.append(item.getText()).append(" ").append(item.getType());
        if (!last) {
          text.append(", ");
        }
        else if (analyzerStatus.getAnalyzingType() != AnalyzingType.COMPLETE) {
          text.append(" ").append(EditorBundle.message("iw.found.so.far.suffix"));
        }
      }

      return new JLabel(text.toString());
    }

    private JPanel createLowerPanel(@Nonnull UIController controller) {
      JPanel panel = new JPanel(new GridBagLayout());
      GridBag gc = new GridBag().nextLine();

      if (PowerSaveMode.isEnabled()) {
        panel.add(new TrackableLinkLabel(EditorBundle.message("iw.disable.powersave"), () -> {
          PowerSaveMode.setEnabled(false);
          hidePopup();
        }), gc.next().anchor(GridBagConstraints.LINE_START));
      }
      else {
        java.util.List<LanguageHighlightLevel> levels = controller.getHighlightLevels();

        if (levels.size() == 1) {
          JLabel highlightLabel = new JLabel(EditorBundle.message("iw.highlight.label") + " ");
          highlightLabel.setForeground(JBUI.CurrentTheme.Link.linkColor());

          panel.add(highlightLabel, gc.next().anchor(GridBagConstraints.LINE_START));
          panel.add(createDropDownLink(levels.get(0), controller), gc.next());
        }
        else if (levels.size() > 1) {
          for (LanguageHighlightLevel level : levels) {
            JLabel highlightLabel = new JLabel(level.getLangID() + ": ");
            highlightLabel.setForeground(JBUI.CurrentTheme.Link.linkColor());

            panel.add(highlightLabel, gc.next().anchor(GridBagConstraints.LINE_START).gridx > 0 ? gc.insetLeft(8) : gc);
            panel.add(createDropDownLink(level, controller), gc.next());
          }
        }
      }
      Component component = Box.createHorizontalGlue();
      panel.add(component, gc.next().fillCellHorizontally().weightx(1.0));

      controller.fillHectorPanels(panel, gc);

      panel.setOpaque(true);
      panel.setBackground(UIUtil.getToolTipActionBackground());
      panel.setBorder(JBUI.Borders.empty(4, 10));
      return panel;
    }

    @Nonnull
    private DropDownLink<InspectionsLevel> createDropDownLink(@Nonnull LanguageHighlightLevel level, @Nonnull UIController controller) {
      return new DropDownLink<>(level.getLevel(), controller.getAvailableLevels(), inspectionsLevel -> {
        controller.setHighLightLevel(level.copy(level.getLangID(), inspectionsLevel));
        myContent.revalidate();

        Dimension size = myContent.getPreferredSize();
        size.width = Math.max(size.width, JBUIScale.scale(296));
        myPopup.setSize(size);

        // Update statistics
        //FeatureUsageData data = new FeatureUsageData().
        //        addProject(myEditor.getProject()).
        //        addLanguage(level.getLangID()).
        //        addData("level", inspectionsLevel.toString());
        //
        //FUCounterUsageLogger.getInstance().logEvent("inspection.widget", "highlight.level.changed", data);
      }, true);
    }
  }

  private class MenuAction extends DefaultActionGroup implements HintManagerImpl.ActionToIgnore {
    private MenuAction(@Nonnull List<? extends AnAction> actions) {
      setPopup(true);
      addAll(actions);
      add(new ToggleAction(EditorBundle.message("iw.compact.view")) {
        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
          return !showToolbar;
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
          showToolbar = !state;
          EditorSettingsExternalizable.getInstance().setShowInspectionWidget(showToolbar);
          updateTrafficLightVisibility();
          //ActionsCollector.getInstance().record(e.getProject(), this, e, null);
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
          super.update(e);
          e.getPresentation().setEnabled(analyzerStatus == null || analyzerStatus.getController().enableToolbar());
        }

        @Override
        public boolean isDumbAware() {
          return true;
        }
      });
    }
  }

  private static class TrackableLinkLabel extends LinkLabel<Object> {
    private InputEvent myEvent;

    private TrackableLinkLabel(@Nonnull String text, @Nonnull Runnable action) {
      super(text, (Image)null);
      //setListener((__, ___) -> {
      //  action.run();
      //  ActionsCollector.getInstance().record(null, myEvent, getClass());
      //}, null);
    }

    @Override
    public void doClick(InputEvent e) {
      myEvent = e;
      super.doClick(e);
    }
  }

  private static final Key<java.util.List<StatusItem>> EXPANDED_STATUS = Key.create("EXPANDED_STATUS");
  private static final Key<Boolean> TRANSLUCENT_STATE = Key.create("TRANSLUCENT_STATE");
  private static final int DELTA_X = 6;
  private static final int DELTA_Y = 6;

  private static final EditorColorKey HOVER_BACKGROUND = EditorColorKey.createColorKey("ActionButton.hoverBackground", TargetAWT.from(JBUI.CurrentTheme.ActionButton.hoverBackground()));

  private static final EditorColorKey PRESSED_BACKGROUND = EditorColorKey.createColorKey("ActionButton.pressedBackground", TargetAWT.from(JBUI.CurrentTheme.ActionButton.pressedBackground()));

  private static final EditorColorKey ICON_TEXT_COLOR = EditorColorKey.createColorKey("ActionButton.iconTextForeground", TargetAWT.from(UIUtil.getContextHelpForeground()));

  private static int getStatusIconSize() {
    return JBUIScale.scale(18);
  }

  private final DesktopEditorMarkupModelImpl myModel;

  private final DesktopEditorImpl myEditor;

  private final ActionToolbar statusToolbar;
  private boolean showToolbar;
  private boolean trafficLightVisible;
  private final ComponentListener toolbarComponentListener;
  private Rectangle cachedToolbarBounds = new Rectangle();
  private AnalyzerStatus analyzerStatus;
  private boolean hasAnalyzed;
  private boolean isAnalyzing;
  private boolean showNavigation;
  private InspectionPopupManager myPopupManager = new InspectionPopupManager();
  private final MergingUpdateQueue myStatusUpdates;

  private DesktopEditorErrorPanel myErrorPanel;

  public DesktopEditorAnalyzeStatusPanel(DesktopEditorMarkupModelImpl model) {
    myModel = model;
    myEditor = (DesktopEditorImpl)model.getEditor();
    myStatusUpdates = new MergingUpdateQueue(getClass().getName(), 50, true, MergingUpdateQueue.ANY_COMPONENT, this);

    AnAction nextErrorAction = createAction("GotoNextError", AllIcons.Actions.FindAndShowNextMatchesSmall);
    AnAction prevErrorAction = createAction("GotoPreviousError", AllIcons.Actions.FindAndShowPrevMatchesSmall);
    DefaultActionGroup navigateGroup = new DefaultActionGroup(nextErrorAction, prevErrorAction) {
      @RequiredUIAccess
      @Override
      public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(showNavigation);
      }
    };

    AnAction statusAction = new StatusAction();
    ActionGroup actions = new DefaultActionGroup(statusAction, navigateGroup);
    //ActionButtonLook editorButtonLook = new EditorToolbarButtonLook();
    statusToolbar = new ActionToolbarImpl(ActionPlaces.EDITOR_INSPECTIONS_TOOLBAR, actions, true) {
      //@Override
      //protected void paintComponent(Graphics g) {
      //  // todo editorButtonLook.paintBackground(g, this, myEditor.getBackgroundColor());
      //}

      //@Override
      //@Nonnull
      //protected Color getSeparatorColor() {
      //  Color separatorColor = myEditor.getColorsScheme().getColor(EditorColors.SEPARATOR_BELOW_COLOR);
      //  return separatorColor != null ? separatorColor : super.getSeparatorColor();
      //}


      @Nonnull
      @Override
      protected ActionButton createToolbarButton(@Nonnull AnAction action,
                                                 boolean minimalMode,
                                                 boolean decorateButtons,
                                                 @Nonnull String place,
                                                 @Nonnull Presentation presentation,
                                                 @Nonnull Size minimumSize) {
        ActionButton actionButton = new ActionButton(action, presentation, place, minimumSize) {

          @Override
          public void updateIcon() {
            super.updateIcon();
            revalidate();
            repaint();
          }

          @Override
          public Insets getInsets() {
            return myAction == nextErrorAction ? JBUI.insets(2, 1) : myAction == prevErrorAction ? JBUI.insets(2, 1, 2, 2) : JBUI.insets(2);
          }

          @Override
          public Dimension getPreferredSize() {
            Image icon = getIcon();
            Dimension size = new Dimension(icon.getWidth(), icon.getHeight());

            int minSize = getStatusIconSize();
            size.width = Math.max(size.width, minSize);
            size.height = Math.max(size.height, minSize);

            JBInsets.addTo(size, getInsets());
            return size;
          }
        };

        actionButton.setWithoutBorder(true);
        return actionButton;
      }

      @Override
      public void doLayout() {
        LayoutManager layoutManager = getLayout();
        if (layoutManager != null) {
          layoutManager.layoutContainer(this);
        }
        else {
          super.doLayout();
        }
      }

      //@Override
      //protected Dimension updatePreferredSize(Dimension preferredSize) {
      //  return preferredSize;
      //}
      //
      //@Override
      //protected Dimension updateMinimumSize(Dimension minimumSize) {
      //  return minimumSize;
      //}
    };

    MessageBusConnection connection = Application.get().getMessageBus().connect(this);
    connection.subscribe(AnActionListener.TOPIC, new AnActionListener() {
      @Override
      public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
        if (action instanceof HintManagerImpl.ActionToIgnore) {
          return;
        }
        myPopupManager.hidePopup();
      }
    });

    statusToolbar.setMiniMode(true);
    toolbarComponentListener = new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent event) {
        Component toolbar = event.getComponent();
        if (toolbar.getWidth() > 0 && toolbar.getHeight() > 0) {
          updateTrafficLightVisibility();
        }
      }
    };

  }
  
  public void updateUI() {
    JComponent toolbar = statusToolbar.getComponent();
    toolbar.setLayout(new StatusComponentLayout());
    toolbar.addComponentListener(toolbarComponentListener);

    DesktopEditorFloatPanel statusPanel = new DesktopEditorFloatPanel() {
      @Override
      public Color getBackground() {
        return TargetAWT.to(myEditor.getBackgroundColor());
      }
    };
    statusPanel.setVisible(!myEditor.isOneLineMode());
    statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
    statusPanel.add(statusToolbar.getComponent());

    statusToolbar.setTargetComponent(statusPanel);
    
    myEditor.setStatusComponent(statusPanel);
  }

  public void setErrorPanel(@Nullable DesktopEditorErrorPanel errorPanel) {
    myErrorPanel = errorPanel;
  }

  private AnAction createAction(@Nonnull String id, @Nonnull Image icon) {
    AnAction delegate = ActionManager.getInstance().getAction(id);
    AnAction result = new DumbAwareAction(delegate.getTemplatePresentation().getText(), null, icon) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        IdeFocusManager focusManager = IdeFocusManager.getInstance(myEditor.getProject());

        AnActionEvent delegateEvent = AnActionEvent.createFromAnAction(delegate, e.getInputEvent(), ActionPlaces.EDITOR_INSPECTIONS_TOOLBAR, myEditor.getDataContext());

        if (focusManager.getFocusOwner() != myEditor.getContentComponent()) {
          focusManager.requestFocus(myEditor.getContentComponent(), true).doWhenDone(() -> {
            delegate.actionPerformed(delegateEvent);
          });
        }
        else {
          delegate.actionPerformed(delegateEvent);
        }
      }
    };

    result.copyShortcutFrom(delegate);
    return result;
  }

  private void updateTrafficLightVisibility() {
    myStatusUpdates.queue(Update.create("visibility", this::doUpdateTrafficLightVisibility));
  }

  private void doUpdateTrafficLightVisibility() {
    if (trafficLightVisible) {
      if (showToolbar && myEditor.myView != null) {
        VisualPosition pos = myEditor.getCaretModel().getPrimaryCaret().getVisualPosition();
        Point point = myEditor.visualPositionToXY(pos);
        point = SwingUtilities.convertPoint(myEditor.getContentComponent(), point, myEditor.getScrollPane());

        JComponent stComponent = statusToolbar.getComponent();
        if (stComponent.isVisible()) {
          Rectangle bounds = SwingUtilities.convertRectangle(stComponent, stComponent.getBounds(), myEditor.getScrollPane());

          if (!bounds.isEmpty() && bounds.contains(point)) {
            cachedToolbarBounds = bounds;
            stComponent.setVisible(false);
            setSmallIconVisible(true);
          }
        }
        else if (!cachedToolbarBounds.contains(point)) {
          stComponent.setVisible(true);
          setSmallIconVisible(false);
        }
      }
      else {
        statusToolbar.getComponent().setVisible(false);
        setSmallIconVisible(true);
      }
    }
    else {
      statusToolbar.getComponent().setVisible(false);
      setSmallIconVisible(false);
    }
  }

  private void setSmallIconVisible(boolean visible) {
    if (myErrorPanel != null) {
      myErrorPanel.setSmallIconVisible(visible);
    }
  }

  public void repaintTrafficLightIcon() {
    ErrorStripeRenderer errorStripeRenderer = myModel.getErrorStripeRenderer();

    if (errorStripeRenderer == null) return;

    myStatusUpdates.queue(Update.create("icon", () -> {
      if (errorStripeRenderer != null) {
        AnalyzerStatus newStatus = errorStripeRenderer.getStatus(myEditor);
        if (!AnalyzerStatus.equals(newStatus, analyzerStatus)) {
          changeStatus(newStatus);
        }

        if(myErrorPanel != null) {
          myErrorPanel.repaint();
        }
      }
    }));
  }

  public void setTrafficLightIconVisible(boolean value) {
    if (value != trafficLightVisible) {
      trafficLightVisible = value;
      updateTrafficLightVisibility();
    }
  }

  private void changeStatus(AnalyzerStatus newStatus) {
    boolean resetAnalyzingStatus = analyzerStatus != null && analyzerStatus.isTextStatus() && analyzerStatus.getAnalyzingType() == AnalyzingType.COMPLETE;
    analyzerStatus = newStatus;
    //smallIconLabel.setIcon(analyzerStatus.getIcon());

    if (showToolbar != analyzerStatus.getController().enableToolbar()) {
      showToolbar = EditorSettingsExternalizable.getInstance().isShowInspectionWidget() && analyzerStatus.getController().enableToolbar();
      updateTrafficLightVisibility();
    }

    boolean analyzing = analyzerStatus.getAnalyzingType() != AnalyzingType.COMPLETE;
    hasAnalyzed = !resetAnalyzingStatus && (hasAnalyzed || (isAnalyzing && !analyzing));
    isAnalyzing = analyzing;

    if (analyzerStatus.getAnalyzingType() != AnalyzingType.EMPTY) {
      showNavigation = analyzerStatus.isShowNavigation();
    }

    myPopupManager.updateVisiblePopup();
    ActivityTracker.getInstance().inc();
  }

  public void showStatusPopup(InputEvent e, @Nonnull Function<Dimension, RelativePoint> function) {
    myPopupManager.showPopup(e, function);
  }

  @Override
  public void dispose() {
  }
}

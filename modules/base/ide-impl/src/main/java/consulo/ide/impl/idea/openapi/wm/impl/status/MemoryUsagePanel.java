// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.project.Project;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.JBSwingUtilities;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.update.Activatable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static consulo.ide.impl.idea.openapi.util.io.FileUtil.MEGABYTE;

public final class MemoryUsagePanel extends JButton implements CustomStatusBarWidget, UISettingsListener, Activatable {
  private static final int INDENT = 6;
  private static final Color USED_COLOR = JBColor.namedColor("MemoryIndicator.usedBackground", new JBColor(Gray._185, Gray._110));
  private static final Color UNUSED_COLOR = JBColor.namedColor("MemoryIndicator.allocatedBackground", new JBColor(Gray._215, Gray._90));

  private final String mySample;
  private final Project myProject;
  private final StatusBarWidgetFactory myFactory;
  private long myLastAllocated = -1;
  private long myLastUsed = -1;
  private BufferedImage myBufferedImage;
  private boolean myWasPressed;
  private ScheduledFuture<?> myFuture;

  public MemoryUsagePanel(Project project, StatusBarWidgetFactory factory) {
    myProject = project;
    myFactory = factory;
    long max = Math.min(Runtime.getRuntime().maxMemory() / MEGABYTE, 9999);
    mySample = UIBundle.message("memory.usage.panel.message.text", max, max);

    setOpaque(false);
    setFocusable(false);

    addActionListener(e -> {
      //noinspection CallToSystemGC
      System.gc();
      updateState();
    });

    setBorder(JBUI.Borders.empty(0, 2));
    updateUI();

    new UiNotifyConnector(this, this);
  }

  @Override
  public void showNotify() {
    myFuture = myProject.getUIAccess().getScheduler().scheduleWithFixedDelay(this::updateState, 1, 5, TimeUnit.SECONDS);
  }

  @Override
  public void hideNotify() {
    if (myFuture != null) {
      myFuture.cancel(true);
      myFuture = null;
    }
  }

  @Override
  public void dispose() {
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
  }

  @Nullable
  @Override
  public WidgetPresentation getPresentation() {
    return null;
  }

  @Nonnull
  @Override
  public String getId() {
    return myFactory.getId();
  }

  public void setShowing(boolean showing) {
    if (showing != isVisible()) {
      setVisible(showing);
      revalidate();
    }
  }

  @Override
  public void updateUI() {
    myBufferedImage = null;
    super.updateUI();
  }

  @Override
  public void uiSettingsChanged(UISettings uiSettings) {
    myBufferedImage = null;
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void paintComponent(Graphics g) {
    boolean pressed = getModel().isPressed();
    boolean stateChanged = myWasPressed != pressed;
    myWasPressed = pressed;

    if (myBufferedImage == null || stateChanged) {
      Dimension size = getSize();
      Insets insets = getInsets();

      int barWidth = size.width - INDENT;
      myBufferedImage = ImageUtil.createImage(g, barWidth, size.height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2 = JBSwingUtilities.runGlobalCGTransform(this, myBufferedImage.createGraphics());
      UISettingsUtil.setupAntialiasing(g2);

      g2.setFont(getFont());
      int textHeight = g2.getFontMetrics().getAscent();

      Runtime rt = Runtime.getRuntime();
      long maxMem = rt.maxMemory();
      long allocatedMem = rt.totalMemory();
      long unusedMem = rt.freeMemory();
      long usedMem = allocatedMem - unusedMem;

      int usedBarLength = (int)(barWidth * usedMem / maxMem);
      int unusedBarLength = (int)(size.height * unusedMem / maxMem);

      // background
      g2.setColor(UIUtil.getPanelBackground());
      g2.fillRect(0, 0, barWidth, size.height);

      // gauge (used)
      g2.setColor(USED_COLOR);
      g2.fillRect(0, 0, usedBarLength, size.height);

      // gauge (unused)
      g2.setColor(UNUSED_COLOR);
      g2.fillRect(usedBarLength, 0, unusedBarLength, size.height);

      // label
      g2.setColor(pressed ? UIUtil.getLabelDisabledForeground() : JBColor.foreground());
      String text = UIBundle.message("memory.usage.panel.message.text", usedMem / MEGABYTE, maxMem / MEGABYTE);
      int textX = insets.left;
      int textY = insets.top + (size.height - insets.top - insets.bottom - textHeight) / 2 + textHeight - JBUIScale.scale(1);
      g2.drawString(text, textX, textY);

      g2.dispose();
    }

    UIUtil.drawImage(g, myBufferedImage, INDENT, 0, null);
  }

  @Override
  public Dimension getPreferredSize() {
    FontMetrics metrics = getFontMetrics(getFont());
    Insets insets = getInsets();
    int width = metrics.stringWidth(mySample) + insets.left + insets.right + JBUIScale.scale(2) + INDENT;
    int height = metrics.getHeight() + insets.top + insets.bottom + JBUIScale.scale(2);
    return new Dimension(width, height);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  private void updateState() {
    if (!isShowing()) {
      return;
    }

    Runtime rt = Runtime.getRuntime();
    long maxMem = rt.maxMemory() / MEGABYTE;
    long allocatedMem = rt.totalMemory() / MEGABYTE;
    long usedMem = allocatedMem - rt.freeMemory() / MEGABYTE;

    if (allocatedMem != myLastAllocated || usedMem != myLastUsed) {
      myLastAllocated = allocatedMem;
      myLastUsed = usedMem;
      UIUtil.invokeLaterIfNeeded(() -> {
        myBufferedImage = null;
        repaint();
      });

      setToolTipText(UIBundle.message("memory.usage.panel.statistics.message", maxMem, allocatedMem, usedMem));
    }
  }
}
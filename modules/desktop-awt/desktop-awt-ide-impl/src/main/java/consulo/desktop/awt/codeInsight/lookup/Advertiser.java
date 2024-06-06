// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.codeInsight.lookup;

import consulo.application.ApplicationManager;
import consulo.language.editor.completion.lookup.LookupAdvertiser;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.collection.Lists;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter
 */
public class Advertiser implements LookupAdvertiser {
  private final List<Item> myTexts = Lists.newLockFreeCopyOnWriteList();
  private final JPanel myComponent = new JPanel(new AdvertiserLayout());

  private final AtomicInteger myCurrentItem = new AtomicInteger(0);
  private final JLabel myTextPanel = createLabel();
  private final JLabel myNextLabel;

  public Advertiser() {
    myNextLabel = new JLabel("Next Tip");
    myNextLabel.setFont(adFont());
    myNextLabel.setForeground(JBCurrentTheme.Link.linkColor());
    new ClickListener() {
      @Override
      public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
        myCurrentItem.incrementAndGet();
        updateAdvertisements();
        return true;
      }
    }.installOn(myNextLabel);

    myNextLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    myComponent.add(myTextPanel);
    myComponent.add(myNextLabel);
    myComponent.setOpaque(true);
    myComponent.setBackground(JBCurrentTheme.Advertiser.background());
    myComponent.setBorder(JBCurrentTheme.Advertiser.border());
  }

  private void updateAdvertisements() {
    myNextLabel.setVisible(myTexts.size() > 1);
    if (!myTexts.isEmpty()) {
      Item item = myTexts.get(myCurrentItem.get() % myTexts.size());
      item.setForLabel(myTextPanel);
    }
    else {
      myTextPanel.setText("");
      myTextPanel.setIcon(null);
      myTextPanel.setForeground(JBCurrentTheme.Advertiser.foreground());
    }
    myComponent.revalidate();
    myComponent.repaint();
  }

  private static JLabel createLabel() {
    JLabel label = new JLabel();
    label.setFont(adFont());
    label.setForeground(JBCurrentTheme.Advertiser.foreground());
    return label;
  }

  @Override
  public void showRandomText() {
    int count = myTexts.size();
    myCurrentItem.set(count > 0 ? new Random().nextInt(count) : 0);
    updateAdvertisements();
  }

  @Override
  public void clearAdvertisements() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myTexts.clear();
    myCurrentItem.set(0);
    updateAdvertisements();
  }

  private static Font adFont() {
    Font font = UIUtil.getLabelFont();
    return font.deriveFont((float)(font.getSize() - JBUIScale.scale(2)));
  }

  public void addAdvertisement(@Nonnull String text, @Nullable Image icon) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myTexts.add(new Item(text, icon));
    updateAdvertisements();
  }

  public void setBackground(@Nullable Color background) {
    myComponent.setBackground(background != null ? background : JBCurrentTheme.Advertiser.background());
  }

  public JComponent getAdComponent() {
    return myComponent;
  }

  public List<String> getAdvertisements() {
    return ContainerUtil.map(myTexts, item -> item.text);
  }

  // ------------------------------------------------------
  // Custom layout
  private class AdvertiserLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      Insets i = parent.getInsets();
      Dimension size = new Dimension();
      Dimension nextButtonSize = myNextLabel.getPreferredSize();

      FontMetrics fm = myTextPanel.getFontMetrics(myTextPanel.getFont());

      for (Item item : myTexts) {
        int width = SwingUtilities.computeStringWidth(fm, item.toString());

        if (item.icon != null) {
          width += myTextPanel.getIconTextGap() + item.icon.getWidth();
        }

        width += nextButtonSize.width + i.left + i.right;

        int height = Math.max(fm.getHeight(), item.icon != null ? item.icon.getHeight() : 0) + i.top + i.bottom;
        size.width = Math.max(size.width, width);
        size.height = Math.max(size.height, Math.max(height, nextButtonSize.height));
      }

      return size;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      Dimension minSize = myNextLabel.getPreferredSize();
      JBInsets.addTo(minSize, parent.getInsets());
      return minSize;
    }

    @Override
    public void layoutContainer(Container parent) {
      Insets i = parent.getInsets();
      Dimension size = parent.getSize();
      Dimension textPrefSize = myTextPanel.getPreferredSize();
      Dimension nextPrefSize = myNextLabel.getPreferredSize();

      int textWidth = (i.left + i.right + textPrefSize.width + nextPrefSize.width <= size.width) ? textPrefSize.width : size.width - nextPrefSize.width - i.left - i.right;

      myTextPanel.setBounds(i.left, (size.height - textPrefSize.height) / 2, textWidth, textPrefSize.height);
      myNextLabel.setBounds(i.left + textWidth, (size.height - nextPrefSize.height) / 2, nextPrefSize.width, nextPrefSize.height);
    }
  }

  private static class Item {
    private final String text;
    private final Image icon;

    private Item(@Nonnull String text, @Nullable Image icon) {
      this.text = text;
      this.icon = icon;
    }

    private void setForLabel(JLabel label) {
      label.setText(toString());
      label.setIcon(TargetAWT.to(icon));
      label.setForeground(icon != null ? UIManager.getColor("Label.foreground") : JBCurrentTheme.Advertiser.foreground());
    }

    @Override
    public String toString() {
      return text + "  ";
    }
  }
}

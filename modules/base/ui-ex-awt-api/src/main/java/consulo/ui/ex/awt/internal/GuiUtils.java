/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.awt.internal;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.awt.hacking.AWTAccessorHacking;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.CharFilter;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class GuiUtils {
  private static final Insets paddingFromDialogBoundaries = new Insets(7, 5, 7, 5);
  private static final Insets paddingInsideDialog = new Insets(5, 5, 5, 5);

  private static final CharFilter NOT_MNEMONIC_CHAR_FILTER = new CharFilter() {
    @Override
    public boolean accept(char ch) {
      return ch != '&' && ch != UIUtil.MNEMONIC;
    }
  };

  public static JPanel constructFieldWithBrowseButton(JComponent aComponent, ActionListener aActionListener) {
    return constructFieldWithBrowseButton(aComponent, aActionListener, 0);
  }

  public static JPanel constructFieldWithBrowseButton(TextFieldWithHistory aComponent, ActionListener aActionListener) {
    return constructFieldWithBrowseButton(aComponent, aActionListener, 0);
  }

  private static JPanel constructFieldWithBrowseButton(JComponent aComponent, ActionListener aActionListener, int delta) {
    JPanel result = new JPanel(new GridBagLayout());
    result.add(aComponent, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    FixedSizeButton browseButton = new FixedSizeButton(aComponent.getPreferredSize().height - delta);//ignore border in case of browse button
    TextFieldWithBrowseButton.MyDoClickAction.addTo(browseButton, aComponent);
    result.add(browseButton, new GridBagConstraints(1, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    browseButton.addActionListener(aActionListener);

    return result;
  }

  public static JPanel constructDirectoryBrowserField(final JTextField field, final String objectName) {
    return constructFieldWithBrowseButton(field, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Select " + objectName);
        VirtualFile file = IdeaFileChooser.chooseFile(descriptor, field, null, null);
        if (file != null) {
          field.setText(FileUtil.toSystemDependentName(file.getPath()));
          field.postActionEvent();
        }
      }
    });
  }

  public static JPanel constructFileURLBrowserField(final TextFieldWithHistory field, final String objectName) {
    return constructFieldWithBrowseButton(field, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select " + objectName);
        VirtualFile file = IdeaFileChooser.chooseFile(descriptor, field, null, null);
        if (file != null) {
          try {
            field.setText(VirtualFileUtil.virtualToIoFile(file).toURI().toURL().toString());
          }
          catch (MalformedURLException e1) {
            field.setText("");
          }
        }
      }
    });
  }

  public static JComponent constructLabeledComponent(String aLabelText, JComponent aComponent, @JdkConstants.BoxLayoutAxis int aAxis) {
    JPanel result = new JPanel();
    BoxLayout boxLayout = new BoxLayout(result, aAxis);
    result.setLayout(boxLayout);

    result.add(new JLabel(aLabelText));
    result.add(aComponent);

    return result;
  }

  public static JPanel makeDialogPanel(JPanel aPanel) {
    JPanel emptyBordered = makePaddedPanel(aPanel, paddingFromDialogBoundaries);
    return wrapWithBorder(emptyBordered, IdeBorderFactory.createRoundedBorder());
  }

  public static JPanel makeTitledPanel(JComponent aComponent, String aTitle) {
    JPanel result = makePaddedPanel(aComponent, false, true, false, true);
    return wrapWithBorder(result, IdeBorderFactory.createTitledBorder(aTitle, true));
  }


  private static JPanel wrapWithBorder(JComponent aPanel, Border aBorder) {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(aPanel, BorderLayout.CENTER);
    wrapper.setBorder(aBorder);
    return wrapper;
  }


  public static BorderLayout createBorderLayout() {
    return new BorderLayout(paddingInsideDialog.left, paddingInsideDialog.top);
  }

  public static GridLayout createGridLayout(int aRows, int aColumns) {
    return new GridLayout(aRows, aColumns, paddingInsideDialog.left, paddingInsideDialog.top);
  }

  public static Component createVerticalStrut() {
    return Box.createRigidArea(new Dimension(0, paddingInsideDialog.top));
  }

  public static Component createHorisontalStrut() {
    return Box.createRigidArea(new Dimension(paddingInsideDialog.left, 0));
  }

  private static JPanel makePaddedPanel(JComponent aComponent, Insets aInsets) {
    return wrapWithBorder(aComponent, BorderFactory.createEmptyBorder(aInsets.top, aInsets.left, aInsets.bottom, aInsets.right));
  }

  private static JPanel makePaddedPanel(JComponent aComponent, boolean aTop, boolean aLeft, boolean aBottom, boolean aRight) {
    return wrapWithBorder(aComponent, BorderFactory
            .createEmptyBorder(aTop ? paddingInsideDialog.top : 0, aLeft ? paddingInsideDialog.left : 0, aBottom ? paddingInsideDialog.bottom : 0, aRight ? paddingInsideDialog.right : 0));
  }

  public static String getTextWithoutMnemonicEscaping(String text) {
    return StringUtil.strip(text, NOT_MNEMONIC_CHAR_FILTER);
  }

  public static char getDisplayedMnemonic(String text) {
    int i = getDisplayedMnemonicIndex(text);
    return i == -1 ? (char)-1 : text.charAt(i + 1);
  }

  public static int getDisplayedMnemonicIndex(String text) {
    return text.indexOf("&");
  }

  public static void packParentDialog(Component component) {
    while (component != null) {
      if (component instanceof JDialog) {
        component.setVisible(true);
        break;
      }
      component = component.getParent();
    }
  }

  public static void replaceJSplitPaneWithIDEASplitter(JComponent root) {
    Container parent = root.getParent();
    if (root instanceof JSplitPane) {
      // we can painlessly replace only splitter which is the only child in container
      if (parent.getComponents().length != 1 && !(parent instanceof Splitter)) {
        return;
      }
      JSplitPane pane = (JSplitPane)root;
      Component component1 = pane.getTopComponent();
      Component component2 = pane.getBottomComponent();
      int orientation = pane.getOrientation();
      Splitter splitter = new JBSplitter(orientation == JSplitPane.VERTICAL_SPLIT);
      splitter.setFirstComponent((JComponent)component1);
      splitter.setSecondComponent((JComponent)component2);
      splitter.setShowDividerControls(pane.isOneTouchExpandable());
      splitter.setHonorComponentsMinimumSize(true);

      if (pane.getDividerLocation() > 0) {
// let the component chance to resize itself
        SwingUtilities.invokeLater(() -> {
          double proportion;
          if (pane.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
            proportion = pane.getDividerLocation() / (double)(parent.getHeight() - pane.getDividerSize());
          }
          else {
            proportion = pane.getDividerLocation() / (double)(parent.getWidth() - pane.getDividerSize());
          }
          if (proportion > 0 && proportion < 1) {
            splitter.setProportion((float)proportion);
          }
        });
      }

      if (parent instanceof Splitter) {
        Splitter psplitter = (Splitter)parent;
        if (psplitter.getFirstComponent() == root) {
          psplitter.setFirstComponent(splitter);
        }
        else {
          psplitter.setSecondComponent(splitter);
        }
      }
      else {
        parent.remove(0);
        parent.setLayout(new BorderLayout());
        parent.add(splitter, BorderLayout.CENTER);
      }
      replaceJSplitPaneWithIDEASplitter((JComponent)component1);
      replaceJSplitPaneWithIDEASplitter((JComponent)component2);
    }
    else {
      Component[] components = root.getComponents();
      for (Component component : components) {
        if (component instanceof JComponent) {
          replaceJSplitPaneWithIDEASplitter((JComponent)component);
        }
      }
    }
  }

  public static void iterateChildren(Component container, Consumer<Component> consumer, JComponent... excludeComponents) {
    if (excludeComponents != null && ArrayUtil.find(excludeComponents, container) != -1) return;
    consumer.accept(container);
    if (container instanceof Container) {
      Component[] components = ((Container)container).getComponents();
      for (Component child : components) {
        iterateChildren(child, consumer, excludeComponents);
      }
    }
  }

  public static void iterateChildren(Consumer<Component> consumer, Component... components) {
    for (Component component : components) {
      iterateChildren(component, consumer);
    }
  }

  public static void enableChildren(boolean enabled, Component... components) {
    for (Component component : components) {
      enableChildren(component, enabled);
    }
  }

  public static void showComponents(boolean visible, Component... components) {
    for (Component component : components) {
      component.setVisible(visible);
    }
  }

  public static void enableChildren(Component container, boolean enabled, JComponent... excludeComponents) {
    iterateChildren(container, t -> enableComponent(t, enabled), excludeComponents);
  }

  private static void enableComponent(Component component, boolean enabled) {
    if (component.isEnabled() == enabled) return;
    component.setEnabled(enabled);
    if (component instanceof JPanel) {
      Border border = ((JPanel)component).getBorder();
      if (border instanceof TitledBorder) {
        Color color = enabled ? component.getForeground() : UIUtil.getInactiveTextColor();
        ((TitledBorder)border).setTitleColor(color);
      }
    }
    else if (component instanceof JLabel) {
      Color color = UIUtil.getInactiveTextColor();
      if (color == null) color = component.getForeground();
      @NonNls String changeColorString = "<font color=#" + colorToHex(color) + ">";
      JLabel label = (JLabel)component;
      @NonNls String text = label.getText();
      if (text != null && text.startsWith("<html>")) {
        if (StringUtil.startsWithConcatenation(text, "<html>", changeColorString) && enabled) {
          text = "<html>" + text.substring(("<html>" + changeColorString).length());
        }
        else if (!StringUtil.startsWithConcatenation(text, "<html>", changeColorString) && !enabled) {
          text = "<html>" + changeColorString + text.substring("<html>".length());
        }
        label.setText(text);
      }
    }
    else if (component instanceof JTable) {
      TableColumnModel columnModel = ((JTable)component).getColumnModel();
      for (int i = 0; i < columnModel.getColumnCount(); i++) {
        TableCellRenderer cellRenderer = columnModel.getColumn(0).getCellRenderer();
        if (cellRenderer instanceof Component) {
          enableComponent((Component)cellRenderer, enabled);
        }
      }
    }
  }

  @Deprecated
  @DeprecationInfo("Use ColorValueUtil#toHex or ColorUtil#toHex")
  public static String colorToHex(Color color) {
    return to2DigitsHex(color.getRed()) + to2DigitsHex(color.getGreen()) + to2DigitsHex(color.getBlue());
  }

  private static String to2DigitsHex(int i) {
    String s = Integer.toHexString(i);
    if (s.length() < 2) s = "0" + s;
    return s;
  }

  public static void runOrInvokeAndWait(@Nonnull @RequiredUIAccess Runnable runnable) throws InvocationTargetException, InterruptedException {
    ApplicationManager.getApplication().invokeAndWait(runnable);
  }

  public static void invokeLaterIfNeeded(@Nonnull @RequiredUIAccess Runnable runnable, @Nonnull consulo.ui.ModalityState modalityState) {
    Application application = Application.get();
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.invokeLater(runnable, modalityState);
    }
  }

  public static void invokeLaterIfNeeded(@Nonnull @RequiredUIAccess Runnable runnable, @Nonnull consulo.ui.ModalityState modalityState, @Nonnull BooleanSupplier expired) {
    Application application = Application.get();
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.invokeLater(runnable, modalityState, expired);
    }
  }

  public static JTextField createUndoableTextField() {
    return new JBTextField();
  }

  /**
   * Returns dimension with width required to type certain number of chars in provided component
   *
   * @param charCount number of chars
   * @param comp      component
   * @return dimension with width enough to insert provided number of chars into component
   */
  @Nonnull
  public static Dimension getSizeByChars(int charCount, @Nonnull JComponent comp) {
    Dimension size = comp.getPreferredSize();
    FontMetrics fontMetrics = comp.getFontMetrics(comp.getFont());
    size.width = fontMetrics.charWidth('a') * charCount;
    return size;
  }

  /**
   * Targets the component to a (screen) device before showing.
   * In case the component is already a part of UI hierarchy (and is thus bound to a device)
   * the method does nothing.
   * <p>
   * The prior targeting to a device is required when there's a need to calculate preferred
   * size of a compound component (such as JEditorPane, for instance) which is not yet added
   * to a hierarchy. The calculation in that case may involve device-dependent metrics
   * (such as font metrics) and thus should refer to a particular device in multi-monitor env.
   * <p>
   * Note that if after calling this method the component is added to another hierarchy,
   * bound to a different device, AWT will throw IllegalArgumentException. To avoid that,
   * the device should be reset by calling {@code targetToDevice(comp, null)}.
   *
   * @param target the component representing the UI hierarchy and the target device
   * @param comp   the component to target
   */
  public static void targetToDevice(@Nonnull Component comp, @Nullable Component target) {
    if (comp.isShowing()) return;
    GraphicsConfiguration gc = target != null ? target.getGraphicsConfiguration() : null;
    setGraphicsConfiguration(comp, gc);
  }

  public static void setGraphicsConfiguration(@Nonnull Component comp, @Nullable GraphicsConfiguration gc) {
    AWTAccessorHacking.setGraphicsConfiguration(comp, gc);
  }

  /**
   * removes all children and parent references, listeners from {@code container} to avoid possible memory leaks
   */
  public static void removePotentiallyLeakingReferences(@Nonnull Container container) {
    assert SwingUtilities.isEventDispatchThread();
    AWTAccessorHacking.setParent(container, null);
    container.removeAll();
    for (ComponentListener c : container.getComponentListeners()) container.removeComponentListener(c);
    for (FocusListener c : container.getFocusListeners()) container.removeFocusListener(c);
    for (HierarchyListener c : container.getHierarchyListeners()) container.removeHierarchyListener(c);
    for (HierarchyBoundsListener c : container.getHierarchyBoundsListeners()) container.removeHierarchyBoundsListener(c);
    for (KeyListener c : container.getKeyListeners()) container.removeKeyListener(c);
    for (MouseListener c : container.getMouseListeners()) container.removeMouseListener(c);
    for (MouseMotionListener c : container.getMouseMotionListeners()) container.removeMouseMotionListener(c);
    for (MouseWheelListener c : container.getMouseWheelListeners()) container.removeMouseWheelListener(c);
    for (InputMethodListener c : container.getInputMethodListeners()) container.removeInputMethodListener(c);
    for (PropertyChangeListener c : container.getPropertyChangeListeners()) container.removePropertyChangeListener(c);
    for (ContainerListener c : container.getContainerListeners()) container.removeContainerListener(c);
  }
}

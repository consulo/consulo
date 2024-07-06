/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.internal.diff.util;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.ScrollingModel;
import consulo.diff.DiffContext;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.EmptyContent;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.UIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.DialogWrapperDialog;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 04-Jul-24
 */
public class AWTDiffUtil {
  @Nonnull
  public static List<JComponent> createSyncHeightComponents(@Nonnull final List<JComponent> components) {
    if (!ContainerUtil.exists(components, Condition.NOT_NULL)) return components;
    List<JComponent> result = new ArrayList<>();
    for (int i = 0; i < components.size(); i++) {
      result.add(new SyncHeightComponent(components, i));
    }
    return result;
  }

  @Nonnull
  public static JComponent createStackedComponents(@Nonnull List<JComponent> components, int gap) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    for (int i = 0; i < components.size(); i++) {
      if (i != 0) panel.add(Box.createVerticalStrut(JBUI.scale(gap)));
      panel.add(components.get(i));
    }

    return panel;
  }

  @Nonnull
  static JComponent createSeparatorPanel(@Nonnull consulo.platform.LineSeparator separator) {
    JLabel label = new JLabel(separator.name());
    Color color;
    if (separator == consulo.platform.LineSeparator.CRLF) {
      color = JBColor.RED;
    }
    else if (separator == consulo.platform.LineSeparator.LF) {
      color = JBColor.BLUE;
    }
    else if (separator == consulo.platform.LineSeparator.CR) {
      color = JBColor.MAGENTA;
    }
    else {
      color = JBColor.BLACK;
    }
    label.setForeground(color);
    return label;
  }

  public static boolean isFocusedComponent(@Nullable Component component) {
    return isFocusedComponent(null, component);
  }

  public static boolean isFocusedComponent(@Nullable Project project, @Nullable Component component) {
    if (component == null) return false;
    return ProjectIdeFocusManager.getInstance(project).getFocusedDescendantFor(component) != null;
  }

  public static void requestFocus(@Nullable Project project, @Nullable Component component) {
    if (component == null) return;
    ProjectIdeFocusManager.getInstance(project).requestFocus(component, true);
  }

  @Nonnull
  public static List<JComponent> createSimpleTitles(@Nonnull ContentDiffRequest request) {
    List<DiffContent> contents = request.getContents();
    List<String> titles = request.getContentTitles();

    if (!ContainerUtil.exists(titles, Condition.NOT_NULL)) {
      return Collections.nCopies(titles.size(), null);
    }

    List<JComponent> components = new ArrayList<>(titles.size());
    for (int i = 0; i < contents.size(); i++) {
      JComponent title = createTitle(StringUtil.notNullize(titles.get(i)));
      title = createTitleWithNotifications(title, contents.get(i));
      components.add(title);
    }

    return components;
  }

  @Nonnull
  public static List<JComponent> createTextTitles(@Nonnull ContentDiffRequest request, @Nonnull List<? extends Editor> editors) {
    List<DiffContent> contents = request.getContents();
    List<String> titles = request.getContentTitles();

    boolean equalCharsets = TextDiffViewerUtil.areEqualCharsets(contents);
    boolean equalSeparators = TextDiffViewerUtil.areEqualLineSeparators(contents);

    List<JComponent> result = new ArrayList<>(contents.size());

    if (equalCharsets && equalSeparators && !ContainerUtil.exists(titles, Condition.NOT_NULL)) {
      return Collections.nCopies(titles.size(), null);
    }

    for (int i = 0; i < contents.size(); i++) {
      JComponent title = createTitle(StringUtil.notNullize(titles.get(i)), contents.get(i), equalCharsets, equalSeparators, editors.get(i));
      title = createTitleWithNotifications(title, contents.get(i));
      result.add(title);
    }

    return result;
  }

  @Nullable
  private static JComponent createTitleWithNotifications(@Nullable JComponent title,
                                                         @Nonnull DiffContent content) {
    List<JComponent> notifications = getCustomNotifications(content);
    if (notifications.isEmpty()) return title;

    List<JComponent> components = new ArrayList<>();
    if (title != null) components.add(title);
    components.addAll(notifications);
    return createStackedComponents(components, DiffImplUtil.TITLE_GAP.get());
  }

  @Nullable
  private static JComponent createTitle(@Nonnull String title,
                                        @Nonnull DiffContent content,
                                        boolean equalCharsets,
                                        boolean equalSeparators,
                                        @Nullable Editor editor) {
    if (content instanceof EmptyContent) return null;

    Charset charset = equalCharsets ? null : ((DocumentContent)content).getCharset();
    consulo.platform.LineSeparator separator = equalSeparators ? null : ((DocumentContent)content).getLineSeparator();
    boolean isReadOnly = editor == null || editor.isViewer() || !DiffImplUtil.canMakeWritable(editor.getDocument());

    return createTitle(title, charset, separator, isReadOnly);
  }

  @Nonnull
  public static JComponent createTitle(@Nonnull String title) {
    return createTitle(title, null, null, false);
  }

  @Nonnull
  public static JComponent createTitle(@Nonnull String title,
                                       @Nullable Charset charset,
                                       @Nullable consulo.platform.LineSeparator separator,
                                       boolean readOnly) {
    if (readOnly) title += " " + DiffLocalize.diffContentReadOnlyContentTitleSuffix().get();

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(JBUI.Borders.empty(2, 4));
    panel.add(new JBLabel(title).setCopyable(true), BorderLayout.CENTER);
    if (charset != null && separator != null) {
      JPanel panel2 = new JPanel();
      panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
      panel2.add(createCharsetPanel(charset));
      panel2.add(Box.createRigidArea(JBUI.size(4, 0)));
      panel2.add(createSeparatorPanel(separator));
      panel.add(panel2, BorderLayout.EAST);
    }
    else if (charset != null) {
      panel.add(createCharsetPanel(charset), BorderLayout.EAST);
    }
    else if (separator != null) {
      panel.add(createSeparatorPanel(separator), BorderLayout.EAST);
    }
    return panel;
  }

  @Nonnull
  private static JComponent createCharsetPanel(@Nonnull Charset charset) {
    JLabel label = new JLabel(charset.displayName());
    // TODO: specific colors for other charsets
    if (charset.equals(Charset.forName("UTF-8"))) {
      label.setForeground(JBColor.BLUE);
    }
    else if (charset.equals(Charset.forName("ISO-8859-1"))) {
      label.setForeground(JBColor.RED);
    }
    else {
      label.setForeground(JBColor.BLACK);
    }
    return label;
  }

  public static void showSuccessPopup(@Nonnull String message,
                                      @Nonnull RelativePoint point,
                                      @Nonnull Disposable disposable,
                                      @Nullable Runnable hyperlinkHandler) {
    HyperlinkListener listener = null;
    if (hyperlinkHandler != null) {
      listener = new HyperlinkAdapter() {
        @Override
        protected void hyperlinkActivated(HyperlinkEvent e) {
          hyperlinkHandler.run();
        }
      };
    }

    Color bgColor = MessageType.INFO.getPopupBackground();

    Balloon balloon = JBPopupFactory.getInstance()
                                    .createHtmlTextBalloonBuilder(message, null, bgColor, listener)
                                    .setAnimationCycle(200)
                                    .createBalloon();
    balloon.show(point, Balloon.Position.below);
    Disposer.register(disposable, balloon);
  }

  @Nonnull
  public static JPanel createMessagePanel(@Nonnull String message) {
    String text = StringUtil.replace(message, "\n", "<br>");
    JLabel label = new JBLabel(text) {
      @Override
      public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.width = Math.min(size.width, 200);
        size.height = Math.min(size.height, 100);
        return size;
      }
    }.setCopyable(true);
    label.setForeground(UIUtil.getInactiveTextColor());

    return new CenteredPanel(label, JBUI.Borders.empty(5));
  }

  @Nonnull
  public static Dimension getDefaultDiffPanelSize() {
    return new Dimension(400, 200);
  }

  @Nonnull
  public static Dimension getDefaultDiffWindowSize() {
    Rectangle screenBounds = ScreenUtil.getMainScreenBounds();
    int width = (int)(screenBounds.width * 0.8);
    int height = (int)(screenBounds.height * 0.8);
    return new Dimension(width, height);
  }

  public static void closeWindow(@Nullable Window window, boolean modalOnly, boolean recursive) {
    if (window == null) return;

    Component component = window;
    while (component != null) {
      if (component instanceof Window) closeWindow((Window)component, modalOnly);

      component = recursive ? component.getParent() : null;
    }
  }

  public static void closeWindow(@Nonnull Window window, boolean modalOnly) {
    consulo.ui.Window uiWindow = TargetAWT.from(window);
    if(uiWindow != null) {
      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if(ideFrame != null) {
        return;
      }
    }

    if (modalOnly && window instanceof Frame) return;

    if (window instanceof DialogWrapperDialog) {
      ((DialogWrapperDialog)window).getDialogWrapper().doCancelAction();
      return;
    }

    window.setVisible(false);
    window.dispose();
  }

  @Nonnull
  public static WindowWrapper.Mode getWindowMode(@Nonnull DiffDialogHints hints) {
    WindowWrapper.Mode mode = hints.getMode();
    if (mode == null) {
      boolean isUnderDialog = UIAccess.current().isInModalContext();
      mode = isUnderDialog ? WindowWrapper.Mode.MODAL : WindowWrapper.Mode.FRAME;
    }
    return mode;
  }

  public static void addNotification(@Nullable JComponent component, @Nonnull UserDataHolder holder) {
    if (component == null) return;
    List<JComponent> oldComponents = Lists.notNullize(holder.getUserData(DiffUserDataKeys.NOTIFICATIONS));
    holder.putUserData(DiffUserDataKeys.NOTIFICATIONS, Lists.append(oldComponents, component));
  }

  @Nonnull
  public static List<JComponent> getCustomNotifications(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    List<JComponent> requestComponents = request.getUserData(DiffUserDataKeys.NOTIFICATIONS);
    List<JComponent> contextComponents = context.getUserData(DiffUserDataKeys.NOTIFICATIONS);
    return ContainerUtil.concat(Lists.notNullize(contextComponents), Lists.notNullize(requestComponents));
  }

  @Nonnull
  public static List<JComponent> getCustomNotifications(@Nonnull DiffContent content) {
    return Lists.notNullize(content.getUserData(DiffUserDataKeys.NOTIFICATIONS));
  }

  public static void scrollToPoint(@Nullable Editor editor, @Nonnull Point point, boolean animated) {
    if (editor == null) return;
    if (!animated) editor.getScrollingModel().disableAnimation();
    editor.getScrollingModel().scrollHorizontally(point.x);
    editor.getScrollingModel().scrollVertically(point.y);
    if (!animated) editor.getScrollingModel().enableAnimation();
  }

  @Nonnull
  public static Point getScrollingPosition(@Nullable Editor editor) {
    if (editor == null) return new Point(0, 0);
    ScrollingModel model = editor.getScrollingModel();
    return new Point(model.getHorizontalScrollOffset(), model.getVerticalScrollOffset());
  }

  public static void disableBlitting(@Nonnull EditorEx editor) {
    if (Registry.is("diff.divider.repainting.disable.blitting")) {
      editor.getScrollPane().getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    }
  }

  public static void registerAction(@Nonnull AnAction action, @Nonnull JComponent component) {
    action.registerCustomShortcutSet(action.getShortcutSet(), component);
  }

  private static class SyncHeightComponent extends JPanel {
    @Nonnull
    private final List<JComponent> myComponents;

    public SyncHeightComponent(@Nonnull List<JComponent> components, int index) {
      super(new BorderLayout());
      myComponents = components;
      JComponent delegate = components.get(index);
      if (delegate != null) add(delegate, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      size.height = getPreferredHeight();
      return size;
    }

    private int getPreferredHeight() {
      int height = 0;
      for (JComponent component : myComponents) {
        if (component == null) continue;
        height = Math.max(height, component.getPreferredSize().height);
      }
      return height;
    }
  }

  public static class CenteredPanel extends JPanel {
    private final JComponent myComponent;

    public CenteredPanel(@Nonnull JComponent component) {
      myComponent = component;
      add(component);
    }

    public CenteredPanel(@Nonnull JComponent component, @Nonnull Border border) {
      this(component);
      setBorder(border);
    }

    @Override
    public void doLayout() {
      final Dimension size = getSize();
      final Dimension preferredSize = myComponent.getPreferredSize();

      Insets insets = getInsets();
      JBInsets.removeFrom(size, insets);

      int width = Math.min(size.width, preferredSize.width);
      int height = Math.min(size.height, preferredSize.height);
      int x = Math.max(0, (size.width - preferredSize.width) / 2);
      int y = Math.max(0, (size.height - preferredSize.height) / 2);

      myComponent.setBounds(insets.left + x, insets.top + y, width, height);
    }

    @Override
    public Dimension getPreferredSize() {
      return addInsets(myComponent.getPreferredSize());
    }

    @Override
    public Dimension getMinimumSize() {
      return addInsets(myComponent.getMinimumSize());
    }

    @Override
    public Dimension getMaximumSize() {
      return addInsets(myComponent.getMaximumSize());
    }

    private Dimension addInsets(Dimension dimension) {
      JBInsets.addTo(dimension, getInsets());
      return dimension;
    }
  }
}

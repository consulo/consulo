// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.codeEditor.Editor;
import consulo.component.util.ComparableObject;
import consulo.webBrowser.BrowserUtil;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ui.ex.awt.hint.TooltipEvent;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.ComponentWithMnemonics;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.util.ListenerUtil;
import consulo.ide.impl.idea.ui.WidthBasedLayout;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.impl.internal.hint.TooltipGroup;
import consulo.language.editor.impl.internal.hint.TooltipRenderer;
import consulo.language.editor.inspection.TooltipLinkHandlers;
import consulo.logging.Logger;
import consulo.ui.ex.Html;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextDelegate;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
public class LineTooltipRenderer extends ComparableObject.Impl implements TooltipRenderer {

  /**
   * Html-like text for showing
   * Please note that the tooltip size is calculated dynamically based on the html so
   * if the html content doesn't allow soft line breaks the tooltip can be too big for showing
   * e.g.
   * <br>
   * very nbsp; long nbsp; text nbsp; with nbsp; 'nbsp;' as spaces cannot be break
   */
  @NonNls
  @Nullable
  protected String myText;

  //is used for suppressing some events while processing links
  private volatile boolean myActiveLink;
  //mostly is used as a marker that we are in popup with description
  protected final int myCurrentWidth;

  @FunctionalInterface
  public interface TooltipReloader {
    void reload(boolean toExpand);
  }

  public LineTooltipRenderer(@Nullable String text, @Nonnull Object[] comparable) {
    this(text, 0, comparable);
  }

  public LineTooltipRenderer(@Nullable final String text, final int width, @Nonnull Object[] comparable) {
    super(comparable);
    myCurrentWidth = width;
    myText = text;
  }

  @Nonnull
  private static JPanel createMainPanel(@Nonnull final HintHint hintHint,
                                        @Nonnull JComponent pane,
                                        @Nonnull JEditorPane editorPane,
                                        boolean newLayout,
                                        boolean highlightActions,
                                        boolean hasSeparators) {
    int leftBorder = newLayout ? 10 : 8;
    int rightBorder = 12;
    class MyPanel extends JPanel implements WidthBasedLayout {
      private MyPanel() {
        super(new GridBagLayout());
      }

      @Override
      public int getPreferredWidth() {
        return getPreferredSize().width;
      }

      @Override
      public int getPreferredHeight(int width) {
        Dimension size = editorPane.getSize();
        int sideComponentsWidth = getSideComponentWidth();
        editorPane.setSize(width - leftBorder - rightBorder - sideComponentsWidth, Math.max(1, size.height));
        int height;
        try {
          height = getPreferredSize().height;
        }
        finally {
          editorPane.setSize(size);
        }
        return height;
      }

      @Override
      public AccessibleContext getAccessibleContext() {
        return new AccessibleContextDelegate(editorPane.getAccessibleContext()) {
          @Override
          protected Container getDelegateParent() {
            return getParent();
          }
        };
      }

      private int getSideComponentWidth() {
        GridBagLayout layout = (GridBagLayout)getLayout();
        Component sideComponent = null;
        GridBagConstraints sideComponentConstraints = null;
        boolean unsupportedLayout = false;
        for (Component component : getComponents()) {
          GridBagConstraints c = layout.getConstraints(component);
          if (c.gridx > 0) {
            if (sideComponent == null && c.gridy == 0) {
              sideComponent = component;
              sideComponentConstraints = c;
            }
            else {
              unsupportedLayout = true;
            }
          }
        }
        if (unsupportedLayout) {
          Logger.getInstance(LineTooltipRenderer.class).error("Unsupported tooltip layout");
        }
        if (sideComponent == null) {
          return 0;
        }
        else {
          Insets insets = sideComponentConstraints.insets;
          return sideComponent.getPreferredSize().width + (insets == null ? 0 : insets.left + insets.right);
        }
      }
    }
    JPanel grid = new MyPanel();
    GridBag bag = new GridBag().anchor(GridBagConstraints.CENTER)
            //weight is required for correct working scrollpane inside gridbaglayout
            .weightx(1.0).weighty(1.0).fillCell();

    pane.setBorder(JBUI.Borders.empty(newLayout ? 10 : 6, leftBorder, newLayout ? (highlightActions ? 10 : (hasSeparators ? 8 : 3)) : 6, rightBorder));
    grid.add(pane, bag);
    grid.setBackground(hintHint.getTextBackground());
    grid.setBorder(JBUI.Borders.empty());
    grid.setOpaque(hintHint.isOpaqueAllowed());

    return grid;
  }

  @Override
  public LightweightHintImpl show(@Nonnull final Editor editor, @Nonnull final Point p, final boolean alignToRight, @Nonnull final TooltipGroup group, @Nonnull final HintHint hintHint) {
    LightweightHintImpl hint = createHint(editor, p, alignToRight, group, hintHint, true, true, true, null);
    if (hint != null) {
      HintManagerImpl.getInstanceImpl()
              .showEditorHint(hint, editor, p, HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_OTHER_HINT | HintManager.HIDE_BY_SCROLLING, 0, false, hintHint);
    }
    return hint;
  }

  public LightweightHintImpl createHint(@Nonnull final Editor editor,
                                        @Nonnull final Point p,
                                        final boolean alignToRight,
                                        @Nonnull final TooltipGroup group,
                                        @Nonnull final HintHint hintHint,
                                        boolean newLayout,
                                        boolean highlightActions,
                                        boolean limitWidthToScreen,
                                        @Nullable TooltipReloader tooltipReloader) {
    if (myText == null) return null;

    //setup text
    String tooltipPreText = myText.replaceAll(String.valueOf(UIUtil.MNEMONIC), "");
    String dressedText = dressDescription(editor, tooltipPreText, myCurrentWidth > 0);

    final boolean expanded = myCurrentWidth > 0 && !dressedText.equals(tooltipPreText);

    final JComponent contentComponent = editor.getContentComponent();

    final JComponent editorComponent = editor.getComponent();
    if (!editorComponent.isShowing()) return null;
    final JLayeredPane layeredPane = editorComponent.getRootPane().getLayeredPane();

    String textToDisplay = newLayout ? colorizeSeparators(dressedText) : dressedText;
    JEditorPane editorPane = IdeTooltipManagerImpl.initPane(new Html(textToDisplay).setKeepFont(true), hintHint, layeredPane, limitWidthToScreen);
    editorPane.putClientProperty(UIUtil.TEXT_COPY_ROOT, Boolean.TRUE);
    hintHint.setContentActive(isContentAction(dressedText));
    if (!hintHint.isAwtTooltip()) {
      correctLocation(editor, editorPane, p, alignToRight, expanded, myCurrentWidth);
    }

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(editorPane, true);

    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    scrollPane.setOpaque(hintHint.isOpaqueAllowed());
    scrollPane.getViewport().setOpaque(hintHint.isOpaqueAllowed());

    scrollPane.setBackground(hintHint.getTextBackground());
    scrollPane.getViewport().setBackground(hintHint.getTextBackground());
    scrollPane.setViewportBorder(null);

    if (!newLayout) editorPane.setBorder(JBUI.Borders.emptyBottom(2));
    if (hintHint.isRequestFocus()) {
      editorPane.setFocusable(true);
    }

    List<AnAction> actions = new ArrayList<>();
    JPanel grid = createMainPanel(hintHint, scrollPane, editorPane, newLayout, highlightActions, !textToDisplay.equals(dressedText));
    if (ScreenReader.isActive()) {
      grid.setFocusTraversalPolicyProvider(true);
      grid.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
        @Override
        public Component getDefaultComponent(Container aContainer) {
          return editorPane;
        }

        @Override
        public boolean getImplicitDownCycleTraversal() {
          return true;
        }
      });
    }
    final LightweightHintImpl hint = new LightweightHintImpl(grid) {

      @Override
      public void hide() {
        onHide(editorPane);
        super.hide();
        for (AnAction action : actions) {
          action.unregisterCustomShortcutSet(contentComponent);
        }
      }

      @Override
      protected boolean canAutoHideOn(TooltipEvent event) {
        return LineTooltipRenderer.this.canAutoHideOn(event) && super.canAutoHideOn(event);
      }
    };


    TooltipReloader reloader = tooltipReloader == null ? toExpand -> reloadFor(hint, editor, p, editorPane, alignToRight, group, hintHint, toExpand) : tooltipReloader;

    actions.add(new AnAction() {
      // an action to expand description when tooltip was shown after mouse move; need to unregister from editor component
      {
        registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_ERROR_DESCRIPTION), contentComponent);
      }

      @Override
      public void actionPerformed(@Nonnull final AnActionEvent e) {
        // The tooltip gets the focus if using a screen reader and invocation through a keyboard shortcut.
        hintHint.setRequestFocus(ScreenReader.isActive() && e.getInputEvent() instanceof KeyEvent);
        //TooltipActionsLogger.INSTANCE.logShowDescription(e.getProject(), "shortcut", e.getInputEvent(), e.getPlace());
        reloader.reload(!expanded);
      }
    });

    editorPane.addHyperlinkListener(e -> {
      myActiveLink = true;
      if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
        myActiveLink = false;
        return;
      }
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        final URL url = e.getURL();
        if (url != null) {
          BrowserUtil.browse(url);
          hint.hide();
          return;
        }

        final String description = e.getDescription();
        if (description != null && handle(description, editor)) {
          hint.hide();
          return;
        }

        //TooltipActionsLogger.INSTANCE.logShowDescription(editor.getProject(), "more.link", e.getInputEvent(), null);
        reloader.reload(!expanded);
      }
    });

    fillPanel(editor, grid, hint, hintHint, actions, reloader, newLayout, highlightActions);

    if (!newLayout) {
      grid.addMouseListener(new MouseAdapter() {

        // This listener makes hint transparent for mouse events. It means that hint is closed
        // by MousePressed and this MousePressed goes into the underlying editor component.
        @Override
        public void mouseReleased(final MouseEvent e) {
          if (!myActiveLink) {
            MouseEvent newMouseEvent = SwingUtilities.convertMouseEvent(e.getComponent(), e, contentComponent);
            hint.hide();
            contentComponent.dispatchEvent(newMouseEvent);
          }
        }
      });

      ListenerUtil.addMouseListener(grid, new MouseAdapter() {
        @Override
        public void mouseExited(final MouseEvent e) {
          if (expanded) return;

          Container parentContainer = grid;
          //ComponentWithMnemonics is top balloon component
          while (!(parentContainer instanceof ComponentWithMnemonics)) {
            Container candidate = parentContainer.getParent();
            if (candidate == null) break;
            parentContainer = candidate;
          }

          MouseEvent newMouseEvent = SwingUtilities.convertMouseEvent(e.getComponent(), e, parentContainer);

          if (parentContainer.contains(newMouseEvent.getPoint())) {
            return;
          }

          hint.hide();
        }
      });
    }

    return hint;
  }

  // Java text components don't support specifying color for 'hr' tag, so we need to replace it with something else,
  // if we need a separator with custom color
  @Nonnull
  private static String colorizeSeparators(@Nonnull String html) {
    String body = UIUtil.getHtmlBody(html);
    List<String> parts = StringUtil.split(body, UIUtil.BORDER_LINE, true, false);
    if (parts.size() <= 1) return html;
    StringBuilder b = new StringBuilder();
    for (String part : parts) {
      boolean addBorder = b.length() > 0;
      b.append("<div");
      if (addBorder) {
        b.append(" style='margin-top:6; padding-top:6; border-top: thin solid #");
        b.append(ColorUtil.toHex(UIUtil.getTooltipSeparatorColor()));
        b.append("'");
      }
      b.append("'>").append(part).append("</div>");
    }
    return XmlStringUtil.wrapInHtml(b.toString());
  }

  protected boolean isContentAction(String dressedText) {
    return isActiveHtml(dressedText);
  }

  protected boolean canAutoHideOn(@Nonnull TooltipEvent event) {
    return true;
  }

  private void reloadFor(@Nonnull LightweightHintImpl hint,
                         @Nonnull Editor editor,
                         @Nonnull Point p,
                         @Nonnull JComponent pane,
                         boolean alignToRight,
                         @Nonnull TooltipGroup group,
                         @Nonnull HintHint hintHint,
                         boolean expand) {
    //required for immediately showing. Otherwise there are several concurrent issues
    hint.hide();

    hintHint.setShowImmediately(true);
    Point point = new Point(p);
    TooltipController.getInstance().showTooltip(editor, point, createRenderer(myText, expand ? pane.getWidth() : 0), alignToRight, group, hintHint);
  }

  protected void fillPanel(@Nonnull Editor editor,
                           @Nonnull JPanel component,
                           @Nonnull LightweightHintImpl hint,
                           @Nonnull HintHint hintHint,
                           @Nonnull List<? super AnAction> actions,
                           @Nonnull TooltipReloader expandCallback,
                           boolean newLayout,
                           boolean highlightActions) {
    hintHint.setComponentBorder(JBUI.Borders.empty());
    hintHint.setBorderInsets(JBUI.insets(0));
  }

  private static boolean handle(@Nonnull final String ref, @Nonnull final Editor editor) {
    return TooltipLinkHandlers.handleLink(ref, editor);
  }

  public static void correctLocation(Editor editor, JComponent tooltipComponent, Point p, boolean alignToRight, boolean expanded, int currentWidth) {
    final JComponent editorComponent = editor.getComponent();
    final JLayeredPane layeredPane = editorComponent.getRootPane().getLayeredPane();

    int widthLimit = layeredPane.getWidth() - 10;
    int heightLimit = layeredPane.getHeight() - 5;

    Dimension dimension = correctLocation(editor, p, alignToRight, expanded, tooltipComponent, layeredPane, widthLimit, heightLimit, currentWidth);

    // in order to restrict tooltip size
    tooltipComponent.setSize(dimension);
    tooltipComponent.setMaximumSize(dimension);
    tooltipComponent.setMinimumSize(dimension);
    tooltipComponent.setPreferredSize(dimension);
  }

  private static Dimension correctLocation(Editor editor,
                                           Point p,
                                           boolean alignToRight,
                                           boolean expanded,
                                           JComponent tooltipComponent,
                                           JLayeredPane layeredPane,
                                           int widthLimit,
                                           int heightLimit,
                                           int currentWidth) {
    Dimension preferredSize = tooltipComponent.getPreferredSize();
    int width = expanded ? 3 * currentWidth / 2 : preferredSize.width;
    int height = expanded ? Math.max(preferredSize.height, 150) : preferredSize.height;
    Dimension dimension = new Dimension(width, height);

    if (alignToRight) {
      p.x = Math.max(0, p.x - width);
    }

    // try to make cursor outside tooltip. SCR 15038
    p.x += 3;
    p.y += 3;

    if (p.x >= widthLimit - width) {
      p.x = widthLimit - width;
      width = Math.min(width, widthLimit);
      height += 20;
      dimension = new Dimension(width, height);
    }

    if (p.x < 3) {
      p.x = 3;
    }

    if (p.y > heightLimit - height) {
      p.y = heightLimit - height;
      height = Math.min(heightLimit, height);
      dimension = new Dimension(width, height);
    }

    if (p.y < 3) {
      p.y = 3;
    }

    locateOutsideMouseCursor(editor, layeredPane, p, width, height, heightLimit);
    return dimension;
  }

  private static void locateOutsideMouseCursor(Editor editor, JComponent editorComponent, Point p, int width, int height, int heightLimit) {
    PointerInfo pointerInfo = MouseInfo.getPointerInfo();
    if (pointerInfo == null) return;
    Point mouse = pointerInfo.getLocation();
    SwingUtilities.convertPointFromScreen(mouse, editorComponent);
    Rectangle tooltipRect = new Rectangle(p, new Dimension(width, height));
    // should show at least one line apart
    tooltipRect.setBounds(tooltipRect.x, tooltipRect.y - editor.getLineHeight(), width, height + 2 * editor.getLineHeight());
    if (tooltipRect.contains(mouse)) {
      if (mouse.y + height + editor.getLineHeight() > heightLimit && mouse.y - height - editor.getLineHeight() > 0) {
        p.y = mouse.y - height - editor.getLineHeight();
      }
      else {
        p.y = mouse.y + editor.getLineHeight();
      }
    }
  }

  protected void onHide(@Nonnull JComponent contentComponent) {
  }

  @Nonnull
  public LineTooltipRenderer createRenderer(@Nullable String text, int width) {
    return new LineTooltipRenderer(text, width, getEqualityObjects());
  }

  @Nonnull
  protected String dressDescription(@Nonnull final Editor editor, @Nonnull String tooltipText, boolean expanded) {
    return tooltipText;
  }

  protected static boolean isActiveHtml(@Nonnull String html) {
    return html.contains("</a>");
  }

  public void addBelow(@Nonnull String text) {
    @NonNls String newBody;
    if (myText == null) {
      newBody = UIUtil.getHtmlBody(text);
    }
    else {
      String html1 = UIUtil.getHtmlBody(myText);
      String html2 = UIUtil.getHtmlBody(text);
      newBody = html1 + UIUtil.BORDER_LINE + html2;
    }
    myText = XmlStringUtil.wrapInHtml(newBody);
  }

  @Nullable
  public String getText() {
    return myText;
  }
}

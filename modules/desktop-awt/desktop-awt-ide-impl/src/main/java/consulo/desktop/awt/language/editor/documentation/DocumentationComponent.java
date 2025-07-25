// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.desktop.awt.language.editor.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.dumb.DumbAware;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.ui.DimensionService;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.FontSize;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.desktop.awt.action.toolbar.AdvancedActionToolbarImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.ide.actions.BaseNavigateToSourceAction;
import consulo.ide.impl.idea.ide.actions.ExternalJavaDocAction;
import consulo.ide.impl.idea.ide.actions.WindowAction;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.editor.documentation.DocumentationMarkup;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ide.impl.idea.ui.WidthBasedLayout;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.PopupPositionManager;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.documentation.*;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.util.PopupUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.ImageKey;
import consulo.ui.util.LightDarkColorValue;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.io.Url;
import consulo.util.io.Urls;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderableImageProducer;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.*;

public class DocumentationComponent extends JPanel implements Disposable, DataProvider, WidthBasedLayout {

    private static final Logger LOG = Logger.getInstance(DocumentationComponent.class);
    private static final String DOCUMENTATION_TOPIC_ID = "reference.toolWindows.Documentation";

    public static final EditorColorKey COLOR_KEY = EditorColorKey.createColorKey("DOCUMENTATION_COLOR", new LightDarkColorValue(new RGBColor(247, 247, 247), new RGBColor(70, 72, 74)));
    public static final Color SECTION_COLOR = Gray.get(0x90);

    private static final Highlighter.HighlightPainter LINK_HIGHLIGHTER = new LinkHighlighter();

    private static final int PREFERRED_HEIGHT_MAX_EM = 10;
    private static final JBDimension MAX_DEFAULT = new JBDimension(650, 500);
    private static final JBDimension MIN_DEFAULT = new JBDimension(300, 36);
    private final ExternalDocAction myExternalDocAction;

    private DocumentationManagerImpl myManager;
    private SmartPsiElementPointer<PsiElement> myElement;
    private long myModificationCount;

    public static final String QUICK_DOC_FONT_SIZE_PROPERTY = "quick.doc.font.size";

    private final Stack<Context> myBackStack = new Stack<>();
    private final Stack<Context> myForwardStack = new Stack<>();
    private final AdvancedActionToolbarImpl myToolBar;
    private volatile boolean myIsEmpty;
    private boolean mySizeTrackerRegistered;
    private JSlider myFontSizeSlider;
    private final JComponent mySettingsPanel;
    private boolean myIgnoreFontSizeSliderChange;
    private String myExternalUrl;
    private DocumentationProvider myProvider;
    private Reference<Component> myReferenceComponent;

    private final MyDictionary<String, Image> myImageProvider = new MyDictionary<>() {
        @Override
        public Image get(Object key) {
            return getImageByKeyImpl(key);
        }
    };

    private Runnable myToolwindowCallback;
    private final ActionToolbar myCornerToolbar;

    private final MyScrollPane myScrollPane;
    private final JEditorPane myEditorPane;
    private String myText; // myEditorPane.getText() surprisingly crashes.., let's cache the text
    private String myDecoratedText; // myEditorPane.getText() surprisingly crashes.., let's cache the text
    private final JComponent myControlPanel;
    private boolean myControlPanelVisible;
    private int myHighlightedLink = -1;
    private Object myHighlightingTag;
    private final boolean myStoreSize;
    private boolean myManuallyResized;

    private AbstractPopup myHint;

    private final Map<KeyStroke, ActionListener> myKeyboardActions = new HashMap<>();

    @Nonnull
    public static DocumentationComponent createAndFetch(@Nonnull Project project, @Nonnull PsiElement element, @Nonnull Disposable disposable) {
        DocumentationManagerImpl manager = (DocumentationManagerImpl) DocumentationManager.getInstance(project);
        DocumentationComponent component = new DocumentationComponent(manager);
        Disposer.register(disposable, component);
        manager.fetchDocInfo(element, component);
        return component;
    }

    public DocumentationComponent(DocumentationManagerImpl manager) {
        this(manager, true);
    }

    public DocumentationComponent(DocumentationManagerImpl manager, boolean storeSize) {
        myManager = manager;
        myIsEmpty = true;
        myStoreSize = storeSize;

        myEditorPane = new JEditorPane(UIUtil.HTML_MIME, "") {
            {
                enableEvents(AWTEvent.KEY_EVENT_MASK);
            }

            @Override
            protected void processKeyEvent(KeyEvent e) {
                KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                ActionListener listener = myKeyboardActions.get(keyStroke);
                if (listener != null) {
                    listener.actionPerformed(new ActionEvent(DocumentationComponent.this, 0, ""));
                    e.consume();
                    return;
                }
                super.processKeyEvent(e);
            }

            Point initialClick;

            @Override
            protected void processMouseEvent(MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_PRESSED && myHint != null) {
                    //DocumentationComponent.this.requestFocus();
                    initialClick = null;
                    StyledDocument document = (StyledDocument) getDocument();
                    int x = e.getX();
                    int y = e.getY();
                    if (!hasTextAt(document, x, y) && !hasTextAt(document, x + 3, y) && !hasTextAt(document, x - 3, y) && !hasTextAt(document, x, y + 3) && !hasTextAt(document, x, y - 3)) {
                        initialClick = e.getPoint();
                    }
                }
                super.processMouseEvent(e);
            }

            private boolean hasTextAt(StyledDocument document, int x, int y) {
                Element element = document.getCharacterElement(viewToModel(new Point(x, y)));
                try {
                    String text = document.getText(element.getStartOffset(), element.getEndOffset() - element.getStartOffset());
                    if (StringUtil.isEmpty(text.trim())) {
                        return false;
                    }
                }
                catch (BadLocationException ignored) {
                    return false;
                }
                return true;
            }

            @Override
            protected void processMouseMotionEvent(MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_DRAGGED && myHint != null && initialClick != null) {
                    Point location = myHint.getLocationOnScreen();
                    myHint.setLocation(new Point(location.x + e.getX() - initialClick.x, location.y + e.getY() - initialClick.y));
                    e.consume();
                    return;
                }
                super.processMouseMotionEvent(e);
            }

            @Override
            protected void paintComponent(Graphics g) {
                GraphicsUtil.setupAntialiasing(g);
                super.paintComponent(g);
            }

            @Override
            public void setDocument(Document doc) {
                super.setDocument(doc);
                doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
                if (doc instanceof StyledDocument) {
                    doc.putProperty("imageCache", myImageProvider);
                }
            }
        };
        boolean newLayout = true;
        DataProvider helpDataProvider = dataId -> HelpManager.HELP_ID == dataId ? DOCUMENTATION_TOPIC_ID : null;
        myEditorPane.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, helpDataProvider);
        myText = "";
        myDecoratedText = "";
        myEditorPane.setEditable(false);
        if (ScreenReader.isActive()) {
            // Note: Making the caret visible is merely for convenience
            myEditorPane.getCaret().setVisible(true);
        }
        else {
            myEditorPane.putClientProperty("caretWidth", 0); // do not reserve space for caret (making content one pixel narrower than component)
            UIUtil.doNotScrollToCaret(myEditorPane);
        }
        myEditorPane.setBackground(TargetAWT.to(EditorColorsUtil.getGlobalOrDefaultColor(COLOR_KEY)));
        HTMLEditorKit editorKit = JBHtmlEditorKit.create(true);
        prepareCSS(editorKit);
        myEditorPane.setEditorKit(editorKit);
        myEditorPane.setBorder(JBUI.Borders.empty());
        myScrollPane = new MyScrollPane();
        myScrollPane.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, helpDataProvider);

        FocusListener focusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Component previouslyFocused = WindowManagerEx.getInstanceEx().getFocusedComponent(manager.getProject(getElement()));

                if (previouslyFocused != myEditorPane) {
                    if (myHint != null && !myHint.isDisposed()) {
                        myHint.cancel();
                    }
                }
            }
        };
        myEditorPane.addFocusListener(focusAdapter);

        Disposer.register(this, () -> myEditorPane.removeFocusListener(focusAdapter));

        setLayout(new BorderLayout());

        mySettingsPanel = createSettingsPanel();
        //add(myScrollPane, BorderLayout.CENTER);
        setOpaque(true);
        myScrollPane.setBorder(JBUI.Borders.empty());

        DefaultActionGroup actions = new DefaultActionGroup();
        BackAction back = new BackAction();
        ForwardAction forward = new ForwardAction();
        EditDocumentationSourceAction edit = new EditDocumentationSourceAction();
        myExternalDocAction = new ExternalDocAction();
        actions.add(back);
        actions.add(forward);
        actions.add(edit);

        try {
            String backKey = ScreenReader.isActive() ? "alt LEFT" : "LEFT";
            CustomShortcutSet backShortcutSet = new CustomShortcutSet(KeyboardShortcut.fromString(backKey), KeymapUtil.parseMouseShortcut("button4"));

            String forwardKey = ScreenReader.isActive() ? "alt RIGHT" : "RIGHT";
            CustomShortcutSet forwardShortcutSet = new CustomShortcutSet(KeyboardShortcut.fromString(forwardKey), KeymapUtil.parseMouseShortcut("button5"));
            back.registerCustomShortcutSet(backShortcutSet, this);
            forward.registerCustomShortcutSet(forwardShortcutSet, this);
            // mouse actions are checked only for exact component over which click was performed,
            // so we need to register shortcuts for myEditorPane as well
            back.registerCustomShortcutSet(backShortcutSet, myEditorPane);
            forward.registerCustomShortcutSet(forwardShortcutSet, myEditorPane);
        }
        catch (InvalidDataException e) {
            LOG.error(e);
        }

        myExternalDocAction.registerCustomShortcutSet(CustomShortcutSet.fromString("UP"), this);
        myExternalDocAction.registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_EXTERNAL_JAVADOC).getShortcutSet(), myEditorPane);
        edit.registerCustomShortcutSet(CommonShortcuts.getEditSource(), this);
        ActionPopupMenu contextMenu = ((ActionManagerImpl) ActionManager.getInstance()).createActionPopupMenu(ActionPlaces.JAVADOC_TOOLBAR, actions, new MenuItemPresentationFactory(true));
        PopupHandler popupHandler = new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                contextMenu.getComponent().show(comp, x, y);
            }
        };
        myEditorPane.addMouseListener(popupHandler);
        Disposer.register(this, () -> myEditorPane.removeMouseListener(popupHandler));

        new NextLinkAction().registerCustomShortcutSet(CustomShortcutSet.fromString("TAB"), this);
        new PreviousLinkAction().registerCustomShortcutSet(CustomShortcutSet.fromString("shift TAB"), this);
        new ActivateLinkAction().registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), this);

        DefaultActionGroup toolbarActions = new DefaultActionGroup();
        toolbarActions.add(actions);

        DocumentationMoreActionGroup rightActions = new DocumentationMoreActionGroup();
        rightActions.add(new ShowAsToolwindowAction());
        rightActions.add(new MyShowSettingsAction(true));
        rightActions.add(new ShowToolbarAction());
        rightActions.add(new RestoreDefaultSizeAction());
        toolbarActions.add(rightActions);

        myToolBar = new AdvancedActionToolbarImpl(ActionPlaces.JAVADOC_TOOLBAR,
            toolbarActions,
            ActionToolbar.Style.HORIZONTAL,
            ActionManager.getInstance()) {
            Point initialClick;

            @Override
            protected void processMouseEvent(MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_PRESSED && myHint != null) {
                    initialClick = e.getPoint();
                }
                super.processMouseEvent(e);
            }

            @Override
            protected void processMouseMotionEvent(MouseEvent e) {
                if (e.getID() == MouseEvent.MOUSE_DRAGGED && myHint != null && initialClick != null) {
                    Point location = myHint.getLocationOnScreen();
                    myHint.setLocation(new Point(location.x + e.getX() - initialClick.x, location.y + e.getY() - initialClick.y));
                    e.consume();
                    return;
                }
                super.processMouseMotionEvent(e);
            }
        };

        JLayeredPane layeredPane = new JBLayeredPane() {
            @Override
            public void doLayout() {
                Rectangle r = getBounds();
                for (Component component : getComponents()) {
                    if (component instanceof JScrollPane) {
                        component.setBounds(0, 0, r.width, r.height);
                    }
                    else {
                        Dimension d = component.getPreferredSize();
                        component.setBounds(r.width - d.width - 2, r.height - d.height - (7), d.width, d.height);
                    }
                }
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension size = myScrollPane.getPreferredSize();
                if (myHint == null && myManager != null && myManager.getToolWindow() == null) {
                    int em = myEditorPane.getFont().getSize();
                    int prefHeightMax = PREFERRED_HEIGHT_MAX_EM * em;
                    return new Dimension(size.width, Math.min(prefHeightMax, size.height + (needsToolbar() ? myControlPanel.getPreferredSize().height : 0)));
                }
                return size;
            }
        };
        layeredPane.add(myScrollPane);
        layeredPane.setLayer(myScrollPane, 0);

        DefaultActionGroup gearActions = new MyGearActionGroup();
        ShowAsToolwindowAction showAsToolwindowAction = new ShowAsToolwindowAction();
        gearActions.add(showAsToolwindowAction);
        gearActions.add(new MyShowSettingsAction(false));
        gearActions.add(new ShowToolbarAction());
        gearActions.add(new RestoreDefaultSizeAction());
        gearActions.addSeparator();
        gearActions.addAll(actions);

        myCornerToolbar = ActionManager.getInstance().createActionToolbar("DocumentationPopup", new WrapperActionGroup(gearActions), true);
        myCornerToolbar.setTargetComponent(this);
        myCornerToolbar.setMiniMode(true);

        JComponent toolbarComponent = myCornerToolbar.getComponent();

        showAsToolwindowAction.registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts("QuickJavaDoc"), toolbarComponent);
        layeredPane.add(toolbarComponent);
        layeredPane.setLayer(toolbarComponent, JLayeredPane.POPUP_LAYER);
        add(layeredPane, BorderLayout.CENTER);

        myControlPanel = myToolBar.getComponent();
        myControlPanel.setBorder(IdeBorderFactory.createBorder(UIUtil.getTooltipSeparatorColor(), SideBorder.BOTTOM));
        myControlPanelVisible = false;

        HyperlinkListener hyperlinkListener = e -> {
            HyperlinkEvent.EventType type = e.getEventType();
            if (type == HyperlinkEvent.EventType.ACTIVATED) {
                manager.navigateByLink(DocumentationComponent.this, e.getDescription());
            }
        };
        myEditorPane.addHyperlinkListener(hyperlinkListener);
        Disposer.register(this, () -> myEditorPane.removeHyperlinkListener(hyperlinkListener));

        if (myHint != null) {
            Disposer.register(myHint, this);
        }
        else if (myManager.getToolWindow() != null) {
            Disposer.register(myManager.getToolWindow().getContentManager(), this);
        }

        registerActions();

        updateControlState();
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        if (myEditorPane != null) {
            myEditorPane.setBackground(color);
        }
        if (myControlPanel != null) {
            myControlPanel.setBackground(color);
        }
    }

    public AnAction[] getActions() {
        return myToolBar.getActions().stream().filter((action -> !(action instanceof AnSeparator))).toArray(AnAction[]::new);
    }

    public AnAction getFontSizeAction() {
        return new MyShowSettingsAction(false);
    }

    public void removeCornerMenu() {
        myCornerToolbar.getComponent().setVisible(false);
    }

    public void setToolwindowCallback(Runnable callback) {
        myToolwindowCallback = callback;
    }

    @RequiredUIAccess
    public void showExternalDoc() {
        DataContext dataContext = DataManager.getInstance().getDataContext(this);
        myExternalDocAction.actionPerformed(AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext));
    }

    @Override
    public boolean requestFocusInWindow() {
        // With a screen reader active, set the focus directly to the editor because
        // it makes it easier for users to read/navigate the documentation contents.
        if (ScreenReader.isActive()) {
            return myEditorPane.requestFocusInWindow();
        }
        else {
            return myScrollPane.requestFocusInWindow();
        }
    }

    @Override
    public void requestFocus() {
        // With a screen reader active, set the focus directly to the editor because
        // it makes it easier for users to read/navigate the documentation contents.
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
            if (ScreenReader.isActive()) {
                IdeFocusManager.getGlobalInstance().requestFocus(myEditorPane, true);
            }
            else {
                IdeFocusManager.getGlobalInstance().requestFocus(myScrollPane, true);
            }
        });
    }

    private static void prepareCSS(HTMLEditorKit editorKit) {
        Color borderColor = UIUtil.getTooltipSeparatorColor();
        int leftPadding = 8;
        int definitionTopPadding = 4;
        int htmlBottomPadding = 8;
        String editorFontName = StringUtil.escapeQuotes(EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName());
        editorKit.getStyleSheet().addRule("code {font-family:\"" + editorFontName + "\"}");
        editorKit.getStyleSheet().addRule("pre {font-family:\"" + editorFontName + "\"}");
        editorKit.getStyleSheet().addRule(".pre {font-family:\"" + editorFontName + "\"}");
        editorKit.getStyleSheet().addRule("html { padding-bottom: " + htmlBottomPadding + "px; }");
        editorKit.getStyleSheet().addRule("h1, h2, h3, h4, h5, h6 { margin-top: 0; padding-top: 1px; }");
        editorKit.getStyleSheet().addRule("a { color: #" + ColorUtil.toHex(getLinkColor()) + "; text-decoration: none;}");
        editorKit.getStyleSheet().addRule(".definition { padding: " + definitionTopPadding + "px 17px 1px " + leftPadding + "px; border-bottom: thin solid #" + ColorUtil.toHex(borderColor) + "; }");
        editorKit.getStyleSheet().addRule(".definition-only { padding: " + definitionTopPadding + "px 17px 0 " + leftPadding + "px; }");
        editorKit.getStyleSheet().addRule(".definition-only pre { margin-bottom: 0 }");
        editorKit.getStyleSheet().addRule(".content { padding: 5px 16px 0 " + leftPadding + "px; max-width: 100% }");
        editorKit.getStyleSheet().addRule(".content-only { padding: 8px 16px 0 " + leftPadding + "px; max-width: 100% }");
        editorKit.getStyleSheet().addRule(".bottom { padding: 3px 16px 0 " + leftPadding + "px; }");
        editorKit.getStyleSheet().addRule(".bottom-no-content { padding: 5px 16px 0 " + leftPadding + "px; }");
        editorKit.getStyleSheet().addRule("p { padding: 1px 0 2px 0; }");
        editorKit.getStyleSheet().addRule("ol { padding: 0 16px 0 0; }");
        editorKit.getStyleSheet().addRule("ul { padding: 0 16px 0 0; }");
        editorKit.getStyleSheet().addRule("li { padding: 1px 0 2px 0; }");
        editorKit.getStyleSheet().addRule(".grayed { color: #909090; display: inline;}");
        editorKit.getStyleSheet().addRule(".centered { text-align: center}");

        // sections table
        editorKit.getStyleSheet().addRule(".sections { padding: 0 16px 0 " + leftPadding + "; border-spacing: 0; }");
        editorKit.getStyleSheet().addRule("tr { margin: 0 0 0 0; padding: 0 0 0 0; }");
        editorKit.getStyleSheet().addRule("table p { padding-bottom: 0}");
        editorKit.getStyleSheet().addRule("td { margin: 4px 0 0 0; padding: 0 0 0 0; }");
        editorKit.getStyleSheet().addRule("th { text-align: left; }");
        editorKit.getStyleSheet().addRule(".section { color: " + ColorUtil.toHtmlColor(SECTION_COLOR) + "; padding-right: 4px}");
    }

    private static Color getLinkColor() {
        return JBCurrentTheme.Link.linkColor();
    }

    @Override
    public Object getData(@Nonnull @NonNls Key dataId) {
        if (DocumentationManagerHelper.SELECTED_QUICK_DOC_TEXT == dataId) {
            // Javadocs often contain &nbsp; symbols (non-breakable white space). We don't want to copy them as is and replace
            // with raw white spaces. See IDEA-86633 for more details.
            String selectedText = myEditorPane.getSelectedText();
            return selectedText == null ? null : selectedText.replace((char) 160, ' ');
        }

        return null;
    }

    private JComponent createSettingsPanel() {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        result.add(new JLabel(ApplicationLocalize.labelFontSize().get()));
        myFontSizeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, FontSize.values().length - 1, 3);
        myFontSizeSlider.setMinorTickSpacing(1);
        myFontSizeSlider.setPaintTicks(true);
        myFontSizeSlider.setPaintTrack(true);
        myFontSizeSlider.setSnapToTicks(true);
        UIUtil.setSliderIsFilled(myFontSizeSlider, true);
        result.add(myFontSizeSlider);
        result.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));

        myFontSizeSlider.addChangeListener(e -> {
            if (myIgnoreFontSizeSliderChange) {
                return;
            }
            setQuickDocFontSize(FontSize.values()[myFontSizeSlider.getValue()]);
            applyFontProps();
            // resize popup according to new font size, if user didn't set popup size manually
            if (!myManuallyResized && myHint != null && myHint.getDimensionServiceKey() == null) {
                showHint();
            }
        });

        String tooltipText = ApplicationLocalize.quickdocTooltipFontSizeByWheel().get();
        result.setToolTipText(tooltipText);
        myFontSizeSlider.setToolTipText(tooltipText);
        result.setVisible(false);
        result.setOpaque(true);
        myFontSizeSlider.setOpaque(true);
        return result;
    }

    @Nonnull
    public static FontSize getQuickDocFontSize() {
        String strValue = PropertiesComponent.getInstance().getValue(QUICK_DOC_FONT_SIZE_PROPERTY);
        if (strValue != null) {
            try {
                return FontSize.valueOf(strValue);
            }
            catch (IllegalArgumentException iae) {
                // ignore, fall back to default font.
            }
        }
        return FontSize.SMALL;
    }

    public void setQuickDocFontSize(@Nonnull FontSize fontSize) {
        PropertiesComponent.getInstance().setValue(QUICK_DOC_FONT_SIZE_PROPERTY, fontSize.toString());
    }

    private void setFontSizeSliderSize(FontSize fontSize) {
        myIgnoreFontSizeSliderChange = true;
        try {
            FontSize[] sizes = FontSize.values();
            for (int i = 0; i < sizes.length; i++) {
                if (fontSize == sizes[i]) {
                    myFontSizeSlider.setValue(i);
                    break;
                }
            }
        }
        finally {
            myIgnoreFontSizeSliderChange = false;
        }
    }

    public boolean isEmpty() {
        return myIsEmpty;
    }

    public void startWait() {
        myIsEmpty = true;
    }

    private void setControlPanelVisible() {
        if (myControlPanelVisible) {
            return;
        }
        add(myControlPanel, BorderLayout.NORTH);
        myControlPanelVisible = true;
    }

    public void setHint(JBPopup hint) {
        myHint = (AbstractPopup) hint;
    }

    public JBPopup getHint() {
        return myHint;
    }

    public JComponent getComponent() {
        return myEditorPane;
    }

    @Nullable
    @RequiredReadAction
    public PsiElement getElement() {
        return myElement != null ? myElement.getElement() : null;
    }

    private void setElement(SmartPsiElementPointer<PsiElement> element) {
        myElement = element;
        myModificationCount = getCurrentModificationCount();
    }

    @RequiredReadAction
    public boolean isUpToDate() {
        return getElement() != null && myModificationCount == getCurrentModificationCount();
    }

    private long getCurrentModificationCount() {
        return myElement != null ? PsiModificationTracker.getInstance(myElement.getProject()).getModificationCount() : -1;
    }

    public void setText(@Nonnull String text, @Nullable PsiElement element, @Nullable DocumentationProvider provider) {
        setData(element, text, null, null, provider);
    }

    @RequiredReadAction
    public void replaceText(@Nonnull String text, @Nullable PsiElement element) {
        PsiElement current = getElement();
        if (current == null || !current.getManager().areElementsEquivalent(current, element)) {
            return;
        }
        restoreContext(saveContext().withText(text));
    }

    public void clearHistory() {
        myForwardStack.clear();
        myBackStack.clear();
    }

    private void pushHistory() {
        if (myElement != null) {
            myBackStack.push(saveContext());
            myForwardStack.clear();
        }
    }

    public void setData(@Nullable PsiElement element, @Nonnull String text, @Nullable String effectiveExternalUrl, @Nullable String ref, @Nullable DocumentationProvider provider) {
        pushHistory();
        myExternalUrl = effectiveExternalUrl;
        myProvider = provider;

        SmartPsiElementPointer<PsiElement> pointer = null;
        if (element != null && element.isValid()) {
            pointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
        }
        setDataInternal(pointer, text, new Rectangle(0, 0), ref);
    }

    private void setDataInternal(@Nullable SmartPsiElementPointer<PsiElement> element, @Nonnull String text, @Nonnull Rectangle viewRect, @Nullable String ref) {
        myIsEmpty = false;
        if (myManager == null) {
            return;
        }

        myText = text;
        setElement(element);
        myDecoratedText = decorate(text);

        showHint(viewRect, ref);
    }

    protected void showHint(@Nonnull Rectangle viewRect, @Nullable String ref) {
        String refToUse;
        Rectangle viewRectToUse;
        if (DocumentationManagerProtocol.KEEP_SCROLLING_POSITION_REF.equals(ref)) {
            refToUse = null;
            viewRectToUse = myScrollPane.getViewport().getViewRect();
        }
        else {
            refToUse = ref;
            viewRectToUse = viewRect;
        }

        updateControlState();

        highlightLink(-1);

        myEditorPane.setText(myDecoratedText);
        applyFontProps();

        showHint();

        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(() -> {
            myEditorPane.scrollRectToVisible(viewRectToUse); // if ref is defined but is not found in document, this provides a default location
            if (refToUse != null) {
                UIUtil.scrollToReference(myEditorPane, refToUse);
            }
            else if (ScreenReader.isActive()) {
                myEditorPane.setCaretPosition(0);
            }
        });
    }

    protected void showHint() {
        if (myHint == null) {
            return;
        }

        setHintSize();

        DataContext dataContext = getDataContext();
        PopupPositionManager.positionPopupInBestPosition(myHint, myManager.getEditor(), dataContext, PopupPositionManager.Position.RIGHT, PopupPositionManager.Position.LEFT);

        Window window = myHint.getPopupWindow();
        if (window != null) {
            window.setFocusableWindowState(true);
        }

        registerSizeTracker();
    }

    private DataContext getDataContext() {
        Component referenceComponent;
        if (myReferenceComponent == null) {
            referenceComponent = ProjectIdeFocusManager.getInstance(myManager.getProject()).getFocusOwner();
            myReferenceComponent = new WeakReference<>(referenceComponent);
        }
        else {
            referenceComponent = SoftReference.dereference(myReferenceComponent);
            if (referenceComponent == null || !referenceComponent.isShowing()) {
                referenceComponent = myHint.getComponent();
            }
        }
        return DataManager.getInstance().getDataContext(referenceComponent);
    }

    private void setHintSize() {
        Dimension hintSize;
        if (!myManuallyResized && myHint.getDimensionServiceKey() == null) {
            hintSize = getOptimalSize();
        }
        else {
            if (myManuallyResized) {
                hintSize = myHint.getSize();
                JBInsets.removeFrom(hintSize, myHint.getContent().getInsets());
            }
            else {
                Size2D size = DimensionService.getInstance().getSize(DocumentationManagerHelper.NEW_JAVADOC_LOCATION_AND_SIZE, myManager.getProject());
                hintSize = size == null ? null : new Dimension(size.width(), size.height());
            }
            if (hintSize == null) {
                hintSize = new Dimension(MIN_DEFAULT);
            }
            else {
                hintSize.width = Math.max(hintSize.width, MIN_DEFAULT.width);
                hintSize.height = Math.max(hintSize.height, MIN_DEFAULT.height);
            }
        }
        myHint.setSize(hintSize);
    }

    public Dimension getOptimalSize() {
        int width = getPreferredWidth();
        int height = getPreferredHeight(width);
        return new Dimension(width, height);
    }

    @Override
    public int getPreferredWidth() {
        int minWidth = JBUIScale.scale(300);
        int maxWidth = getPopupAnchor() != null ? JBUIScale.scale(435) : MAX_DEFAULT.width;

        int width = definitionPreferredWidth();
        if (width < 0) { // no definition found
            width = myEditorPane.getPreferredSize().width;
        }
        else {
            width = Math.max(width, myEditorPane.getMinimumSize().width);
        }
        Insets insets = getInsets();
        return Math.min(maxWidth, Math.max(minWidth, width)) + insets.left + insets.right;
    }

    @Override
    public int getPreferredHeight(int width) {
        myEditorPane.setBounds(0, 0, width, MAX_DEFAULT.height);
        myEditorPane.setText(myDecoratedText);
        Dimension preferredSize = myEditorPane.getPreferredSize();

        int height = preferredSize.height + (needsToolbar() ? myControlPanel.getPreferredSize().height : 0);
        JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
        int reservedForScrollBar = width < preferredSize.width && scrollBar.isOpaque() ? scrollBar.getPreferredSize().height : 0;
        Insets insets = getInsets();
        return Math.min(MAX_DEFAULT.height, Math.max(MIN_DEFAULT.height, height)) + insets.top + insets.bottom + reservedForScrollBar;
    }

    private Component getPopupAnchor() {
        LookupEx lookup = myManager == null ? null : LookupManager.getActiveLookup(myManager.getEditor());

        if (lookup != null && lookup.getCurrentItem() != null && lookup.getComponent().isShowing()) {
            return lookup.getComponent();
        }
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JBPopup popup = PopupUtil.getPopupContainerFor(focusOwner);
        if (popup != null && popup != myHint && !popup.isDisposed()) {
            return popup.getContent();
        }
        return null;
    }

    private void registerSizeTracker() {
        AbstractPopup hint = myHint;
        if (hint == null || mySizeTrackerRegistered) {
            return;
        }
        mySizeTrackerRegistered = true;
        hint.addResizeListener(this::onManualResizing, this);
        Application.get().getMessageBus().connect(this).subscribe(
            AnActionListener.class,
            new AnActionListener() {
                @Override
                public void afterActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
                    if (action instanceof WindowAction) {
                        onManualResizing();
                    }
                }
            }
        );
    }

    private void onManualResizing() {
        myManuallyResized = true;
        if (myStoreSize && myHint != null) {
            myHint.setDimensionServiceKey(DocumentationManagerHelper.NEW_JAVADOC_LOCATION_AND_SIZE);
            myHint.storeDimensionSize();
        }
    }

    private int definitionPreferredWidth() {
        TextUI ui = myEditorPane.getUI();
        View view = ui.getRootView(myEditorPane);
        View definition = findDefinition(view);
        return definition != null ? (int) definition.getPreferredSpan(View.X_AXIS) : -1;
    }

    private static View findDefinition(View view) {
        if ("definition".equals(view.getElement().getAttributes().getAttribute(HTML.Attribute.CLASS))) {
            return view;
        }
        for (int i = 0; i < view.getViewCount(); i++) {
            View definition = findDefinition(view.getView(i));
            if (definition != null) {
                return definition;
            }
        }
        return null;
    }

    @RequiredReadAction
    private String decorate(String text) {
        text = StringUtil.replaceIgnoreCase(text, "</html>", "");
        text = StringUtil.replaceIgnoreCase(text, "</body>", "");
        text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.SECTIONS_START + DocumentationMarkup.SECTIONS_END, "");
        text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.SECTIONS_START + "<p>" + DocumentationMarkup.SECTIONS_END, "");
        boolean hasContent = text.contains(DocumentationMarkup.CONTENT_START);
        if (!hasContent) {
            if (!text.contains(DocumentationMarkup.DEFINITION_START)) {
                int bodyStart = findContentStart(text);
                if (bodyStart > 0) {
                    text = text.substring(0, bodyStart) + DocumentationMarkup.CONTENT_START + text.substring(bodyStart) +
                        DocumentationMarkup.CONTENT_END;
                }
                else {
                    text = DocumentationMarkup.CONTENT_START + text + DocumentationMarkup.CONTENT_END;
                }
                hasContent = true;
            }
            else if (!text.contains(DocumentationMarkup.SECTIONS_START)) {
                text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.DEFINITION_START, "<div class='definition-only'><pre>");
            }
        }
        if (!text.contains(DocumentationMarkup.DEFINITION_START)) {
            text = text.replace("class='content'", "class='content-only'");
        }
        String location = getLocationText();
        if (location != null) {
            text = text + getBottom(hasContent) + location + "</div>";
        }
        String links = getExternalText(myManager, getElement(), myExternalUrl, myProvider);
        if (links != null) {
            text = text + getBottom(location != null) + links;
        }
        //workaround for Swing html renderer not removing empty paragraphs before non-inline tags
        text = text.replaceAll("<p>\\s*(<(?:[uo]l|h\\d|p))", "$1");
        text = addExternalLinksIcon(text);
        return text;
    }

    @Nullable
    private static String getExternalText(@Nonnull DocumentationManagerImpl manager, @Nullable PsiElement element, @Nullable String externalUrl, @Nullable DocumentationProvider provider) {
        if (element == null || provider == null) {
            return null;
        }

        PsiElement originalElement = DocumentationManagerHelper.getOriginalElement(element);
        if (!shouldShowExternalDocumentationLink(provider, element, originalElement)) {
            return null;
        }

        String title = manager.getTitle(element);
        if (title != null) {
            title = StringUtil.escapeXmlEntities(title);
        }
        if (externalUrl == null) {
            List<String> urls = provider.getUrlFor(element, originalElement);
            if (urls != null) {
                boolean hasBadUrl = false;
                StringBuilder result = new StringBuilder();
                for (String url : urls) {
                    String link = getLink(title, url);
                    if (link == null) {
                        hasBadUrl = true;
                        break;
                    }

                    if (result.length() > 0) {
                        result.append("<p>");
                    }
                    result.append(link);
                }
                if (!hasBadUrl) {
                    return result.toString();
                }
            }
            else {
                return null;
            }
        }
        else {
            String link = getLink(title, externalUrl);
            if (link != null) {
                return link;
            }
        }

        return "<a href='external_doc'>External documentation" + (title == null ? "" : (" for `" + title + "`")) + buildIconTag(PlatformIconGroup.ideExternallink()) + "</a></div>";
    }

    private static String getLink(String title, String url) {
        StringBuilder result = new StringBuilder();
        String hostname = getHostname(url);
        if (hostname == null) {
            return null;
        }

        result.append("<a href='").append(url).append("'>");
        if (title == null) {
            result.append("Documentation");
        }
        else {
            result.append("`").append(title).append("`");
        }
        result.append(" on ").append(hostname).append("</a>");
        return result.toString();
    }

    static boolean shouldShowExternalDocumentationLink(DocumentationProvider provider, PsiElement element, PsiElement originalElement) {
        if (provider instanceof CompositeDocumentationProvider) {
            List<DocumentationProvider> providers = ((CompositeDocumentationProvider) provider).getProviders();
            for (DocumentationProvider p : providers) {
                if (p instanceof ExternalDocumentationHandler) {
                    return ((ExternalDocumentationHandler) p).canHandleExternal(element, originalElement);
                }
            }
        }
        else if (provider instanceof ExternalDocumentationHandler) {
            return ((ExternalDocumentationHandler) provider).canHandleExternal(element, originalElement);
        }
        return true;
    }

    private static String getHostname(String url) {
        try {
            return new URL(url).toURI().getHost();
        }
        catch (URISyntaxException | MalformedURLException ignored) {
        }
        return null;
    }

    private static int findContentStart(String text) {
        int index = StringUtil.indexOfIgnoreCase(text, "<body>", 0);
        if (index >= 0) {
            return index + 6;
        }
        index = StringUtil.indexOfIgnoreCase(text, "</head>", 0);
        if (index >= 0) {
            return index + 7;
        }
        index = StringUtil.indexOfIgnoreCase(text, "</style>", 0);
        if (index >= 0) {
            return index + 8;
        }
        index = StringUtil.indexOfIgnoreCase(text, "<html>", 0);
        if (index >= 0) {
            return index + 6;
        }
        return -1;
    }

    @Nonnull
    private static String getBottom(boolean hasContent) {
        return "<div class='" + (hasContent ? "bottom" : "bottom-no-content") + "'>";
    }

    private static String addExternalLinksIcon(String text) {
        return text.replaceAll("(<a\\s*href=[\"']http[^>]*>)([^>]*)(</a>)", "$1$2" +
            buildIconTag(PlatformIconGroup.ideExternallink()) + "$3");
    }

    @RequiredReadAction
    private String getLocationText() {
        PsiElement element = getElement();
        if (element != null) {
            PsiFile file = element.getContainingFile();
            VirtualFile vfile = file == null ? null : file.getVirtualFile();

            if (vfile == null) {
                return null;
            }

            ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
            Module module = fileIndex.getModuleForFile(vfile);

            if (module != null) {
                if (ModuleManager.getInstance(element.getProject()).getModules().length == 1) {
                    return null;
                }
                return buildIconTag(PlatformIconGroup.nodesModule()) + "&nbsp;" + XmlStringUtil.escapeString(module.getName());
            }
            else {
                List<OrderEntry> entries = fileIndex.getOrderEntriesForFile(vfile);
                for (OrderEntry order : entries) {
                    if (order instanceof OrderEntryWithTracking) {
                        return buildIconTag(PlatformIconGroup.nodesPplib()) + "&nbsp;" +
                            XmlStringUtil.escapeString(order.getPresentableName());
                    }
                }
            }
        }

        return null;
    }

    private static String buildIconTag(ImageKey imageKey) {
        StringBuilder builder = new StringBuilder("<icon src='");
        builder.append(imageKey.getGroupId());
        builder.append("@");
        builder.append(imageKey.getImageId());
        builder.append("'>");
        return builder.toString();
    }

    private void applyFontProps() {
        Document document = myEditorPane.getDocument();
        if (!(document instanceof StyledDocument)) {
            return;
        }
        String fontName = Registry.is("documentation.component.editor.font") ? EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName() : myEditorPane.getFont().getFontName();

        // changing font will change the doc's CSS as myEditorPane has JEditorPane.HONOR_DISPLAY_PROPERTIES via UIUtil.getHTMLEditorKit
        myEditorPane.setFont(UIUtil.getFontWithFallback(fontName, Font.PLAIN, JBUIScale.scale(getQuickDocFontSize().getSize())));
    }

    @Nullable
    @RequiredReadAction
    private Image getImageByKeyImpl(Object key) {
        if (myManager == null || key == null) {
            return null;
        }
        PsiElement element = getElement();
        if (element == null) {
            return null;
        }
        URL url = (URL) key;
        Image inMemory = myManager.getElementImage(element, url.toExternalForm());
        if (inMemory != null) {
            return inMemory;
        }

        Url parsedUrl = Urls.parseEncoded(url.toExternalForm());
        BuiltInServerManager builtInServerManager = BuiltInServerManager.getInstance();
        if (parsedUrl != null && builtInServerManager.isOnBuiltInWebServer(parsedUrl)) {
            try {
                url = new URL(builtInServerManager.addAuthToken(parsedUrl).toExternalForm());
            }
            catch (MalformedURLException e) {
                LOG.warn(e);
            }
        }
        URL imageUrl = url;
        return Toolkit.getDefaultToolkit().createImage(new RenderableImageProducer(new RenderableImage() {
            private consulo.ui.image.Image myImage;
            private boolean myImageLoaded;

            @Override
            public Vector<RenderableImage> getSources() {
                return null;
            }

            @Override
            public Object getProperty(String name) {
                return null;
            }

            @Override
            public String[] getPropertyNames() {
                return ArrayUtil.EMPTY_STRING_ARRAY;
            }

            @Override
            public boolean isDynamic() {
                return false;
            }

            @Override
            public float getWidth() {
                return getImage().getWidth();
            }

            @Override
            public float getHeight() {
                return getImage().getHeight();
            }

            @Override
            public float getMinX() {
                return 0;
            }

            @Override
            public float getMinY() {
                return 0;
            }

            @Override
            public RenderedImage createScaledRendering(int w, int h, RenderingHints hints) {
                return createDefaultRendering();
            }

            @Override
            public RenderedImage createDefaultRendering() {
                return (RenderedImage) TargetAWT.toAWTImage(getImage());
            }

            @Override
            public RenderedImage createRendering(RenderContext renderContext) {
                return createDefaultRendering();
            }

            private consulo.ui.image.Image getImage() {
                if (!myImageLoaded) {
                    consulo.ui.image.Image image = null;
                    try {
                        image = consulo.ui.image.Image.fromUrl(imageUrl);
                    }
                    catch (IOException e) {
                        LOG.warn(e);
                    }
                    if (image == null) {
                        image = PlatformIconGroup.actionsHelp();
                    }
                    myImage = image;
                    myImageLoaded = true;
                }
                return myImage;
            }
        }, null));
    }

    private void goBack() {
        if (myBackStack.isEmpty()) {
            return;
        }
        Context context = myBackStack.pop();
        myForwardStack.push(saveContext());
        restoreContext(context);
    }

    private void goForward() {
        if (myForwardStack.isEmpty()) {
            return;
        }
        Context context = myForwardStack.pop();
        myBackStack.push(saveContext());
        restoreContext(context);
    }

    private Context saveContext() {
        Rectangle rect = myScrollPane.getViewport().getViewRect();
        return new Context(myElement, myText, myExternalUrl, myProvider, rect, myHighlightedLink);
    }

    @RequiredReadAction
    private void restoreContext(@Nonnull Context context) {
        myExternalUrl = context.externalUrl;
        myProvider = context.provider;
        setDataInternal(context.element, context.text, context.viewRect, null);
        highlightLink(context.highlightedLink);

        if (myManager != null) {
            PsiElement element = context.element.getElement();
            if (element != null) {
                myManager.updateToolWindowTabName(element);
            }
        }
    }

    @RequiredUIAccess
    private void updateControlState() {
        if (needsToolbar()) {
            myToolBar.updateActionsAsync(); // update faster
            setControlPanelVisible();
            removeCornerMenu();
        }
        else {
            myControlPanelVisible = false;
            remove(myControlPanel);
            if (myManager.getToolWindow() != null) {
                return;
            }
            myCornerToolbar.getComponent().setVisible(true);
        }
    }

    public boolean needsToolbar() {
        return myManager.getToolWindow() == null && Registry.is("documentation.show.toolbar");
    }

    private static class WrapperActionGroup extends ActionGroup implements DumbAware, HintManagerImpl.ActionToIgnore {
        private final AnAction[] myActions;

        public WrapperActionGroup(@Nonnull AnAction... actions) {
            myActions = actions;
        }

        @Nonnull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return myActions;
        }
    }

    private static class MyGearActionGroup extends DefaultActionGroup implements HintManagerImpl.ActionToIgnore {
        MyGearActionGroup(@Nonnull AnAction... actions) {
            super(actions);
            setPopup(true);
        }

        @Override
        public boolean showBelowArrow() {
            return false;
        }

        @Nullable
        @Override
        protected consulo.ui.image.Image getTemplateIcon() {
            return PlatformIconGroup.actionsMorevertical();
        }
    }

    private class BackAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        BackAction() {
            super(CodeInsightLocalize.javadocActionBack(), LocalizeValue.empty(), PlatformIconGroup.actionsBack());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            goBack();
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(!myBackStack.isEmpty());
            if (!isToolbar(e)) {
                presentation.setVisible(presentation.isEnabled());
            }
        }
    }

    private class ForwardAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        ForwardAction() {
            super(CodeInsightLocalize.javadocActionForward(), LocalizeValue.empty(), PlatformIconGroup.actionsForward());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            goForward();
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(!myForwardStack.isEmpty());
            if (!isToolbar(e)) {
                presentation.setVisible(presentation.isEnabled());
            }
        }
    }

    private class EditDocumentationSourceAction extends BaseNavigateToSourceAction {

        private EditDocumentationSourceAction() {
            super(true);
            getTemplatePresentation().setIcon(AllIcons.Actions.EditSource);
            getTemplatePresentation().setText("Edit Source");
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            super.actionPerformed(e);
            JBPopup hint = myHint;
            if (hint != null && hint.isVisible()) {
                hint.cancel();
            }
        }

        @Nullable
        @Override
        @RequiredReadAction
        protected Navigatable[] getNavigatables(DataContext dataContext) {
            SmartPsiElementPointer<PsiElement> element = myElement;
            if (element != null) {
                PsiElement psiElement = element.getElement();
                return psiElement instanceof Navigatable ? new Navigatable[]{(Navigatable) psiElement} : null;
            }
            return null;
        }
    }

    private static boolean isToolbar(@Nonnull AnActionEvent e) {
        return ActionPlaces.JAVADOC_TOOLBAR.equals(e.getPlace());
    }


    private class ExternalDocAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        private ExternalDocAction() {
            super(CodeInsightLocalize.javadocActionViewExternal(), LocalizeValue.empty(), PlatformIconGroup.actionsPreviousoccurence());
            registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_EXTERNAL_JAVADOC).getShortcutSet(), null);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (myElement == null) {
                return;
            }

            PsiElement element = myElement.getElement();
            PsiElement originalElement = DocumentationManagerHelper.getOriginalElement(element);

            ExternalJavaDocAction.showExternalJavadoc(element, originalElement, myExternalUrl, e.getDataContext());
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(hasExternalDoc());
        }
    }

    @RequiredReadAction
    private boolean hasExternalDoc() {
        boolean enabled = false;
        if (myElement != null && myProvider != null) {
            PsiElement element = myElement.getElement();
            PsiElement originalElement = DocumentationManagerHelper.getOriginalElement(element);
            enabled = element != null && CompositeDocumentationProvider.hasUrlsFor(myProvider, element, originalElement);
        }
        return enabled;
    }

    private void registerActions() {
        // With screen readers, we want the default keyboard behavior inside
        // the document text editor, i.e. the caret moves with cursor keys, etc.
        if (!ScreenReader.isActive()) {
            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), e -> {
                JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
                int value = scrollBar.getValue() - scrollBar.getUnitIncrement(-1);
                value = Math.max(value, 0);
                scrollBar.setValue(value);
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), e -> {
                JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
                int value = scrollBar.getValue() + scrollBar.getUnitIncrement(+1);
                value = Math.min(value, scrollBar.getMaximum());
                scrollBar.setValue(value);
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), e -> {
                JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
                int value = scrollBar.getValue() - scrollBar.getUnitIncrement(-1);
                value = Math.max(value, 0);
                scrollBar.setValue(value);
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), e -> {
                JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
                int value = scrollBar.getValue() + scrollBar.getUnitIncrement(+1);
                value = Math.min(value, scrollBar.getMaximum());
                scrollBar.setValue(value);
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), e -> {
                JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
                int value = scrollBar.getValue() - scrollBar.getBlockIncrement(-1);
                value = Math.max(value, 0);
                scrollBar.setValue(value);
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), e -> {
                JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
                int value = scrollBar.getValue() + scrollBar.getBlockIncrement(+1);
                value = Math.min(value, scrollBar.getMaximum());
                scrollBar.setValue(value);
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), e -> {
                JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
                scrollBar.setValue(0);
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), e -> {
                JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
                scrollBar.setValue(scrollBar.getMaximum());
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_MASK), e -> {
                JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
                scrollBar.setValue(0);
            });

            myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_MASK), e -> {
                JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
                scrollBar.setValue(scrollBar.getMaximum());
            });
        }
    }

    public String getText() {
        return myText;
    }

    public String getDecoratedText() {
        return myDecoratedText;
    }

    @Override
    public void dispose() {
        myEditorPane.getCaret().setVisible(false); // Caret, if blinking, has to be deactivated.
        myBackStack.clear();
        myForwardStack.clear();
        myKeyboardActions.clear();
        myElement = null;
        myManager = null;
        myHint = null;
    }

    private int getLinkCount() {
        HTMLDocument document = (HTMLDocument) myEditorPane.getDocument();
        int linkCount = 0;
        for (HTMLDocument.Iterator it = document.getIterator(HTML.Tag.A); it.isValid(); it.next()) {
            if (it.getAttributes().isDefined(HTML.Attribute.HREF)) {
                linkCount++;
            }
        }
        return linkCount;
    }

    @Nullable
    private HTMLDocument.Iterator getLink(int n) {
        if (n >= 0) {
            HTMLDocument document = (HTMLDocument) myEditorPane.getDocument();
            int linkCount = 0;
            for (HTMLDocument.Iterator it = document.getIterator(HTML.Tag.A); it.isValid(); it.next()) {
                if (it.getAttributes().isDefined(HTML.Attribute.HREF) && linkCount++ == n) {
                    return it;
                }
            }
        }
        return null;
    }

    private void highlightLink(int n) {
        myHighlightedLink = n;
        Highlighter highlighter = myEditorPane.getHighlighter();
        HTMLDocument.Iterator link = getLink(n);
        if (link != null) {
            int startOffset = link.getStartOffset();
            int endOffset = link.getEndOffset();
            try {
                if (myHighlightingTag == null) {
                    myHighlightingTag = highlighter.addHighlight(startOffset, endOffset, LINK_HIGHLIGHTER);
                }
                else {
                    highlighter.changeHighlight(myHighlightingTag, startOffset, endOffset);
                }
                myEditorPane.setCaretPosition(startOffset);
                if (!ScreenReader.isActive()) {
                    // scrolling to target location explicitly, as we've disabled auto-scrolling to caret
                    myEditorPane.scrollRectToVisible(myEditorPane.modelToView(startOffset));
                }
            }
            catch (BadLocationException e) {
                LOG.warn("Error highlighting link", e);
            }
        }
        else if (myHighlightingTag != null) {
            highlighter.removeHighlight(myHighlightingTag);
            myHighlightingTag = null;
        }
    }

    private void activateLink(int n) {
        HTMLDocument.Iterator link = getLink(n);
        if (link != null) {
            String href = (String) link.getAttributes().getAttribute(HTML.Attribute.HREF);
            myManager.navigateByLink(this, href);
        }
    }

    private static class Context {
        final SmartPsiElementPointer<PsiElement> element;
        final String text;
        final String externalUrl;
        final DocumentationProvider provider;
        final Rectangle viewRect;
        final int highlightedLink;

        Context(SmartPsiElementPointer<PsiElement> element, String text, String externalUrl, DocumentationProvider provider, Rectangle viewRect, int highlightedLink) {
            this.element = element;
            this.text = text;
            this.externalUrl = externalUrl;
            this.provider = provider;
            this.viewRect = viewRect;
            this.highlightedLink = highlightedLink;
        }

        @Nonnull
        Context withText(@Nonnull String text) {
            return new Context(element, text, externalUrl, provider, viewRect, highlightedLink);
        }
    }

    private class MyShowSettingsAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        private final boolean myOnToolbar;

        MyShowSettingsAction(boolean onToolbar) {
            super(LocalizeValue.localizeTODO("Adjust font size..."));
            myOnToolbar = onToolbar;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            if (myManager == null || myOnToolbar && myManager.getToolWindow() != null) {
                e.getPresentation().setEnabledAndVisible(false);
            }
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(mySettingsPanel, myFontSizeSlider).createPopup();
            setFontSizeSliderSize(getQuickDocFontSize());
            mySettingsPanel.setVisible(true);
            Point location = MouseInfo.getPointerInfo().getLocation();
            popup.show(new RelativePoint(new Point(
                location.x - mySettingsPanel.getPreferredSize().width / 2,
                location.y - mySettingsPanel.getPreferredSize().height / 2
            )));
        }
    }

    private abstract static class MyDictionary<K, V> extends Dictionary<K, V> {
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration<K> keys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration<V> elements() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }
    }

    private class PreviousLinkAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            int linkCount = getLinkCount();
            if (linkCount <= 0) {
                return;
            }
            highlightLink(myHighlightedLink < 0 ? (linkCount - 1) : (myHighlightedLink + linkCount - 1) % linkCount);
        }
    }

    private class NextLinkAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            int linkCount = getLinkCount();
            if (linkCount <= 0) {
                return;
            }
            highlightLink((myHighlightedLink + 1) % linkCount);
        }
    }

    private class ActivateLinkAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            activateLink(myHighlightedLink);
        }
    }

    private static class LinkHighlighter implements Highlighter.HighlightPainter {
        private static final Stroke STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{1}, 0);

        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            try {
                Rectangle target = c.getUI().getRootView(c).modelToView(p0, Position.Bias.Forward, p1, Position.Bias.Backward, bounds).getBounds();
                Graphics2D g2d = (Graphics2D) g.create();
                try {
                    g2d.setStroke(STROKE);
                    g2d.setColor(c.getSelectionColor());
                    g2d.drawRect(target.x, target.y, target.width - 1, target.height - 1);
                }
                finally {
                    g2d.dispose();
                }
            }
            catch (Exception e) {
                LOG.warn("Error painting link highlight", e);
            }
        }
    }

    private class ShowToolbarAction extends ToggleAction implements HintManagerImpl.ActionToIgnore {
        ShowToolbarAction() {
            super(LocalizeValue.localizeTODO("Show Toolbar"));
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            if (myManager == null || myManager.getToolWindow() != null) {
                e.getPresentation().setEnabledAndVisible(false);
            }
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return Registry.get("documentation.show.toolbar").asBoolean();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            Registry.get("documentation.show.toolbar").setValue(state);
            updateControlState();
            showHint();
        }
    }

    private class MyScrollPane extends JBScrollPane {
        MyScrollPane() {
            super(myEditorPane, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
            setLayout(new ScrollPaneLayout() {
                @Override
                public void layoutContainer(Container parent) {
                    super.layoutContainer(parent);
                    if (!myCornerToolbar.getComponent().isVisible()) {
                        return;
                    }
                    if (vsb != null) {
                        Rectangle bounds = vsb.getBounds();
                        vsb.setBounds(bounds.x, bounds.y, bounds.width, bounds.height - myCornerToolbar.getComponent().getPreferredSize().height - 3);
                    }
                    if (hsb != null) {
                        Rectangle bounds = hsb.getBounds();
                        int vsbOffset = vsb != null ? vsb.getBounds().width : 0;
                        hsb.setBounds(bounds.x, bounds.y, bounds.width - myCornerToolbar.getComponent().getPreferredSize().width - 3 + vsbOffset, bounds.height);
                    }
                }
            });
        }

        @Override
        public Border getViewportBorder() {
            return null;
        }

        @Override
        protected void processMouseWheelEvent(MouseWheelEvent e) {
            if (!EditorSettingsExternalizable.getInstance().isWheelFontChangeEnabled() || !EditorUtil.isChangeFontSize(e)) {
                super.processMouseWheelEvent(e);
                return;
            }

            int rotation = e.getWheelRotation();
            if (rotation == 0) {
                return;
            }
            int change = Math.abs(rotation);
            boolean increase = rotation <= 0;
            FontSize newFontSize = getQuickDocFontSize();
            for (; change > 0; change--) {
                if (increase) {
                    newFontSize = newFontSize.larger();
                }
                else {
                    newFontSize = newFontSize.smaller();
                }
            }

            if (newFontSize == getQuickDocFontSize()) {
                return;
            }

            setQuickDocFontSize(newFontSize);
            applyFontProps();
            setFontSizeSliderSize(newFontSize);
        }
    }

    private class ShowAsToolwindowAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        ShowAsToolwindowAction() {
            super(LocalizeValue.localizeTODO("Open as Tool Window"));
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            if (myManager == null) {
                presentation.setEnabledAndVisible(false);
            }
            else {
                presentation.setIcon(ToolWindowManagerEx.getInstanceEx(myManager.getProject())
                    .getLocationIcon(ToolWindowId.DOCUMENTATION, consulo.ui.image.Image.empty(16)));
                presentation.setEnabledAndVisible(myToolwindowCallback != null);
            }
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myToolwindowCallback.run();
        }
    }

    private class RestoreDefaultSizeAction extends AnAction implements HintManagerImpl.ActionToIgnore {
        RestoreDefaultSizeAction() {
            super(LocalizeValue.localizeTODO("Restore Size"));
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(myHint != null && (myManuallyResized || myHint.getDimensionServiceKey() != null));
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myManuallyResized = false;
            if (myStoreSize) {
                DimensionService.getInstance().setSize(DocumentationManagerHelper.NEW_JAVADOC_LOCATION_AND_SIZE, null, myManager.getProject());
                myHint.setDimensionServiceKey(null);
            }
            showHint();
        }
    }

    private class MyScalingImageView extends ImageView {
        private MyScalingImageView(Element elem) {
            super(elem);
        }

        @Override
        public float getMaximumSpan(int axis) {
            return super.getMaximumSpan(axis) / JBUIScale.sysScale(myEditorPane);
        }

        @Override
        public float getMinimumSpan(int axis) {
            return super.getMinimumSpan(axis) / JBUIScale.sysScale(myEditorPane);
        }

        @Override
        public float getPreferredSpan(int axis) {
            return super.getPreferredSpan(axis) / JBUIScale.sysScale(myEditorPane);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            Rectangle bounds = a.getBounds();
            int width = (int) super.getPreferredSpan(View.X_AXIS);
            int height = (int) super.getPreferredSpan(View.Y_AXIS);
            if (width <= 0 || height <= 0) {
                return;
            }
            @SuppressWarnings("UndesirableClassUsage") BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            super.paint(graphics, new Rectangle(image.getWidth(), image.getHeight()));
            UIUtil.drawImage(g, ImageUtil.ensureHiDPI(image, JBUI.ScaleContext.create(myEditorPane)), bounds.x, bounds.y, null);
        }
    }

    private static class MyIconView extends View {
        private final consulo.ui.image.Image myViewIcon;

        private MyIconView(Element elem, consulo.ui.image.Image viewIcon) {
            super(elem);
            myViewIcon = viewIcon;
        }

        @Override
        public float getPreferredSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return myViewIcon.getWidth();
                case View.Y_AXIS:
                    return myViewIcon.getHeight();
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }

        @Override
        public String getToolTipText(float x, float y, Shape allocation) {
            return (String) super.getElement().getAttributes().getAttribute(HTML.Attribute.ALT);
        }

        @Override
        public void paint(Graphics g, Shape allocation) {
            TargetAWT.to(myViewIcon).paintIcon(null, g, allocation.getBounds().x, allocation.getBounds().y - 4);
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            int p0 = getStartOffset();
            int p1 = getEndOffset();
            if ((pos >= p0) && (pos <= p1)) {
                Rectangle r = a.getBounds();
                if (pos == p1) {
                    r.x += r.width;
                }
                r.width = 0;
                return r;
            }
            throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
        }

        @Override
        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            Rectangle alloc = (Rectangle) a;
            if (x < alloc.x + (alloc.width / 2f)) {
                bias[0] = Position.Bias.Forward;
                return getStartOffset();
            }
            bias[0] = Position.Bias.Backward;
            return getEndOffset();
        }
    }
}

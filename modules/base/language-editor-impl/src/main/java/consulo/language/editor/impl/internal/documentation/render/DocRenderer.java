// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.CustomFoldRegionRenderer;
import consulo.codeEditor.Editor;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.documentation.DocumentationManagerProtocol;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.color.ColorValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.MathUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Paints a documentation comment as rendered HTML inside a {@link CustomFoldRegion}.
 */
public class DocRenderer implements CustomFoldRegionRenderer {
    private static final Logger LOG = Logger.getInstance(DocRenderer.class);
    private static final Key<EditorInlineHtmlPane> CACHED_LOADING_PANE = Key.create("cached.loading.pane");

    private static final int MIN_WIDTH = 350;
    private static final int MAX_WIDTH = 680;

    private static final int LEFT_INSET = 14;
    private static final int RIGHT_INSET = 12;
    private static final int TOP_BOTTOM_INSETS = 2;
    private static final int TOP_BOTTOM_MARGINS = 4;
    private static final int LINE_WIDTH = 2;
    private static final int ARC_RADIUS = 5;

    private static final Color INLINE_CODE_FALLBACK_BACKGROUND = new Color(0x5A5D6B);

    private final DocRenderItem myItem;

    private EditorInlineHtmlPane myPane;
    private int myCachedWidth = -1;
    private int myCachedHeight = -1;
    private boolean myContentUpdateNeeded;

    public DocRenderer(DocRenderItem item) {
        myItem = item;
    }

    public DocRenderItem getItem() {
        return myItem;
    }

    void update(boolean updateSize, boolean updateContent, @Nullable List<Runnable> foldingTasks) {
        CustomFoldRegion foldRegion = myItem.getFoldRegion();
        if (foldRegion != null) {
            if (updateSize) {
                myCachedWidth = -1;
                myCachedHeight = -1;
            }
            myContentUpdateNeeded = updateContent;
            Runnable task = foldRegion::update;
            if (foldingTasks == null) {
                task.run();
            }
            else {
                foldingTasks.add(task);
            }
        }
    }

    @Override
    public int calcWidthInPixels(CustomFoldRegion region) {
        if (myCachedWidth < 0) {
            return myCachedWidth = calcWidth(region.getEditor());
        }
        return myCachedWidth;
    }

    @Override
    public int calcHeightInPixels(CustomFoldRegion region) {
        if (myCachedHeight < 0) {
            Editor editor = region.getEditor();
            int indent = 0;
            // optimize editor opening: skip 'proper' width calculation for 'Loading...' inlays
            if (myItem.getTextToRender() != null) {
                indent = calcInlayStartX() - editor.getInsets().left;
            }
            int width = Math.max(0, calcWidth(editor) - indent - scale(LEFT_INSET) - scale(RIGHT_INSET));
            JComponent component = getRendererComponent(editor, width, null);
            return myCachedHeight = Math.max(editor.getLineHeight(),
                component.getPreferredSize().height + scale(TOP_BOTTOM_INSETS) * 2 + scale(TOP_BOTTOM_MARGINS) * 2);
        }
        return myCachedHeight;
    }

    @Override
    public void paint(CustomFoldRegion region, Graphics2D g, Rectangle2D r, TextAttributes textAttributes) {
        int startX = calcInlayStartX();
        int endX = (int) r.getX() + (int) r.getWidth();
        if (startX >= endX) {
            return;
        }
        int margin = scale(TOP_BOTTOM_MARGINS);
        int filledHeight = (int) r.getHeight() - margin * 2;
        if (filledHeight <= 0) {
            return;
        }
        int filledStartY = (int) r.getY() + margin;

        Editor editor = region.getEditor();
        Color defaultBgColor = TargetAWT.to(editor.getColorsScheme().getDefaultBackground());
        Color currentBgColor = TargetAWT.to(textAttributes.getBackgroundColor());
        Color bgColor = currentBgColor == null ? defaultBgColor : ColorUtil.mix(defaultBgColor, currentBgColor, .5);
        if (myPane != null) {
            Color selectionColor = TargetAWT.to(editor.getSelectionModel().getTextAttributes().getBackgroundColor());
            if (selectionColor != null) {
                myPane.setSelectionColor(selectionColor);
            }
        }
        if (currentBgColor != null) {
            g.setColor(bgColor);
            int arcDiameter = ARC_RADIUS * 2;
            if (endX - startX >= arcDiameter) {
                g.fillRect(startX, filledStartY, endX - startX - ARC_RADIUS, filledHeight);
                Object savedHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.fillRoundRect(endX - arcDiameter, filledStartY, arcDiameter, filledHeight, arcDiameter, arcDiameter);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedHint);
            }
            else {
                g.fillRect(startX, filledStartY, endX - startX, filledHeight);
            }
        }
        Color guideColor = TargetAWT.to(editor.getColorsScheme().getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_GUIDE));
        g.setColor(guideColor == null ? bgColor : guideColor);
        g.fillRect(startX, filledStartY, scale(LINE_WIDTH), filledHeight);

        int topBottomInset = scale(TOP_BOTTOM_INSETS);
        int componentWidth = endX - startX - scale(LEFT_INSET) - scale(RIGHT_INSET);
        int componentHeight = filledHeight - topBottomInset * 2;
        if (componentWidth > 0 && componentHeight > 0) {
            EditorInlineHtmlPane component = getRendererComponent(editor, componentWidth, bgColor);
            Graphics dg = g.create(startX + scale(LEFT_INSET), filledStartY + topBottomInset, componentWidth, componentHeight);
            UISettingsUtil.setupAntialiasing(dg);
            component.paint(dg);
            dg.dispose();
        }
    }

    @Override
    public @Nullable GutterIconRenderer calcGutterIconRenderer(CustomFoldRegion region) {
        assert myItem.getFoldRegion() == region || myItem.getFoldRegion() == null;
        return myItem.calcFoldingGutterIconRenderer();
    }

    @Override
    public ActionGroup getContextMenuGroup(CustomFoldRegion region) {
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(new CopySelection());
        group.addSeparator();
        group.add(new ToggleRenderingAction(myItem));
        AnAction toggleRenderAllAction = ActionManager.getInstance().getAction("ToggleRenderedDocPresentationForAll");
        if (toggleRenderAllAction != null) {
            group.add(toggleRenderAllAction);
        }
        return group.build();
    }

    private static int scale(int value) {
        return (int) (value * UISettingsUtil.getDefFontScale());
    }

    static int calcWidth(Editor editor) {
        int availableWidth = editor.getScrollingModel().getVisibleArea().width;
        if (availableWidth <= 0) {
            // if editor is not shown yet, we create the inlay with maximum possible width,
            // assuming that there's a higher probability that editor will be shown with larger width than with smaller width
            return MAX_WIDTH;
        }
        return MathUtil.clamp(availableWidth, scale(MIN_WIDTH), scale(MAX_WIDTH));
    }

    private int calcInlayStartX() {
        Editor editor = myItem.getEditor();
        RangeHighlighter highlighter = myItem.getHighlighter();
        if (highlighter.isValid()) {
            Document document = editor.getDocument();
            int nextLineNumber = document.getLineNumber(highlighter.getEndOffset()) + 1;
            if (nextLineNumber < document.getLineCount()) {
                int lineStartOffset = document.getLineStartOffset(nextLineNumber);
                int contentStartOffset = CharArrayUtil.shiftForward(document.getImmutableCharSequence(), lineStartOffset, " \t\n");
                return editor.offsetToXY(contentStartOffset, false, true).x;
            }
        }
        return editor.getInsets().left;
    }

    Rectangle getEditorPaneBoundsWithinRenderer(int width, int height) {
        int relativeX = calcInlayStartX() - myItem.getEditor().getInsets().left + scale(LEFT_INSET);
        int relativeY = scale(TOP_BOTTOM_MARGINS) + scale(TOP_BOTTOM_INSETS);
        return new Rectangle(relativeX, relativeY, width - relativeX - scale(RIGHT_INSET), height - relativeY * 2);
    }

    EditorInlineHtmlPane getRendererComponent(Editor editor, int width, @Nullable Color backgroundColor) {
        boolean newInstance = false;
        EditorInlineHtmlPane pane = myPane;
        if (pane == null || myContentUpdateNeeded) {
            myContentUpdateNeeded = false;
            clearCachedComponent();
            if (myItem.getTextToRender() == null) {
                pane = getLoadingPane(editor);
            }
            else {
                myPane = pane = createEditorPane(editor, myItem.getTextToRender(), backgroundColor, false);
                newInstance = true;
            }
        }
        GuiUtils.targetToDevice(pane, editor.getContentComponent());
        pane.setSize(width, 10_000_000 /* Arbitrary large value, that doesn't lead to overflows and precision loss */);
        if (newInstance) {
            // trigger internal layout, so that image elements are created
            // this is done after 'targetToDevice' call to take correct graphics context into account
            pane.getPreferredSize();
        }
        else if (backgroundColor != null && pane.getBackground().getRGB() != backgroundColor.getRGB()) {
            pane.setBackground(backgroundColor);
            // trigger CSS styles update
            pane.getPreferredSize();
        }
        return pane;
    }

    private EditorInlineHtmlPane getLoadingPane(Editor editor) {
        EditorInlineHtmlPane pane = editor.getUserData(CACHED_LOADING_PANE);
        if (pane == null) {
            editor.putUserData(CACHED_LOADING_PANE, pane = createEditorPane(
                editor, CodeInsightLocalize.docRenderLoadingText().get(), null, true));
        }
        return pane;
    }

    static void clearCachedLoadingPane(Editor editor) {
        editor.putUserData(CACHED_LOADING_PANE, null);
    }

    private EditorInlineHtmlPane createEditorPane(Editor editor,
                                                  @Nullable String text,
                                                  @Nullable Color backgroundColor,
                                                  boolean reusable) {
        EditorInlineHtmlPane pane = new EditorInlineHtmlPane();
        pane.getCaret().setSelectionVisible(!reusable);
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        HTMLEditorKit editorKit = JBHtmlEditorKit.create(true);
        applyStyleSheet(editorKit, editor);
        pane.setEditorKit(editorKit);
        pane.setContentType(UIUtil.HTML_MIME);
        EditorColorsScheme scheme = editor.getColorsScheme();
        Color textColor = getTextColor(scheme);
        pane.setForeground(textColor);
        pane.setSelectedTextColor(textColor);
        pane.setBackground(backgroundColor != null ? backgroundColor : TargetAWT.to(scheme.getDefaultBackground()));
        UIUtil.enableEagerSoftWrapping(pane);
        pane.putClientProperty(JBHtmlEditorKit.InlineCodeStyle.class, new JBHtmlEditorKit.InlineCodeStyle(
            inlineCodeBackground(scheme), scale(4), scale(1), scale(10)));
        pane.setText(text == null ? "" : text);
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                activateLink(editor, e.getDescription(), linkAnchor(e));
            }
        });
        return pane;
    }

    /**
     * Position of the activated link, in editor content component coordinates, so the popup can be anchored under it
     * instead of under the caret.
     */
    private @Nullable RelativePoint linkAnchor(HyperlinkEvent event) {
        CustomFoldRegion foldRegion = myItem.getFoldRegion();
        Element sourceElement = event.getSourceElement();
        if (foldRegion == null || sourceElement == null || !(event.getSource() instanceof JEditorPane pane)) {
            return null;
        }
        Point rendererLocation = foldRegion.getLocation();
        if (rendererLocation == null) {
            return null;
        }
        Rectangle2D locationInPane;
        try {
            locationInPane = pane.modelToView2D(sourceElement.getStartOffset());
        }
        catch (BadLocationException e) {
            return null;
        }
        if (locationInPane == null) {
            return null;
        }
        Rectangle relativeBounds = getEditorPaneBoundsWithinRenderer(foldRegion.getWidthInPixels(), foldRegion.getHeightInPixels());
        Point point = new Point(
            rendererLocation.x + relativeBounds.x + (int) locationInPane.getX(),
            rendererLocation.y + relativeBounds.y + (int) Math.ceil(locationInPane.getMaxY())
        );
        return new RelativePoint(myItem.getEditor().getContentComponent(), point);
    }

    @RequiredUIAccess
    private void activateLink(Editor editor, @Nullable String url, @Nullable RelativePoint popupAnchor) {
        Project project = editor.getProject();
        if (project == null || url == null || !url.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) {
            return;
        }

        RangeHighlighter highlighter = myItem.getHighlighter();
        if (!highlighter.isValid()) {
            return;
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        PsiElement context = file == null ? null : file.findElementAt(highlighter.getStartOffset());
        if (context == null) {
            return;
        }

        String refText = url.substring(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL.length());
        int separatorPos = refText.lastIndexOf(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL_REF_SEPARATOR);
        if (separatorPos >= 0) {
            refText = refText.substring(0, separatorPos);
        }

        PsiElement target = resolveLink(context, refText);
        if (target == null) {
            return;
        }

        if (isGotoDeclarationEvent() && target instanceof Navigatable navigatable && navigatable.canNavigate()) {
            navigatable.navigate(true);
        }
        else {
            DocumentationManager.getInstance(project).showJavaDocInfo(editor, target, context, popupAnchor);
        }
    }

    private static boolean isGotoDeclarationEvent() {
        KeymapManager keymapManager = KeymapManager.getInstance();
        if (keymapManager == null || !(IdeEventQueueProxy.getInstance().getTrueCurrentEvent() instanceof MouseEvent mouseEvent)) {
            return false;
        }
        int clickCount = mouseEvent.getClickCount();
        if (clickCount < 1) {
            // the queue can hand back a move/drag event, which MouseShortcut rejects
            return false;
        }
        int button = MouseShortcut.getButton(mouseEvent);
        int modifiers = button == MouseShortcut.BUTTON_WHEEL_UP || button == MouseShortcut.BUTTON_WHEEL_DOWN
            ? mouseEvent.getModifiers()
            : mouseEvent.getModifiersEx();
        MouseShortcut shortcut = new MouseShortcut(button, modifiers, clickCount);
        for (String actionId : keymapManager.getActiveKeymap().getActionIds(shortcut)) {
            if (IdeActions.ACTION_GOTO_DECLARATION.equals(actionId)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable PsiElement resolveLink(PsiElement context, String refText) {
        PsiManager manager = context.getManager();
        PsiElement target =
            DocumentationManagerHelper.getProviderFromElement(context).getDocumentationElementForLink(manager, refText, context);
        if (target != null) {
            return target;
        }
        for (Language language : Language.getRegisteredLanguages()) {
            DocumentationProvider provider = LanguageDocumentationProvider.forLanguageComposite(language);
            if (provider != null) {
                target = provider.getDocumentationElementForLink(manager, refText, context);
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    private static Color getTextColor(EditorColorsScheme scheme) {
        TextAttributes attributes = scheme.getAttributes(DefaultLanguageHighlighterColors.DOC_COMMENT);
        ColorValue color = attributes == null ? null : attributes.getForegroundColor();
        return TargetAWT.to(color == null ? scheme.getDefaultForeground() : color);
    }

    private static void applyStyleSheet(HTMLEditorKit editorKit, Editor editor) {
        EditorColorsScheme colorsScheme = editor.getColorsScheme();
        Color textColor = getTextColor(colorsScheme);
        ColorValue linkColorValue = colorsScheme.getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_LINK);
        Color linkColor = linkColorValue == null ? textColor : TargetAWT.to(linkColorValue);
        StyleSheet styleSheet = editorKit.getStyleSheet();
        styleSheet.addRule("body {overflow-wrap: anywhere; padding-top: " + scale(2) + "px; color: #"
            + ColorUtil.toHex(textColor) + "}");
        styleSheet.addRule("pre {white-space: pre-wrap}");
        styleSheet.addRule("a {color: #" + ColorUtil.toHex(linkColor) + "; text-decoration: none}");
        styleSheet.addRule(".sections {border-spacing: 0}");
        styleSheet.addRule(".section {padding-right: " + scale(5) + "; white-space: nowrap}");
        styleSheet.addRule(inlineCodeRule(colorsScheme, textColor));
    }

    private static Color inlineCodeBackground(EditorColorsScheme colorsScheme) {
        TextAttributes attributes = colorsScheme.getAttributes(DefaultLanguageHighlighterColors.DOC_CODE_INLINE);
        ColorValue background = attributes == null ? null : attributes.getBackgroundColor();
        return background == null
            ? ColorUtil.mix(TargetAWT.to(colorsScheme.getDefaultBackground()), INLINE_CODE_FALLBACK_BACKGROUND, .1)
            : TargetAWT.to(background);
    }

    private static String inlineCodeRule(EditorColorsScheme colorsScheme, Color textColor) {
        TextAttributes attributes = colorsScheme.getAttributes(DefaultLanguageHighlighterColors.DOC_CODE_INLINE);
        ColorValue background = attributes == null ? null : attributes.getBackgroundColor();
        ColorValue foreground = attributes == null ? null : attributes.getForegroundColor();

        Color backgroundColor = background == null
            ? ColorUtil.mix(TargetAWT.to(colorsScheme.getDefaultBackground()), INLINE_CODE_FALLBACK_BACKGROUND, .1)
            : TargetAWT.to(background);
        Color foregroundColor = foreground == null ? textColor : TargetAWT.to(foreground);

        return "code {color: #" + ColorUtil.toHex(foregroundColor) + "}";
    }

    void clearCachedComponent() {
        myPane = null;
    }

    public void dispose() {
        clearCachedComponent();
    }

    final class EditorInlineHtmlPane extends JEditorPane {
        private final AtomicBoolean myUpdateScheduled = new AtomicBoolean();
        private final AtomicBoolean myRepaintScheduled = new AtomicBoolean();

        private boolean myRepaintRequested;

        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
            myRepaintRequested = true;
        }

        void doWithRepaintTracking(Runnable task) {
            myRepaintRequested = false;
            task.run();
            if (myRepaintRequested) {
                repaintRenderer();
            }
        }

        private void repaintRenderer() {
            CustomFoldRegion foldRegion = myItem.getFoldRegion();
            if (foldRegion != null) {
                foldRegion.repaint();
            }
        }

        Editor getEditor() {
            return myItem.getEditor();
        }

        void removeSelection() {
            doWithRepaintTracking(() -> select(0, 0));
        }

        boolean hasSelection() {
            return getSelectionStart() != getSelectionEnd();
        }

        @Nullable
        Point getSelectionPositionInEditor() {
            if (myPane != this) {
                return null;
            }
            CustomFoldRegion foldRegion = myItem.getFoldRegion();
            if (foldRegion == null || foldRegion.getRenderer() != DocRenderer.this) {
                return null;
            }
            Point rendererLocation = foldRegion.getLocation();
            if (rendererLocation == null) {
                return null;
            }
            Rectangle boundsWithinRenderer =
                getEditorPaneBoundsWithinRenderer(foldRegion.getWidthInPixels(), foldRegion.getHeightInPixels());
            Rectangle2D locationInPane;
            try {
                locationInPane = modelToView2D(getSelectionStart());
            }
            catch (BadLocationException e) {
                LOG.error(e);
                locationInPane = new Rectangle();
            }
            return new Point(rendererLocation.x + boundsWithinRenderer.x + (int) locationInPane.getX(),
                rendererLocation.y + boundsWithinRenderer.y + (int) locationInPane.getY());
        }

        @Override
        public void revalidate() {
            super.revalidate();
            myCachedHeight = -1;
            myCachedWidth = -1;
            scheduleUpdate();
        }

        private void scheduleUpdate() {
            if (myUpdateScheduled != null && myUpdateScheduled.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(() -> {
                    myRepaintScheduled.set(false);
                    myUpdateScheduled.set(false);
                    if (this == myPane) {
                        CustomFoldRegion foldRegion = myItem.getFoldRegion();
                        if (foldRegion != null) {
                            DocRenderItemUpdater.getInstance().updateFoldRegions(Collections.singleton(foldRegion), false);
                        }
                    }
                });
            }
        }

        private void scheduleRepaint() {
            if (!myUpdateScheduled.get() && myRepaintScheduled.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(() -> {
                    myRepaintScheduled.set(false);
                    if (this == myPane) {
                        repaintRenderer();
                    }
                });
            }
        }
    }

    private final class CopySelection extends DumbAwareAction implements AnActionWithSyncUpdate {
        CopySelection() {
            super(CodeInsightLocalize.docRenderCopyActionText(), LocalizeValue.empty(), PlatformIconGroup.actionsCopy());
            AnAction copyAction = ActionManager.getInstance().getAction(IdeActions.ACTION_COPY);
            if (copyAction != null) {
                copyShortcutFrom(copyAction);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setVisible(myPane != null && myPane.hasSelection());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            String text = myPane == null ? null : myPane.getSelectedText();
            if (!StringUtil.isEmpty(text)) {
                CopyPasteManager.getInstance().setText(text);
            }
        }
    }

    static final class ToggleRenderingAction extends DumbAwareAction {
        private final DocRenderItem item;

        ToggleRenderingAction(DocRenderItem i) {
            copyFrom(ActionManager.getInstance().getAction("ToggleRenderedDocPresentation"));
            item = i;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            item.toggle();
        }
    }
}

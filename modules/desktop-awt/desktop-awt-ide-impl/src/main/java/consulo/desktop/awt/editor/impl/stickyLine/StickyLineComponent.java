// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.stickyLine;

import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.ScrollType;
import consulo.desktop.awt.editor.impl.DesktopEditorImpl;
import consulo.desktop.awt.editor.impl.EditorGutterComponentImpl;
import consulo.document.DocCommandGroupId;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUIUtil;
import consulo.language.Language;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.MouseEventAdapter;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.util.dataholder.Key;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class StickyLineComponent extends JComponent {
    private final EditorEx editor;
    private int primaryVisualLine = -1;
    private int scopeVisualLine = -1;
    private int offsetOnClick = -1;
    private String debugText = null;
    private BufferedImage dumbTextImage = null;
    private boolean isHovered = false;
    private final StickyMouseListener mouseListener = new StickyMouseListener();

    public StickyLineComponent(EditorEx editor) {
        this.editor = editor;
        setBorder(null);
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        addMouseWheelListener(mouseListener);
    }

    public void setLine(int primaryVisualLine, int scopeVisualLine, int offsetOnClick, String debugText) {
        this.primaryVisualLine = primaryVisualLine;
        this.scopeVisualLine = scopeVisualLine;
        this.offsetOnClick = offsetOnClick;
        this.debugText = debugText;
        this.dumbTextImage = null;
        this.isHovered = false;
        this.mouseListener.isPopup = false;
        this.mouseListener.isGutterHovered = false;
    }

    public void resetLine() {
        setLine(-1, -1, -1, null);
    }

    public boolean isEmpty() {
        return primaryVisualLine == -1 || scopeVisualLine == -1 || offsetOnClick == -1;
    }

    public void startDumb() {
        paintStickyLine(null);
    }

    public void repaintIfInRange(int startVisualLine, int endVisualLine) {
        if (primaryVisualLine >= startVisualLine && primaryVisualLine <= endVisualLine) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintStickyLine(g);
    }

    private void paintStickyLine(Graphics graphicsOrDumb) {
        if (isEmpty()) {
            throw new AssertionError("sticky panel should mark this line as not visible");
        }
        int editorY = editorY();
        int lineHeight = lineHeight();
        int[] widths = gutterAndTextWidth();
        int gutterWidth = widths[0];
        int textWidth = widths[1];
        ColorValue editorBackground = editor.getBackgroundColor();
        boolean isBackgroundChanged = false;
        ((DesktopEditorImpl) editor).setStickyLinePainting(true);
        try {
            isBackgroundChanged = setStickyLineBackgroundColor();
            if (graphicsOrDumb != null) {
                int editorStartY = isLineOutOfPanel() ? editorY + getY() : editorY;
                graphicsOrDumb.translate(0, -editorStartY);
                paintGutter(graphicsOrDumb, editorY, lineHeight, gutterWidth);
                paintText(graphicsOrDumb, editorY, lineHeight, gutterWidth, textWidth);
            }
            else {
                dumbTextImage = prepareDumbTextImage(editorY, lineHeight, textWidth);
            }
        }
        finally {
            ((DesktopEditorImpl) editor).setStickyLinePainting(false);
            if (isBackgroundChanged) {
                editor.setBackgroundColor(editorBackground);
            }
        }
    }

    private int[] gutterAndTextWidth() {
        int lineWidth = lineWidth();
        int gutterWidth = ((EditorGutterComponentImpl) editor.getGutterComponentEx()).getWidth();
        if (gutterWidth > lineWidth) {
            return new int[]{lineWidth, 0};
        }
        return new int[]{gutterWidth, lineWidth - gutterWidth};
    }

    private boolean setStickyLineBackgroundColor() {
        ColorValue backgroundColor = editor.getColorsScheme().getColor(isHovered
            ? EditorColors.STICKY_LINES_HOVERED_COLOR
            : EditorColors.STICKY_LINES_BACKGROUND);
        if (backgroundColor != null) {
            editor.setBackgroundColor(backgroundColor);
            return true;
        }
        return false;
    }

    private void paintGutter(Graphics g, int editorY, int lineHeight, int gutterWidth) {
        g.setClip(0, editorY, gutterWidth, lineHeight);
        ((EditorGutterComponentImpl) editor.getGutterComponentEx()).paint(g);
    }

    private void paintText(Graphics g, int editorY, int lineHeight, int gutterWidth, int textWidth) {
        g.translate(gutterWidth, 0);
        g.setClip(0, editorY, textWidth, lineHeight);
        if (dumbTextImage != null && ((DesktopEditorImpl) editor).isDumb()) {
            UIUtil.drawImage(g, dumbTextImage, 0, editorY, null);
        }
        else {
            doPaintText(g);
            dumbTextImage = null;
        }
    }

    private BufferedImage prepareDumbTextImage(int editorY, int lineHeight, int textWidth) {
        BufferedImage image = UIUtil.createImage(editor.getContentComponent(), textWidth, lineHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        EditorUIUtil.setupAntialiasing(g);
        g.translate(0, -editorY);
        g.setClip(0, editorY, textWidth, lineHeight);
        doPaintText(g);
        g.dispose();
        return image;
    }

    private void doPaintText(Graphics g) {
        editor.getContentComponent().paint(g);
    }

    private int lineWidth() {
        return ((DesktopEditorImpl) editor).getStickyLinesPanelWidth();
    }

    private int lineHeight() {
        int height = editor.getLineHeight();
        return isLineOutOfPanel() ? height + getY() : height;
    }

    private int editorY() {
        int y = editor.visualLineToY(primaryVisualLine);
        return isLineOutOfPanel() ? y - getY() : y;
    }

    private boolean isLineOutOfPanel() {
        return getY() < 0;
    }

    @Override
    public String toString() {
        return (debugText != null ? debugText : "") +
            "(primary=" + primaryVisualLine +
            ", scope=" + scopeVisualLine +
            ", onClick=" + offsetOnClick + ")";
    }

    public static class MyMouseEvent extends MouseEvent {
        public MyMouseEvent(MouseEvent e, Component source, int y) {
            super(source, e.getID(), e.getWhen(), UIUtil.getAllModifiers(e), e.getX(), y,
                e.getClickCount(), e.isPopupTrigger(), e.getButton());
        }
    }

    private class StickyMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener {
        private final JPopupMenu popMenu;
        public boolean isPopup = false;
        public boolean isGutterHovered = false;

        public StickyMouseListener() {
            ActionManager actionManager = ActionManager.getInstance();
            DefaultActionGroup group = (DefaultActionGroup) actionManager.getAction("EditorStickyLinesSettings");
            popMenu = actionManager.createActionPopupMenu("StickyLine", group).getComponent();
        }

        private void handleEvent(MouseEvent e) {
            if (e == null || e.isConsumed() || isEmpty()) return;

            switch (e.getID()) {
                case MouseEvent.MOUSE_ENTERED:
                case MouseEvent.MOUSE_EXITED:
                case MouseEvent.MOUSE_MOVED:
                    onHover(e);
                    break;
                case MouseEvent.MOUSE_PRESSED:
                case MouseEvent.MOUSE_RELEASED:
                case MouseEvent.MOUSE_CLICKED:
                    if (isGutterEvent(e)) {
                        gutterClick(e);
                    }
                    else {
                        popupOrNavigate(e);
                    }
                    break;
                case MouseEvent.MOUSE_WHEEL:
                    forwardToScrollPane(e);
                    break;
            }

            e.consume();
        }

        private void forwardToScrollPane(MouseEvent e) {
            MouseEvent converted = MouseEventAdapter.convert(e, editor.getScrollPane());
            editor.getScrollPane().dispatchEvent(converted);
        }

        private void onHover(MouseEvent e) {
            boolean gutter = isGutterEvent(e);
            switch (e.getID()) {
                case MouseEvent.MOUSE_ENTERED:
                    onTextHover(!isGutterHovered);
                    onGutterHover(isGutterHovered);
                    break;
                case MouseEvent.MOUSE_EXITED:
                    onTextHover(false);
                    onGutterHover(false);
                    break;
                case MouseEvent.MOUSE_MOVED:
                    if (gutter && isHovered && !isGutterHovered) {
                        onTextHover(false);
                        onGutterHover(true);
                    }
                    else if (!gutter && !isHovered && isGutterHovered) {
                        onTextHover(true);
                        onGutterHover(false);
                    }
                    break;
            }
        }

        private void onTextHover(boolean hover) {
            if (hover != isHovered) {
                isHovered = hover;
                repaint();
            }
        }

        private void onGutterHover(boolean hover) {
            if (hover != isGutterHovered) {
                isGutterHovered = hover;
                repaint();
            }
        }

        private void gutterClick(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_PRESSED || (e.getID() == MouseEvent.MOUSE_RELEASED && e.isPopupTrigger())) {
                MouseEvent converted = convert(e);
                MouseListener listener = ((DesktopEditorImpl) editor).getMouseListener();
                if (!e.isPopupTrigger()) {
                    e.consume();
                    return;
                }
                if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                    listener.mousePressed(converted);
                }
                else {
                    listener.mouseReleased(converted);
                }
            }
        }

        private void popupOrNavigate(MouseEvent e) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                    isPopup = e.isPopupTrigger();
                    if (isPopup) {
                        popMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if (!isPopup && e.isPopupTrigger()) {
                        isPopup = true;
                        popMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    if (!isPopup) {
                        StickyLineComponent.this.requestFocusInWindow();
                        CommandProcessor.getInstance().executeCommand(
                            editor.getProject(),
                            () -> {
                                editor.getCaretModel().moveToOffset(offsetOnClick);
                                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                                editor.getSelectionModel().removeSelection(true);
                                IdeDocumentHistory.getInstance(editor.getProject()).includeCurrentCommandAsNavigation();
                            },
                            "",
                            DocCommandGroupId.noneGroupId(editor.getDocument()),
                            UndoConfirmationPolicy.DEFAULT,
                            editor.getDocument()
                        );
                        //UIEventLogger.StickyLineNavigate.log(editor.getProject(), editor.getUserData(EDITOR_LANGUAGE));
                    }
                    break;
            }
        }

        private boolean isGutterEvent(MouseEvent e) {
            return e.getX() <= ((EditorGutterComponentImpl) editor.getGutterComponentEx()).getWidth();
        }

        private MouseEvent convert(MouseEvent e) {
            EditorGutterComponentImpl gutterComponentEx = (EditorGutterComponentImpl) editor.getGutterComponentEx();

            int y;
            if (e.isPopupTrigger()) {
                Point point = e.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(point, gutterComponentEx);
                y = point.y;
            }
            else {
                y = editor.visualLineToY(primaryVisualLine) + e.getY();
            }
            return new MyMouseEvent(e, gutterComponentEx, y);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            handleEvent(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            handleEvent(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            handleEvent(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            handleEvent(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            handleEvent(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            handleEvent(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            handleEvent(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            handleEvent(e);
        }
    }

    public static final Key<Language> EDITOR_LANGUAGE = Key.create("editor.settings.language");
}

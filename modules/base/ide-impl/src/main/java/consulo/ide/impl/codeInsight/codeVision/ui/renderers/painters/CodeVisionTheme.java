package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorFontType;
import consulo.ui.ex.awt.JBUI;

import java.awt.Font;
import java.awt.Rectangle;

public class CodeVisionTheme {
    public int iconGap;
    public int left;
    public int right;
    public int top;
    public int bottom;

    public CodeVisionTheme() {
        this(JBUI.scale(2), 0, 0, 0, 0);
    }

    public CodeVisionTheme(int iconGap, int left, int right, int top, int bottom) {
        this.iconGap = iconGap;
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public static Font editorFont(Editor editor) {
        return editorFont(editor, EditorFontType.PLAIN);
    }

    public static Font editorFont(Editor editor, EditorFontType style) {
        return editor.getColorsScheme().getFont(style);
    }

    public static boolean yInInlayBounds(int y, Rectangle size) {
        return y >= size.y && y <= (size.y + size.height - (size.height / 4));
    }
}

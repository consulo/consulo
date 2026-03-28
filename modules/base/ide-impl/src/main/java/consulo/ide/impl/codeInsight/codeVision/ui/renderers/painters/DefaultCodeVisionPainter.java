package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.function.BiFunction;

public class DefaultCodeVisionPainter<T> implements ICodeVisionEntryBasePainter<T> {
    /**
     * Function that provides an icon for a given (Project, value, InlayState) combination.
     * Returns null if no icon should be shown.
     */
    @FunctionalInterface
    public interface IconProvider<T> {
        @Nullable Icon getIcon(Project project, T value, RangeCodeVisionModel.InlayState state);
    }

    private final IconProvider<T> iconProvider;
    private final ICodeVisionEntryBasePainter<T> textPainter;
    final CodeVisionTheme theme;

    private final CodeVisionScaledIconPainter iconPainter = new CodeVisionScaledIconPainter();

    public DefaultCodeVisionPainter(IconProvider<T> iconProvider, ICodeVisionEntryBasePainter<T> textPainter) {
        this(iconProvider, textPainter, null);
    }

    public DefaultCodeVisionPainter(IconProvider<T> iconProvider,
                                    ICodeVisionEntryBasePainter<T> textPainter,
                                    @Nullable CodeVisionTheme theme) {
        this.iconProvider = iconProvider;
        this.textPainter = textPainter;
        this.theme = theme != null ? theme : new CodeVisionTheme();
    }

    @Override
    public void paint(
        Editor editor,
        TextAttributes textAttributes,
        Graphics g,
        T value,
        Point point,
        RangeCodeVisionModel.InlayState state,
        boolean hovered,
        @Nullable CodeVisionEntry hoveredEntry
    ) {
        Dimension pureSize = pureSize(editor, state, value);

        int x = point.x + theme.left;
        int y = point.y + theme.top;

        Project project = editor.getProject();

        if (project != null) {
            Icon icon = iconProvider.getIcon(project, value, state);
            if (icon != null) {
                float scaleFactor = iconPainter.scaleFactor(icon.getIconHeight(), pureSize.height);
                iconPainter.paint(editor, g, icon, new Point(x, y), scaleFactor);
                x += iconPainter.width(icon, scaleFactor) + theme.iconGap;
            }
        }

        textPainter.paint(editor, textAttributes, g, value, new Point(x, y), state, hovered, hoveredEntry);
    }

    private Dimension pureSize(Editor editor, RangeCodeVisionModel.InlayState state, T value) {
        Dimension size = textPainter.size(editor, state, value);

        Project project = editor.getProject();
        int width = 0;

        if (project != null) {
            Icon icon = iconProvider.getIcon(project, value, state);
            if (icon != null) {
                float scaleFactor = iconPainter.scaleFactor(icon.getIconHeight(), size.height);
                width = theme.iconGap + iconPainter.width(icon, scaleFactor);
            }
        }

        return new Dimension(size.width + width, size.height);
    }

    @Override
    public Dimension size(Editor editor, RangeCodeVisionModel.InlayState state, T value) {
        Dimension size = pureSize(editor, state, value);
        java.awt.FontMetrics editorMetrics =
            editor.getComponent().getFontMetrics(CodeVisionTheme.editorFont(editor));
        return new Dimension(
            size.width + theme.left + theme.right,
            size.height + theme.top + theme.bottom + (editorMetrics.getHeight() - size.height)
        );
    }
}

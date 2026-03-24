package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;

import javax.swing.Icon;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;

public class CodeVisionScaledIconPainter implements ICodeVisionPainter {
    private final double yShiftIconMultiplier;
    private final double scaleMultiplier;

    public CodeVisionScaledIconPainter() {
        this(0.865, 0.8);
    }

    public CodeVisionScaledIconPainter(double yShiftIconMultiplier, double scaleMultiplier) {
        this.yShiftIconMultiplier = yShiftIconMultiplier;
        this.scaleMultiplier = scaleMultiplier;
    }

    public void paint(Editor editor, Graphics g, Icon icon, Point point, float scaleFactor) {
        int scaledH = height(icon, scaleFactor);
        Graphics2D g2d = (Graphics2D) g;
        Composite composite = g2d.getComposite();
        AffineTransform savedTransform = g2d.getTransform();
        g2d.setComposite(AlphaComposite.SrcOver);
        int paintY = point.y - (int) (yShiftIconMultiplier * scaledH);
        g2d.translate(point.x, paintY);
        g2d.scale(scaleFactor, scaleFactor);
        icon.paintIcon(editor.getComponent(), g2d, 0, 0);
        g2d.setTransform(savedTransform);
        g2d.setComposite(composite);
    }

    public float scaleFactor(int iconValue, int neededValue) {
        return (float) (neededValue * scaleMultiplier) / iconValue;
    }

    public int width(Icon icon, float scaleFactor) {
        return (int) Math.round(icon.getIconWidth() * scaleFactor);
    }

    public int height(Icon icon, float scaleFactor) {
        return (int) Math.round(icon.getIconHeight() * scaleFactor);
    }
}

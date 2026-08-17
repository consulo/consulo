package consulo.desktop.awt.ui.impl.progressBar;

import java.awt.*;

public interface SpinnerRender {
    boolean isPaintComplete();

    void paintCompleteIndeterminate(Graphics2D g2, Component component, Rectangle rec, float last, float f, float p);

    void paintIndeterminate(Graphics2D g2, Component component, Rectangle rec, float f);

    void paintDeterminate(Graphics2D g2, Component component, Rectangle rec, float p);
}
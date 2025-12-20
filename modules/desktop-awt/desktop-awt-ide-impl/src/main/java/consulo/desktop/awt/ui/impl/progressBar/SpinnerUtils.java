package consulo.desktop.awt.ui.impl.progressBar;

public class SpinnerUtils {
    public static float easeInOutQuad(float x) {
        double v = x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;
        return (float) v;
    }
}
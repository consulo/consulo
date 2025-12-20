package consulo.desktop.awt.ui.impl.progressBar;

import javax.swing.*;

public class SpinnerProgress extends JProgressBar {

    private Icon icon;

    private int verticalAlignment = CENTER;
    private int horizontalAlignment = CENTER;

    private int verticalTextPosition = CENTER;
    private int horizontalTextPosition = TRAILING;

    private int iconTextGap = 4;
    private int space = 8;

    public SpinnerProgress() {
        init();
    }

    public SpinnerProgress(Icon icon) {
        this();
        this.icon = icon;
    }

    @Override
    public void updateUI() {
        setUI(new SpinnerProgressUI());
    }

    private void init() {
        setUI(new SpinnerProgressUI());
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        repaint();
        revalidate();
    }

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        if (this.verticalAlignment != alignment) {
            this.verticalAlignment = alignment;
            revalidate();
        }
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        if (this.horizontalAlignment != alignment) {
            this.horizontalAlignment = alignment;
            revalidate();
        }
    }

    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        if (this.verticalTextPosition != textPosition) {
            this.verticalTextPosition = textPosition;
            revalidate();
        }
    }

    public int getHorizontalTextPosition() {
        return horizontalTextPosition;
    }


    public void setHorizontalTextPosition(int textPosition) {
        if (this.horizontalTextPosition != textPosition) {
            this.horizontalTextPosition = textPosition;
            revalidate();
        }
    }

    public int getIconTextGap() {
        return iconTextGap;
    }

    public void setIconTextGap(int iconTextGap) {
        if (this.iconTextGap != iconTextGap) {
            this.iconTextGap = iconTextGap;
            revalidate();
        }
    }

    public int getSpace() {
        return space;
    }

    public void setSpace(int space) {
        if (this.space != space) {
            this.space = space;
            revalidate();
        }
    }
}
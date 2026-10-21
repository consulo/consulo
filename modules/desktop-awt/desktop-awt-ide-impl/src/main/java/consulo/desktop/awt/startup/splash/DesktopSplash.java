/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.startup.splash;

import consulo.application.ApplicationProperties;
import consulo.application.internal.StartupProgress;
import consulo.desktop.awt.ui.impl.window.JDialogAsUIWindow;
import consulo.platform.Platform;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.RootPaneUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * @author Konstantin Bulenkov
 */
public class DesktopSplash extends JDialogAsUIWindow implements StartupProgress {
    public static @Nullable Rectangle BOUNDS;

    private static final int CORNER_RADIUS = 24;
    private static final String WINDOW_CORNER_RADIUS = "apple.awt.windowCornerRadius";

    private final AnimatedLogoLabel myLabel;

    /**
     * @param unstableScaling - cache scale value or always recalculate it.
     *                        On start - scaling may changed, and will provide some artifacts after LafManager loading
     */
    public DesktopSplash(boolean unstableScaling) {
        super(null, false);

        setRootPane(new JRootPane() {
            @Override
            public void updateUI() {
                setUI(new RootPaneUI() {
                });
            }
        });

        setUndecorated(true);
        setResizable(false);
        setFocusableWindowState(false);

        Color foreground = ApplicationProperties.isInSandbox() ? Color.WHITE : Color.BLACK;
        myLabel = new AnimatedLogoLabel(14, foreground, true, unstableScaling);

        JBDimension dimension = JBUI.size(602, 294);
        JPanel contentPane = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                if (isOpaque()) {
                    super.paintComponent(g);
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.Clear);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int arc = JBUI.scale(CORNER_RADIUS) * 2;
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
                g2.dispose();
            }
        };
        contentPane.setBackground(Color.LIGHT_GRAY);
        contentPane.setPreferredSize(dimension);
        contentPane.add(myLabel, BorderLayout.SOUTH);
        setContentPane(contentPane);
        roundCorners(contentPane);
        Dimension size = getPreferredSize();
        setSize(size);
        pack();
        setLocationInTheCenterOfScreen(dimension);
    }

    private void roundCorners(JPanel contentPane) {
        if ("sun.awt.wl.WLToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName())) {
            getRootPane().putClientProperty(WINDOW_CORNER_RADIUS, "full");
            return;
        }
        if (Platform.current().os().isMac()) {
            getRootPane().putClientProperty(WINDOW_CORNER_RADIUS, (float) CORNER_RADIUS);
            return;
        }
        GraphicsDevice device = getGraphicsConfiguration().getDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            setBackground(new Color(0, 0, 0, 0));
            contentPane.setOpaque(false);
        }
    }

    private void setLocationInTheCenterOfScreen(Dimension size) {
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        Rectangle bounds = graphicsConfiguration.getBounds();
        if (Platform.current().os().isWindows()) {
            Insets insets = ScreenUtil.getScreenInsets(graphicsConfiguration);
            if (insets != null) {
                bounds.x += insets.left;
                bounds.y += insets.top;
                bounds.width -= insets.left + insets.right;
                bounds.height -= insets.top + insets.bottom;
            }
        }

        setLocation(UIUtil.getCenterPoint(bounds, size));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void show() {
        super.show();
        toFront();
        BOUNDS = getBounds();
    }

    @Override
    public void showProgress(String message, float progress) {
        myLabel.setValue((int)(progress * 100f));
    }
}

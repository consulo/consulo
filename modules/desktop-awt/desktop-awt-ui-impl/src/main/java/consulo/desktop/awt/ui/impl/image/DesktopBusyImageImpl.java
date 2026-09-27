/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl.image;

import consulo.desktop.awt.ui.impl.image.reference.DesktopAWTImageKey;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 2026-09-27
 */
public final class DesktopBusyImageImpl extends DesktopAnimatedImageBase {
    private record Key(int width, int height, boolean disabled) {
    }

    private record ScaledFrame(int step, int rgb, int width, int height, Icon icon) {
    }

    private static final ConcurrentMap<Key, DesktopBusyImageImpl> ourImages = new ConcurrentHashMap<>();

    public static DesktopBusyImageImpl of(int width, int height) {
        return of(width, height, false);
    }

    private static DesktopBusyImageImpl of(int width, int height, boolean disabled) {
        return ourImages.computeIfAbsent(new Key(Math.max(width, 0), Math.max(height, 0), disabled), DesktopBusyImageImpl::new);
    }

    private final int myWidth;
    private final int myHeight;
    private final boolean myDisabled;
    private final Icon myStillIcon;

    private volatile @Nullable ScaledFrame myFrame;

    private DesktopBusyImageImpl(Key key) {
        super(1);
        myWidth = key.width();
        myHeight = key.height();
        myDisabled = key.disabled();
        myStillIcon = createStillIcon(myWidth, myHeight, myDisabled);
    }

    private static Icon createStillIcon(int width, int height, boolean disabled) {
        if (width == 0 || height == 0) {
            return DesktopEmptyImageImpl.get(width, height);
        }

        ImageKey passive = PlatformIconGroup.processStep_passive();
        DesktopAWTImageKey still = new DesktopAWTImageKey(null, passive.getGroupId(), passive.getImageId(), width, height);
        return disabled ? new DesktopLazyImageImpl(still::copyGrayed) : still;
    }

    @Override
    public int getWidth() {
        return JBUI.scale(myWidth);
    }

    @Override
    public int getHeight() {
        return JBUI.scale(myHeight);
    }

    @Override
    protected Icon getFrameIcon(int index) {
        if (myWidth == 0 || myHeight == 0) {
            return myStillIcon;
        }

        int step = DesktopSpinnerFrames.getStepAt(System.nanoTime());
        int rgb = getColorRGB();
        int width = getWidth();
        int height = getHeight();

        ScaledFrame frame = myFrame;
        if (frame == null || frame.step() != step || frame.rgb() != rgb || frame.width() != width || frame.height() != height) {
            Image image = DesktopSpinnerFrames.getFrame(step, new Color(rgb, true));
            Image scaled = image instanceof DesktopAWTImage awtImage ? awtImage.copyWithNewSize(width, height) : image;
            frame = new ScaledFrame(step, rgb, width, height, TargetAWT.to(scaled));
            myFrame = frame;
        }
        return frame.icon();
    }

    @Override
    protected int getFrameDelay(int index) {
        return DesktopSpinnerFrames.getMillisToNextStep(System.nanoTime());
    }

    @Override
    protected Icon getStillIcon() {
        return myStillIcon;
    }

    private int getColorRGB() {
        int rgb = DesktopSpinnerFrames.getProgressIconColor().getRGB();
        if (!myDisabled) {
            return rgb;
        }
        return UIUtil.getGrayFilter(StyleManager.get().getCurrentStyle().isDark()).filterRGB(0, 0, rgb);
    }

    @Override
    public DesktopAWTImage copyWithNewSize(int width, int height) {
        return of(width, height, myDisabled);
    }

    @Override
    public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
        return this;
    }

    @Override
    public DesktopAWTImage copyGrayed() {
        return myDisabled ? this : of(myWidth, myHeight, true);
    }
}

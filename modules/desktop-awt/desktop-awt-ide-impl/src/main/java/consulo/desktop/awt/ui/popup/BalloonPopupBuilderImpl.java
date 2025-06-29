/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.popup;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BalloonPopupBuilderImpl implements BalloonBuilder {
    @Nullable
    private final Map<Disposable, List<Balloon>> myStorage;
    @Nullable
    private Disposable myAnchor;

    private final JComponent myContent;

    private Color myBorder = JBColor.border();
    @Nullable
    private Insets myBorderInsets = null;
    private Color myFill = MessageType.INFO.getPopupBackground();
    private boolean myHideOnMouseOutside = true;
    private boolean myHideOnKeyOutside = true;
    private long myFadeoutTime = -1;
    private boolean myShowCallout = true;
    private boolean myCloseButtonEnabled = false;
    private boolean myHideOnFrameResize = true;
    private boolean myHideOnLinkClick = false;

    private ActionListener myClickHandler;
    private boolean myCloseOnClick;
    private int myAnimationCycle = 500;

    private int myCalloutShift;
    private int myPositionChangeXShift;
    private int myPositionChangeYShift;
    private boolean myHideOnAction = true;
    private boolean myHideOnCloseClick = true;
    private boolean myDialogMode;
    private String myTitle;
    private Insets myContentInsets = JBUI.insets(2);
    private boolean myShadow = true;
    private boolean mySmallVariant = false;

    private Dimension myPointerSize;
    private int myCornerToPointerDistance = -1;

    private Balloon.Layer myLayer;
    private boolean myBlockClicks = false;
    private boolean myRequestFocus = false;
    private boolean myPointerShiftedToStart;

    public BalloonPopupBuilderImpl(@Nullable Map<Disposable, List<Balloon>> storage, @Nonnull JComponent content) {
        myStorage = storage;
        myContent = content;
    }

    @Nonnull
    @Override
    public BalloonBuilder setHideOnAction(boolean hideOnAction) {
        myHideOnAction = hideOnAction;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setDialogMode(boolean dialogMode) {
        myDialogMode = dialogMode;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setBorderColor(@Nonnull Color color) {
        myBorder = color;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setBorderInsets(@Nullable Insets insets) {
        myBorderInsets = insets;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setFillColor(@Nonnull Color color) {
        myFill = color;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setHideOnClickOutside(boolean hide) {
        myHideOnMouseOutside = hide;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setHideOnKeyOutside(boolean hide) {
        myHideOnKeyOutside = hide;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setShowCallout(boolean show) {
        myShowCallout = show;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setFadeoutTime(long fadeoutTime) {
        myFadeoutTime = fadeoutTime;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setBlockClicksThroughBalloon(boolean block) {
        myBlockClicks = block;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setRequestFocus(boolean requestFocus) {
        myRequestFocus = requestFocus;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setPointerSize(Dimension size) {
        myPointerSize = size;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setCornerToPointerDistance(int distance) {
        myCornerToPointerDistance = distance;
        return this;
    }

    @Override
    public BalloonBuilder setHideOnCloseClick(boolean hideOnCloseClick) {
        myHideOnCloseClick = hideOnCloseClick;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setAnimationCycle(int time) {
        myAnimationCycle = time;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setHideOnFrameResize(boolean hide) {
        myHideOnFrameResize = hide;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setHideOnLinkClick(boolean hide) {
        myHideOnLinkClick = hide;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setPositionChangeXShift(int positionChangeXShift) {
        myPositionChangeXShift = positionChangeXShift;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setPositionChangeYShift(int positionChangeYShift) {
        myPositionChangeYShift = positionChangeYShift;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setCloseButtonEnabled(boolean enabled) {
        myCloseButtonEnabled = enabled;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setClickHandler(ActionListener listener, boolean closeOnClick) {
        myClickHandler = listener;
        myCloseOnClick = closeOnClick;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setCalloutShift(int length) {
        myCalloutShift = length;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setTitle(@Nullable String title) {
        myTitle = title;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setContentInsets(Insets insets) {
        myContentInsets = insets;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setShadow(boolean shadow) {
        myShadow = shadow;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setSmallVariant(boolean smallVariant) {
        mySmallVariant = smallVariant;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setLayer(Balloon.Layer layer) {
        myLayer = layer;
        return this;
    }

    @Nonnull
    @Override
    public BalloonBuilder setDisposable(@Nonnull Disposable anchor) {
        myAnchor = anchor;
        return this;
    }

    @Override
    public BalloonBuilder setPointerShiftedToStart(boolean pointerShiftedToStart) {
        myPointerShiftedToStart = pointerShiftedToStart;
        return this;
    }

    @Nonnull
    @Override
    public Balloon createBalloon() {
        final BalloonImpl result = new BalloonImpl(
            myContent,
            myBorder,
            myBorderInsets,
            myFill,
            myHideOnMouseOutside,
            myHideOnKeyOutside,
            myHideOnAction,
            myHideOnCloseClick,
            myShowCallout,
            myCloseButtonEnabled,
            myFadeoutTime,
            myHideOnFrameResize,
            myHideOnLinkClick,
            myClickHandler,
            myCloseOnClick,
            myAnimationCycle,
            myCalloutShift,
            myPositionChangeXShift,
            myPositionChangeYShift,
            myDialogMode,
            myTitle,
            myContentInsets,
            myShadow,
            mySmallVariant,
            myBlockClicks,
            myLayer,
            myRequestFocus,
            myPointerSize,
            myCornerToPointerDistance
        );

        result.setPointerShiftedToStart(myPointerShiftedToStart);

        if (myStorage != null && myAnchor != null) {
            List<Balloon> balloons = myStorage.get(myAnchor);
            if (balloons == null) {
                myStorage.put(myAnchor, balloons = new ArrayList<>());
                Disposer.register(
                    myAnchor,
                    () -> {
                        List<Balloon> toDispose = myStorage.remove(myAnchor);
                        if (toDispose != null) {
                            for (Balloon balloon : toDispose) {
                                if (!balloon.isDisposed()) {
                                    Disposer.dispose(balloon);
                                }
                            }
                        }
                    }
                );
            }
            balloons.add(result);
            result.addListener(new JBPopupAdapter() {
                @Override
                public void onClosed(LightweightWindowEvent event) {
                    if (!result.isDisposed()) {
                        Disposer.dispose(result);
                    }
                }
            });
        }

        return result;
    }
}

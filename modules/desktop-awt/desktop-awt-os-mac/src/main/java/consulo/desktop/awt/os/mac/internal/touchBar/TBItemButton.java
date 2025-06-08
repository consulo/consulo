// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import com.sun.jna.Pointer;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.performance.ActivityTracker;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.mac.foundation.ID;
import consulo.desktop.awt.os.mac.internal.icon.MacIconGroup;
import consulo.ui.ModalityState;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.lang.Pair;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class TBItemButton extends TBItem {
    private static final int TEST_DELAY_MS = Integer.getInteger("touchbar.test.delay", 0);
    private static final Executor ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Touchbar buttons updater", 2);

    final @Nullable TouchBarStats.AnActionStats actionStats;

    private @Nullable String myText;
    private @Nullable String myHint;
    private boolean myIsHintDisabled = false;
    private int myLayoutBits = 0;
    private boolean myHasArrowIcon = false;
    private boolean isGetDisabledIconNeeded = false;
    private AsyncPromise<Pair<Pointer, Dimension>> rasterPromise;

    // action parameters
    private @Nullable Runnable action;
    private @Nullable NSTLibrary.Action nativeCallback;
    private boolean executeOnEdt = true;
    private ModalityState modality = null;

    protected @Nullable Image originIcon;
    protected int myFlags = 0;
    protected int updateOptions;

    TBItemButton(@Nullable ItemListener listener, @Nullable TouchBarStats.AnActionStats actionStats) {
        super("button", listener);
        this.actionStats = actionStats;
    }

    private @Nullable Image getDarkIcon(@Nullable Image icon) {
        if (icon == null) {
            return null;
        }

        long startNs = actionStats == null ? 0 : System.nanoTime();
        icon = NST.getDarkIconVariant(icon);
        if (actionStats != null) {
            actionStats.iconGetDarkDurationNs += System.nanoTime() - startNs;
        }

        return icon;
    }

    void setIcon(Image icon, boolean needGetDisabled) {
        if (isGetDisabledIconNeeded != needGetDisabled || !Objects.equals(icon, originIcon)) {
            originIcon = icon;
            isGetDisabledIconNeeded = needGetDisabled;
            updateOptions |= NSTLibrary.BUTTON_UPDATE_IMG;
        }
    }

    public TBItemButton setIcon(Image icon) {
        if (!Objects.equals(icon, originIcon) || isGetDisabledIconNeeded) {
            originIcon = icon;
            isGetDisabledIconNeeded = false;
            updateOptions |= NSTLibrary.BUTTON_UPDATE_IMG;
        }

        return this;
    }

    // convenience method
    void setIconFromPresentation(@Nonnull Presentation presentation) {
        Image icon;
        boolean needGetDisabledIcon = false;
        if (presentation.isEnabled()) {
            icon = presentation.getIcon();
        }
        else {
            icon = presentation.getDisabledIcon();
            if (icon == null && presentation.getIcon() != null) {
                needGetDisabledIcon = true;
                icon = presentation.getIcon();
            }
        }

        setIcon(icon, needGetDisabledIcon);
    }

    // convenience method
    void setIconAndTextFromPresentation(@Nonnull Presentation presentation, @Nullable TouchbarActionCustomizations touchBarAction) {
        if (touchBarAction != null) {
            if (touchBarAction.isShowImage() && touchBarAction.isShowText()) {
                setIconFromPresentation(presentation);
                setText(presentation.getText());
            }
            else if (touchBarAction.isShowText()) {
                setText(presentation.getText());
                setIcon(null);
            }
            else {
                setIconFromPresentation(presentation);
                setText(originIcon != null ? null : presentation.getText());
            }
        }
        else {
            setIconFromPresentation(presentation);
            setText(originIcon != null ? null : presentation.getText());
        }
    }

    void setHasArrowIcon(boolean hasArrowIcon) {
        if (hasArrowIcon != myHasArrowIcon) {
            myHasArrowIcon = hasArrowIcon;
            synchronized (this) {
                if (myNativePeer != ID.NIL) {
                    NST.setArrowImage(myNativePeer, myHasArrowIcon ? MacIconGroup.touchbarPopoverarrow() : null);
                }
            }
        }
    }

    public TBItemButton setText(String text) {
        if (!Objects.equals(text, myText)) {
            myText = text;
            updateOptions |= NSTLibrary.BUTTON_UPDATE_TEXT;
        }

        return this;
    }

    void setText(String text, String hint, boolean isHintDisabled) {
        if (!Objects.equals(text, myText) || !Objects.equals(hint, myHint) || (hint != null && myIsHintDisabled != isHintDisabled)) {
            myText = text;
            myHint = hint;
            myIsHintDisabled = isHintDisabled;
            updateOptions |= NSTLibrary.BUTTON_UPDATE_TEXT;
        }
    }

    void setModality(ModalityState modality) {
        this.modality = modality;
    }

    public TBItemButton setAction(Runnable action, boolean executeOnEDT) {
        if (action == this.action && this.executeOnEdt == executeOnEDT) {
            return this;
        }

        this.action = action;
        this.executeOnEdt = executeOnEDT;
        if (action == null) {
            nativeCallback = null;
        }
        else {
            nativeCallback = () -> {
                // NOTE: executed from AppKit thread
                if (executeOnEdt) {
                    Application app = ApplicationManager.getApplication();
                    if (modality == null) {
                        app.invokeLater(this.action);
                    }
                    else {
                        app.invokeLater(this.action, modality);
                    }
                }
                else {
                    this.action.run();
                }

                ActivityTracker.getInstance().inc();

                if (myListener != null) {
                    myListener.onItemEvent(this, 0);
                }
            };
        }

        updateOptions |= NSTLibrary.BUTTON_UPDATE_ACTION;

        return this;
    }

    public TBItemButton setWidth(int width) {
        return setLayout(width, 0, 2, 8);
    }

    TBItemButton setLayout(int width, int widthFlags, int margin, int border) {
        if (width < 0) {
            width = 0;
        }
        if (margin < 0) {
            margin = 0;
        }
        if (border < 0) {
            border = 0;
        }

        int newLayout = width & NSTLibrary.LAYOUT_WIDTH_MASK;
        newLayout |= widthFlags;
        newLayout |= NSTLibrary.margin2mask((byte) margin);
        newLayout |= NSTLibrary.border2mask((byte) border);
        if (myLayoutBits != newLayout) {
            myLayoutBits = newLayout;
            updateOptions |= NSTLibrary.BUTTON_UPDATE_LAYOUT;
        }

        return this;
    }

    public void setToggle() {
        _setFlag(NSTLibrary.BUTTON_FLAG_TOGGLE, true);
    }

    void setColored() {
        _setFlag(NSTLibrary.BUTTON_FLAG_COLORED, true);
    }

    void setSelected(boolean isSelected) {
        _setFlag(NSTLibrary.BUTTON_FLAG_SELECTED, isSelected);
    }

    void setDisabled(boolean isDisabled) {
        _setFlag(NSTLibrary.BUTTON_FLAG_DISABLED, isDisabled);
    }

    TBItemButton setTransparentBg() {
        return _setFlag(NSTLibrary.BUTTON_FLAG_TRANSPARENT_BG, true);
    }

    private TBItemButton _setFlag(int nstLibFlag, boolean val) {
        final int flags = _applyFlag(myFlags, val, nstLibFlag);
        if (flags != myFlags) {
            myFlags = flags;
            updateOptions |= NSTLibrary.BUTTON_UPDATE_FLAGS;
        }
        return this;
    }

    // Icon calculations can be slow, so we use async update
    void updateLater(boolean force) {
        if (!force && (!myIsVisible || updateOptions == 0 || myNativePeer == ID.NIL)) {
            return;
        }

        if (rasterPromise != null && !rasterPromise.isDone()) {
            rasterPromise.cancel();
        }

        rasterPromise = new AsyncPromise<>();
        rasterPromise.onSuccess(raster -> {
            //
            // update native peer
            //
            int updateOptions = this.updateOptions;
            String text = (updateOptions & NSTLibrary.BUTTON_UPDATE_TEXT) != 0 ? myText : null;
            String hint = (updateOptions & NSTLibrary.BUTTON_UPDATE_TEXT) != 0 ? myHint : null;
            NSTLibrary.Action callback = (updateOptions & NSTLibrary.BUTTON_UPDATE_ACTION) != 0 ? nativeCallback : null;
            int validFlags = _validateFlags();
            int isHintDisabled = myIsHintDisabled ? 1 : 0;
            int layoutBits = myLayoutBits;

            this.updateOptions = 0;

            synchronized (this) {
                if (myNativePeer.equals(ID.NIL)) {
                    return;
                }
                NST.updateButton(myNativePeer, updateOptions, layoutBits, validFlags, text, hint, isHintDisabled, raster, callback);
            }
        });

        ourExecutor.execute(() -> {
            if (originIcon == null || (!force && (updateOptions & NSTLibrary.BUTTON_UPDATE_IMG) == 0)) {
                if (TEST_DELAY_MS > 0) {
                    waitTheTestDelay();
                }
                rasterPromise.setResult(null);
                return;
            }

            // load icon (can be quite slow)
            long startNs = actionStats == null ? 0 : System.nanoTime();
            if (TEST_DELAY_MS > 0) {
                waitTheTestDelay();
            }

            Image icon = getDarkIcon(originIcon);
            if (icon != null && isGetDisabledIconNeeded) {
                icon = ImageEffects.grayed(icon);
            }

            // prepare raster (not very fast)
            Pair<Pointer, Dimension> raster = NST.get4ByteRGBARaster(icon);
            if (actionStats != null && raster != null) {
                actionStats.iconUpdateIconRasterCount++;
                actionStats.iconRenderingDurationNs += System.nanoTime() - startNs;
            }

            rasterPromise.setResult(raster);
        });
    }

    private static void waitTheTestDelay() {
        if (TEST_DELAY_MS <= 0) {
            return;
        }

        ProgressIndicator progress = Objects.requireNonNull(ProgressIndicatorProvider.getGlobalProgressIndicator());
        long start = System.currentTimeMillis();
        while (true) {
            progress.checkCanceled();
            if (System.currentTimeMillis() - start > TEST_DELAY_MS) {
                break;
            }
            TimeoutUtil.sleep(1);
        }
    }

    @Override
    protected ID _createNativePeer() {
        if (originIcon != null && rasterPromise == null) {
            updateLater(true);
        }
        final ID result = NST.createButton(
            getUid(),
            myLayoutBits, _validateFlags(),
            myText, myHint, myIsHintDisabled ? 1 : 0,
            rasterPromise == null || !rasterPromise.isSucceeded() ? null : unsafeGet(rasterPromise),
            nativeCallback);
        if (myHasArrowIcon) {
            NST.setArrowImage(result, MacIconGroup.touchbarPopoverarrow());
        }

        return result;
    }

    private static <T> T unsafeGet(AsyncPromise<T> promise) {
        try {
            return promise.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private int _validateFlags() {
        int flags = myFlags;
        if ((flags & NSTLibrary.BUTTON_FLAG_COLORED) != 0 && (flags & NSTLibrary.BUTTON_FLAG_DISABLED) != 0) {
            return flags & ~NSTLibrary.BUTTON_FLAG_COLORED;
        }
        return flags;
    }

    private static int _applyFlag(int src, boolean include, int flag) {
        return include ? (src | flag) : (src & ~flag);
    }
}

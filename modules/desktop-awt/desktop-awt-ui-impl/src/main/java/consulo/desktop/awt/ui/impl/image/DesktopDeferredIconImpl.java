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
package consulo.desktop.awt.ui.impl.image;

import com.google.common.annotations.VisibleForTesting;
import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScalableIcon;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * @author max
 */
public class DesktopDeferredIconImpl<T> extends JBUI.CachingScalableJBIcon<DesktopDeferredIconImpl<T>>
    implements DeferredIcon, RetrievableIcon, consulo.ui.image.Image {
    private static final consulo.ui.image.Image EMPTY_ICON = Image.empty(16);

    private static final Logger LOG = Logger.getInstance(DesktopDeferredIconImpl.class);
    private static final int MIN_AUTO_UPDATE_MILLIS = 950;
    private static final DeferredIconRepaintScheduler ourRepaintScheduler = new DeferredIconRepaintScheduler();
    
    private consulo.ui.image.Image myDelegateIcon;
    private volatile consulo.ui.image.Image myScaledDelegateIcon;
    private Function<T, consulo.ui.image.Image> myEvaluator;
    private volatile boolean myIsScheduled;
    private final T myParam;

    private final boolean myNeedReadAction;
    private boolean myDone;
    private final boolean myAutoUpdatable;
    private long myLastCalcTime;
    private long myLastTimeSpent;

    private static final ExecutorService ourIconCalculatingExecutor =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("OurIconCalculating Pool", 1);

    private final IconListener<T> myEvalListener;

    private final Map<Component, RepaintRequest> myPendingRepaints = new LinkedHashMap<>();

    private DesktopDeferredIconImpl(DesktopDeferredIconImpl<T> icon) {
        super(icon);
        myDelegateIcon = icon.myDelegateIcon;
        myScaledDelegateIcon = icon.myDelegateIcon;
        myEvaluator = icon.myEvaluator;
        myIsScheduled = icon.myIsScheduled;
        myParam = icon.myParam;
        myNeedReadAction = icon.myNeedReadAction;
        myDone = icon.myDone;
        myAutoUpdatable = icon.myAutoUpdatable;
        myLastCalcTime = icon.myLastCalcTime;
        myLastTimeSpent = icon.myLastTimeSpent;
        myEvalListener = icon.myEvalListener;
    }

    
    @Override
    protected DesktopDeferredIconImpl<T> copy() {
        return new DesktopDeferredIconImpl<>(this);
    }

    
    @Override
    public Icon scale(float scale) {
        if (getScale() != scale && myDelegateIcon instanceof ScalableIcon scalableIcon) {
            myScaledDelegateIcon = TargetAWT.from(scalableIcon.scale(scale));
            super.scale(scale);
        }
        return this;
    }

    @Override
    public int getHeight() {
        return getIconHeight();
    }

    @Override
    public int getWidth() {
        return getIconWidth();
    }

    private static class Holder {
        private static final boolean CHECK_CONSISTENCY = Application.get().isUnitTestMode();
    }

    DesktopDeferredIconImpl(
        consulo.ui.image.Image baseIcon,
        T param,
        Function<T, Image> evaluator,
        IconListener<T> listener,
        boolean autoUpdatable
    ) {
        this(baseIcon, param, true, evaluator, listener, autoUpdatable);
    }

    public DesktopDeferredIconImpl(
        consulo.ui.image.Image baseIcon,
        T param,
        boolean needReadAction,
        Function<T, consulo.ui.image.Image> evaluator
    ) {
        this(baseIcon, param, needReadAction, evaluator, null, false);
    }

    private DesktopDeferredIconImpl(
        consulo.ui.image.Image baseIcon,
        T param,
        boolean needReadAction,
        Function<T, consulo.ui.image.Image> evaluator,
        @Nullable IconListener<T> listener,
        boolean autoUpdatable
    ) {
        myParam = param;
        myDelegateIcon = nonNull(baseIcon);
        myScaledDelegateIcon = myDelegateIcon;
        myEvaluator = evaluator;
        myNeedReadAction = needReadAction;
        myEvalListener = listener;
        myAutoUpdatable = autoUpdatable;
        checkDelegationDepth();
    }

    private void checkDelegationDepth() {
        int depth = 0;
        DesktopDeferredIconImpl each = this;
        while (each.myScaledDelegateIcon instanceof DesktopDeferredIconImpl && depth < 50) {
            depth++;
            each = (DesktopDeferredIconImpl) each.myScaledDelegateIcon;
        }
        if (depth >= 50) {
            LOG.error("Too deep deferred icon nesting");
        }
    }

    
    private static consulo.ui.image.Image nonNull(consulo.ui.image.Image icon) {
        return ObjectUtil.notNull(icon, EMPTY_ICON);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (!(myScaledDelegateIcon instanceof DesktopDeferredIconImpl && ((DesktopDeferredIconImpl) myScaledDelegateIcon).myScaledDelegateIcon instanceof DesktopDeferredIconImpl)) {
            TargetAWT.to(myScaledDelegateIcon).paintIcon(c, g, x, y); //SOE protection
        }

        if (isDone() || PowerSaveMode.isEnabled()) {
            return;
        }

        if (myIsScheduled) {
            addPendingRepaint(c, x, y);
            return;
        }

        scheduleEvaluation(c, x, y);
    }

    private void addPendingRepaint(Component c, int x, int y) {
        RepaintRequest request = ourRepaintScheduler.createRepaintRequest(c, x, y);
        myPendingRepaints.putIfAbsent(Objects.requireNonNull(request.target()), request);
    }

    @VisibleForTesting
    Future<?> scheduleEvaluation(Component c, int x, int y) {
        myIsScheduled = true;

        addPendingRepaint(c, x, y);

        return ourIconCalculatingExecutor.submit(() -> {
            int oldWidth = myScaledDelegateIcon.getWidth();
            SimpleReference<Image> evaluated = SimpleReference.create();

            long startTime = System.currentTimeMillis();
            if (myNeedReadAction) {
                boolean result = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> {
                    DesktopIconDeferrerImpl.evaluateDeferred(() -> evaluated.set(evaluateImage()));
                    if (myAutoUpdatable) {
                        myLastCalcTime = System.currentTimeMillis();
                        myLastTimeSpent = myLastCalcTime - startTime;
                    }
                });
                if (!result) {
                    myIsScheduled = false;
                    return;
                }
            }
            else {
                DesktopIconDeferrerImpl.evaluateDeferred(() -> evaluated.set(evaluateImage()));
                if (myAutoUpdatable) {
                    myLastCalcTime = System.currentTimeMillis();
                    myLastTimeSpent = myLastCalcTime - startTime;
                }
            }
            Image result = evaluated.get();
            myScaledDelegateIcon = result;
            checkDelegationDepth();

            boolean shouldRevalidate =
                Registry.is("ide.tree.deferred.icon.invalidates.cache") && myScaledDelegateIcon.getWidth() != oldWidth;

            SwingUtilities.invokeLater(() -> {
                setDone(result);

                List<RepaintRequest> pendingRepaints = new ArrayList<>(myPendingRepaints.values());
                myPendingRepaints.clear();

                for (RepaintRequest pendingRepaint : pendingRepaints) {
                    repaint(pendingRepaint, shouldRevalidate);
                }
            });
        });
    }

    @RequiredUIAccess
    private void repaint(RepaintRequest pendingRepaint, boolean shouldRevalidate) {
        Component actualTarget = pendingRepaint.getActualTarget();
        if (actualTarget == null) {
            return;
        }

        if (shouldRevalidate) {
            // revalidate will not work: JTree caches size of nodes
            if (actualTarget instanceof JTree) {
                TreeUI ui = ((JTree) actualTarget).getUI();
                if (ui instanceof BasicTreeUI) {
                    // this call is "fake" and only need to reset tree layout cache
                    ((BasicTreeUI) ui).setLeftChildIndent(UIUtil.getTreeLeftChildIndent());
                }
            }
        }

        ourRepaintScheduler.scheduleRepaint(pendingRepaint, getIconWidth(), getIconHeight(), false);
    }

    void setDone(consulo.ui.image.Image result) {
        if (myEvalListener != null) {
            myEvalListener.evalDone(this, myParam, result);
        }

        myDone = true;
        if (!myAutoUpdatable) {
            myEvaluator = null;
        }
    }

    @Override
    public @Nullable Icon retrieveIcon() {
        return isDone() ? TargetAWT.to(myScaledDelegateIcon) : evaluate();
    }

    @Override
    public Icon evaluate() {
        return TargetAWT.to(evaluateImage());
    }

    public Image evaluateImage() {
        consulo.ui.image.Image result;
        try {
            result = nonNull(myEvaluator.apply(myParam));
        }
        catch (IndexNotReadyException e) {
            result = EMPTY_ICON;
        }

        if (Holder.CHECK_CONSISTENCY) {
            checkDoesntReferenceThis(result);
        }

        return result;
    }

    private void checkDoesntReferenceThis(consulo.ui.image.Image icon) {
        if (icon == this) {
            throw new IllegalStateException("Loop in icons delegation");
        }

        if (icon instanceof DesktopDeferredIconImpl desktopDeferredIcon) {
            checkDoesntReferenceThis(desktopDeferredIcon.myScaledDelegateIcon);
        }
        else if (icon instanceof LayeredIcon layeredIcon) {
            for (Icon layer : layeredIcon.getAllLayers()) {
                checkDoesntReferenceThis(TargetAWT.from(layer));
            }
        }
        else if (icon instanceof RowIcon rowIcon) {
            for (int i = 0, count = rowIcon.getIconCount(); i < count; i++) {
                checkDoesntReferenceThis(TargetAWT.from(rowIcon.getIcon(i)));
            }
        }
    }

    @Override
    public int getIconWidth() {
        return TargetAWT.to(myScaledDelegateIcon).getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return TargetAWT.to(myScaledDelegateIcon).getIconHeight();
    }

    public boolean isDone() {
        if (myAutoUpdatable && myDone && myLastCalcTime > 0
            && System.currentTimeMillis() - myLastCalcTime > Math.max(MIN_AUTO_UPDATE_MILLIS, 10 * myLastTimeSpent)) {
            myDone = false;
            myIsScheduled = false;
        }
        return myDone;
    }

    @FunctionalInterface
    interface IconListener<T> {
        void evalDone(DesktopDeferredIconImpl<T> source, T key, consulo.ui.image.Image result);
    }

    static boolean equalIcons(consulo.ui.image.Image icon1, consulo.ui.image.Image icon2) {
        if (icon1 instanceof DesktopDeferredIconImpl desktopDeferredIcon1) {
            return desktopDeferredIcon1.isDeferredAndEqual(icon2);
        }
        if (icon2 instanceof DesktopDeferredIconImpl desktopDeferredIcon2) {
            return desktopDeferredIcon2.isDeferredAndEqual(icon1);
        }
        return Comparing.equal(icon1, icon2);
    }

    private boolean isDeferredAndEqual(consulo.ui.image.Image icon) {
        return icon instanceof DesktopDeferredIconImpl desktopDeferredIcon
            && Objects.equals(myParam, desktopDeferredIcon.myParam)
            && equalIcons(myScaledDelegateIcon, desktopDeferredIcon.myScaledDelegateIcon);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this
            || obj instanceof Image that && equalIcons(this, that);
    }

    @Override
    public String toString() {
        return "Deferred. Base=" + myScaledDelegateIcon;
    }

    public Image getDelegateIcon() {
        return myDelegateIcon;
    }
}

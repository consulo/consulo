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

/*
 * @author max
 */
package consulo.desktop.awt.uiOld;

import com.google.common.annotations.VisibleForTesting;
import consulo.application.PowerSaveMode;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.ui.PaintingParent;
import consulo.ide.impl.idea.ui.RetrievableIcon;
import consulo.ide.impl.idea.ui.tabs.impl.TabLabel;
import consulo.util.lang.ObjectUtil;
import consulo.application.ApplicationManager;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScalableIcon;
import consulo.ui.ex.awt.UIUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.desktop.awt.ui.impl.image.DesktopImage;
import consulo.logging.Logger;
import consulo.application.dumb.IndexNotReadyException;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.concurrent.EdtExecutorService;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.ref.SimpleReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

public class DesktopDeferredIconImpl<T> extends JBUI.CachingScalableJBIcon<DesktopDeferredIconImpl<T>>
        implements DeferredIcon, DesktopImage<DesktopDeferredIconImpl<T>>, RetrievableIcon, consulo.ui.image.Image {
  private static final consulo.ui.image.Image EMPTY_ICON = Image.empty(16);

  private static final Logger LOG = Logger.getInstance(DesktopDeferredIconImpl.class);
  private static final int MIN_AUTO_UPDATE_MILLIS = 950;
  private static final RepaintScheduler ourRepaintScheduler = new RepaintScheduler();
  @Nonnull
  private consulo.ui.image.Image myDelegateIcon;
  private volatile consulo.ui.image.Image myScaledDelegateIcon;
  private Function<T, consulo.ui.image.Image> myEvaluator;
  private volatile boolean myIsScheduled;
  private final T myParam;

  protected float myEvaluateScale = 1f;

  private final boolean myNeedReadAction;
  private boolean myDone;
  private final boolean myAutoUpdatable;
  private long myLastCalcTime;
  private long myLastTimeSpent;

  private static final ExecutorService ourIconCalculatingExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("OurIconCalculating Pool", 1);

  private final IconListener<T> myEvalListener;

  private DesktopDeferredIconImpl(@Nonnull DesktopDeferredIconImpl<T> icon) {
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

  @Nonnull
  @Override
  protected DesktopDeferredIconImpl<T> copy() {
    return new DesktopDeferredIconImpl<>(this);
  }

  @Nonnull
  @Override
  public Icon scale(float scale) {
    if (getScale() != scale && myDelegateIcon instanceof ScalableIcon) {
      myScaledDelegateIcon = TargetAWT.from(((ScalableIcon)myDelegateIcon).scale(scale));
      super.scale(scale);
    }
    return this;
  }

  @Nonnull
  @Override
  // special case - we don't return copy of that icon
  public DesktopDeferredIconImpl<T> copyWithScale(float scale) {
    if (scale == 1f) {
      return this;
    }

    // one time scale only
    if(myEvaluateScale == 1f) {
      Image delegate = ImageEffects.resize(myDelegateIcon, scale);
      myDelegateIcon = delegate;
      myScaledDelegateIcon = delegate;
      myEvaluateScale = scale;
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
    private static final boolean CHECK_CONSISTENCY = ApplicationManager.getApplication().isUnitTestMode();
  }

  DesktopDeferredIconImpl(consulo.ui.image.Image baseIcon, T param, @Nonnull Function<T, Image> evaluator, @Nonnull IconListener<T> listener, boolean autoUpdatable) {
    this(baseIcon, param, true, evaluator, listener, autoUpdatable);
  }

  public DesktopDeferredIconImpl(consulo.ui.image.Image baseIcon, T param, final boolean needReadAction, @Nonnull Function<T, consulo.ui.image.Image> evaluator) {
    this(baseIcon, param, needReadAction, evaluator, null, false);
  }

  private DesktopDeferredIconImpl(consulo.ui.image.Image baseIcon,
                                  T param,
                                  boolean needReadAction,
                                  @Nonnull Function<T, consulo.ui.image.Image> evaluator,
                                  @Nullable IconListener<T> listener,
                                  boolean autoUpdatable) {
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
      each = (DesktopDeferredIconImpl)each.myScaledDelegateIcon;
    }
    if (depth >= 50) {
      LOG.error("Too deep deferred icon nesting");
    }
  }

  @Nonnull
  private static consulo.ui.image.Image nonNull(final consulo.ui.image.Image icon) {
    return ObjectUtil.notNull(icon, EMPTY_ICON);
  }

  @Override
  public void paintIcon(final Component c, @Nonnull final Graphics g, final int x, final int y) {
    if (!(myScaledDelegateIcon instanceof DesktopDeferredIconImpl && ((DesktopDeferredIconImpl)myScaledDelegateIcon).myScaledDelegateIcon instanceof DesktopDeferredIconImpl)) {
      TargetAWT.to(myScaledDelegateIcon).paintIcon(c, g, x, y); //SOE protection
    }

    if (isDone() || myIsScheduled || PowerSaveMode.isEnabled()) {
      return;
    }

    scheduleEvaluation(c, x, y);
  }

  @VisibleForTesting
  Future<?> scheduleEvaluation(Component c, int x, int y) {
    myIsScheduled = true;

    final Component target = getTarget(c);
    final Component paintingParent = SwingUtilities.getAncestorOfClass(PaintingParent.class, c);
    final Rectangle paintingParentRec = paintingParent == null ? null : ((PaintingParent)paintingParent).getChildRec(c);
    return ourIconCalculatingExecutor.submit(() -> {
      int oldWidth = myScaledDelegateIcon.getWidth();
      final SimpleReference<Image> evaluated = SimpleReference.create();

      final long startTime = System.currentTimeMillis();
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
      final Image result = evaluated.get();
      myScaledDelegateIcon = result;
      checkDelegationDepth();

      final boolean shouldRevalidate = Registry.is("ide.tree.deferred.icon.invalidates.cache") && myScaledDelegateIcon.getWidth() != oldWidth;

      EdtExecutorService.getInstance().execute(() -> {
        setDone(result);

        Component actualTarget = target;
        if (actualTarget != null && SwingUtilities.getWindowAncestor(actualTarget) == null) {
          actualTarget = paintingParent;
          if (actualTarget == null || SwingUtilities.getWindowAncestor(actualTarget) == null) {
            actualTarget = null;
          }
        }

        if (actualTarget == null) return;

        if (shouldRevalidate) {
          // revalidate will not work: JTree caches size of nodes
          if (actualTarget instanceof JTree) {
            final TreeUI ui = ((JTree)actualTarget).getUI();
            if (ui instanceof BasicTreeUI) {
              // this call is "fake" and only need to reset tree layout cache
              ((BasicTreeUI)ui).setLeftChildIndent(UIUtil.getTreeLeftChildIndent());
            }
          }
        }

        if (c == actualTarget) {
          c.repaint(x, y, getIconWidth(), getIconHeight());
        }
        else {
          ourRepaintScheduler.pushDirtyComponent(actualTarget, paintingParentRec);
        }
      });
    });
  }

  private static Component getTarget(Component c) {
    final Component target;

    final Container list = SwingUtilities.getAncestorOfClass(JList.class, c);
    if (list != null) {
      target = list;
    }
    else {
      final Container tree = SwingUtilities.getAncestorOfClass(JTree.class, c);
      if (tree != null) {
        target = tree;
      }
      else {
        final Container table = SwingUtilities.getAncestorOfClass(JTable.class, c);
        if (table != null) {
          target = table;
        }
        else {
          final Container box = SwingUtilities.getAncestorOfClass(JComboBox.class, c);
          if (box != null) {
            target = box;
          }
          else {
            final Container tabLabel = SwingUtilities.getAncestorOfClass(TabLabel.class, c);
            target = tabLabel == null ? c : tabLabel;
          }
        }
      }
    }
    return target;
  }

  void setDone(@Nonnull consulo.ui.image.Image result) {
    if (myEvalListener != null) {
      myEvalListener.evalDone(this, myParam, result);
    }

    myDone = true;
    if (!myAutoUpdatable) {
      myEvaluator = null;
    }
  }

  @Nullable
  @Override
  public Icon retrieveIcon() {
    return isDone() ? TargetAWT.to(myScaledDelegateIcon) : evaluate();
  }

  @Nonnull
  @Override
  public Icon evaluate() {
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

    result = ImageEffects.resize(result, myEvaluateScale);
    return TargetAWT.to(result);
  }

  @Nonnull
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

    return ImageEffects.resize(result, myEvaluateScale);
  }

  private void checkDoesntReferenceThis(final consulo.ui.image.Image icon) {
    if (icon == this) {
      throw new IllegalStateException("Loop in icons delegation");
    }

    if (icon instanceof DesktopDeferredIconImpl) {
      checkDoesntReferenceThis(((DesktopDeferredIconImpl)icon).myScaledDelegateIcon);
    }
    else if (icon instanceof LayeredIcon) {
      for (Icon layer : ((LayeredIcon)icon).getAllLayers()) {
        checkDoesntReferenceThis(TargetAWT.from(layer));
      }
    }
    else if (icon instanceof RowIcon) {
      final RowIcon rowIcon = (RowIcon)icon;
      final int count = rowIcon.getIconCount();
      for (int i = 0; i < count; i++) {
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
    if (myAutoUpdatable && myDone && myLastCalcTime > 0 && System.currentTimeMillis() - myLastCalcTime > Math.max(MIN_AUTO_UPDATE_MILLIS, 10 * myLastTimeSpent)) {
      myDone = false;
      myIsScheduled = false;
    }
    return myDone;
  }

  private static class RepaintScheduler {
    private final Alarm myAlarm = new Alarm();
    private final Set<RepaintRequest> myQueue = new LinkedHashSet<>();

    private void pushDirtyComponent(@Nonnull Component c, final Rectangle rec) {
      ApplicationManager.getApplication().assertIsDispatchThread(); // assert myQueue accessed from EDT only
      myAlarm.cancelAllRequests();
      myAlarm.addRequest(() -> {
        for (RepaintRequest each : myQueue) {
          Rectangle r = each.getRectangle();
          if (r == null) {
            each.getComponent().repaint();
          }
          else {
            each.getComponent().repaint(r.x, r.y, r.width, r.height);
          }
        }
        myQueue.clear();
      }, 50);

      myQueue.add(new RepaintRequest(c, rec));
    }
  }

  private static class RepaintRequest {
    private final Component myComponent;
    private final Rectangle myRectangle;

    private RepaintRequest(@Nonnull Component component, Rectangle rectangle) {
      myComponent = component;
      myRectangle = rectangle;
    }

    @Nonnull
    public Component getComponent() {
      return myComponent;
    }

    public Rectangle getRectangle() {
      return myRectangle;
    }
  }

  @FunctionalInterface
  interface IconListener<T> {
    void evalDone(DesktopDeferredIconImpl<T> source, T key, @Nonnull consulo.ui.image.Image result);
  }

  static boolean equalIcons(consulo.ui.image.Image icon1, consulo.ui.image.Image icon2) {
    if (icon1 instanceof DesktopDeferredIconImpl) {
      return ((DesktopDeferredIconImpl)icon1).isDeferredAndEqual(icon2);
    }
    if (icon2 instanceof DesktopDeferredIconImpl) {
      return ((DesktopDeferredIconImpl)icon2).isDeferredAndEqual(icon1);
    }
    return Comparing.equal(icon1, icon2);
  }

  private boolean isDeferredAndEqual(consulo.ui.image.Image icon) {
    return icon instanceof DesktopDeferredIconImpl &&
           Comparing.equal(myParam, ((DesktopDeferredIconImpl)icon).myParam) &&
           equalIcons(myScaledDelegateIcon, ((DesktopDeferredIconImpl)icon).myScaledDelegateIcon);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Image) {
      return equalIcons(this, (Image)obj);
    }
    return false;
  }

  @Override
  public String toString() {
    return "Deferred. Base=" + myScaledDelegateIcon;
  }
}

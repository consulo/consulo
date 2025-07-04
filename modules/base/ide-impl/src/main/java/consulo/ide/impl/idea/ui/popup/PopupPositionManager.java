// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup;

import consulo.application.ui.DimensionService;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.ui.Size2D;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.util.PopupUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author pegov
 * @author Konstantin Bulenkov
 */
public class PopupPositionManager {
  private static final Position[] DEFAULT_POSITION_ORDER = {Position.RIGHT, Position.LEFT, Position.TOP, Position.BOTTOM};

  private PopupPositionManager() {
  }

  public enum Position {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
  }

  public static void positionPopupInBestPosition(final JBPopup hint, @Nullable final Editor editor, @Nullable DataContext dataContext) {
    positionPopupInBestPosition(hint, editor, dataContext, DEFAULT_POSITION_ORDER);
  }

  public static void positionPopupInBestPosition(final JBPopup hint, @Nullable final Editor editor, @Nullable DataContext dataContext, @Nonnull Position... relationToExistingPopup) {
    final LookupEx lookup = LookupManager.getActiveLookup(editor);
    if (lookup != null && lookup.getCurrentItem() != null && lookup.getComponent().isShowing()) {
      new PositionAdjuster(lookup.getComponent()).adjust(hint, relationToExistingPopup);
      lookup.addLookupListener(new LookupListener() {
        @Override
        public void lookupCanceled(@Nonnull LookupEvent event) {
          if (hint.isVisible()) {
            hint.cancel();
          }
        }
      });
      return;
    }

    final PositionAdjuster positionAdjuster = createPositionAdjuster(hint);
    if (positionAdjuster != null) {
      positionAdjuster.adjust(hint, relationToExistingPopup);
      return;
    }

    if (editor != null && editor.getComponent().isShowing() && editor instanceof EditorEx) {
      dataContext = ((EditorEx)editor).getDataContext();
    }

    if (dataContext != null) {
      if (hint.canShow()) {
        hint.showInBestPositionFor(dataContext);
      }
      else {
        hint.setLocation(hint.getBestPositionFor(dataContext));
      }
    }
  }

  private static Component discoverPopup(final Key<JBPopup> datakey, Component focusOwner) {
    if (focusOwner == null) {
      focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    if (focusOwner == null) return null;

    final DataContext dataContext = DataManager.getInstance().getDataContext(focusOwner);
    final JBPopup popup = dataContext.getData(datakey);
    if (popup != null && popup.isVisible() && !popup.isDisposed()) {
      return popup.getContent();
    }

    return null;
  }

  @Nullable
  private static PositionAdjuster createPositionAdjuster(JBPopup hint) {
    final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner == null) return null;

    JBPopup popup = PopupUtil.getPopupContainerFor(focusOwner);
    if (popup != null && popup != hint && !popup.isDisposed()) {
      return new PositionAdjuster(popup.getContent());
    }

    final Component existing = discoverPopup(LangDataKeys.POSITION_ADJUSTER_POPUP, focusOwner);
    if (existing != null) {
      return new PositionAdjuster2(existing, discoverPopup(LangDataKeys.PARENT_POPUP, focusOwner));
    }

    return null;
  }

  private static class PositionAdjuster2 extends PositionAdjuster {

    private final Component myTopComponent;

    private PositionAdjuster2(final Component relativeTo, final Component topComponent) {
      super(relativeTo);
      myTopComponent = topComponent == null ? relativeTo : topComponent;
    }

    @Override
    protected int getYForTopPositioning() {
      return myTopComponent.getLocationOnScreen().y;
    }
  }

  public static class PositionAdjuster {
    protected final int myGap;

    protected final Component myRelativeTo;
    protected final Point myRelativeOnScreen;
    protected final Rectangle myScreenRect;

    public PositionAdjuster(final Component relativeTo, int gap) {
      myRelativeTo = relativeTo;
      myRelativeOnScreen = relativeTo.getLocationOnScreen();
      myScreenRect = ScreenUtil.getScreenRectangle(myRelativeOnScreen);
      myGap = gap;
    }

    public PositionAdjuster(final Component relativeTo) {
      this(relativeTo, 5);
    }

    protected Rectangle positionRight(final Dimension d) {
      return new Rectangle(myRelativeOnScreen.x + myRelativeTo.getWidth() + myGap, myRelativeOnScreen.y, d.width, d.height);
    }

    protected Rectangle positionLeft(final Dimension d) {
      return new Rectangle(myRelativeOnScreen.x - myGap - d.width, myRelativeOnScreen.y, d.width, d.height);
    }

    protected Rectangle positionAbove(final Dimension d) {
      return new Rectangle(myRelativeOnScreen.x, getYForTopPositioning() - myGap - d.height, d.width, d.height);
    }

    protected Rectangle positionUnder(final Dimension d) {
      return new Rectangle(myRelativeOnScreen.x, myRelativeOnScreen.y + myGap + myRelativeTo.getHeight(), d.width, d.height);
    }

    protected int getYForTopPositioning() {
      return myRelativeOnScreen.y;
    }

    /**
     * Try to position:
     * 1. to the right
     * 2. to the left
     * 3. above
     * 4. under
     *
     * @param popup
     */
    public void adjust(final JBPopup popup) {
      adjust(popup, DEFAULT_POSITION_ORDER);
    }

    public void adjust(final JBPopup popup, Position... traversalPolicy) {
      if (traversalPolicy.length == 0) traversalPolicy = DEFAULT_POSITION_ORDER;

      Size2D popupSize = getPopupSize(popup);
      assert popupSize != null;
      final Dimension d = new Dimension(popupSize.width(), popupSize.height());

      Rectangle popupRect = null;
      Rectangle r = null;
      final List<Rectangle> boxes = new ArrayList<>();

      for (Position position : traversalPolicy) {
        switch (position) {
          case TOP:
            r = positionAbove(d);
            boxes.add(crop(myScreenRect, new Rectangle(myRelativeOnScreen.x, myScreenRect.y, myScreenRect.width, getYForTopPositioning() - myScreenRect.y - myGap)));
            break;
          case BOTTOM:
            r = positionUnder(d);
            boxes.add(crop(myScreenRect, new Rectangle(myRelativeOnScreen.x, myRelativeOnScreen.y + myRelativeTo.getHeight() + myGap, myScreenRect.width, myScreenRect.height)));
            break;
          case LEFT:
            r = positionLeft(d);
            boxes.add(crop(myScreenRect, new Rectangle(myScreenRect.x, myRelativeOnScreen.y, myRelativeOnScreen.x - myScreenRect.x - myGap, myScreenRect.height)));
            break;
          case RIGHT:
            r = positionRight(d);
            boxes.add(crop(myScreenRect, new Rectangle(myRelativeOnScreen.x + myRelativeTo.getWidth() + myGap, myRelativeOnScreen.y, myScreenRect.width, myScreenRect.height)));
            break;
        }
        if (myScreenRect.contains(r)) {
          popupRect = r;
          break;
        }
      }

      if (popupRect != null) {
        if (popup.canShow()) {
          final Point p = new Point(r.x - myRelativeOnScreen.x, r.y - myRelativeOnScreen.y);
          popup.show(new RelativePoint(myRelativeTo, p));
        }
        else {
          popup.setLocation(new Point(r.x, r.y));
        }
      }
      else {
        // ok, popup does not fit, will try to resize it
        Collections.sort(boxes, Comparator.comparingInt((Rectangle o) -> o.width).thenComparingInt(o -> o.height));

        final Rectangle suitableBox = boxes.get(boxes.size() - 1);
        final Rectangle crop = crop(suitableBox, new Rectangle(suitableBox.x < myRelativeOnScreen.x ? suitableBox.x + suitableBox.width - d.width : suitableBox.x,
                                                               suitableBox.y < myRelativeOnScreen.y ? suitableBox.y + suitableBox.height - d.height : suitableBox.y, d.width, d.height));

        popup.setSize(crop.getSize());
        if (popup.canShow()) {
          popup.show(new RelativePoint(myRelativeTo, new Point(crop.getLocation().x - myRelativeOnScreen.x, crop.getLocation().y - myRelativeOnScreen.y)));
        }
        else {
          popup.setLocation(crop.getLocation());
        }
      }
    }

    protected static Rectangle crop(final Rectangle source, final Rectangle toCrop) {
      final Rectangle result = new Rectangle(toCrop);
      if (toCrop.x < source.x) {
        result.width -= source.x - toCrop.x;
        result.x = source.x;
      }

      if (toCrop.y < source.y) {
        result.height -= source.y - toCrop.y;
        result.y = source.y;
      }

      if (result.x + result.width > source.x + source.width) {
        result.width = source.x + source.width - result.x;
      }

      if (result.y + result.height > source.y + source.height) {
        result.height = source.y + source.height - result.y;
      }

      return result;
    }

    @Nullable
    public static Size2D getPopupSize(final JBPopup popup) {
      Size2D size = null;
      if (popup instanceof AbstractPopup) {
        final String dimensionKey = ((AbstractPopup)popup).getDimensionServiceKey();
        if (dimensionKey != null) {
          size = DimensionService.getInstance().getSize(dimensionKey);
        }
      }

      if (size == null) {
        Dimension preferredSize = popup.getContent().getPreferredSize();
        size = preferredSize == null ? null : new Size2D(preferredSize.width, preferredSize.height);
      }

      return size;
    }
  }
}

/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.util;

import consulo.diff.DiffContext;
import consulo.diff.request.DiffRequest;
import consulo.desktop.awt.internal.diff.EditorHolder;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import jakarta.annotation.Nonnull;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

public abstract class FocusTrackerSupport<S> {
  @Nonnull
  public abstract S getCurrentSide();

  public abstract void setCurrentSide(@Nonnull S side);

  public abstract void processContextHints(@Nonnull DiffRequest request, @Nonnull DiffContext context);

  public abstract void updateContextHints(@Nonnull DiffRequest request, @Nonnull DiffContext context);

  //
  // Impl
  //

  public static class Twoside extends FocusTrackerSupport<Side> {
    @Nonnull
    private Side myCurrentSide;

    public Twoside(@Nonnull List<? extends EditorHolder> holders) {
      assert holders.size() == 2;

      myCurrentSide = Side.RIGHT;

      addListener(holders, Side.LEFT);
      addListener(holders, Side.RIGHT);
    }

    @Override
    @Nonnull
    public Side getCurrentSide() {
      return myCurrentSide;
    }

    @Override
    public void setCurrentSide(@Nonnull Side side) {
      myCurrentSide = side;
    }

    @Override
    public void processContextHints(@Nonnull DiffRequest request, @Nonnull DiffContext context) {
      Side side = DiffImplUtil.getUserData(request, context, DiffUserDataKeys.PREFERRED_FOCUS_SIDE);
      if (side != null) setCurrentSide(side);
    }

    @Override
    public void updateContextHints(@Nonnull DiffRequest request, @Nonnull DiffContext context) {
      context.putUserData(DiffUserDataKeys.PREFERRED_FOCUS_SIDE, myCurrentSide);
    }

    private void addListener(@Nonnull List<? extends EditorHolder> holders, @Nonnull Side side) {
      side.select(holders).installFocusListener(new MyFocusListener(side));
    }

    private class MyFocusListener extends FocusAdapter {
      @Nonnull
      private final Side mySide;

      private MyFocusListener(@Nonnull Side side) {
        mySide = side;
      }

      @Override
      public void focusGained(FocusEvent e) {
        myCurrentSide = mySide;
      }
    }
  }

  public static class Threeside extends FocusTrackerSupport<ThreeSide> {
    @Nonnull
    private ThreeSide myCurrentSide;

    public Threeside(@Nonnull List<? extends EditorHolder> holders) {
      myCurrentSide = ThreeSide.BASE;

      addListener(holders, ThreeSide.LEFT);
      addListener(holders, ThreeSide.BASE);
      addListener(holders, ThreeSide.RIGHT);
    }

    @Override
    @Nonnull
    public ThreeSide getCurrentSide() {
      return myCurrentSide;
    }

    @Override
    public void setCurrentSide(@Nonnull ThreeSide side) {
      myCurrentSide = side;
    }

    @Override
    public void processContextHints(@Nonnull DiffRequest request, @Nonnull DiffContext context) {
      ThreeSide side = DiffImplUtil.getUserData(request, context, DiffUserDataKeys.PREFERRED_FOCUS_THREESIDE);
      if (side != null) setCurrentSide(side);
    }

    @Override
    public void updateContextHints(@Nonnull DiffRequest request, @Nonnull DiffContext context) {
      context.putUserData(DiffUserDataKeys.PREFERRED_FOCUS_THREESIDE, myCurrentSide);
    }

    private void addListener(@Nonnull List<? extends EditorHolder> holders, @Nonnull ThreeSide side) {
      side.select(holders).installFocusListener(new MyFocusListener(side));
    }

    private class MyFocusListener extends FocusAdapter {
      @Nonnull
      private final ThreeSide mySide;

      private MyFocusListener(@Nonnull ThreeSide side) {
        mySide = side;
      }

      @Override
      public void focusGained(FocusEvent e) {
        myCurrentSide = mySide;
      }
    }
  }
}

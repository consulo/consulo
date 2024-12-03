// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf2;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.SwingActionDelegate;
import consulo.util.lang.ObjectUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;
import javax.swing.*;

import static consulo.ui.ex.awt.speedSearch.SpeedSearchSupply.getSupply;

public abstract class TreeActions extends SwingActionDelegate {
  private TreeActions(String actionId) {
    super(actionId);
  }

  @Nullable
  @Override
  protected JTree getComponent(AnActionEvent event) {
    JTree tree = ObjectUtil.tryCast(super.getComponent(event), JTree.class);
    return tree == null || getSupply(tree) != null ? null : tree;
  }

  public static final class Home extends TreeActions {
    @NonNls
    public static final String ID = "selectFirst";

    public Home() {
      super(ID);
    }
  }

  public static final class ShiftHome extends TreeActions {
    @NonNls
    public static final String ID = "selectFirstExtendSelection";

    public ShiftHome() {
      super(ID);
    }
  }

  public static final class End extends TreeActions {
    @NonNls
    public static final String ID = "selectLast";

    public End() {
      super(ID);
    }
  }

  public static final class ShiftEnd extends TreeActions {
    @NonNls
    public static final String ID = "selectLastExtendSelection";

    public ShiftEnd() {
      super(ID);
    }
  }

  public static final class Up extends TreeActions {
    @NonNls
    public static final String ID = "selectPrevious";

    public Up() {
      super(ID);
    }
  }

  public static final class ShiftUp extends TreeActions {
    @NonNls
    public static final String ID = "selectPreviousExtendSelection";

    public ShiftUp() {
      super(ID);
    }
  }

  public static final class Down extends TreeActions {
    @NonNls
    public static final String ID = "selectNext";

    public Down() {
      super(ID);
    }
  }

  public static final class ShiftDown extends TreeActions {
    @NonNls
    public static final String ID = "selectNextExtendSelection";

    public ShiftDown() {
      super(ID);
    }
  }

  public static final class Left extends TreeActions {
    @NonNls
    public static final String ID = "selectParent";

    public Left() {
      super(ID);
    }
  }

  public static final class ShiftLeft extends TreeActions {
    @NonNls
    public static final String ID = "selectParentExtendSelection";

    public ShiftLeft() {
      super(ID);
    }
  }

  public static final class Right extends TreeActions {
    @NonNls
    public static final String ID = "selectChild";

    public Right() {
      super(ID);
    }
  }

  public static final class ShiftRight extends TreeActions {
    @NonNls
    public static final String ID = "selectChildExtendSelection";

    public ShiftRight() {
      super(ID);
    }
  }

  public static final class PageUp extends TreeActions {
    @NonNls
    public static final String ID = "scrollUpChangeSelection";

    public PageUp() {
      super(ID);
    }
  }

  public static final class ShiftPageUp extends TreeActions {
    @NonNls
    public static final String ID = "scrollUpExtendSelection";

    public ShiftPageUp() {
      super(ID);
    }
  }

  public static final class PageDown extends TreeActions {
    @NonNls
    public static final String ID = "scrollDownChangeSelection";

    public PageDown() {
      super(ID);
    }
  }

  public static final class ShiftPageDown extends TreeActions {
    @NonNls
    public static final String ID = "scrollDownExtendSelection";

    public ShiftPageDown() {
      super(ID);
    }
  }

  public static final class NextSibling extends TreeActions {
    @NonNls
    public static final String ID = "selectNextSibling";

    public NextSibling() {
      super(ID);
    }
  }

  public static final class PreviousSibling extends TreeActions {
    @NonNls
    public static final String ID = "selectPreviousSibling";

    public PreviousSibling() {
      super(ID);
    }
  }
}

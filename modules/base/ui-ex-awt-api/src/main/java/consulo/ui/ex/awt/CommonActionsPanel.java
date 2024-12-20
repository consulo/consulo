/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * @author Konstantin Bulenkov
 */
public class CommonActionsPanel extends JPanel {
  public enum Buttons {
    ADD, REMOVE, EDIT,  UP, DOWN;

    public static Buttons[] ALL = {ADD, REMOVE, EDIT,  UP, DOWN};

    public Image getIcon() {
      switch (this) {
        case ADD:    return AllIcons.General.Add;
        case EDIT:    return AllIcons.Actions.Edit;
        case REMOVE: return AllIcons.General.Remove;
        case UP:     return AllIcons.Actions.MoveUp;
        case DOWN:   return AllIcons.Actions.MoveDown;
      }
      return null;
    }

    MyActionButton createButton(final Listener listener, String name, Image icon) {
      return new MyActionButton(this, listener, name == null ? StringUtil.capitalize(name().toLowerCase()) : name, icon);
    }

    public String getText() {
      return StringUtil.capitalize(name().toLowerCase());
    }

    public void performAction(Listener listener) {
      switch (this) {
        case ADD: listener.doAdd(); break;
        case EDIT: listener.doEdit(); break;
        case REMOVE: listener.doRemove(); break;
        case UP: listener.doUp(); break;
        case DOWN: listener.doDown(); break;
      }
    }
  }
  public interface Listener {
    void doAdd();
    void doRemove();
    void doUp();
    void doDown();
    void doEdit();

    class Adapter implements Listener {
      @Override
      public void doAdd() {}
      @Override
      public void doRemove() {}
      @Override
      public void doUp() {}
      @Override
      public void doDown() {}
      @Override
      public void doEdit() {}
    }
  }

  private Map<Buttons, MyActionButton> myButtons = new HashMap<>();
  private final AnAction[] myActions;

  CommonActionsPanel(ListenerFactory factory, @Nullable JComponent contextComponent, ActionToolbarPosition position,
                     @Nullable AnAction[] additionalActions, @Nullable Comparator<AnAction> buttonComparator,
                     String addName, String removeName, String moveUpName, String moveDownName, String editName,
                     Image addIcon, Buttons... buttons) {
    super(new BorderLayout());
    final Listener listener = factory.createListener(this);
    AnAction[] actions = new AnAction[buttons.length];
    for (int i = 0; i < buttons.length; i++) {
      Buttons button = buttons[i];
      String name = null;
      switch (button) {
        case ADD:    name = addName;      break;        
        case EDIT:   name = editName;     break;
        case REMOVE: name = removeName;   break;
        case UP:     name = moveUpName;   break;
        case DOWN:   name = moveDownName; break;
      }
      final MyActionButton b = button.createButton(listener, name, button == Buttons.ADD && addIcon != null ? addIcon : button.getIcon());
      actions[i] = b;
      myButtons.put(button, b);
    }
    if (additionalActions != null && additionalActions.length > 0) {
      final ArrayList<AnAction> allActions = new ArrayList<>(Arrays.asList(actions));
      allActions.addAll(Arrays.asList(additionalActions));
      actions = allActions.toArray(new AnAction[allActions.size()]);
    }
    myActions = actions;
    for (AnAction action : actions) {
      if(action instanceof AnActionButton) {
        ((AnActionButton)action).setContextComponent(contextComponent);
      }
    }
    if (buttonComparator != null) {
      Arrays.sort(myActions, buttonComparator);
    }
    ArrayList<AnAction> toolbarActions = new ArrayList<>(Arrays.asList(myActions));
    for (int i = 0; i < toolbarActions.size(); i++) {
        if (toolbarActions.get(i) instanceof AnActionButton.CheckedAnActionButton) {
          toolbarActions.set(i, ((AnActionButton.CheckedAnActionButton)toolbarActions.get(i)).getDelegate());
        }
    }

    final ActionManager mgr = ActionManager.getInstance();
    final ActionToolbar toolbar = mgr.createActionToolbar(ActionPlaces.UNKNOWN,
                                                          new DefaultActionGroup(toolbarActions.toArray(new AnAction[toolbarActions.size()])),
                                                          position == ActionToolbarPosition.BOTTOM || position == ActionToolbarPosition.TOP);
    toolbar.getComponent().setOpaque(false);
    toolbar.setTargetComponent(contextComponent);
    toolbar.getComponent().setBorder(null);
    add(toolbar.getComponent(), BorderLayout.CENTER);
  }

  public AnActionButton getAnActionButton(Buttons button) {
    return myButtons.get(button);
  }

  @Override
  public void addNotify() {
    final JRootPane pane = getRootPane();
    for (AnAction button : myActions) {
      final ShortcutSet shortcut = button instanceof AnActionButton ? ((AnActionButton)button).getShortcut() : null;
      if (shortcut != null) {
        if (button instanceof MyActionButton
            && ((MyActionButton)button).isAddButton()
            && UIUtil.isDialogRootPane(pane)) {
          button.registerCustomShortcutSet(shortcut, pane);
        } else {
          button.registerCustomShortcutSet(shortcut, ((AnActionButton)button).getContextComponent());
        }
        if (button instanceof MyActionButton && ((MyActionButton)button).isRemoveButton()) {
          registerDeleteHook((MyActionButton)button);
        }
      }
    }
    
    super.addNotify(); // call after all to construct actions tooltips properly
  }

  private static void registerDeleteHook(final MyActionButton removeButton) {
    new AnAction("Delete Hook") {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        removeButton.actionPerformed(e);
      }

      @RequiredUIAccess
      @Override
      public void update(@Nonnull AnActionEvent e) {
        final JComponent contextComponent = removeButton.getContextComponent();
        if (contextComponent instanceof JTable && ((JTable)contextComponent).isEditing()) {
          e.getPresentation().setEnabled(false);
          return;
        }
        final SpeedSearchSupply supply = SpeedSearchSupply.getSupply(contextComponent);
        if (supply != null && supply.isPopupActive()) {
          e.getPresentation().setEnabled(false);
          return;
        }
        removeButton.update(e);
      }
    }.registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"), removeButton.getContextComponent());
  }

  public void setEnabled(Buttons button, boolean enabled) {
    final MyActionButton b = myButtons.get(button);
    if (b != null) {
      b.setEnabled(enabled);
    }
  }

  static class MyActionButton extends AnActionButton implements DumbAware {
    private final Buttons myButton;
    private final Listener myListener;

    MyActionButton(Buttons button, Listener listener, String name, Image icon) {
      super(name, name, icon);
      myButton = button;
      myListener = listener;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myButton.performAction(myListener);
    }

    @Override
    public ShortcutSet getShortcut() {
      return getCommonShortcut(myButton);
    }

    @Override
    public void updateButton(AnActionEvent e) {
      super.updateButton(e);
      if (!e.getPresentation().isEnabled()) return;

      final JComponent c = getContextComponent();
      if (c instanceof JTable || c instanceof JList) {
        final ListSelectionModel model = c instanceof JTable ? ((JTable)c).getSelectionModel() 
                                                             : ((JList)c).getSelectionModel();
        final int size = c instanceof JTable ? ((JTable)c).getRowCount()  
                                             : ((JList)c).getModel().getSize();
        final int min = model.getMinSelectionIndex();
        final int max = model.getMaxSelectionIndex();

        if ((myButton == Buttons.UP && min < 1)
            || (myButton == Buttons.DOWN && max == size - 1)
            || (myButton != Buttons.ADD && size == 0)
            || (myButton == Buttons.EDIT && (min != max || min == -1))) {
          e.getPresentation().setEnabled(false);
        }
        else {
          e.getPresentation().setEnabled(isEnabled());
        }
      }
    }

    //@Override
    //public boolean isEnabled() {
    //  if (myButton == Buttons.REMOVE) {
    //    final JComponent c = getContextComponent();
    //    if (c instanceof JTable && ((JTable)c).isEditing()) return false;
    //  }
    //  return super.isEnabled();
    //}

    boolean isAddButton() {
      return myButton == Buttons.ADD;
    }

    boolean isRemoveButton() {
      return myButton == Buttons.REMOVE;
    }
  }

  public static ShortcutSet getCommonShortcut(Buttons button) {
    switch (button) {
      case ADD: return CommonShortcuts.getNewForDialogs();
      case EDIT: return CustomShortcutSet.fromString("ENTER");
      case REMOVE: return CustomShortcutSet.fromString(Platform.current().os().isMac() ? "meta BACK_SPACE" : "alt DELETE");
      case UP: return CommonShortcuts.MOVE_UP;
      case DOWN: return CommonShortcuts.MOVE_DOWN;
    }
    return null;
  }

  public interface ListenerFactory {
    Listener createListener(CommonActionsPanel panel);
  }
}

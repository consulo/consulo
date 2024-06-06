/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.ui;

import consulo.ide.setting.ui.MasterDetailsComponent;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.Comparing;
import consulo.util.lang.function.Condition;
import consulo.component.util.Iconable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ide.impl.idea.openapi.util.*;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.ui.image.Image;
import consulo.util.collection.HashingStrategy;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class NamedItemsListEditor<T> extends MasterDetailsComponent {
  public interface Cloner<T> {
    T cloneOf(T t);

    T copyOf(T t);
  }

  public interface Namer<T> {
    String getName(T t);

    boolean canRename(T item);

    void setName(T t, String name);
  }

  private final Namer<T> myNamer;
  private final Supplier<T> myFactory;
  private final Cloner<T> myCloner;
  private final List<T> myItems = new ArrayList<T>();
  private final HashingStrategy<T> myComparer;
  private List<T> myResultItems;
  private final List<T> myOriginalItems;
  private boolean myShowIcons;

  protected NamedItemsListEditor(Namer<T> namer, Supplier<T> factory, Cloner<T> cloner, HashingStrategy<T> comparer, List<T> items) {
    this(namer, factory, cloner, comparer, items, true);
  }

  protected NamedItemsListEditor(Namer<T> namer, Supplier<T> factory, Cloner<T> cloner, HashingStrategy<T> comparer, List<T> items, boolean initInConstructor) {
    myNamer = namer;
    myFactory = factory;
    myCloner = cloner;
    myComparer = comparer;

    myOriginalItems = items;
    myResultItems = items;
    if (initInConstructor) {
      reset();
      initTree();
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myResultItems = myOriginalItems;
    myItems.clear();

    clearChildren();
    for (T item : myOriginalItems) {
      addNewNode(myCloner.cloneOf(item));
    }

    super.reset();
  }

  @Override
  protected void processRemovedItems() {
  }

  @Override
  protected boolean wasObjectStored(Object editableObject) {
    return true;
  }

  protected String subjDisplayName() {
    return "item";
  }

  @Nullable
  public String askForProfileName(String titlePattern) {
    String title = MessageFormat.format(titlePattern, subjDisplayName());
    return Messages.showInputDialog("New " + subjDisplayName() + " name:", title, Messages.getQuestionIcon(), "", new InputValidator() {
      @Override
      public boolean checkInput(String s) {
        return s.length() > 0 && findByName(s) == null;
      }

      @Override
      public boolean canClose(String s) {
        return checkInput(s);
      }
    });
  }

  @Nullable
  private T findByName(String name) {
    for (T item : myItems) {
      if (Comparing.equal(name, myNamer.getName(item))) return item;
    }

    return null;
  }

  @Override
  @Nullable
  protected ArrayList<AnAction> createActions(boolean fromPopup) {
    ArrayList<AnAction> result = new ArrayList<AnAction>();
    result.add(new AddAction());

    result.add(new MyDeleteAction(forAll(new Condition<Object>() {
      @Override
      @SuppressWarnings({"unchecked"})
      public boolean value(Object o) {
        return canDelete((T)((MyNode)o).getConfigurable().getEditableObject());
      }
    })));

    result.add(new CopyAction());

    return result;
  }

  private void addNewNode(T item) {
    addNode(new MyNode(new ItemConfigurable(item)), myRoot);
    myItems.add(item);
  }

  protected boolean canDelete(T item) {
    return true;
  }

  protected abstract UnnamedConfigurable createConfigurable(T item);

  @Override
  protected void onItemDeleted(Object item) {
    myItems.remove((T)item);
  }

  protected void setDisplayName(T item, String name) {
    myNamer.setName(item, name);
  }

  public void setShowIcons(boolean showIcons) {
    myShowIcons = showIcons;
  }

  @Nullable
  protected UnnamedConfigurable getItemConfigurable(final T item) {
    final Ref<UnnamedConfigurable> result = new Ref<UnnamedConfigurable>();
    TreeUtil.traverse((TreeNode)myTree.getModel().getRoot(), new TreeUtil.Traverse() {
      @Override
      public boolean accept(Object node) {
        final MasterDetailsConfigurable configurable = (MasterDetailsConfigurable)((DefaultMutableTreeNode)node).getUserObject();
        if (configurable.getEditableObject() == item) {
          result.set(((ItemConfigurable)configurable).myConfigurable);
          return false;
        }
        else {
          return true;
        }
      }
    });
    return result.get();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    super.disposeUIResources();    //To change body of overridden methods use File | Settings | File Templates.
  }

  private class ItemConfigurable extends NamedConfigurable {
    private final T myItem;
    private final UnnamedConfigurable myConfigurable;

    public ItemConfigurable(T item) {
      super(myNamer.canRename(item), TREE_UPDATER);
      myItem = item;
      myConfigurable = createConfigurable(item);
    }

    @Override
    public void setDisplayName(String name) {
      NamedItemsListEditor.this.setDisplayName(myItem, name);
    }

    @Override
    public Object getEditableObject() {
      return myItem;
    }

    @Override
    public String getBannerSlogan() {
      return myNamer.getName(myItem);
    }

    @Override
    public JComponent createOptionsPanel() {
      return myConfigurable.createComponent();
    }

    @Override
    public String getDisplayName() {
      return myNamer.getName(myItem);
    }

    @Override
    public Image getIcon(boolean expanded) {
      if (myShowIcons && myConfigurable instanceof Iconable) {
        return ((Iconable)myConfigurable).getIcon(0);
      }
      return null;
    }

    @Override
    public boolean isModified() {
      return myConfigurable.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
      myConfigurable.apply();
    }

    @Override
    public void reset() {
      myConfigurable.reset();
    }

    @Override
    public void disposeUIResources() {
      myConfigurable.disposeUIResources();
    }
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    if (myResultItems.size() != myItems.size()) return true;

    for (int i = 0; i < myItems.size(); i++) {
      if (!myComparer.equals(myItems.get(i), myResultItems.get(i))) return true;
    }

    return super.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    myResultItems = myItems;
  }

  protected List<T> getCurrentItems() {
    return Collections.unmodifiableList(myItems);
  }

  public List<T> getItems() {
    return myResultItems;
  }

  public T getSelectedItem() {
    return (T)getSelectedObject();
  }


  private class CopyAction extends DumbAwareAction {
    public CopyAction() {
      super("Copy", "Copy", PlatformIconGroup.actionsCopy());
      registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)), myTree);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
      final String profileName = askForProfileName("Copy {0}");
      if (profileName == null) return;

      final T clone = myCloner.copyOf((T)getSelectedObject());
      myNamer.setName(clone, profileName);
      addNewNode(clone);
      selectNodeInTree(clone);
      onItemCloned(clone);
    }


    @Override
    public void update(AnActionEvent event) {
      super.update(event);
      event.getPresentation().setEnabled(getSelectedObject() != null);
    }
  }

  protected void onItemCloned(T clone) {
  }

  private class AddAction extends DumbAwareAction {
    public AddAction() {
      super("Add", "Add", IconUtil.getAddIcon());
      registerCustomShortcutSet(CommonShortcuts.INSERT, myTree);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
      final T newItem = createItem();
      if (newItem != null) {
        onItemCreated(newItem);
      }
    }
  }

  public void selectItem(T item) {
    selectNodeInTree(findByName(myNamer.getName(item)));
  }

  @Nullable
  protected T createItem() {
    final String name = askForProfileName("Create new {0}");
    if (name == null) return null;
    final T newItem = myFactory.get();
    myNamer.setName(newItem, name);
    return newItem;
  }

  protected void onItemCreated(T newItem) {
    addNewNode(newItem);
    selectNodeInTree(newItem);
  }

}

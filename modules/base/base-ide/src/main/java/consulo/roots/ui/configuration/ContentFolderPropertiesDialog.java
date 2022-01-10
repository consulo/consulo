/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.ui.configuration;

import com.intellij.ide.util.ChooseElementsDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ComboBoxCellEditor;
import com.intellij.util.ui.ListTableModel;
import consulo.roots.ContentFolderPropertyProvider;
import javax.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 05.02.14
 */
public class ContentFolderPropertiesDialog extends DialogWrapper {
  private static class Item {
    public ContentFolderPropertyProvider myProvider;
    public Key myKey;
    public Object myValue;

    private Item(ContentFolderPropertyProvider provider, Key key, Object value) {
      myProvider = provider;
      myKey = key;
      myValue = value;
    }
  }

  private static class ChooseProvidersDialog extends ChooseElementsDialog<ContentFolderPropertyProvider<?>> {

    public ChooseProvidersDialog(Project project, List<? extends ContentFolderPropertyProvider<?>> items, String title, String description) {
      super(project, items, title, description);
    }

    @Override
    protected String getItemText(ContentFolderPropertyProvider<?> item) {
      return item.getKey().toString();
    }

    @Nullable
    @Override
    protected Image getItemIcon(ContentFolderPropertyProvider<?> item) {
      return null;
    }
  }

  private static final ColumnInfo[] ourColumns = new ColumnInfo[]{new ColumnInfo<Item, String>("Name") {

    @Nullable
    @Override
    public String valueOf(Item info) {
      return info.myKey.toString();
    }
  }, new ColumnInfo<Item, String>("Value") {

    @Nullable
    @Override
    public String valueOf(Item info) {
      return String.valueOf(info.myValue);
    }

    @Nullable
    @Override
    public TableCellRenderer getRenderer(Item item) {
      return new ComboBoxTableRenderer<Object>(item.myProvider.getValues());
    }

    @Nullable
    @Override
    public TableCellEditor getEditor(final Item o) {
      return new ComboBoxCellEditor() {
        @Override
        protected List<String> getComboBoxItems() {
          Object[] values = o.myProvider.getValues();
          List<String> items = new ArrayList<String>();
          for (Object value : values) {
            items.add(String.valueOf(value));
          }
          return items;
        }
      };
    }

    @Override
    public boolean isCellEditable(Item item) {
      return item.myProvider != null;
    }

    @Override
    public void setValue(Item item, String value) {
      item.myValue = item.myProvider.fromString(value);
    }
  }};

  @Nullable private final Project myProject;
  private final ContentFolder myContentFolder;
  private List<Item> myItems = new ArrayList<Item>();

  public ContentFolderPropertiesDialog(@Nullable Project project, ContentFolder contentFolder) {
    super(project);
    myProject = project;
    myContentFolder = contentFolder;

    for (Map.Entry<Key, Object> entry : contentFolder.getProperties().entrySet()) {
      ContentFolderPropertyProvider provider = null;
      for (ContentFolderPropertyProvider propertyProvider : ContentFolderPropertyProvider.EP_NAME.getExtensions()) {
        if (propertyProvider.getKey() == entry.getKey()) {
          provider = propertyProvider;
          break;
        }
      }

      myItems.add(new Item(provider, entry.getKey(), entry.getValue()));
    }

    setTitle(ProjectBundle.message("module.paths.properties.tooltip"));

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    ListTableModel<Item> model = new ListTableModel<Item>(ourColumns, myItems, 0);
    TableView<Item> table = new TableView<Item>(model);

    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table);
    decorator.setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {
        List<ContentFolderPropertyProvider<?>> list = new ArrayList<ContentFolderPropertyProvider<?>>();
        loop:
        for (ContentFolderPropertyProvider propertyProvider : ContentFolderPropertyProvider.EP_NAME.getExtensions()) {
          for (Item item : myItems) {
            if (item.myProvider == propertyProvider) {
              continue loop;
            }
          }

          list.add(propertyProvider);
        }

        ChooseProvidersDialog d = new ChooseProvidersDialog(myProject, list, ProjectBundle.message("module.paths.add.properties.title"),
                                                            ProjectBundle.message("module.paths.add.properties.desc"));

        List<ContentFolderPropertyProvider<?>> temp = d.showAndGetResult();
        for (ContentFolderPropertyProvider<?> propertyProvider : temp) {
          model.addRow(new Item(propertyProvider, propertyProvider.getKey(), propertyProvider.getValues()[0]));
        }
      }
    });
    decorator.disableUpDownActions();
    return decorator.createPanel();
  }

  @Override
  protected void doOKAction() {
    Map<Key, Object> properties = myContentFolder.getProperties();
    for (Key<?> key : properties.keySet()) {
      myContentFolder.setPropertyValue(key, null);
    }

    for (Item item : myItems) {
      myContentFolder.setPropertyValue(item.myKey, item.myValue);
    }
    super.doOKAction();
  }
}

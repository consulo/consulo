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
package consulo.ide.impl.roots.ui.configuration;

import consulo.application.Application;
import consulo.content.ContentFolderPropertyProvider;
import consulo.disposer.Disposable;
import consulo.module.content.layer.ContentFolder;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.dialog.ChooseItemsDialogDescriptor;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2014-02-05
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ContentFolderPropertiesDialogDescriptor extends DialogDescriptor {
    private final ContentFolder myContentFolder;
    private final MutableFlatDataModel<ContentFolderPropertyItem> myModel;

    private @Nullable Table<ContentFolderPropertyItem> myTable;

    public ContentFolderPropertiesDialogDescriptor(ContentFolder contentFolder) {
        super(ProjectLocalize.modulePathsPropertiesTooltip());
        myContentFolder = contentFolder;

        List<ContentFolderPropertyItem> items = new ArrayList<>();
        for (Map.Entry<Key, Object> entry : contentFolder.getProperties().entrySet()) {
            ContentFolderPropertyProvider provider = Application.get()
                .getExtensionPoint(ContentFolderPropertyProvider.class)
                .findFirstSafe(propertyProvider -> propertyProvider.getKey() == entry.getKey());
            items.add(new ContentFolderPropertyItem(provider, entry.getKey(), entry.getValue()));
        }
        myModel = FlatDataModel.of(items);
    }

    @RequiredUIAccess
    public static CompletableFuture<Boolean> showAsync(Component parent, ContentFolder contentFolder) {
        ContentFolderPropertiesDialogDescriptor descriptor = new ContentFolderPropertiesDialogDescriptor(contentFolder);

        Dialog dialog = Application.get().getInstance(DialogService.class).build(parent, descriptor);

        return dialog.showAsync().handleAsync((value, error) -> {
            if (error != null || value == null) {
                return Boolean.FALSE;
            }

            descriptor.applyProperties();
            return Boolean.TRUE;
        }, UIAccess.current());
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(40, 15);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        Table<ContentFolderPropertyItem> table = Table.create(myModel);
        table.addColumn(ProjectLocalize.modulePathsPropertiesNameColumn(), item -> item.getKey().toString());
        table.addColumn(ProjectLocalize.modulePathsPropertiesValueColumn(), item -> String.valueOf(item.getValue()))
            .setEditor(new ValueEditor());
        myTable = table;

        return ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new PropertyAddAction())
            .addOrReplaceAction(new PropertyRemoveAction())
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .build();
    }

    @RequiredUIAccess
    private void applyProperties() {
        for (Key key : new ArrayList<>(myContentFolder.getProperties().keySet())) {
            myContentFolder.setPropertyValue(key, null);
        }

        for (ContentFolderPropertyItem item : myModel) {
            myContentFolder.setPropertyValue(item.getKey(), item.getValue());
        }
    }

    private class ValueEditor implements TableItemEditor<ContentFolderPropertyItem, String> {
        @RequiredUIAccess
        @Override
        public ValueComponent<String> createComponent(ContentFolderPropertyItem item) {
            List<String> values = new ArrayList<>();
            ContentFolderPropertyProvider provider = item.getProvider();
            if (provider != null) {
                for (Object value : provider.getValues()) {
                    values.add(String.valueOf(value));
                }
            }
            return ComboBox.create(values);
        }

        @RequiredUIAccess
        @Override
        public void commit(ContentFolderPropertyItem item, @Nullable String value) {
            ContentFolderPropertyProvider provider = item.getProvider();
            if (provider != null && value != null) {
                item.setValue(provider.fromString(value));
                myModel.update(item);
            }
        }

        @Override
        public boolean isEditable(ContentFolderPropertyItem item) {
            return item.getProvider() != null;
        }
    }

    private class PropertyAddAction extends AddAction<ContentFolderPropertyItem> {
        @RequiredUIAccess
        @Override
        protected void doAdd(AnActionEvent e) {
            List<ContentFolderPropertyProvider<?>> providers = new ArrayList<>();
            Application.get().getExtensionPoint(ContentFolderPropertyProvider.class).forEachExtensionSafe(provider -> {
                for (ContentFolderPropertyItem item : myModel) {
                    if (item.getProvider() == provider) {
                        return;
                    }
                }
                providers.add(provider);
            });

            if (providers.isEmpty()) {
                return;
            }

            UIAccess uiAccess = UIAccess.current();
            ChooseItemsDialogDescriptor.showAsync(
                myTable,
                ProjectLocalize.modulePathsAddPropertiesTitle(),
                ProjectLocalize.modulePathsAddPropertiesDesc(),
                providers,
                (provider, presentation) -> presentation.append(provider.getKey().toString()),
                true,
                List.of()
            ).whenCompleteAsync((chosen, error) -> {
                if (chosen == null) {
                    return;
                }

                for (ContentFolderPropertyProvider<?> provider : chosen) {
                    Object[] values = provider.getValues();
                    myModel.add(new ContentFolderPropertyItem(provider, provider.getKey(), values.length == 0 ? null : values[0]));
                }
            }, uiAccess);
        }
    }

    private class PropertyRemoveAction extends RemoveAction<ContentFolderPropertyItem> {
        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            for (ContentFolderPropertyItem item : getSelectedValues(e)) {
                myModel.remove(item);
            }
        }
    }
}

// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTreeUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.EmptyAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.impl.internal.action.QuickListsManager;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Chooses the actions to add to a customized group, and the icon of a single chosen action.
 */
public class AddActionDialogDescriptor extends DialogDescriptor {
    private static final Logger LOG = Logger.getInstance(AddActionDialogDescriptor.class);

    private final CustomActionsSchemaImpl mySchema;
    private final CustomActionsSchemaImpl myApplicationSchema;
    private final boolean myWithNoneItem;

    private KeymapGroupImpl myRoot = new KeymapGroupImpl("root");
    private @Nullable Tree<CustomizationTreeItem> myTree;
    private @Nullable BrowseIconsComboBox myComboBox;
    private @Nullable TextBox myFilter;
    private @Nullable Dialog myDialog;

    public AddActionDialogDescriptor(CustomActionsSchemaImpl schema, CustomActionsSchemaImpl applicationSchema, boolean withNoneItem) {
        super(KeyMapLocalize.actionChooseActionsToAdd());
        mySchema = schema;
        myApplicationSchema = applicationSchema;
        myWithNoneItem = withNoneItem;
    }

    /**
     * @return the components to add: action ids, separators, groups or quick lists
     */
    @RequiredUIAccess
    public static CompletableFuture<List<Object>> showAsync(
        Component parent,
        CustomActionsSchemaImpl schema,
        CustomActionsSchemaImpl applicationSchema,
        boolean withNoneItem
    ) {
        AddActionDialogDescriptor descriptor = new AddActionDialogDescriptor(schema, applicationSchema, withNoneItem);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(parent, descriptor);
        descriptor.myDialog = dialog;
        return dialog.showAsync().thenApply(value -> descriptor.applyAndGetAddedActions());
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        TreeModel<CustomizationTreeItem> model = (nodeFactory, parentValue) -> {
            KeymapGroupImpl group = parentValue == null ? myRoot : parentValue.getGroup();
            if (group == null) {
                return;
            }
            for (CustomizationTreeItem item : CustomizationTreeItem.children(group)) {
                TreeNode<CustomizationTreeItem> node = nodeFactory.apply(item);
                node.setLeaf(item.getGroup() == null);
                node.setRenderer(this::render);
            }
        };

        Tree<CustomizationTreeItem> tree = Tree.create(model);
        Disposer.register(uiDisposable, tree.destroyHook());
        tree.setAllowMultipleSelect(true);
        tree.setSpeedSearchConverter(node -> {
            CustomizationTreeItem item = node.getValue();
            return item == null ? "" : CustomizationUtil.getText(item.getComponent());
        });
        tree.addDoubleClickListener(event -> {
            Dialog dialog = myDialog;
            if (dialog != null && !getSelectedItems().isEmpty()) {
                dialog.doOkAction(getOkValue());
            }
        });
        tree.addSelectListener(event -> onSelectionChanged());
        myTree = tree;

        BrowseIconsComboBox comboBox = new BrowseIconsComboBox(mySchema, myApplicationSchema, myWithNoneItem);
        myComboBox = comboBox;

        TextBox filter = TextBox.create();
        filter.addStyle(TextBoxStyle.HAS_CLEAR_BUTTON);
        Label searchIcon = Label.create();
        searchIcon.setImage(PlatformIconGroup.actionsSearch());
        filter.setPrefixComponent(searchIcon);
        filter.addValueListener(event -> CustomizableActionsPanel.selectFirstMatch(tree, myRoot, StringUtil.notNullize(event.getValue())));
        myFilter = filter;

        Label hint = Label.create(KeyMapLocalize.browseCustomIconHint());
        hint.addStyle(LabelStyle.FONT_SMALL);
        hint.setForegroundColor(ComponentColors.INFO_FOREGROUND);

        VerticalLayout bottom = VerticalLayout.create();
        bottom.add(LabeledLayout.create(KeyMapLocalize.labelIconPath(), comboBox.getComponent()));
        bottom.add(hint);

        DockLayout layout = DockLayout.create();
        layout.top(filter);
        layout.center(ScrollableLayout.create(tree));
        layout.bottom(bottom);

        UIAccess uiAccess = UIAccess.current();
        ActionsTreeUtil.createMainGroupAsync(
            null,
            null,
            QuickListsManager.getInstance().getAllQuickLists(),
            null,
            true,
            action -> !(action instanceof AnSeparator) && !(action instanceof EmptyAction)
        ).whenCompleteAsync((rootGroup, error) -> {
            if (error != null) {
                LOG.error("Failed to build the action tree", error);
                return;
            }
            rootGroup.insertChild(0, AnSeparator.getInstance());
            myRoot = rootGroup;
            tree.refreshAll();
        }, uiAccess);

        return layout;
    }

    private void render(CustomizationTreeItem item, TextItemPresentation presentation) {
        Object component = item.getComponent();
        if (component instanceof AnSeparator) {
            presentation.withIcon(PlatformIconGroup.generalSeparatorh());
            presentation.append(KeyMapLocalize.actionSeparator());
            return;
        }
        Function<String, @Nullable Image> iconOf = actionId -> mySchema.getActionIcon(actionId, myApplicationSchema);
        CustomizationUtil.customizePresentation(component, presentation, true, iconOf);
    }

    @RequiredUIAccess
    private void onSelectionChanged() {
        BrowseIconsComboBox comboBox = myComboBox;
        if (comboBox == null) {
            return;
        }
        List<CustomizationTreeItem> selected = getSelectedItems();
        comboBox.setEnabled(selected.size() < 2);
        if (selected.size() == 1) {
            comboBox.selectIconForAction(selected.get(0).getActionId());
        }
    }

    private List<CustomizationTreeItem> getSelectedItems() {
        Tree<CustomizationTreeItem> tree = myTree;
        if (tree == null) {
            return List.of();
        }
        List<CustomizationTreeItem> result = new ArrayList<>();
        for (TreeNode<CustomizationTreeItem> node : tree.getSelectedNodes()) {
            CustomizationTreeItem item = node.getValue();
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        return myFilter;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofPixel(500, 600);
    }

    @Override
    public String getDimensionServiceKey() {
        return "#consulo.ide.impl.idea.ide.ui.customization.CustomizableActionsPanel.FindAvailableActionsDialog";
    }

    @RequiredUIAccess
    private List<Object> applyAndGetAddedActions() {
        List<CustomizationTreeItem> selected = getSelectedItems();
        BrowseIconsComboBox comboBox = myComboBox;
        if (selected.size() == 1 && comboBox != null) {
            ActionIconInfo iconInfo = comboBox.getSelectedItem();
            String actionId = selected.get(0).getActionId();
            if (iconInfo != null && actionId != null) {
                CustomizableActionsPanel.setCustomIcon(mySchema, myApplicationSchema, actionId, iconInfo);
            }
        }

        List<Object> result = new ArrayList<>();
        for (CustomizationTreeItem item : selected) {
            result.add(item.getComponent());
        }
        return result;
    }
}

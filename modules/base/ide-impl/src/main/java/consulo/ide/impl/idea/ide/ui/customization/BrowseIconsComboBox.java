// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserFactory;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Alerts;
import consulo.ui.Button;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Icons to choose for an action: none (in menus), the default icons, the custom icon files in use and the icons of all
 * actions, plus a browse button adding an icon file.
 */
public final class BrowseIconsComboBox {
    private final CustomActionsSchemaImpl mySchema;
    private final CustomActionsSchemaImpl myApplicationSchema;
    private final MutableFlatDataModel<ActionIconInfo> myModel;
    private final ComboBox<ActionIconInfo> myComboBox;
    private final Button myBrowseButton;
    private final DockLayout myLayout;
    private final int myDefaultIconsCount;

    @RequiredUIAccess
    public BrowseIconsComboBox(CustomActionsSchemaImpl schema, CustomActionsSchemaImpl applicationSchema, boolean withNoneItem) {
        mySchema = schema;
        myApplicationSchema = applicationSchema;

        List<ActionIconInfo> items = new ArrayList<>();
        if (withNoneItem) {
            items.add(ActionIconInfo.NONE);
        }
        List<ActionIconInfo> defaultIcons = ActionIconInfo.getDefaultIcons();
        items.addAll(defaultIcons);
        myDefaultIconsCount = items.size();
        for (ActionIconInfo info : ActionIconInfo.getCustomIcons(schema)) {
            if (items.stream().noneMatch(it -> Objects.equals(it.getIconPath(), info.getIconPath()))) {
                items.add(info);
            }
        }
        for (ActionIconInfo info : ActionIconInfo.getAvailableIcons()) {
            if (defaultIcons.stream().noneMatch(it -> it.getIcon() == info.getIcon())) {
                items.add(info);
            }
        }

        myModel = FlatDataModel.of(items);
        myComboBox = ComboBox.create(myModel);
        myComboBox.setRenderer((presentation, item) -> {
            ActionIconInfo info = item.getValue();
            if (info == null) {
                return;
            }
            presentation.withIcon(info.getIcon() != null ? info.getIcon() : Image.empty(Image.DEFAULT_ICON_SIZE));
            presentation.append(info.getText());
        });

        myBrowseButton = Button.create(LocalizeValue.empty(), event -> browseIconAndSelect());
        myBrowseButton.setIcon(PlatformIconGroup.nodesFolderopened());
        myBrowseButton.setToolTipText(KeyMapLocalize.titleBrowseIcon());

        myLayout = DockLayout.create();
        myLayout.center(myComboBox);
        myLayout.right(myBrowseButton);
    }

    public Component getComponent() {
        return myLayout;
    }

    public ComboBox<ActionIconInfo> getComboBox() {
        return myComboBox;
    }

    public @Nullable ActionIconInfo getSelectedItem() {
        return myComboBox.getValue();
    }

    @RequiredUIAccess
    public void setEnabled(boolean enabled) {
        myComboBox.setEnabled(enabled);
        myBrowseButton.setEnabled(enabled);
    }

    @RequiredUIAccess
    public void selectIconForAction(@Nullable String actionId) {
        if (actionId == null) {
            selectByCondition(info -> info == ActionIconInfo.NONE);
            return;
        }

        String iconReference = mySchema.getIconCustomizations().get(actionId);
        if (iconReference != null) {
            if (selectByCondition(info -> iconReference.equals(info.getIconPath()) || iconReference.equals(info.getActionId()))) {
                return;
            }
        }
        else if (selectByCondition(info -> actionId.equals(info.getActionId()))) {
            return;
        }

        Image icon = mySchema.getActionIcon(actionId, myApplicationSchema);
        if (icon == null || !selectByCondition(info -> info.getIcon() == icon)) {
            if (!selectByCondition(info -> info == ActionIconInfo.NONE)) {
                myComboBox.setValue(null);
            }
        }
    }

    @RequiredUIAccess
    private boolean selectByCondition(Predicate<ActionIconInfo> predicate) {
        for (ActionIconInfo info : myModel) {
            if (predicate.test(info)) {
                myComboBox.setValue(info);
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private void browseIconAndSelect() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withExtensionFilter(KeyMapLocalize.iconFileFilterLabel(), "svg", "png")
            .withTitle(KeyMapLocalize.titleBrowseIcon())
            .withDescription(KeyMapLocalize.browseCustomIconHint());

        UIAccess uiAccess = UIAccess.current();
        FileChooserFactory.getInstance().newBuilder(descriptor).showSingleAsVirtualFile().whenCompleteAsync((file, error) -> {
            if (file != null) {
                selectIconFile(file);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void selectIconFile(VirtualFile file) {
        String path = FileUtil.toSystemIndependentName(file.getPath());
        Image icon = CustomActionsSchemaImpl.loadIconFile(path);
        if (icon == null) {
            Alerts.okError(KeyMapLocalize.errorFileNotFoundMessage(path)).showAsync(myLayout);
            return;
        }
        if (icon.getWidth() > Image.DEFAULT_ICON_SIZE || icon.getHeight() > Image.DEFAULT_ICON_SIZE) {
            Alerts.okError(KeyMapLocalize.customIconValidationMessage()).showAsync(myLayout);
            return;
        }

        if (!selectByCondition(info -> path.equals(info.getIconPath()))) {
            ActionIconInfo info = new ActionIconInfo(icon, LocalizeValue.of(file.getName()), null, path);
            myModel.add(info, myDefaultIconsCount);
            myComboBox.setValue(info);
        }
    }
}

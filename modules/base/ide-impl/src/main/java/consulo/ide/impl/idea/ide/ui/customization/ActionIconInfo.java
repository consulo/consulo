// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.internal.ActionStubBase;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * An icon the customization page offers for an action.
 *
 * @param actionId id of the action that uses this icon
 * @param iconPath icon key ({@code groupId@imageId}) or path of the icon file
 */
public final class ActionIconInfo {
    public static final ActionIconInfo NONE = new ActionIconInfo(null, KeyMapLocalize.defaultIconsNoneText(), "", null);

    private final @Nullable Image myIcon;
    private final LocalizeValue myText;
    private final @Nullable String myActionId;
    private final @Nullable String myIconPath;

    public ActionIconInfo(@Nullable Image icon, LocalizeValue text, @Nullable String actionId, @Nullable String iconPath) {
        myIcon = icon;
        myText = text;
        myActionId = actionId;
        myIconPath = iconPath;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public LocalizeValue getText() {
        return myText;
    }

    public @Nullable String getActionId() {
        return myActionId;
    }

    public @Nullable String getIconPath() {
        return myIconPath;
    }

    public String getIconReference() {
        if (myIconPath != null) {
            return myIconPath;
        }
        if (myActionId != null) {
            return myActionId;
        }
        throw new IllegalStateException("Either actionId or iconPath should be set");
    }

    @Override
    public String toString() {
        return myText.get();
    }

    public static List<ActionIconInfo> getDefaultIcons() {
        List<ActionIconInfo> icons = new ArrayList<>();
        addIconInfo(icons, PlatformIconGroup.toolbarUnknown(), KeyMapLocalize.defaultIconsUnknownText());
        addIconInfo(icons, PlatformIconGroup.generalAdd(), KeyMapLocalize.defaultIconsAddText());
        addIconInfo(icons, PlatformIconGroup.generalRemove(), KeyMapLocalize.defaultIconsRemoveText());
        addIconInfo(icons, PlatformIconGroup.actionsEdit(), KeyMapLocalize.defaultIconsEditText());
        addIconInfo(icons, PlatformIconGroup.generalFilter(), KeyMapLocalize.defaultIconsFilterText());
        addIconInfo(icons, PlatformIconGroup.actionsFind(), KeyMapLocalize.defaultIconsFindText());
        addIconInfo(icons, PlatformIconGroup.generalGearplain(), KeyMapLocalize.defaultIconsGearPlainText());
        addIconInfo(icons, PlatformIconGroup.actionsListfiles(), KeyMapLocalize.defaultIconsListFilesText());
        addIconInfo(icons, PlatformIconGroup.actionsExport(), KeyMapLocalize.defaultIconsExportText());
        addIconInfo(icons, PlatformIconGroup.actionsImport(), KeyMapLocalize.defaultIconsImportText());
        return icons;
    }

    private static void addIconInfo(List<ActionIconInfo> icons, Image icon, LocalizeValue text) {
        if (icon instanceof ImageKey imageKey) {
            icons.add(new ActionIconInfo(icon, text, null, CustomActionsSchemaImpl.getIconReference(imageKey)));
        }
    }

    public static List<ActionIconInfo> getAvailableIcons() {
        ActionManager actionManager = ActionManager.getInstance();
        List<ActionIconInfo> icons = new ArrayList<>();
        for (String actionId : actionManager.getActionIds("")) {
            AnAction action = actionManager.getActionOrStub(actionId);
            if (action == null) {
                continue;
            }

            Image icon;
            if (action instanceof ActionStubBase stub) {
                icon = ImageKey.fromString(stub.getIconPath(), Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
            }
            else {
                icon = action.getTemplatePresentation().getIcon();
            }

            if (icon != null) {
                String text = action.getTemplatePresentation().getText();
                icons.add(new ActionIconInfo(icon, LocalizeValue.of(StringUtil.isEmpty(text) ? actionId : text), actionId, null));
            }
        }
        icons.sort(Comparator.comparing(ActionIconInfo::toString, String.CASE_INSENSITIVE_ORDER));
        return icons;
    }

    public static List<ActionIconInfo> getCustomIcons(CustomActionsSchemaImpl schema) {
        ActionManager actionManager = ActionManager.getInstance();
        List<ActionIconInfo> icons = new ArrayList<>();
        for (Map.Entry<String, @Nullable String> entry : schema.getIconCustomizations().entrySet()) {
            String iconReference = entry.getValue();
            if (iconReference == null
                || actionManager.getActionOrStub(iconReference) != null
                || ImageKey.fromString(iconReference, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE) != null) {
                continue;
            }

            Image icon = CustomActionsSchemaImpl.loadIconFile(iconReference);
            if (icon != null) {
                String name = iconReference.substring(iconReference.lastIndexOf('/') + 1);
                icons.add(new ActionIconInfo(icon, LocalizeValue.of(name), entry.getKey(), iconReference));
            }
        }
        return icons;
    }
}

// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @see DaemonBoundCodeVisionProvider
 */
public abstract class CodeVisionEntry extends UserDataHolderBase {
    /**
     * The provider ID of this entry
     */
    public final String providerId;

    /**
     * Icon to show near this entry in editor
     */
    public final @Nullable Icon icon;

    /**
     * The text in 'More' menu
     */
    public final String longPresentation;

    /**
     * Tooltip text
     */
    public final String tooltip;

    /**
     * Extra actions available with right click on inlay
     */
    public final List<CodeVisionEntryExtraActionModel> extraActions;

    /**
     * Defines if we show entry in 'More' popup
     */
    public boolean showInMorePopup = true;

    protected CodeVisionEntry(String providerId,
                               @Nullable Icon icon,
                               String longPresentation,
                               String tooltip,
                               List<CodeVisionEntryExtraActionModel> extraActions) {
        this.providerId = providerId;
        this.icon = icon;
        this.longPresentation = longPresentation;
        this.tooltip = tooltip;
        this.extraActions = extraActions;
    }

    @Override
    public String toString() {
        return "CodeVisionEntry('" + longPresentation + "')";
    }
}

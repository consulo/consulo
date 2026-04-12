// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.searchEverywhere;

import consulo.ui.ex.action.AnAction;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for contributors that provide actions on the right side of the SE search input field.
 */
public interface SearchFieldActionsContributor {
    /**
     * Creates actions that are placed on the right side of SE search input field.
     *
     * @param registerShortcut callback to register keyboard shortcuts for actions
     * @param onChanged        callback to trigger search rebuild when action state changes
     * @return list of actions
     */
    List<AnAction> createRightActions(Consumer<AnAction> registerShortcut, Runnable onChanged);
}

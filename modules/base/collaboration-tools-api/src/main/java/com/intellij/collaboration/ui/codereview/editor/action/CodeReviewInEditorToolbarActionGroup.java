// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor.action;

import com.intellij.collaboration.ui.codereview.diff.DiscussionsViewOption;
import com.intellij.collaboration.ui.codereview.editor.CodeReviewInEditorViewModel;
import consulo.application.dumb.DumbAware;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.HelpTooltip;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.util.function.Supplier;

@ApiStatus.Internal
public final class CodeReviewInEditorToolbarActionGroup extends ActionGroup implements DumbAware {
    private final CodeReviewInEditorViewModel vm;
    private final @Nullable Supplier<@Nls String> customWarningSupplier;

    private final AnAction updateAction;
    private final AnAction disableReviewAction;
    private final AnAction hideResolvedAction;
    private final AnAction showAllAction;

    public CodeReviewInEditorToolbarActionGroup(@Nonnull CodeReviewInEditorViewModel vm) {
        this(vm, null);
    }

    public CodeReviewInEditorToolbarActionGroup(
        @Nonnull CodeReviewInEditorViewModel vm,
        @Nullable Supplier<@Nls String> customWarningSupplier
    ) {
        this.vm = vm;
        this.customWarningSupplier = customWarningSupplier;
        this.updateAction = new UpdateAction();
        this.disableReviewAction = new ViewOptionToggleAction(
            DiscussionsViewOption.DONT_SHOW,
            CollaborationToolsLocalize.reviewEditorActionDisableText().get()
        );
        this.hideResolvedAction = new ViewOptionToggleAction(
            DiscussionsViewOption.UNRESOLVED_ONLY,
            CollaborationToolsLocalize.reviewEditorActionShowUnresolvedText().get()
        );
        this.showAllAction = new ViewOptionToggleAction(
            DiscussionsViewOption.ALL,
            CollaborationToolsLocalize.reviewEditorActionShowAllText().get()
        );

        getTemplatePresentation().setPopupGroup(true);
        getTemplatePresentation().putClientProperty(ActionUtil.HIDE_DROPDOWN_ICON, true);
        getTemplatePresentation().putClientProperty(ActionUtil.USE_SMALL_FONT_IN_TOOLBAR, true);
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public AnAction @Nonnull [] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[]{updateAction, Separator.create(), disableReviewAction, hideResolvedAction, showAllAction};
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean shown = vm.getDiscussionsViewOption().getValue() != DiscussionsViewOption.DONT_SHOW;
        boolean synced = !(Boolean) vm.getUpdateRequired().getValue();
        e.getPresentation().setDescriptionValue(CollaborationToolsLocalize.reviewEditorModeDescriptionTitle());
        String customWarning = customWarningSupplier != null ? customWarningSupplier.get() : null;
        String detailedDescription = customWarning != null
            ? customWarning
            : CollaborationToolsLocalize.reviewEditorModeDescription().get();
        HelpTooltip tooltip = new HelpTooltip()
            .setTitle(CollaborationToolsLocalize.reviewEditorModeDescriptionTitle().get())
            .setDescription(detailedDescription);
        e.getPresentation().putClientProperty(ActionButton.CUSTOM_HELP_TOOLTIP, tooltip);

        if (shown) {
            e.getPresentation().setTextValue(CollaborationToolsLocalize.reviewEditorModeTitle());
            e.getPresentation().putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true);
            e.getPresentation().setIcon(customWarning != null || !synced ? getWarningIcon() : null);
        }
        else {
            e.getPresentation().setTextValue(LocalizeValue.empty());
            e.getPresentation().putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, false);
            e.getPresentation().setIcon(PlatformIconGroup.actionsPreview());
        }
    }

    private static @Nonnull Icon getWarningIcon() {
        HighlightDisplayLevel level = HighlightDisplayLevel.find(HighlightSeverity.WARNING);
        return level != null && level.getIcon() != null ? level.getIcon() : PlatformIconGroup.generalWarning();
    }

    private final class UpdateAction extends DumbAwareAction {
        UpdateAction() {
            super(CollaborationToolsLocalize.reviewEditorActionUpdateText(), LocalizeValue.empty(), PlatformIconGroup.actionsCheckout());
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible((Boolean) vm.getUpdateRequired().getValue());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            vm.updateBranch();
        }
    }

    private final class ViewOptionToggleAction extends ToggleAction implements DumbAware {
        private final DiscussionsViewOption option;

        ViewOptionToggleAction(
            @Nonnull DiscussionsViewOption option,
            @Nonnull @NlsActions.ActionText String text
        ) {
            super(text);
            this.option = option;
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            if (option == DiscussionsViewOption.DONT_SHOW) {
                e.getPresentation().setKeepPopupOnPerform(KeepPopupOnPerform.Never);
            }
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return vm.getDiscussionsViewOption().getValue() == option;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            vm.setDiscussionsViewOption(option);
        }
    }
}

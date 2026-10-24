// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.comment;

import consulo.diff.impl.internal.util.DiffDrawUtil;
import consulo.diff.util.TextDiffType;
import consulo.disposer.Disposable;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.review.util.ReviewUIUtil;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestedChangeBlock;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestedChangeSupport;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionApplicability;

import java.util.ArrayList;
import java.util.List;

final class CodeReviewSuggestedChangeComponentFactory {
    private CodeReviewSuggestedChangeComponentFactory() {
    }

    @RequiredUIAccess
    static Component createIn(Disposable parentDisposable, CodeReviewCommentBodyViewModelImpl vm, CodeReviewSuggestedChangeBlock block) {
        Card card = Card.create();
        card.addStyle(CardStyle.OUTLINED);
        card.addStyle(CardStyle.COMPACT);
        card.setTitle(ReviewLocalize.reviewSuggestedChangeTitle());

        CodeReviewSuggestedChangeSupport support = vm.getSupport();
        if (support.canApplyLocally()) {
            OptionButton optionButton = OptionButton.create(ReviewLocalize.reviewSuggestedChangeApplyLocally(), event -> {
                TextFilePatch patch = block.getPatch();
                if (patch != null) {
                    vm.applySuggestionLocally(patch);
                }
            });
            if (support.canCommit()) {
                optionButton.setOptions(List.of(OptionButtonItem.of(ReviewLocalize.reviewSuggestedChangeCommitAction(),
                    event -> showCommitPopup(vm, block, optionButton))));
            }
            applyApplicability(optionButton, block.getApplicability());
            ReviewUIUtil.bind(parentDisposable, vm.isOnReviewBranch(), isOnReviewBranch -> {
                if (!isOnReviewBranch) {
                    optionButton.setEnabled(false);
                    optionButton.setToolTipText(ReviewLocalize.reviewSuggestedChangeTooltipDifferentBranch());
                }
                else {
                    applyApplicability(optionButton, block.getApplicability());
                }
            });
            card.setHeaderSuffix(optionButton);
        }

        card.add(createSuggestedChangeContent(parentDisposable, vm, block));
        return card;
    }

    @RequiredUIAccess
    private static Component createSuggestedChangeContent(Disposable parentDisposable,
                                                          CodeReviewCommentBodyViewModelImpl vm,
                                                          CodeReviewSuggestedChangeBlock block) {
        TextFilePatch patch = block.getPatch();
        PatchHunk hunk = patch == null || patch.getHunks().isEmpty() ? null : patch.getHunks().get(0);
        if (hunk == null) {
            return createHighlightedContent(vm, block);
        }
        return vm.getDiffComponentFactory().createDiffComponent(parentDisposable, hunk, null);
    }

    /**
     * The commented lines over the suggested ones, coloured the way a diff colours deleted and inserted lines, for a
     * suggestion which cannot be applied any more.
     */
    @RequiredUIAccess
    private static Component createHighlightedContent(CodeReviewCommentBodyViewModelImpl vm, CodeReviewSuggestedChangeBlock block) {
        List<String> changed = trimStartWithMinIndent(block.getChangedLines());
        List<String> suggested = trimStartWithMinIndent(block.getSuggestedLines());
        List<String> lines = new ArrayList<>(changed);
        lines.addAll(suggested);

        return vm.getDiffComponentFactory().createDiffEditor(String.join("\n", lines), editor -> {
            editor.getSettings().setLineNumbersShown(false);
            if (!changed.isEmpty()) {
                DiffDrawUtil.createHighlighter(editor, 0, changed.size(), TextDiffType.DELETED, false);
            }
            if (!suggested.isEmpty()) {
                DiffDrawUtil.createHighlighter(editor, changed.size(), lines.size(), TextDiffType.INSERTED, false);
            }
        });
    }

    static List<String> trimStartWithMinIndent(List<String> lines) {
        List<String> result = new ArrayList<>(lines);
        if (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }
        if (result.isEmpty()) {
            return lines;
        }

        int minIndent = Integer.MAX_VALUE;
        for (String line : result) {
            int currentIndent = 0;
            for (int i = 0; i < line.length() && line.charAt(i) == ' '; i++) {
                currentIndent++;
            }
            minIndent = Math.min(minIndent, currentIndent);
        }
        List<String> trimmed = new ArrayList<>(result.size());
        for (String line : result) {
            trimmed.add(line.substring(Math.min(minIndent, line.length())));
        }
        return trimmed;
    }

    @RequiredUIAccess
    private static void applyApplicability(OptionButton optionButton, CodeReviewSuggestionApplicability applicability) {
        optionButton.setEnabled(applicability == CodeReviewSuggestionApplicability.APPLICABLE);
        optionButton.setToolTipText(switch (applicability) {
            case APPLICABLE -> LocalizeValue.empty();
            case RESOLVED -> ReviewLocalize.reviewSuggestedChangeTooltipResolved();
            case OUTDATED -> ReviewLocalize.reviewSuggestedChangeTooltipOutdated();
        });
    }

    @RequiredUIAccess
    private static void showCommitPopup(CodeReviewCommentBodyViewModelImpl vm, CodeReviewSuggestedChangeBlock block, Component anchor) {
        TextFilePatch patch = block.getPatch();
        if (patch == null) {
            return;
        }

        EditorBox commitEditor = vm.getProject().getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(vm.getProject())
            .multiline()
            .placeholder(VcsLocalize.commitPanelMessagePlaceholder())
            .build();
        commitEditor.setWidth(450);
        commitEditor.setHeight(110);

        Label errorLabel = Label.create(ReviewLocalize.reviewSuggestedChangeCommitMessageEmpty());
        errorLabel.setImage(PlatformIconGroup.generalError());
        errorLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
        errorLabel.setVisible(false);
        commitEditor.addValueListener(event -> errorLabel.setVisible(false));

        Button commitButton = Button.create(ReviewLocalize.reviewSuggestedChangeCommitButton());
        commitButton.addStyle(ButtonStyle.PRIMARY);
        Button cancelButton = Button.create(CommonLocalize.buttonCancel());

        VerticalLayout root = VerticalLayout.create(Space.SMALL);
        root.add(commitEditor);
        root.add(errorLabel);
        root.add(HorizontalLayout.create(Space.SMALL).add(commitButton).add(cancelButton));
        root.paddingBuilder().allSet(Space.MEDIUM).apply();

        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(root, commitEditor)
            .setFocusable(true)
            .setRequestFocus(true)
            .setResizable(true)
            .createPopup();

        commitButton.addClickListener(event -> {
            String message = commitEditor.getValue();
            if (message == null || message.isBlank()) {
                errorLabel.setVisible(true);
                popup.pack(true, true);
                return;
            }
            vm.commitSuggestion(patch, message);
            popup.cancel();
        });
        cancelButton.addClickListener(event -> popup.cancel());

        popup.showBy(anchor, null, PopupPosition.BOTTOM);
    }
}

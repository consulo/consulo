// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.commits;

import com.intellij.collaboration.ui.SingleValueModel;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.ListSpeedSearch;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.log.VcsCommitMetadata;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public final class CommitsBrowserComponentBuilder {
    public static final Key<JList<VcsCommitMetadata>> COMMITS_LIST_KEY = Key.create("COMMITS_LIST");

    private final Project project;
    private final SingleValueModel<List<VcsCommitMetadata>> commitsModel;
    private ListCellRenderer<VcsCommitMetadata> commitRenderer = new CommitsListCellRenderer();
    private boolean showCommitDetailsPanel = true;
    private Consumer<VcsCommitMetadata> onCommitSelected = commit -> {
    };
    private String emptyListText;
    private ActionGroup popupActionGroup;
    private String popupPlace;

    public CommitsBrowserComponentBuilder(
        @Nonnull Project project,
        @Nonnull SingleValueModel<List<VcsCommitMetadata>> commitsModel
    ) {
        this.project = project;
        this.commitsModel = commitsModel;
    }

    public @Nonnull CommitsBrowserComponentBuilder setCustomCommitRenderer(@Nonnull ListCellRenderer<VcsCommitMetadata> customRenderer) {
        commitRenderer = customRenderer;
        return this;
    }

    public @Nonnull CommitsBrowserComponentBuilder setEmptyCommitListText(@NlsContexts.StatusText @Nonnull String emptyText) {
        this.emptyListText = emptyText;
        return this;
    }

    public @Nonnull CommitsBrowserComponentBuilder installPopupActions(@Nonnull ActionGroup actionGroup, @Nonnull String place) {
        popupActionGroup = actionGroup;
        popupPlace = place;
        return this;
    }

    public @Nonnull CommitsBrowserComponentBuilder showCommitDetails(boolean show) {
        showCommitDetailsPanel = show;
        return this;
    }

    public @Nonnull CommitsBrowserComponentBuilder onCommitSelected(@Nonnull Consumer<VcsCommitMetadata> onCommitSelected) {
        this.onCommitSelected = onCommitSelected;
        return this;
    }

    public @Nonnull JComponent create() {
        CollectionListModel<VcsCommitMetadata> commitsListModel = new CollectionListModel<>(commitsModel.getValue());

        JBList<VcsCommitMetadata> commitsList = new JBList<>(commitsListModel);
        if (emptyListText != null) {
            commitsList.getEmptyText().setText(emptyListText);
        }
        commitsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListCellRenderer<VcsCommitMetadata> renderer = commitRenderer;
        commitsList.setCellRenderer(renderer);
        if (renderer instanceof JComponent jRenderer) {
            UIUtil.putClientProperty(commitsList, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, List.of(jRenderer));
        }

        ScrollingUtil.installActions(commitsList);
        ListUiUtil.Selection.installSelectionOnFocus(commitsList);
        ListUiUtil.Selection.installSelectionOnRightClick(commitsList);

        ListSpeedSearch.installOn(commitsList, VcsCommitMetadata::getSubject);

        if (popupActionGroup != null) {
            PopupHandler.installSelectionListPopup(commitsList, popupActionGroup, popupPlace);
        }

        commitsModel.addAndInvokeListener(() -> {
            List<VcsCommitMetadata> currentList = commitsListModel.toList();
            List<VcsCommitMetadata> newList = commitsModel.getValue();
            if (!currentList.equals(newList)) {
                VcsCommitMetadata selectedCommit = commitsList.getSelectedValue();
                commitsListModel.replaceAll(newList);
                commitsList.setSelectedValue(selectedCommit, true);
            }
        });

        SingleValueModel<VcsCommitMetadata> commitDetailsModel = new SingleValueModel<>(null);
        JComponent commitDetailsComponent = showCommitDetailsPanel ? createCommitDetailsComponent(commitDetailsModel) : null;

        commitsList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            onCommitSelected.accept(commitsList.getSelectedValue());
        });

        JScrollPane commitsScrollPane = ScrollPaneFactory.createScrollPane(commitsList, true);
        commitsScrollPane.setOpaque(false);
        commitsScrollPane.getViewport().setOpaque(false);
        commitsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ScrollableContentBorder.setup(commitsScrollPane, Side.TOP);

        OnePixelSplitter commitsBrowserComponent = new OnePixelSplitter(true, "Github.PullRequest.Commits.Browser", 0.7f);
        commitsBrowserComponent.setFirstComponent(commitsScrollPane);
        commitsBrowserComponent.setSecondComponent(commitDetailsComponent);

        UIUtil.putClientProperty(commitsBrowserComponent, COMMITS_LIST_KEY, commitsList);

        commitsList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int index = commitsList.getSelectedIndex();
            commitDetailsModel.setValue(index != -1 ? commitsListModel.getElementAt(index) : null);
            commitsBrowserComponent.validate();
            commitsBrowserComponent.repaint();
            if (index != -1) {
                ScrollingUtil.ensureRangeIsVisible(commitsList, index, index);
            }
        });

        return commitsBrowserComponent;
    }

    private @Nonnull JComponent createCommitDetailsComponent(@Nonnull SingleValueModel<VcsCommitMetadata> model) {
        CommitDetailsPanel commitDetailsPanel = new CommitDetailsPanel();
        JScrollPane scrollpane = ScrollPaneFactory.createScrollPane(commitDetailsPanel, true);
        scrollpane.setVisible(false);
        scrollpane.setOpaque(false);
        scrollpane.getViewport().setOpaque(false);
        scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        model.addAndInvokeListener(() -> {
            VcsCommitMetadata commit = model.getValue();
            if (commit != null) {
                String hashAndAuthor = CommitPresentationUtil.formatCommitHashAndAuthor(
                    commit.getId(), commit.getAuthor(), commit.getAuthorTime(),
                    commit.getCommitter(), commit.getCommitTime()
                );

                CommitPresentationUtil.CommitPresentation presentation = new CommitPresentationUtil.CommitPresentation(
                    project,
                    commit.getRoot(),
                    commit.getFullMessage(),
                    hashAndAuthor,
                    MultiMap.empty()
                ) {
                    @Override
                    public String getText() {
                        String rawMessage = myRawMessage;
                        int separator = rawMessage.indexOf("\n\n");
                        String subject = separator > 0 ? rawMessage.substring(0, separator) : rawMessage;
                        String description = rawMessage.substring(subject.length());
                        if (subject.contains("\n")) {
                            // subject has new lines => that is not a subject
                            return rawMessage;
                        }

                        HtmlBuilder builder = new HtmlBuilder().append(HtmlChunk.raw(subject).bold());
                        if (!description.isBlank()) {
                            builder.br().br().appendRaw(description);
                        }
                        return builder.toString();
                    }
                };
                commitDetailsPanel.setCommit(presentation);
            }
            scrollpane.setVisible(commit != null);
        });
        return scrollpane;
    }
}

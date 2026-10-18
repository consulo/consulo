/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.review.details.data.CodeReviewCIJob;
import consulo.review.details.data.CodeReviewCIJobState;
import consulo.review.icon.CIBuildStatusIcons;
import consulo.review.list.CommentsCounter;
import consulo.review.list.ReviewListItemPresentation;
import consulo.review.list.ReviewListItemStatus;
import consulo.review.toolwindow.ReviewToolwindowTabs;
import consulo.review.util.ComputedResult;
import consulo.sandboxPlugin.ide.review.SandReviewComment;
import consulo.sandboxPlugin.ide.review.SandReviewProjectViewModel;
import consulo.sandboxPlugin.ide.review.SandReviewReplyViewModel;
import consulo.sandboxPlugin.ide.review.SandReviewService;
import consulo.sandboxPlugin.ide.review.SandReviewTab;
import consulo.sandboxPlugin.ide.review.SandReviewTabViewModel;
import consulo.sandboxPlugin.ide.review.SandReviewThreadViewModel;
import consulo.sandboxPlugin.ide.review.SandReviewViewModel;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl(id = "review", order = "after messageList")
public class ReviewUITesterTab implements UITesterTab {
    private final ApplicationConcurrency myApplicationConcurrency;
    private final ProjectManager myProjectManager;

    @Inject
    public ReviewUITesterTab(ApplicationConcurrency applicationConcurrency, ProjectManager projectManager) {
        myApplicationConcurrency = applicationConcurrency;
        myProjectManager = projectManager;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Code Review");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        UIAccess uiAccess = UIAccess.current();
        ScheduledExecutorService scheduler = myApplicationConcurrency.getScheduledExecutorService();
        Project project = myProjectManager.getDefaultProject();
        SandReviewProjectViewModel projectVm = SandReviewService.create(scheduler).getProjectVm().get();

        MutableFlatDataModel<SandReviewViewModel> reviews = FlatDataModel.of(new ArrayList<>());
        ListBox<SandReviewViewModel> list = ListBox.create(reviews);
        list.setRenderer((presentation, item) -> {
            SandReviewViewModel review = item.getValue();
            if (review == null) {
                return;
            }
            ReviewListItemPresentation itemPresentation = review.toPresentation();
            ReviewListItemStatus buildStatus = itemPresentation.getBuildStatus();
            presentation.withIcon(buildStatus == null ? null : buildStatus.icon());
            presentation.append(itemPresentation.getTitle());
            presentation.append("  " + itemPresentation.getId(), TextAttribute.GRAYED);
            CommentsCounter counter = itemPresentation.getCommentsCounter();
            if (counter != null) {
                presentation.withSuffix(LocalizeValue.of(String.valueOf(counter.count())), null);
            }
        });
        list.addValueListener(event -> {
            SandReviewViewModel value = event.getValue();
            if (value != null) {
                projectVm.openReview(value);
            }
        });

        Label listState = Label.create();
        bind(uiDisposable, uiAccess, projectVm.getListVm().getReviews(), result -> {
            listState.setText(result.fold(
                () -> LocalizeValue.localizeTODO("Loading reviews..."),
                loaded -> LocalizeValue.localizeTODO(loaded.size() + " reviews"),
                error -> LocalizeValue.of(String.valueOf(error.getMessage()))
            ));
            List<SandReviewViewModel> loaded = result.getOrNull();
            reviews.replaceAll(loaded == null ? List.of() : loaded);
        });

        Button refresh = Button.create(LocalizeValue.localizeTODO("Refresh"), event -> projectVm.refresh());
        HorizontalLayout listToolbar = HorizontalLayout.create(Space.SMALL)
            .add(refresh)
            .add(Label.create(projectVm.getProjectName()))
            .add(listState);

        DockLayout details = DockLayout.create();
        Disposable[] detailsDisposable = {Disposable.newDisposable()};
        Disposer.register(uiDisposable, detailsDisposable[0]);
        bind(uiDisposable, uiAccess, projectVm.getTabs(), tabs -> {
            Disposer.dispose(detailsDisposable[0]);
            detailsDisposable[0] = Disposable.newDisposable();
            Disposer.register(uiDisposable, detailsDisposable[0]);
            details.removeAll();
            details.center(createDetails(tabs, project, scheduler, uiAccess, detailsDisposable[0]));
        });

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(DockLayout.create().top(listToolbar).center(ScrollableLayout.create(list)));
        split.setSecondComponent(details);
        split.setProportion(35);
        return split;
    }

    @RequiredUIAccess
    private static Component createDetails(ReviewToolwindowTabs<SandReviewTab, SandReviewTabViewModel> tabs,
                                           Project project,
                                           ScheduledExecutorService scheduler,
                                           UIAccess uiAccess,
                                           Disposable disposable) {
        SandReviewTab selected = tabs.selectedTab();
        SandReviewTabViewModel tabVm = selected == null ? null : tabs.tabs().get(selected);
        if (tabVm == null) {
            return Label.create(LocalizeValue.localizeTODO("Select a review"));
        }
        SandReviewViewModel review = tabVm.review();

        VerticalLayout root = VerticalLayout.create(Space.MEDIUM);

        Label title = Label.create();
        title.addStyle(LabelStyle.FONT_LARGE);
        title.addStyle(LabelStyle.FONT_BOLD);
        bind(disposable, uiAccess, review.getTitle(), text -> title.setText(LocalizeValue.of(text)));
        Avatar author = Avatar.create(LocalizeValue.of(review.getAuthor().fullName()));
        author.addStyle(AvatarStyle.SMALL);
        root.add(HorizontalLayout.create(Space.SMALL).add(author).add(title));

        Label state = Label.create();
        bind(disposable, uiAccess, review.getReviewRequestState(), value -> state.setText(LocalizeValue.of(
            "#" + review.getNumber() + " · " + value.name() + " · " + review.getAuthor().fullName() + " · " + review.getUrl()
        )));
        root.add(state);

        ObservableValue<String> description = review.getDescription();
        Label descriptionLabel = Label.create();
        bind(disposable, uiAccess, description, text -> descriptionLabel.setText(LocalizeValue.of(text)));
        root.add(descriptionLabel);

        Label conflicts = Label.create();
        bind(disposable, uiAccess, review.getHasConflicts(), value -> conflicts.setText(LocalizeValue.localizeTODO(
            value == null ? "Mergeability is being checked" : value ? "Has conflicts" : "No conflicts"
        )));
        root.add(conflicts);

        HorizontalLayout checks = HorizontalLayout.create(Space.SMALL);
        Label jobsDetails = Label.create();
        bind(disposable, uiAccess, review.getCiJobs(), jobs -> {
            checks.removeAll();
            for (CodeReviewCIJob job : jobs) {
                Label jobLabel = Label.create(LocalizeValue.of(job.name()));
                jobLabel.setImage(switch (job.status()) {
                    case FAILED -> CIBuildStatusIcons.failed();
                    case PENDING -> CIBuildStatusIcons.inProgress();
                    case SKIPPED -> CIBuildStatusIcons.skipped();
                    case SUCCESS -> CIBuildStatusIcons.success();
                });
                checks.add(jobLabel);
            }
            checks.add(Button.create(LocalizeValue.localizeTODO("Show checks"), event -> review.showJobsDetails()));
        });
        review.addShowJobsDetailsRequestListener(disposable, jobs -> uiAccess.give(() -> {
            StringBuilder text = new StringBuilder();
            for (CodeReviewCIJob job : jobs) {
                text.append(job.name()).append(": ").append(job.status() == CodeReviewCIJobState.SUCCESS ? "passed" : job.status().name()).append("   ");
            }
            jobsDetails.setText(LocalizeValue.of(text.toString()));
        }));
        root.add(checks);
        root.add(jobsDetails);

        for (SandReviewThreadViewModel thread : review.getThreads()) {
            root.add(createThread(thread, project, scheduler, uiAccess, disposable));
        }
        return ScrollableLayout.create(root);
    }

    @RequiredUIAccess
    private static Component createThread(SandReviewThreadViewModel thread,
                                          Project project,
                                          ScheduledExecutorService scheduler,
                                          UIAccess uiAccess,
                                          Disposable disposable) {
        Label location = Label.create(LocalizeValue.of(thread.getLocation()));
        location.addStyle(LabelStyle.FONT_BOLD);

        Button resolve = Button.create(LocalizeValue.localizeTODO("Resolve"), event -> thread.changeResolvedState());
        bind(disposable, uiAccess, thread.isResolved(),
            resolved -> resolve.setText(LocalizeValue.localizeTODO(resolved ? "Unresolve" : "Resolve")));
        bind(disposable, uiAccess, thread.isBusy(), busy -> resolve.setEnabled(!busy));

        Label replies = Label.create();
        bind(disposable, uiAccess, thread.getRepliesState(), data -> replies.setText(LocalizeValue.of(
            data.getRepliesCount() == 0 ? "No replies" : data.getRepliesCount() + " replies, last "
                + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(data.getLastReplyDate())
        )));

        MessageList messages = MessageList.create();
        messages.setMarkdown(true);
        messages.setShowOwnAvatar(true);
        bind(disposable, uiAccess, thread.getComments(), comments -> {
            messages.clear();
            for (SandReviewComment comment : comments) {
                MessageListItem item = messages.add(LocalizeValue.of(comment.author().fullName()), comment.text());
                item.setTime(comment.time());
                item.setStyle(comment.author().equals(SandReviewService.CURRENT_USER) ? MessageListItemStyle.OWN : MessageListItemStyle.OTHER);
            }
        });

        SandReviewReplyViewModel replyVm = new SandReviewReplyViewModel(project, disposable, thread, SandReviewService.CURRENT_USER, scheduler);
        MessageInput input = MessageInput.create();
        input.setPlaceholder(LocalizeValue.localizeTODO("Reply (text with 'fail' is rejected)"));
        input.setSendText(LocalizeValue.localizeTODO("Reply"));
        input.addSubmitListener(event -> {
            replyVm.getText().set(event.getValue());
            replyVm.submitReply();
        });

        Label submitState = Label.create();
        bind(disposable, uiAccess, replyVm.getState(), result -> {
            submitState.setText(describe(result));
            input.setEnabled(result == null || !result.isInProgress());
        });

        HorizontalLayout header = HorizontalLayout.create(Space.SMALL).add(location).add(resolve).add(replies).add(submitState);
        return DockLayout.create(Space.SMALL).top(header).center(messages).bottom(input);
    }

    private static LocalizeValue describe(@Nullable ComputedResult<?> result) {
        if (result == null) {
            return LocalizeValue.empty();
        }
        return result.fold(
            () -> LocalizeValue.localizeTODO("Sending..."),
            value -> LocalizeValue.localizeTODO("Sent"),
            error -> LocalizeValue.of(String.valueOf(error.getMessage()))
        );
    }

    @RequiredUIAccess
    private static <T> void bind(Disposable parentDisposable, UIAccess uiAccess, ObservableValue<T> value, Consumer<T> uiSetter) {
        uiSetter.accept(value.get());
        Runnable unsubscribe = value.addListener(newValue -> uiAccess.give(() -> uiSetter.accept(newValue)));
        Disposer.register(parentDisposable, unsubscribe::run);
    }
}

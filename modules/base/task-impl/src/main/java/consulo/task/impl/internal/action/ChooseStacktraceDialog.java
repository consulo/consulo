/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.task.impl.internal.action;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.task.Comment;
import consulo.task.Task;
import consulo.task.localize.TaskLocalize;
import consulo.ui.Component;
import consulo.ui.MultiSelectListBox;
import consulo.ui.TextArea;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Dmitry Avdeev
 */
public class ChooseStacktraceDialog extends DialogDescriptor {
    private final List<Comment> myComments;
    private @Nullable MultiSelectListBox<Comment> myList;

    private ChooseStacktraceDialog(Task issue) {
        super(TaskLocalize.dialogTitleChooseStacktraceToAnalyze());
        Comment[] comments = issue.getComments();
        myComments = new ArrayList<>(comments.length + 1);
        String description = issue.getDescription();
        if (description != null) {
            myComments.add(new Description(description));
        }
        ContainerUtil.addAll(myComments, comments);
    }

    @RequiredUIAccess
    public static CompletableFuture<List<Comment>> show(Task issue) {
        ChooseStacktraceDialog descriptor = new ChooseStacktraceDialog(issue);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync().handleAsync(
            (value, error) -> error != null || value == null || descriptor.myList == null ? List.of() : descriptor.myList.getValue(),
            UIAccess.current()
        );
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(60, 25);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        TextArea text = TextArea.create().withEditable(false);

        MultiSelectListBox<Comment> list = MultiSelectListBox.create(FlatDataModel.of(myComments));
        list.setRenderer((presentation, item) -> {
            Comment value = item.getValue();
            if (value instanceof Description) {
                presentation.append(TaskLocalize.labelDescription());
            }
            else if (value != null) {
                presentation.append(TaskLocalize.labelCommentedBy(value.getAuthor(), value.getDate()));
            }
        });
        list.addValueListener(event -> {
            List<Comment> selected = event.getValue();
            text.setValue(selected == null || selected.isEmpty() ? "" : selected.get(0).getText());
            updateOkButtonState();
        });
        myList = list;
        if (!myComments.isEmpty()) {
            list.setValue(List.of(myComments.get(0)));
            text.setValue(myComments.get(0).getText());
        }

        return TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL)
            .withProportion(30)
            .withFirstComponent(ScrollableLayout.create(list))
            .withSecondComponent(ScrollableLayout.create(text));
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myList;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myList == null || !myList.getValue().isEmpty();
    }

    private static class Description extends Comment {
        private final String myDescription;

        public Description(String description) {
            myDescription = description;
        }

        @Override
        public String getText() {
            return myDescription;
        }

        @Override
        public String getAuthor() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }
    }
}

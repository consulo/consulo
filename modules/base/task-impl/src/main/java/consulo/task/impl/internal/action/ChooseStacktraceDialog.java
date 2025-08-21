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

import consulo.execution.unscramble.AnalyzeStacktraceUtil;
import consulo.project.Project;
import consulo.task.Comment;
import consulo.task.Task;
import consulo.task.localize.TaskLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ChooseStacktraceDialog extends DialogWrapper {
    private JList<Comment> myList;
    private JPanel myPanel;
    private JPanel myEditorPanel;
    private AnalyzeStacktraceUtil.StacktraceEditorPanel myEditor;

    public ChooseStacktraceDialog(Project project, Task issue) {
        super(project, false);

        setTitle(TaskLocalize.dialogTitleChooseStacktraceToAnalyze());
        Comment[] comments = issue.getComments();
        List<Comment> list = new ArrayList<>(comments.length + 1);
        String description = issue.getDescription();
        if (description != null) {
            list.add(new Description(description));
        }
        ContainerUtil.addAll(list, comments);

        myList.setModel(new CollectionListModel<>(list));
        myList.setCellRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(
                @Nonnull JList<? extends Comment> list,
                Comment value,
                int index,
                boolean selected,
                boolean hasFocus
            ) {
                if (value instanceof Description) {
                    append(TaskLocalize.labelDescription());
                }
                else {
                    append(TaskLocalize.labelCommentedBy(value.getAuthor(), value.getDate()));
                }
            }
        });

        myEditor = AnalyzeStacktraceUtil.createEditorPanel(project, myDisposable);
        myEditorPanel.add(myEditor, BorderLayout.CENTER);
        myList.getSelectionModel().addListSelectionListener(e -> {
            if (myList.getSelectedValue() instanceof Comment comment) {
                myEditor.setText(comment.getText());
            }
            else {
                myEditor.setText("");
            }
        });
        myList.setSelectedValue(list.get(0), false);
        init();
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myList;
    }

    @Override
    protected JComponent createCenterPanel() {
        return myPanel;
    }

    public Comment[] getTraces() {
        return ArrayUtil.toObjectArray(Comment.class, myList.getSelectedValuesList());
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

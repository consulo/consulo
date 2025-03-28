/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.dvcs.push.ui;

import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer;
import consulo.versionControlSystem.impl.internal.change.ui.issueLink.IssueLinkRenderer;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import jakarta.annotation.Nonnull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class CommitNode extends DefaultMutableTreeNode implements CustomRenderedTreeNode, TooltipNode {
    @Nonnull
    private final Project myProject;

    public CommitNode(@Nonnull Project project, @Nonnull VcsFullCommitDetails commit) {
        super(commit, false);
        myProject = project;
    }

    @Override
    public VcsFullCommitDetails getUserObject() {
        return (VcsFullCommitDetails)super.getUserObject();
    }

    @Override
    public void render(@Nonnull ColoredTreeCellRenderer renderer) {
        renderer.append("   ");
        TreeNode parent = getParent();
        new IssueLinkRenderer(myProject, renderer).appendTextWithLinks(
            getUserObject().getSubject(),
            PushLogTreeUtil.addTransparencyIfNeeded(
                SimpleTextAttributes.REGULAR_ATTRIBUTES,
                !(parent instanceof RepositoryNode repositoryNode && !repositoryNode.isChecked())
            )
        );
    }

    @Override
    public String getTooltip() {
        String hash = DvcsUtil.getShortHash(getUserObject().getId().toString());
        String date = DvcsUtil.getDateString(getUserObject());
        String author = getUserObject().getAuthor().getName();
        String message = IssueLinkHtmlRenderer.formatTextWithLinks(myProject, getUserObject().getFullMessage());
        return String.format("%s  %s  by %s\n\n%s", hash, date, author, message);
    }
}

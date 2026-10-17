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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.util.VcsUserUtil;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
final class PushCommitDetailsPanel {
    private final HtmlLabel myMessage = HtmlLabel.create(LocalizeValue.empty());
    private final Label myFooter = Label.create();
    private final ScrollableLayout myRoot;

    @RequiredUIAccess
    PushCommitDetailsPanel() {
        myFooter.setForegroundColor(ComponentColors.DISABLED_TEXT);

        VerticalLayout content = VerticalLayout.create(Space.MEDIUM);
        content.add(myMessage);
        content.add(myFooter);
        content.paddingBuilder().allSet(Space.MEDIUM).apply();
        myRoot = ScrollableLayout.create(content);
    }

    Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    void setCommit(VcsFullCommitDetails commit) {
        String fullMessage = commit.getFullMessage();
        String subject = commit.getSubject();
        String body = fullMessage.startsWith(subject) ? fullMessage.substring(subject.length()).strip() : fullMessage.strip();

        StringBuilder html = new StringBuilder();
        html.append("<b>").append(escape(subject)).append("</b>");
        if (!body.isEmpty()) {
            html.append("<br><br>").append(escape(body));
        }
        myMessage.setText(LocalizeValue.of(html.toString()));

        myFooter.setText(LocalizeValue.of(DvcsUtil.getShortHash(commit.getId().asString())
            + "  " + VcsUserUtil.getShortPresentation(commit.getAuthor())
            + "  " + DvcsUtil.getDateString(commit)));
    }

    private static String escape(String text) {
        return StringUtil.escapeXmlEntities(text).replace("\n", "<br>");
    }
}

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
package consulo.desktop.qt.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.HtmlLabel;
import io.qt.core.Qt;
import io.qt.widgets.QLabel;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtHtmlLabelImpl extends DesktopQtLabelImpl implements HtmlLabel {
    public DesktopQtHtmlLabelImpl(LocalizeValue html) {
        super(html);
    }

    @Override
    protected void initialize(QLabel component) {
        // a qt label guesses between plain and rich text, and markup which does not look like html - a bare
        // ampersand of a build string - is then drawn as it stands
        component.setTextFormat(Qt.TextFormat.RichText);
        component.setWordWrap(true);
        component.setTextInteractionFlags(
            Qt.TextInteractionFlag.LinksAccessibleByMouse,
            Qt.TextInteractionFlag.TextSelectableByMouse
        );

        super.initialize(component);
    }
}

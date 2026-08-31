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

import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.localize.LocalizeValue;
import consulo.ui.AdvancedLabel;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QPixmap;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLabel;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class DesktopQtAdvancedLabelImpl extends QtComponentDelegate<DesktopQtAdvancedLabelImpl.QtAdvancedLabel>
    implements AdvancedLabel {

    public static class QtAdvancedLabel extends QWidget {
        private final QLabel myIcon = new QLabel();
        private final QLabel myText = new QLabel();

        public QtAdvancedLabel(QWidget parent) {
            super(parent);

            QHBoxLayout layout = new QHBoxLayout(this);
            layout.setContentsMargins(0, 0, 0, 0);
            layout.setSpacing(4);
            layout.addWidget(myIcon);
            layout.addWidget(myText);
            layout.addStretch(1);

            myIcon.setVisible(false);
            myText.setTextFormat(Qt.TextFormat.RichText);
        }
    }

    private record Fragment(LocalizeValue text, @Nullable TextAttribute attribute) {
    }

    private static class Presentation implements TextItemPresentation {
        private final List<Fragment> myFragments = new ArrayList<>();
        private @Nullable Image myImage;

        @Override
        public TextItemPresentation withIcon(@Nullable Image image) {
            myImage = image;
            return this;
        }

        @Override
        public void clearText() {
            myFragments.clear();
        }

        @Override
        public void append(LocalizeValue text, TextAttribute textAttribute) {
            myFragments.add(new Fragment(text, textAttribute));
        }
    }

    private @Nullable Image myIcon;
    private String myHtml = "";

    @Override
    protected QtAdvancedLabel createQt(QWidget parent) {
        return new QtAdvancedLabel(parent);
    }

    @Override
    protected void initialize(QtAdvancedLabel component) {
        super.initialize(component);

        apply(component);
    }

    @Override
    public AdvancedLabel updatePresentation(Consumer<TextItemPresentation> consumer) {
        Presentation presentation = new Presentation();

        consumer.accept(presentation);

        myIcon = presentation.myImage;

        StringBuilder html = new StringBuilder();
        for (Fragment fragment : presentation.myFragments) {
            appendFragment(html, fragment);
        }
        myHtml = html.toString();

        if (myComponent != null) {
            apply(myComponent);
        }

        return this;
    }

    private void apply(QtAdvancedLabel component) {
        component.myText.setText(myHtml);

        QPixmap pixmap = myIcon instanceof DesktopQtImage qtImage ? qtImage.toQPixmap() : null;

        component.myIcon.setPixmap(pixmap);
        component.myIcon.setVisible(pixmap != null);
    }

    private static void appendFragment(StringBuilder html, Fragment fragment) {
        TextAttribute attribute = fragment.attribute();

        StringBuilder style = new StringBuilder();
        if (attribute != null) {
            appendColor(style, "color", attribute.getForegroundColor());
            appendColor(style, "background-color", attribute.getBackgroundColor());

            if ((attribute.getStyle() & Font.BOLD) != 0) {
                style.append("font-weight:bold;");
            }
            if ((attribute.getStyle() & Font.ITALIC) != 0) {
                style.append("font-style:italic;");
            }
        }

        html.append("<span style=\"").append(style).append("\">")
            .append(StringUtil.escapeXmlEntities(fragment.text().get()))
            .append("</span>");
    }

    private static void appendColor(StringBuilder style, String property, @Nullable ColorValue colorValue) {
        if (colorValue == null) {
            return;
        }

        QColor color = TargetQt.to(colorValue);
        style.append(property).append(':').append(color.name()).append(';');
    }
}

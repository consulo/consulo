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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class WebTextBoxWithExpandActionImpl extends WebTextBoxImpl implements TextBoxWithExpandAction {
    private final Function<String, List<String>> myParser;
    private final Function<List<String>, String> myJoiner;

    private String myDialogTitle;

    @RequiredUIAccess
    public WebTextBoxWithExpandActionImpl(
        @Nullable Image editButtonImage,
        String dialogTitle,
        Function<String, List<String>> parser,
        Function<List<String>, String> joiner
    ) {
        super("");

        myDialogTitle = StringUtil.notNullize(dialogTitle);
        myParser = parser;
        myJoiner = joiner;

        com.vaadin.flow.component.Component icon = editButtonImage == null
            ? new Icon(VaadinIcon.EXPAND_FULL)
            : WebImageConverter.getImage(editButtonImage);

        Button expand = new Button(icon, event -> showExpandDialog());
        WebButtonImpl.applyInplaceStyle(expand);
        expand.getElement().setAttribute("title", myDialogTitle);

        toVaadinComponent().setSuffixComponent(expand);
    }

    /**
     * The api splits the text into lines only while it is being edited, so the parser runs on the way into the
     * dialog and the joiner on the way back - the value itself is never held in the split form.
     */
    @RequiredUIAccess
    private void showExpandDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(myDialogTitle);
        dialog.setWidth("500px");
        dialog.setHeight("350px");

        TextArea editor = new TextArea();
        editor.setSizeFull();
        editor.setValue(String.join("\n", myParser.apply(StringUtil.notNullize(getValue()))));
        dialog.add(editor);

        Button ok = new Button("OK", event -> {
            List<String> lines = new ArrayList<>();
            for (String line : StringUtil.splitByLines(editor.getValue())) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }

            setValue(myJoiner.apply(lines), true);
            dialog.close();
        });
        ok.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), ok);
        dialog.open();
    }

    @Override
    public TextBoxWithExpandAction withDialogTitle(String text) {
        myDialogTitle = StringUtil.notNullize(text);
        return this;
    }
}

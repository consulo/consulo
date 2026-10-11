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

import com.vaadin.flow.component.select.Select;
import consulo.ui.Component;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
final class WebSelectSuffix {
    static final String SUFFIX_CLASS = "consulo-select-suffix";
    private static final String WITH_SUFFIX_CLASS = "consulo-select-with-suffix";

    private final Select<?> mySelect;

    private @Nullable Component mySuffixComponent;
    private com.vaadin.flow.component.@Nullable Component myShownSuffix;

    WebSelectSuffix(Select<?> select) {
        mySelect = select;
    }

    @Nullable Component get() {
        return mySuffixComponent;
    }

    void set(@Nullable Component suffixComponent) {
        com.vaadin.flow.component.Component oldSuffix = myShownSuffix;
        if (oldSuffix != null) {
            mySelect.getElement().removeChild(oldSuffix.getElement());
            mySelect.removeClassName(WITH_SUFFIX_CLASS);
        }

        mySuffixComponent = suffixComponent;

        com.vaadin.flow.component.Component suffix = WebTextBoxImpl.toVaadinOrNull(suffixComponent);
        myShownSuffix = suffix;
        if (suffix == null) {
            return;
        }

        suffix.getElement().setAttribute("slot", "prefix");
        suffix.getElement().getClassList().add(SUFFIX_CLASS);
        suffix.getElement().addEventListener("click", event -> {
        }).addEventData("event.stopPropagation()");
        mySelect.getElement().appendChild(suffix.getElement());
        mySelect.addClassName(WITH_SUFFIX_CLASS);
    }
}

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
package consulo.it.ui;

import consulo.it.HeadlessApplicationExtension;
import consulo.it.HeadlessUIThread;
import consulo.it.internal.ui.HeadlessComboBoxWithCustomPopup;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.model.FlatDataModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ComboBoxCustomPopupHandlerTest {
    private static final class PopupLog {
        private final List<String> myEvents = new ArrayList<>();
        private final List<Runnable> myOnClosed = new ArrayList<>();

        JBPopup create(Runnable onClosed) {
            myEvents.add("create");
            myOnClosed.add(onClosed);
            return (JBPopup) Proxy.newProxyInstance(JBPopup.class.getClassLoader(), new Class<?>[]{JBPopup.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "showBy" -> myEvents.add("show");
                    case "cancel" -> {
                        myEvents.add("cancel");
                        onClosed.run();
                    }
                    case "hashCode" -> {
                        return System.identityHashCode(proxy);
                    }
                    case "equals" -> {
                        return proxy == args[0];
                    }
                    default -> {
                    }
                }
                Class<?> type = method.getReturnType();
                return type == boolean.class ? false : type == int.class ? 0 : null;
            });
        }

        void closeOutside() {
            myOnClosed.get(myOnClosed.size() - 1).run();
        }
    }

    private static HeadlessComboBoxWithCustomPopup<String> create() {
        return (HeadlessComboBoxWithCustomPopup<String>) ComboBoxWithCustomPopup.create(FlatDataModel.of(List.of("value")));
    }

    @Test
    public void aClickShowsThePopup() {
        HeadlessUIThread.run(() -> {
            HeadlessComboBoxWithCustomPopup<String> comboBox = create();
            PopupLog log = new PopupLog();
            comboBox.addPopupHandler(log::create);

            comboBox.click();

            assertThat(log.myEvents).containsExactly("create", "show");
        });
    }

    @Test
    public void aSecondClickClosesThePopup() {
        HeadlessUIThread.run(() -> {
            HeadlessComboBoxWithCustomPopup<String> comboBox = create();
            PopupLog log = new PopupLog();
            comboBox.addPopupHandler(log::create);

            comboBox.click();
            comboBox.click();

            assertThat(log.myEvents).containsExactly("create", "show", "cancel");
        });
    }

    @Test
    public void aDisabledComboBoxShowsNothing() {
        HeadlessUIThread.run(() -> {
            HeadlessComboBoxWithCustomPopup<String> comboBox = create();
            PopupLog log = new PopupLog();
            comboBox.addPopupHandler(log::create);
            comboBox.setEnabled(false);

            comboBox.click();

            assertThat(log.myEvents).isEmpty();
        });
    }

    @Test
    public void theClickWhichClosedThePopupOutsideDoesNotReopenIt() {
        HeadlessUIThread.run(() -> {
            HeadlessComboBoxWithCustomPopup<String> comboBox = create();
            PopupLog log = new PopupLog();
            comboBox.addPopupHandler(log::create);

            comboBox.click();
            log.closeOutside();
            comboBox.click();

            assertThat(log.myEvents).containsExactly("create", "show");
        });
    }
}

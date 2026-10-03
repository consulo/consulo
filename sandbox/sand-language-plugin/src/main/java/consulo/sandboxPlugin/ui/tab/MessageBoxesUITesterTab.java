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
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageBoxRemember;
import consulo.ui.MessageBoxes;
import consulo.ui.MessageButtonRole;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.cursor.StandardCursors;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "messageBoxes", order = "after treeTable")
public class MessageBoxesUITesterTab implements UITesterTab {
    private static final class SandRemember implements MessageBoxRemember<Boolean> {
        private @Nullable Boolean myValue;

        @Override
        public void setValue(@Nullable Boolean value) {
            myValue = value;
        }

        @Override
        public @Nullable Boolean getValue() {
            return myValue;
        }

        @Override
        public LocalizeValue getMessageText() {
            return LocalizeValue.of("Do not ask again");
        }
    }

    private final SandRemember myRemember = new SandRemember();

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Message Boxes");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();
        layout.add(
            Button.create(
                    LocalizeValue.of("Info. Hand Cursor"),
                    event -> MessageBoxes.okInfo(LocalizeValue.of("This is INFO")).showAsync()
                )
                .withCursor(StandardCursors.HAND)
        );
        layout.add(
            Button.create(
                LocalizeValue.of("Warning"),
                event -> MessageBoxes.okWarning(LocalizeValue.of("This is WARN")).showAsync()
            )
        );
        layout.add(
            Button.create(
                    LocalizeValue.of("Error. Wait Cursor"),
                    event -> MessageBoxes.okError(LocalizeValue.of("This is ERROR")).showAsync()
                )
                .withCursor(StandardCursors.WAIT)
        );
        layout.add(Button.create(
            LocalizeValue.of("Question"),
            event -> MessageBoxes.okQuestion(LocalizeValue.of("This is QUESTION")).showAsync()
        ));

        layout.add(Button.create(
            LocalizeValue.of("Yes / No"),
            event -> MessageBoxes.yesNo()
                .text(LocalizeValue.of("Proceed?"))
                .showAsync()
                .whenComplete((answer, error) -> UITesterTabUtil.report("yesNo", answer, error))
        ));

        layout.add(Button.create(
            LocalizeValue.of("Yes / No / Cancel"),
            event -> MessageBoxes.yesNoCancel()
                .text(LocalizeValue.of("Save before closing?"))
                .showAsync()
                .whenComplete((answer, error) -> UITesterTabUtil.report("yesNoCancel", answer, error))
        ));

        layout.add(Button.create(
            LocalizeValue.of("Custom labels on standard roles"),
            event -> {
                MessageBoxBuilder<String> box = MessageBoxBuilder.create();
                box.asQuestion();
                box.text(LocalizeValue.of("A file of that name already exists."));
                box.button(MessageButtonRole.YES, LocalizeValue.of("Overwrite"), "overwrite");
                box.asDefaultButton();
                box.button(MessageButtonRole.NO, LocalizeValue.of("Skip"), "skip");
                box.button(MessageButtonRole.YES_TO_ALL, LocalizeValue.of("Overwrite All"), "overwriteAll");
                box.button(MessageButtonRole.NO_TO_ALL, LocalizeValue.of("Skip All"), "skipAll");
                box.button(MessageButtonRole.CANCEL, "cancel");
                box.asExitButton();
                box.showAsync().whenComplete((answer, error) -> UITesterTabUtil.report("customLabels", answer, error));
            }
        ));

        layout.add(Button.create(
            LocalizeValue.of("Detail (collapsible)"),
            event -> MessageBoxes.okError(LocalizeValue.of("The operation failed."))
                .detail(LocalizeValue.of("java.lang.IllegalStateException: nothing here\n\tat sand.Tester.run(Tester.java:1)"))
                .showAsync()
        ));

        layout.add(Button.create(
            LocalizeValue.of("Remember my choice"),
            event -> {
                MessageBoxBuilder<Boolean> box = MessageBoxBuilder.create();
                box.asQuestion();
                box.text(LocalizeValue.of("Remembering this answer skips the box next time."));
                box.button(MessageButtonRole.YES, Boolean.TRUE);
                box.asDefaultButton();
                box.button(MessageButtonRole.NO, Boolean.FALSE);
                box.asExitButton();
                box.remember(myRemember);
                box.showAsync().whenComplete((answer, error) -> UITesterTabUtil.report("remember", answer, error));
            }
        ));

        layout.add(Button.create(
            LocalizeValue.of("Forget remembered answer"),
            event -> myRemember.setValue(null)
        ));

        layout.add(Button.create(
            LocalizeValue.of("Rich text"),
            event -> MessageBoxes.okInfo(LocalizeValue.of("<html><b>Bold</b> and <i>italic</i>.</html>"))
                .richText()
                .showAsync()
        ));

        layout.add(Button.create(
            LocalizeValue.of("Dismissed after 3s"),
            event -> {
                CompletableFuture<?> shown =
                    MessageBoxes.okInfo(LocalizeValue.of("This closes itself in three seconds.")).showAsync();

                UIAccess.current().getScheduler().schedule(() -> shown.cancel(false), 3, TimeUnit.SECONDS);
            }
        ));

        return layout;
    }
}

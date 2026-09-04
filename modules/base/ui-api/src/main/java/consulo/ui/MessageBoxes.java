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
package consulo.ui;

import consulo.localize.LocalizeValue;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;


/**
 * The message box shapes which cover nearly every caller, so that a yes/no is never hand-assembled.
 *
 * @author VISTALL
 * @since 2026-09-04                                    
 */
public final class MessageBoxes {
    public static MessageBoxBuilder<@Nullable Object> okInfo(LocalizeValue text) {
        return okTemplate(MessageBoxBuilder.<@Nullable Object>create().asInfo(), text);
    }

    public static MessageBoxBuilder<@Nullable Object> okWarning(LocalizeValue text) {
        return okTemplate(MessageBoxBuilder.<@Nullable Object>create().asWarning(), text);
    }

    public static MessageBoxBuilder<@Nullable Object> okError(LocalizeValue text) {
        return okTemplate(MessageBoxBuilder.<@Nullable Object>create().asError(), text);
    }

    public static MessageBoxBuilder<@Nullable Object> okError(Throwable throwable) {
        return okError(LocalizeValue.of(throwable));
    }

    public static MessageBoxBuilder<@Nullable Object> okQuestion(LocalizeValue text) {
        return okTemplate(MessageBoxBuilder.<@Nullable Object>create().asQuestion(), text);
    }

    public static MessageBoxBuilder<Boolean> okCancel() {
        MessageBoxBuilder<Boolean> builder = MessageBoxBuilder.create();
        builder.asQuestion();

        builder.button(MessageButtonRole.OK, Boolean.TRUE);
        builder.asDefaultButton();

        builder.button(MessageButtonRole.CANCEL, Boolean.FALSE);
        builder.asExitButton();

        return builder;
    }

    public static MessageBoxBuilder<Boolean> yesNo() {
        MessageBoxBuilder<Boolean> builder = MessageBoxBuilder.create();
        builder.asQuestion();

        builder.button(MessageButtonRole.YES, Boolean.TRUE);
        builder.asDefaultButton();

        builder.button(MessageButtonRole.NO, Boolean.FALSE);
        builder.asExitButton();

        return builder;
    }

    public static MessageBoxBuilder<ThreeState> yesNoCancel() {
        MessageBoxBuilder<ThreeState> builder = MessageBoxBuilder.create();
        builder.asQuestion();

        builder.button(MessageButtonRole.YES, ThreeState.YES);
        builder.asDefaultButton();

        builder.button(MessageButtonRole.NO, ThreeState.NO);

        builder.button(MessageButtonRole.CANCEL, ThreeState.UNSURE);
        builder.asExitButton();

        return builder;
    }

    private static MessageBoxBuilder<@Nullable Object> okTemplate(MessageBoxBuilder<@Nullable Object> builder,
                                                                  LocalizeValue text) {
        builder.text(text);

        builder.button(MessageButtonRole.OK, (Object)null);
        builder.asDefaultButton();
        builder.asExitButton();

        return builder;
    }

    private MessageBoxes() {
    }
}

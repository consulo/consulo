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

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.textCompletion.DefaultTextCompletionValueDescriptor;
import consulo.ide.impl.idea.util.textCompletion.TextCompletionProvider;
import consulo.ide.impl.idea.util.textCompletion.TextFieldWithCompletion;
import consulo.ide.impl.idea.util.textCompletion.ValuesCompletionProvider.ValuesCompletionProviderDumbAware;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

public class PushTargetTextField extends TextFieldWithCompletion {
    public PushTargetTextField(@Nonnull Project project, @Nonnull List<String> targetVariants, @Nonnull String defaultTargetName) {
        super(project, getCompletionProvider(targetVariants), defaultTargetName, true, true, true);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                removeSelection();
            }
        });
    }

    @Nonnull
    private static TextCompletionProvider getCompletionProvider(@Nonnull List<String> targetVariants) {
        return new ValuesCompletionProviderDumbAware<>(
            new DefaultTextCompletionValueDescriptor.StringValueDescriptor() {
                @Override
                public int compare(String item1, String item2) {
                    return Integer.valueOf(ContainerUtil.indexOf(targetVariants, item1))
                        .compareTo(ContainerUtil.indexOf(targetVariants, item2));
                }
            },
            targetVariants
        );
    }
}

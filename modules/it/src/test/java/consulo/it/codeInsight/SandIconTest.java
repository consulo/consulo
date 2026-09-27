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
package consulo.it.codeInsight;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.component.util.Iconable;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.sandboxPlugin.lang.SandIconDescriptorUpdater;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Icons composed by the icon descriptor updaters - {@link SandIconDescriptorUpdater} together with the platform's own -
 * compared against icons built with the same image API.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SandIconTest {
    @Test
    public void aClassShowsTheClassIcon(CodeInsightTestFixture fixture) throws Exception {
        fixture.configureByText("some.sand", "class <caret>Item { \"body\" }\n");

        assertThat(iconAtCaret(fixture, 0)).isEqualTo(PlatformIconGroup.nodesClass());
    }

    @Test
    public void aReadOnlyClassGetsTheLockLayer(CodeInsightTestFixture fixture) throws Exception {
        fixture.configureByText("some.sand", "class <caret>Item { \"body\" }\n");
        VirtualFile file = fixture.getFile().getVirtualFile();
        WriteAction.run(() -> file.setWritable(false));

        assertThat(iconAtCaret(fixture, Iconable.ICON_FLAG_READ_STATUS))
            .isEqualTo(ImageEffects.layered(PlatformIconGroup.nodesClass(), PlatformIconGroup.nodesLocked()));
    }

    @Test
    public void aWritableClassHasNoLockLayer(CodeInsightTestFixture fixture) throws Exception {
        fixture.configureByText("some.sand", "class <caret>Item { \"body\" }\n");

        assertThat(iconAtCaret(fixture, Iconable.ICON_FLAG_READ_STATUS)).isEqualTo(PlatformIconGroup.nodesClass());
    }

    private static Image iconAtCaret(CodeInsightTestFixture fixture, int flags) {
        int offset = fixture.getExpectedHighlightingData().getCaretOffset();
        return ReadAction.compute(() -> {
            SandClass sandClass = PsiTreeUtil.getParentOfType(fixture.getFile().findElementAt(offset), SandClass.class);
            assertThat(sandClass).as("a class sits at the caret").isNotNull();
            return IconDescriptorUpdaters.getIconWithoutCache(sandClass, flags);
        });
    }
}

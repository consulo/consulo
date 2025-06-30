/*
 * Copyright 2013-2025 consulo.io
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
package consulo.usage.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.popup.JBPopup;
import consulo.usage.UsageView;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2025-06-29
 */
public interface ImplementationViewComponent {
    UsageView showInUsageView();

    JComponent getPreferredFocusableComponent();

    void setHint(JBPopup hint, @Nonnull LocalizeValue title);

    JComponent getComponent();
}

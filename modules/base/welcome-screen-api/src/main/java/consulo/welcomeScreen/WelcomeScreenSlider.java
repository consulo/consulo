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
package consulo.welcomeScreen;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.layout.Layout;
import consulo.util.dataholder.Key;

import java.util.function.Supplier;

/**
 * The welcome screen showing a slide - the new project wizard or a checkout - in place of its own content.
 *
 * @author VISTALL
 * @since 2026-08-15
 */
public interface WelcomeScreenSlider {
    Key<WelcomeScreenSlider> KEY = Key.create(WelcomeScreenSlider.class);

    void setTitle(LocalizeValue title);

    @RequiredUIAccess
    Layout showSlide(String id, @RequiredUIAccess Supplier<Layout> layoutSupplier);

    @RequiredUIAccess
    void removeSlide(Layout target);

    TitlelessDecorator getTitlelessDecorator();

    Disposable getDisposable();
}

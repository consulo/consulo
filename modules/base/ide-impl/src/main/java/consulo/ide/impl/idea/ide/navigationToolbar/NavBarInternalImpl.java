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
package consulo.ide.impl.idea.ide.navigationToolbar;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.util.DeleteHandler;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.language.editor.util.IdeView;
import consulo.navigationBar.internal.NavBarInternal;
import consulo.ui.ex.DeleteProvider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-07-04
 */
@Singleton
@ServiceImpl
public class NavBarInternalImpl implements NavBarInternal {
    @Override
    public @Nullable Object getOrChooseDirectory(Object ideView) {
        return DirectoryChooserUtil.getOrChooseDirectory((IdeView) ideView);
    }

    @Override
    public DeleteProvider getModuleDeleteProvider() {
        return ModuleDeleteProvider.getInstance();
    }

    @Override
    public DeleteProvider getDefaultDeleteProvider() {
        return DeleteHandler.DefaultDeleteProvider.INSTANCE;
    }
}

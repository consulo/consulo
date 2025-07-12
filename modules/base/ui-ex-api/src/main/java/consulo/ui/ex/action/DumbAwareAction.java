/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ui.ex.action;

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * @author nik
 */
public abstract class DumbAwareAction extends AnAction implements DumbAware {

    @Nonnull
    public static DumbAwareAction create(@RequiredUIAccess @Nonnull Consumer<? super AnActionEvent> actionPerformed) {
        return new DumbAwareAction() {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                actionPerformed.accept(e);
            }
        };
    }

    @Nonnull
    @Deprecated
    public static DumbAwareAction create(@Nullable String text,
                                         @Nullable Image image,
                                         @RequiredUIAccess @Nonnull Consumer<? super AnActionEvent> actionPerformed) {
        return new DumbAwareAction(text, null, image) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                actionPerformed.accept(e);
            }
        };
    }

    @Nonnull
    @Deprecated
    public static DumbAwareAction create(@Nullable String text, @RequiredUIAccess @Nonnull Consumer<? super AnActionEvent> actionPerformed) {
        return new DumbAwareAction(text) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                actionPerformed.accept(e);
            }
        };
    }

    @Nonnull
    public static DumbAwareAction create(@Nonnull LocalizeValue text,
                                         @RequiredUIAccess @Nonnull Consumer<? super AnActionEvent> actionPerformed) {
        return new DumbAwareAction(text) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                actionPerformed.accept(e);
            }
        };
    }

    protected DumbAwareAction() {
    }

    protected DumbAwareAction(Image icon) {
        super(icon);
    }

    @Deprecated
    protected DumbAwareAction(@Nullable String text) {
        super(text);
    }

    @Deprecated
    protected DumbAwareAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected DumbAwareAction(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected DumbAwareAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected DumbAwareAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }
}
// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.component.util.pointer.Pointer;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.navigationBar.NavBarItem;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public class ModuleNavBarItem extends DefaultNavBarItem<Module> implements Pointer<NavBarItem> {
    public ModuleNavBarItem(Module data) {
        super(data);
    }

    @Override
    public Pointer<? extends NavBarItem> createPointer() {
        return this;
    }

    @Override
    public @Nullable NavBarItem dereference() {
        return getData().isDisposed() ? null : this;
    }

    @Override
    public @Nullable Navigatable navigationRequest() {
        return new Navigatable() {
            @Override
            public void navigate(boolean requestFocus) {
                // Is currently a no-op because AbstractProjectViewPane doesn't support navigating to modules.
                // In the vast majority of cases, it's a non-issue because a module is normally represented by its content root,
                // and that is supported (and this class isn't even used then).
                // Previously, this function would just throw an exception, but it's better to just do nothing.
            }

            @Override
            public boolean canNavigate() {
                return true;
            }

            @Override
            public boolean canNavigateToSource() {
                return true;
            }
        };
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesModule();
    }

    @Override
    public SimpleTextAttributes getTextAttributes() {
        WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(getData().getProject());
        boolean hasProblems = problemSolver.hasProblemFilesBeneath(getData());

        return hasProblems ? DefaultNavBarItem.navBarErrorAttributes() : SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    @Override
    public int weight() {
        return 5;
    }
}

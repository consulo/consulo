// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.component.util.pointer.Pointer;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.navigationBar.NavBarItem;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public class ProjectNavBarItem extends DefaultNavBarItem<Project> implements Pointer<NavBarItem> {
    public ProjectNavBarItem(Project data) {
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
    public Image getIcon() {
        return PlatformIconGroup.nodesProject();
    }

    @Override
    public SimpleTextAttributes getTextAttributes() {
        WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(getData());
        boolean hasProblems = false;
        for (Module module : ModuleManager.getInstance(getData()).getModules()) {
            if (problemSolver.hasProblemFilesBeneath(module)) {
                hasProblems = true;
                break;
            }
        }
        return hasProblems ? DefaultNavBarItem.navBarErrorAttributes() : SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }
}

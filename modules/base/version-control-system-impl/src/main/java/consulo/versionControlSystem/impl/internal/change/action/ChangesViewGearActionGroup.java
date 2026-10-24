// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.DefaultActionGroup;

@ActionImpl(id = "LocalChangesView.GearActions", children = @ActionRef(type = ShowOnDoubleClickGroup.class))
public class ChangesViewGearActionGroup extends DefaultActionGroup implements DumbAware {
}

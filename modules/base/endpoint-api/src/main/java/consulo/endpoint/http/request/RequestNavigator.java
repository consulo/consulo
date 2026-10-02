// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.http.request;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

@ExtensionAPI(ComponentScope.PROJECT)
public interface RequestNavigator {
    static List<RequestNavigator> getRequestNavigators(Project project) {
        return project.getExtensionPoint(RequestNavigator.class).collectMapped(Function.identity());
    }

    static List<RequestNavigator> getRequestNavigators(Project project, NavigatorHttpRequest request) {
        return project.getExtensionPoint(RequestNavigator.class).collectFiltered(navigator -> navigator.accept(request));
    }

    String getId();

    @Nullable Image getIcon();

    LocalizeValue getDisplayText();

    default LocalizeValue getNavigationGroupName() {
        return LocalizeValue.empty();
    }

    boolean accept(NavigatorHttpRequest request);

    @RequiredUIAccess
    void navigate(NavigatorHttpRequest request, String hint);

    default boolean hasTarget() {
        return false;
    }

    default LocalizeValue getNavigationMessage(NavigatorHttpRequest request) {
        return LocalizeValue.empty();
    }
}

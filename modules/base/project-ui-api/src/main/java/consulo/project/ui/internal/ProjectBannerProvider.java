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
package consulo.project.ui.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.Toast;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * A banner on top of the list of running tasks of the status bar while the project is in some state - indexing, for
 * one.
 * Each banner can be closed until the state comes again, or dismissed for good with "don't show again", which is kept
 * with the settings of the user.
 * <p/>
 * A provider asks {@link ProjectBannerManager#update()} once the answer of {@link #isVisible()} may have changed.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface ProjectBannerProvider {
    /**
     * A stable id - the key of "don't show again", namespaced as {@code banner.<name>}.
     */
    String getId();

    /**
     * Asked on every update; the banner is up while this holds and the user did not close it. Called on the ui thread.
     */
    boolean isVisible();

    /**
     * Fills a fresh toast - title, text, style. Close and "don't show again" are added around it.
     */
    @RequiredUIAccess
    void customize(Toast toast);
}

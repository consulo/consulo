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
package consulo.continuousIntegration;

import consulo.accountManagement.Account;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.continuousIntegration.ui.BaseCIServerEditor;
import consulo.continuousIntegration.ui.CIServerEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import org.jdom.Element;

import java.util.List;
import java.util.function.Consumer;

/**
 * Kind of CI server (Jenkins, TeamCity, GitHub Actions). Servers are created from options; the URL and credentials come
 * from an account of {@link #getAccountTypeId()}.
 *
 * @since 2026-11-09
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CIServerType<O extends CIServerOptions> {
    public abstract String getId();

    public abstract LocalizeValue getPresentableName();

    public abstract Image getIcon();

    /**
     * Icon of the tool window when this is the only provider: monochrome and outlined like other tool window icons.
     */
    public Image getToolWindowIcon() {
        return getIcon();
    }

    public abstract String getAccountTypeId();

    public abstract O createOptions();

    public abstract CIServer createServer(Project project, O options);

    /**
     * Servers to show for an account without any setup. Called when accounts change. Account id and server URL are
     * filled by the caller when they stay empty.
     */
    public List<O> detect(Project project, Account account) {
        return List.of(createOptions());
    }

    /**
     * Two options with the same key describe the same server; detected servers are not added twice.
     */
    public String getDetectionKey(O options) {
        return getId() + ":" + options.accountId + ":" + options.serverUrl;
    }

    public CIServerEditor createEditor(O options, Project project, Consumer<O> changeListener) {
        return new BaseCIServerEditor<>(project, this, options, changeListener);
    }

    public final O copyOptions(CIServerOptions options) {
        return CIServerOptionsUtil.deserializeInto(CIServerOptionsUtil.serialize(options), createOptions());
    }

    public final O readOptions(Element element) {
        return CIServerOptionsUtil.deserializeInto(element, createOptions());
    }

    @SuppressWarnings("unchecked")
    public final CIServer createServerUnchecked(Project project, CIServerOptions options) {
        return createServer(project, (O) options);
    }

    @SuppressWarnings("unchecked")
    public final String getDetectionKeyUnchecked(CIServerOptions options) {
        return getDetectionKey((O) options);
    }

    @SuppressWarnings("unchecked")
    public final List<CIServerOptions> detectUnchecked(Project project, Account account) {
        return (List<CIServerOptions>) detect(project, account);
    }

    @SuppressWarnings("unchecked")
    public final CIServerEditor createEditorUnchecked(CIServerOptions options, Project project, Consumer<CIServerOptions> changeListener) {
        return createEditor((O) options, project, (Consumer<O>) changeListener);
    }
}

// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.service.execution;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.build.ui.output.BuildOutputParser;
import consulo.component.extension.ExtensionPointName;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ExternalSystemTask;
import consulo.externalSystem.model.task.ExternalSystemTaskId;

import java.util.List;

/**
 * Provides build output parsers for external system task.
 *
 * @see BuildOutputParser
 * @see ExternalSystemTask
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalSystemOutputParserProvider {
    ExtensionPointName<ExternalSystemOutputParserProvider> EP_NAME =
        ExtensionPointName.create(ExternalSystemOutputParserProvider.class);

    /**
     * External system id is needed to find applicable parsers provider for external system task.
     */
    ProjectSystemId getExternalSystemId();

    /**
     * Creates build output parsers.
     *
     * @param taskId is id of build task that output should be patched by these parsers.
     * @return parsers for messages from text and build events.
     */
    List<BuildOutputParser> getBuildOutputParsers(ExternalSystemTaskId taskId);
}

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
package consulo.mcpServer.impl.internal.activity;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.localize.LocalizeValue;
import consulo.mcp.tool.McpToolDescriptor;
import consulo.mcpServer.impl.internal.McpSession;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.UIAccess;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpToolApprovalService {
    private static final long APPROVAL_TIMEOUT_MINUTES = 5;
    private static final int MAX_ARGUMENTS_TEXT = 1500;

    private final McpServerSettings mySettings;
    private final ProjectManager myProjectManager;

    @Inject
    public McpToolApprovalService(McpServerSettings settings, ProjectManager projectManager) {
        mySettings = settings;
        myProjectManager = projectManager;
    }

    public boolean requiresApproval(McpToolDescriptor descriptor) {
        if (!mySettings.isRequireApproval() || Boolean.TRUE.equals(descriptor.getReadOnlyHint())) {
            return false;
        }
        return Boolean.TRUE.equals(descriptor.getDestructiveHint()) || Boolean.TRUE.equals(descriptor.getOpenWorldHint());
    }

    public boolean isPreApproved(@Nullable McpSession session, String toolName) {
        return session != null && (session.isToolApproved(toolName) || mySettings.isAlwaysAllowed(session.getPrincipal().id(), toolName));
    }

    public CompletableFuture<Boolean> askApproval(@Nullable McpSession session,
                                                  @Nullable Project project,
                                                  String clientName,
                                                  String toolName,
                                                  String arguments) {
        UIAccess uiAccess = findUIAccess(project);
        if (uiAccess == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        LocalizeValue detail = McpServerLocalize.approvalDetail(StringUtil.first(arguments, MAX_ARGUMENTS_TEXT, true));
        return CompletableFuture.supplyAsync(() -> MessageBoxBuilder.<McpToolApproval>create()
                .asWarning()
                .title(McpServerLocalize.approvalTitle())
                .text(McpServerLocalize.approvalText(clientName, toolName))
                .detail(detail)
                .button(MessageButtonRole.YES, McpServerLocalize.approvalAllowOnce(), McpToolApproval.ONCE)
                .button(MessageButtonRole.YES_TO_ALL, McpServerLocalize.approvalAllowSession(), McpToolApproval.SESSION)
                .button(MessageButtonRole.OK, McpServerLocalize.approvalAllowAlways(), McpToolApproval.ALWAYS)
                .button(MessageButtonRole.NO, McpServerLocalize.approvalDeny(), McpToolApproval.DENY)
                .asDefaultButton()
                .asExitButton()
                .showAsync(), uiAccess)
            .thenCompose(future -> future)
            .completeOnTimeout(McpToolApproval.DENY, APPROVAL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .thenApply(approval -> {
                switch (approval) {
                    case SESSION -> {
                        if (session != null) {
                            session.approveTool(toolName);
                        }
                    }
                    case ALWAYS -> {
                        if (session != null) {
                            mySettings.allowAlways(session.getPrincipal().id(), toolName);
                        }
                    }
                    default -> {
                    }
                }
                return approval != McpToolApproval.DENY;
            });
    }

    private @Nullable UIAccess findUIAccess(@Nullable Project project) {
        if (project != null && !project.isDisposed() && project.getUIAccess().isValid()) {
            return project.getUIAccess();
        }
        for (Project openProject : myProjectManager.getOpenProjects()) {
            if (!openProject.isDisposed() && openProject.getUIAccess().isValid()) {
                return openProject.getUIAccess();
            }
        }
        return null;
    }
}

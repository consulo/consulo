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
package consulo.execution.profiler.impl.internal.toolwindow;

import consulo.execution.attach.XAttachHost;
import consulo.platform.ProcessInfo;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public record ProfilerAttachRow(XAttachHost host, String hostName, int hostOrder, ProcessInfo process, List<ProfilerAttachOption> options) {
    public String getProfilersText() {
        StringBuilder builder = new StringBuilder();
        for (ProfilerAttachOption option : options) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(option.name().get());
        }
        return builder.toString();
    }
}

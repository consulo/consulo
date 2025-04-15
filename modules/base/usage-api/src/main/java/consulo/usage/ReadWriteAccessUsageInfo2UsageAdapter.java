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
package consulo.usage;

import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * @since 2005-01-17
 */
public class ReadWriteAccessUsageInfo2UsageAdapter extends UsageInfo2UsageAdapter implements ReadWriteAccessUsage {
    private final boolean myAccessedForReading;
    private final boolean myAccessedForWriting;

    public ReadWriteAccessUsageInfo2UsageAdapter(@Nonnull UsageInfo usageInfo, boolean accessedForReading, boolean accessedForWriting) {
        super(usageInfo);
        myAccessedForReading = accessedForReading;
        myAccessedForWriting = accessedForWriting;
        if (myAccessedForReading && myAccessedForWriting) {
            myIcon = PlatformIconGroup.nodesRw_access();
        }
        else if (myAccessedForWriting) {
            myIcon = PlatformIconGroup.nodesWrite_access();
        }
        else if (myAccessedForReading) {
            myIcon = PlatformIconGroup.nodesRead_access();
        }
    }

    @Override
    public boolean isAccessedForWriting() {
        return myAccessedForWriting;
    }

    @Override
    public boolean isAccessedForReading() {
        return myAccessedForReading;
    }
}

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
package consulo.usage.impl.internal.rule;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageView;
import consulo.usage.localize.UsageLocalize;
import consulo.usage.rule.UsageGroupingRule;
import consulo.usage.rule.UsageInLibrary;
import consulo.usage.rule.UsageInModule;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

/**
 * @author max
 */
public class ModuleGroupingRule implements UsageGroupingRule {
    @Override
    public UsageGroup groupUsage(Usage usage) {
        if (usage instanceof UsageInModule) {
            UsageInModule usageInModule = (UsageInModule) usage;
            Module module = usageInModule.getModule();
            if (module != null) {
                return new ModuleUsageGroup(module);
            }
        }

        if (usage instanceof UsageInLibrary) {
            UsageInLibrary usageInLibrary = (UsageInLibrary) usage;
            OrderEntry entry = usageInLibrary.getLibraryEntry();
            if (entry != null) {
                return new LibraryUsageGroup(entry);
            }
        }

        return null;
    }

    private static class LibraryUsageGroup implements UsageGroup {

        private final OrderEntry myEntry;

        @Override
        public void update() {
        }

        public LibraryUsageGroup(OrderEntry entry) {
            myEntry = entry;
        }

        @Override
        public Image getIcon() {
            return PlatformIconGroup.nodesPplibfolder();
        }

        @Override
        public String getText(UsageView view) {
            return myEntry.getPresentableName();
        }

        @Override
        public FileStatus getFileStatus() {
            return null;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public int compareTo(UsageGroup usageGroup) {
            if (usageGroup instanceof ModuleUsageGroup) {
                return 1;
            }
            return getText(null).compareToIgnoreCase(usageGroup.getText(null));
        }

        @Override
        @RequiredUIAccess
        public void navigate(boolean requestFocus) {
        }

        @Override
        @RequiredReadAction
        public boolean canNavigate() {
            return false;
        }

        @Override
        @RequiredReadAction
        public boolean canNavigateToSource() {
            return canNavigate();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof LibraryUsageGroup that
                && myEntry.equals(that.myEntry);
        }

        @Override
        public int hashCode() {
            return myEntry.hashCode();
        }
    }

    private static class ModuleUsageGroup implements UsageGroup, UiDataProvider {
        private final Module myModule;

        public ModuleUsageGroup(Module module) {
            myModule = module;
        }

        @Override
        public void update() {
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof ModuleUsageGroup that
                && myModule.equals(that.myModule);
        }

        @Override
        public int hashCode() {
            return myModule.hashCode();
        }

        @Override
        public Image getIcon() {
            return myModule.isDisposed() ? null : AllIcons.Nodes.Module;
        }

        @Override
        public String getText(UsageView view) {
            return myModule.isDisposed() ? "" : myModule.getName();
        }

        @Override
        public FileStatus getFileStatus() {
            return null;
        }

        @Override
        public boolean isValid() {
            return !myModule.isDisposed();
        }

        @Override
        @RequiredUIAccess
        public void navigate(boolean focus) throws UnsupportedOperationException {
        }

        @Override
        @RequiredReadAction
        public boolean canNavigate() {
            return false;
        }

        @Override
        @RequiredReadAction
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        public int compareTo(UsageGroup o) {
            if (o instanceof LibraryUsageGroup) {
                return -1;
            }
            return getText(null).compareToIgnoreCase(o.getText(null));
        }

        @Override
        public String toString() {
            return UsageLocalize.nodeGroupModule().get() + getText(null);
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            if (!isValid()) {
                return;
            }
            sink.set(LangDataKeys.MODULE_CONTEXT, myModule);
        }
    }
}

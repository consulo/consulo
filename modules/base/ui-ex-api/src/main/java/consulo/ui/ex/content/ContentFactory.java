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
package consulo.ui.ex.content;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.ui.Component;
import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.APPLICATION)
public interface ContentFactory {
    
    static ContentFactory getInstance() {
        return Application.get().getInstance(ContentFactory.class);
    }

    
    ContentManager createContentManager(ContentUI contentUI, boolean canCloseContents, ComponentManager project);

    
    ContentManager createContentManager(boolean canCloseContents, ComponentManager project);

    /**
     * do not rename due it will be conflicted with deprecated method
     */
    default Content createUIContent(@Nullable Component component, String displayName, boolean isLockable) {
        throw new AbstractMethodError();
    }

    // TODO [VISTALL] AWT & Swing dependency
    // region AWT & Swing dependency
    
    @Deprecated
    @DeprecationInfo("Use createUIContent")
    default Content createContent(javax.swing.JComponent component, String displayName, boolean isLockable) {
        throw new AbstractMethodError();
    }

    
    @Deprecated
    default TabbedContent createTabbedContent(javax.swing.JComponent component, String displayName, boolean isPinnable, String titlePrefix) {
        throw new AbstractMethodError();
    }
    // endregion

    // region Deprecated staff
    @Deprecated
    class SERVICE {
        private SERVICE() {
        }

        
        @Deprecated
        public static ContentFactory getInstance() {
            return Application.get().getInstance(ContentFactory.class);
        }
    }
    // endregion
}

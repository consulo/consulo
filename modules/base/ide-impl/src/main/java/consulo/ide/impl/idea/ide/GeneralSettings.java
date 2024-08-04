/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.execution.ProcessCloseConfirmation;
import consulo.ide.ServiceManager;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.OptionTag;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;
import org.intellij.lang.annotations.MagicConstant;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "GeneralSettings", storages = @Storage("ide.general.xml"))
public class GeneralSettings implements PersistentStateComponent<GeneralSettings> {
    public static final String PROP_SUPPORT_SCREEN_READERS = "supportScreenReaders";
    public static final String PROP_INACTIVE_TIMEOUT = "inactiveTimeout";

    private String myBrowserPath;

    private boolean myShowTipsOnStartup = true;
    private int myLastTip = 0;
    private boolean mySupportScreenReaders = false;
    private boolean myReopenLastProject = true;
    private boolean mySyncOnFrameActivation = true;
    private boolean mySaveOnFrameDeactivation = true;
    private boolean myAutoSaveIfInactive = false;  // If true the IDEA automatically saves files if it is inactive for some seconds
    private int myInactiveTimeout = 15; // Number of seconds of inactivity after which IDEA automatically saves all files
    private boolean myUseSafeWrite = true;
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);
    private boolean myUseDefaultBrowser = true;
    private boolean myConfirmExtractFiles = true;
    private boolean mySearchInBackground;
    private boolean myConfirmExit = true;
    private int myConfirmOpenNewProject = OPEN_PROJECT_ASK;
    private ProcessCloseConfirmation myProcessCloseConfirmation = ProcessCloseConfirmation.ASK;

    public static GeneralSettings getInstance() {
        return ServiceManager.getService(GeneralSettings.class);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    public String getBrowserPathOrDefault() {
        if (!StringUtil.isEmpty(myBrowserPath)) {
            return myBrowserPath;
        }

        return BrowserUtil.getDefaultAlternativeBrowserPath();
    }

    public String getBrowserPath() {
        return myBrowserPath;
    }

    public void setBrowserPath(String browserPath) {
        myBrowserPath = browserPath;
    }

    @Deprecated
    public boolean showTipsOnStartup() {
        return isShowTipsOnStartup();
    }

    public boolean isShowTipsOnStartup() {
        return myShowTipsOnStartup;
    }

    public void setShowTipsOnStartup(boolean b) {
        myShowTipsOnStartup = b;
    }

    public int getLastTip() {
        return myLastTip;
    }

    public void setLastTip(int i) {
        myLastTip = i;
    }

    public boolean isReopenLastProject() {
        return myReopenLastProject;
    }

    public void setReopenLastProject(boolean reopenLastProject) {
        myReopenLastProject = reopenLastProject;
    }

    public boolean isSyncOnFrameActivation() {
        return mySyncOnFrameActivation;
    }

    public void setSyncOnFrameActivation(boolean syncOnFrameActivation) {
        mySyncOnFrameActivation = syncOnFrameActivation;
    }

    public boolean isSupportScreenReaders() {
        return mySupportScreenReaders;
    }

    public void setSupportScreenReaders(boolean enabled) {
        boolean oldValue = mySupportScreenReaders;
        mySupportScreenReaders = enabled;
        myPropertyChangeSupport.firePropertyChange(PROP_SUPPORT_SCREEN_READERS, Boolean.valueOf(oldValue), Boolean.valueOf(enabled));
    }

    public ProcessCloseConfirmation getProcessCloseConfirmation() {
        return myProcessCloseConfirmation;
    }

    public void setProcessCloseConfirmation(ProcessCloseConfirmation processCloseConfirmation) {
        myProcessCloseConfirmation = processCloseConfirmation;
    }

    public boolean isSaveOnFrameDeactivation() {
        return mySaveOnFrameDeactivation;
    }

    public void setSaveOnFrameDeactivation(boolean saveOnFrameDeactivation) {
        mySaveOnFrameDeactivation = saveOnFrameDeactivation;
    }

    /**
     * @return <code>true</code> if IDEA saves all files after "idle" timeout.
     */
    public boolean isAutoSaveIfInactive() {
        return myAutoSaveIfInactive;
    }

    public void setAutoSaveIfInactive(boolean autoSaveIfInactive) {
        myAutoSaveIfInactive = autoSaveIfInactive;
    }

    /**
     * @return timeout in seconds after which IDEA saves all files if there was no user activity.
     * The method always return non positive (more then zero) value.
     */
    public int getInactiveTimeout() {
        return myInactiveTimeout;
    }

    public void setInactiveTimeout(int inactiveTimeout) {
        int oldInactiveTimeout = myInactiveTimeout;

        myInactiveTimeout = inactiveTimeout;
        myPropertyChangeSupport.firePropertyChange(PROP_INACTIVE_TIMEOUT, Integer.valueOf(oldInactiveTimeout), Integer.valueOf(inactiveTimeout));
    }

    public boolean isUseSafeWrite() {
        return myUseSafeWrite;
    }

    public void setUseSafeWrite(final boolean useSafeWrite) {
        myUseSafeWrite = useSafeWrite;
    }

    @Nullable
    @Override
    public GeneralSettings getState() {
        return this;
    }

    @Override
    public void loadState(GeneralSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public boolean isUseDefaultBrowser() {
        return myUseDefaultBrowser;
    }

    public void setUseDefaultBrowser(boolean value) {
        myUseDefaultBrowser = value;
    }

    public boolean isConfirmExtractFiles() {
        return myConfirmExtractFiles;
    }

    public void setConfirmExtractFiles(boolean value) {
        myConfirmExtractFiles = value;
    }

    public boolean isConfirmExit() {
        return myConfirmExit;
    }

    public void setConfirmExit(boolean confirmExit) {
        myConfirmExit = confirmExit;
    }

    @MagicConstant(intValues = {OPEN_PROJECT_ASK, OPEN_PROJECT_NEW_WINDOW, OPEN_PROJECT_SAME_WINDOW})
    @interface OpenNewProjectOption {
    }

    /**
     * @return <ul>
     * <li>{@link GeneralSettings#OPEN_PROJECT_NEW_WINDOW} if new project should be opened in new window
     * <li>{@link GeneralSettings#OPEN_PROJECT_SAME_WINDOW} if new project should be opened in same window
     * <li>{@link GeneralSettings#OPEN_PROJECT_ASK} if a confirmation dialog should be shown
     * </ul>
     */
    @OpenNewProjectOption
    @OptionTag("confirmOpenNewProject2")
    public int getConfirmOpenNewProject() {
        return myConfirmOpenNewProject;
    }

    public void setConfirmOpenNewProject(@OpenNewProjectOption int confirmOpenNewProject) {
        myConfirmOpenNewProject = confirmOpenNewProject;
    }

    @Deprecated
    public static final int OPEN_PROJECT_ASK = -1;
    @Deprecated
    public static final int OPEN_PROJECT_NEW_WINDOW = 0;
    @Deprecated
    public static final int OPEN_PROJECT_SAME_WINDOW = 1;

    public boolean isSearchInBackground() {
        return mySearchInBackground;
    }

    public void setSearchInBackground(final boolean searchInBackground) {
        mySearchInBackground = searchInBackground;
    }
}
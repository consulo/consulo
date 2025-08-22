/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.task.impl.internal;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.task.*;
import consulo.task.icon.TaskIconGroup;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@Tag("task")
public class LocalTaskImpl extends LocalTask {
    public static final String DEFAULT_TASK_ID = "Default";

    private String myId = "";
    private String mySummary = "";
    private String myDescription = null;
    private Comment[] myComments = Comment.EMPTY_ARRAY;
    private boolean myClosed = false;
    private Date myCreated;
    private Date myUpdated;
    private TaskType myType = TaskType.OTHER;
    private String myPresentableName;
    private String myCustomIcon = null;

    private String myProject = null;
    private String myNumber = "";
    private String myPresentableId = "";

    private boolean myIssue = false;
    private TaskRepository myRepository = null;
    private String myIssueUrl = null;

    private boolean myActive;
    private List<ChangeListInfo> myChangeLists = new ArrayList<>();
    private boolean myRunning = false;
    private List<WorkItem> myWorkItems = new ArrayList<>();
    private Date myLastPost;
    private List<BranchInfo> myBranches = new ArrayList<>();

    /**
     * for serialization
     */
    @SuppressWarnings("unused")
    public LocalTaskImpl() {
    }

    public LocalTaskImpl(@Nonnull String id, @Nonnull String summary) {
        myId = id;
        mySummary = summary;
    }

    public LocalTaskImpl(Task origin) {
        myId = origin.getId();
        myIssue = origin.isIssue();
        myRepository = origin.getRepository();

        copy(origin);

        if (origin instanceof LocalTaskImpl) {
            myChangeLists = ((LocalTaskImpl) origin).getChangeLists();
            myBranches = ((LocalTaskImpl) origin).getBranches();
            myActive = ((LocalTaskImpl) origin).isActive();
            myWorkItems = ((LocalTaskImpl) origin).getWorkItems();
            myRunning = ((LocalTaskImpl) origin).isRunning();
            myLastPost = ((LocalTaskImpl) origin).getLastPost();
        }
    }

    @Override
    @Attribute("id")
    @Nonnull
    public String getId() {
        return myId;
    }

    @Override
    @Attribute("summary")
    @Nonnull
    public String getSummary() {
        return mySummary;
    }

    @Override
    public String getDescription() {
        return myDescription;
    }

    @Nonnull
    @Override
    public Comment[] getComments() {
        return myComments;
    }

    @Override
    @Tag("updated")
    public Date getUpdated() {
        return myUpdated == null ? getCreated() : myUpdated;
    }

    @Override
    @Tag("created")
    public Date getCreated() {
        if (myCreated == null) {
            myCreated = new Date();
        }
        return myCreated;
    }

    @Override
    @Attribute("active")
    public boolean isActive() {
        return myActive;
    }

    @Override
    public void updateFromIssue(Task issue) {
        copy(issue);
        myIssue = true;
    }

    private void copy(Task issue) {
        mySummary = issue.getSummary();
        myDescription = issue.getDescription();
        myComments = issue.getComments();
        myClosed = issue.isClosed();
        myCreated = issue.getCreated();
        if (Comparing.compare(myUpdated, issue.getUpdated()) < 0) {
            myUpdated = issue.getUpdated();
        }
        myType = issue.getType();
        myPresentableName = issue.getPresentableName();
        myCustomIcon = issue.getCustomIcon();
        myIssueUrl = issue.getIssueUrl();
        myRepository = issue.getRepository();

        myProject = issue.getProject();
        myNumber = issue.getNumber();
        myPresentableId = issue.getPresentableId();
    }

    public void setId(String id) {
        myId = id;
    }

    public void setSummary(String summary) {
        mySummary = summary;
    }

    @Override
    public void setActive(boolean active) {
        myActive = active;
    }

    @Override
    public boolean isIssue() {
        return myIssue;
    }

    @Override
    public String getIssueUrl() {
        return myIssueUrl;
    }

    public void setIssue(boolean issue) {
        myIssue = issue;
    }

    @Override
    public TaskRepository getRepository() {
        return myRepository;
    }

    public void setCreated(Date created) {
        myCreated = created;
    }

    @Override
    public void setUpdated(Date updated) {
        myUpdated = updated;
    }

    @Override
    @Nonnull
    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false, elementTag = "changelist")
    public List<ChangeListInfo> getChangeLists() {
        return myChangeLists;
    }

    public void setChangeLists(List<ChangeListInfo> changeLists) {
        myChangeLists = changeLists;
    }

    @Override
    public void addChangelist(ChangeListInfo info) {
        if (!myChangeLists.contains(info)) {
            myChangeLists.add(info);
        }
    }

    @Override
    public void removeChangelist(ChangeListInfo info) {
        myChangeLists.remove(info);
    }

    @Nonnull
    @Override
    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false, elementTag = "branch")
    public List<BranchInfo> getBranches() {
        return myBranches;
    }

    public void setBranches(List<BranchInfo> branches) {
        myBranches = branches;
    }

    @Override
    public void addBranch(BranchInfo info) {
        myBranches.add(info);
    }

    @Override
    public void removeBranch(BranchInfo info) {
        myBranches.add(info);
    }

    @Override
    public boolean isClosed() {
        return myClosed;
    }

    public void setClosed(boolean closed) {
        myClosed = closed;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        String customIcon = getCustomIcon();
        if (customIcon != null) {
            try {
                return Image.fromUrl(new File(customIcon).toURI().toURL());
            }
            catch (IOException ignored) {
            }
        }
        return getIcon(myType, isIssue());
    }

    private static Image getIcon(TaskType taskType, boolean issue) {
        switch (taskType) {
            case BUG:
                return TaskIconGroup.bug();
            case EXCEPTION:
                return TaskIconGroup.exception();
            case FEATURE:
                return PlatformIconGroup.nodesFavorite();
            default:
            case OTHER:
                return issue ? PlatformIconGroup.filetypesAny_type() : TaskIconGroup.task();
        }
    }

    @Nonnull
    @Override
    public TaskType getType() {
        return myType;
    }

    public void setType(TaskType type) {
        myType = type == null ? TaskType.OTHER : type;
    }

    @Override
    public boolean isDefault() {
        return myId.equals(DEFAULT_TASK_ID);
    }

    @Override
    public String getPresentableName() {
        return myPresentableName != null ? myPresentableName : toString();
    }

    @Override
    public String getCustomIcon() {
        return myCustomIcon;
    }

    @Override
    public long getTotalTimeSpent() {
        long timeSpent = 0;
        for (WorkItem item : myWorkItems) {
            timeSpent += item.duration;
        }
        return timeSpent;
    }

    @Tag("running")
    @Override
    public boolean isRunning() {
        return myRunning;
    }

    @Override
    public void setRunning(boolean running) {
        myRunning = running;
    }

    @Override
    public void setWorkItems(List<WorkItem> workItems) {
        myWorkItems = workItems;
    }

    @Nonnull
    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false, elementTag = "workItem")
    @Override
    public List<WorkItem> getWorkItems() {
        return myWorkItems;
    }

    @Override
    public void addWorkItem(WorkItem workItem) {
        myWorkItems.add(workItem);
    }

    @Tag("lastPost")
    @Override
    public Date getLastPost() {
        return myLastPost;
    }

    @Override
    public void setLastPost(Date date) {
        myLastPost = date;
    }

    @Override
    public long getTimeSpentFromLastPost() {
        long timeSpent = 0;
        if (myLastPost != null) {
            for (WorkItem item : myWorkItems) {
                if (item.from.getTime() < myLastPost.getTime()) {
                    if (item.from.getTime() + item.duration > myLastPost.getTime()) {
                        timeSpent += item.from.getTime() + item.duration - myLastPost.getTime();
                    }
                }
                else {
                    timeSpent += item.duration;
                }
            }
        }
        else {
            for (WorkItem item : myWorkItems) {
                timeSpent += item.duration;
            }
        }
        return timeSpent;
    }

    @Nonnull
    @Override
    public String getNumber() {
        // extract number from ID for compatibility
        return StringUtil.isEmpty(myNumber) ? extractNumberFromId(myId) : myNumber;
    }

    public void setNumber(@Nonnull String number) {
        myNumber = number;
    }

    @Nullable
    @Override
    public String getProject() {
        // extract project from ID for compatibility
        return StringUtil.isEmpty(myProject) ? extractProjectFromId(myId) : myProject;
    }

    public void setProject(@Nullable String project) {
        myProject = project;
    }

    public void setPresentableId(@Nonnull String presentableId) {
        myPresentableId = presentableId;
    }

    @Nonnull
    @Override
    public String getPresentableId() {
        // Use global ID for compatibility
        return StringUtil.isEmpty(myPresentableId) ? getId() : myPresentableId;
    }
}

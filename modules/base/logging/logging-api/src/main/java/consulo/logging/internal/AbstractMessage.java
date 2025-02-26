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
package consulo.logging.internal;

import java.util.Calendar;
import java.util.Date;

public abstract class AbstractMessage {

    private boolean myIsRead = false;
    private boolean myIsSubmitting = false;
    private SubmittedReportInfo mySubmissionInfo;
    private String myAdditionalInfo;
    private Runnable myNotificationExpire;
    private long myAssigneeId;

    private final Date myDate;

    public AbstractMessage() {
        myDate = Calendar.getInstance().getTime();
    }

    public abstract String getThrowableText();

    public abstract Throwable getThrowable();

    public abstract String getMessage();

    public boolean isRead() {
        return myIsRead;
    }

    public void setRead(boolean aReadFlag) {
        myIsRead = aReadFlag;
        if (myNotificationExpire != null && aReadFlag) {
            myNotificationExpire.run();
            myNotificationExpire = null;
        }
    }

    public void setSubmitted(SubmittedReportInfo info) {
        myIsSubmitting = false;
        mySubmissionInfo = info;
    }

    public SubmittedReportInfo getSubmissionInfo() {
        return mySubmissionInfo;
    }

    public void setNotificationExpire(Runnable notification) {
        myNotificationExpire = notification;
    }

    public boolean isSubmitting() {
        return myIsSubmitting;
    }

    public void setSubmitting(boolean isSubmitting) {
        myIsSubmitting = isSubmitting;
    }

    public boolean isSubmitted() {
        return mySubmissionInfo != null &&
            (mySubmissionInfo.getStatus() == SubmittedReportInfo.SubmissionStatus.NEW_ISSUE ||
                mySubmissionInfo.getStatus() == SubmittedReportInfo.SubmissionStatus.DUPLICATE);
    }

    public String getAdditionalInfo() {
        return myAdditionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        myAdditionalInfo = additionalInfo;
    }

    public Date getDate() {
        return myDate;
    }

    public long getAssigneeId() {
        return myAssigneeId;
    }

    public void setAssigneeId(long assigneeId) {
        myAssigneeId = assigneeId;
    }
}

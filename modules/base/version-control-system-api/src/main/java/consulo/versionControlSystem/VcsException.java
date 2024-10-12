/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class VcsException extends Exception {
    public static final VcsException[] EMPTY_ARRAY = new VcsException[0];

    private VirtualFile myVirtualFile;
    private Collection<LocalizeValue> myMessages;
    private boolean isWarning = false;

    public VcsException(@Nonnull LocalizeValue message) {
        super(message.get());
        initMessage(message);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public VcsException(String message) {
        super(message);
        initMessage(message);
    }

    @SuppressWarnings("deprecation")
    public VcsException(Throwable throwable, final boolean isWarning) {
        this(getMessage(throwable), throwable);
        this.isWarning = isWarning;
    }

    public VcsException(Throwable throwable) {
        this(throwable, false);
    }

    public VcsException(@Nonnull LocalizeValue message, final Throwable cause) {
        super(message.get(), cause);
        initMessage(message);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public VcsException(final String message, final Throwable cause) {
        super(message, cause);
        initMessage(message);
    }

    public VcsException(final String message, final boolean isWarning) {
        this(message);
        this.isWarning = isWarning;
    }

    public VcsException(@Nonnull List<LocalizeValue> messages) {
        myMessages = List.copyOf(messages);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public VcsException(Collection<String> messages) {
        myMessages = messages.stream()
            .map(message -> message == null ? LocalizeValue.empty() : LocalizeValue.of(message))
            .collect(Collectors.toList());
    }

    private void initMessage(@Nonnull LocalizeValue message) {
        myMessages = List.of(message);
    }

    private void initMessage(final String message) {
        myMessages = List.of(message == null ? VcsLocalize.exceptionTextUnknownError() : LocalizeValue.of(message));
    }

    //todo: should be in constructor?
    public void setVirtualFile(VirtualFile virtualFile) {
        myVirtualFile = virtualFile;
    }

    public VirtualFile getVirtualFile() {
        return myVirtualFile;
    }

    public String[] getMessages() {
        return myMessages.stream().map(LocalizeValue::get).toArray(String[]::new);
    }

    public VcsException setIsWarning(boolean warning) {
        isWarning = warning;
        return this;
    }

    public boolean isWarning() {
        return isWarning;
    }

    @Override
    public String getMessage() {
        return StringUtil.join(myMessages, ", ");
    }

    @Nullable
    public static String getMessage(@Nullable Throwable throwable) {
        return throwable != null ? ObjectUtil.chooseNotNull(throwable.getMessage(), throwable.getLocalizedMessage()) : null;
    }
}

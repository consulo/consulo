// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.editor;

public interface ComplexPath {
    /**
     * The session id is required to differentiate files between launches.
     * This is necessary to make the files appear in "Recent Files" correctly.
     * Without this field files are saved in the editor history via pointers and urls are saved to disk.
     * After reopening the project manager will try to restore the files and will not find them since necessary components are not ready yet
     * and despite this history entry will still be created using a url-only identity pointer where pointers are cached.
     * As a result all previously opened files will be seen by history manager as non-existent.
     * Including this session id helps distinguish files between launches.
     */
    String getSessionId();

    String getProjectHash();
}

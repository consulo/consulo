/* Copyright (c) 2020 Daniel Widdis, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package consulo.fileChooser.impl.system.windows2;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;

public interface IFileSaveDialog extends IFileDialog {

    IID IID_IFILESAVEDIALOG = new IID("{84bccd23-5fde-4cdb-aea4-af64b83d78ab}");
    CLSID CLSID_FILESAVEDIALOG = new CLSID("{C0B4E2F3-BA21-4773-8DBA-335EC946EB8B}");

    HRESULT SetSaveAsItem(Pointer psi);

    HRESULT SetProperties(Pointer pStore); // IPropertyStore

    HRESULT SetCollectedProperties(Pointer pList, boolean fAppendDefault); // IPropertyDescriptionList

    HRESULT GetProperties(PointerByReference ppStore); // IPropertyStore

    HRESULT ApplyProperties(Pointer psi, Pointer pStore, HWND hwnd, Pointer pSink); // IShellItem, IPropertyStore,
                                                                                    // HWND, IFileOperationProgressSink
}

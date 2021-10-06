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
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.Guid.REFIID;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface IShellItem extends IUnknown {

    IID IID_ISHELLITEM = new IID("{43826d1e-e718-42ee-bc55-a1e261c37bfe}");

    CLSID CLSID_SHELLITEM = new CLSID("{9ac9fbe1-e0a2-4ad6-b4ee-e212013ea917}");

    HRESULT BindToHandler(Pointer pbc, GUID.ByReference bhid, REFIID riid, PointerByReference ppv); // IBindCtx

    HRESULT GetParent(PointerByReference ppsi); // IShellItem

    HRESULT GetDisplayName(int sigdnName, PointerByReference ppszName); // SIGDN, WString

    HRESULT GetAttributes(int sfgaoMask, IntByReference psfgaoAttribs); // SFGAOF, SFGAOF

    HRESULT Compare(Pointer psi, int hint, IntByReference piOrder); // IShellItem , SICHINTF
}

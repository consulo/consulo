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
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.REFIID;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class ShellItem extends Unknown implements IShellItem {

    public ShellItem() {
    }

    public ShellItem(Pointer pvInstance) {
        super(pvInstance);
    }

    // VTBL Id indexing starts at 3 after Unknown's 0, 1, 2

    public HRESULT BindToHandler(Pointer pbc, GUID.ByReference bhid, REFIID riid, PointerByReference ppv) {
        return (HRESULT) this._invokeNativeObject(3, new Object[] { this.getPointer(), pbc, bhid, riid, ppv },
                HRESULT.class);
    }

    public HRESULT GetParent(PointerByReference ppsi) {
        return (HRESULT) this._invokeNativeObject(4, new Object[] { this.getPointer(), ppsi }, HRESULT.class);
    }

    public HRESULT GetDisplayName(int sigdnName, PointerByReference ppszName) {
        return (HRESULT) this._invokeNativeObject(5, new Object[] { this.getPointer(), sigdnName, ppszName },
                HRESULT.class);
    }

    public HRESULT GetAttributes(int sfgaoMask, IntByReference psfgaoAttribs) {
        return (HRESULT) this._invokeNativeObject(6, new Object[] { this.getPointer(), sfgaoMask, psfgaoAttribs },
                HRESULT.class);
    }

    public HRESULT Compare(Pointer psi, int hint, IntByReference piOrder) {
        return (HRESULT) this._invokeNativeObject(7, new Object[] { this.getPointer(), psi, hint, piOrder },
                HRESULT.class);
    }
}

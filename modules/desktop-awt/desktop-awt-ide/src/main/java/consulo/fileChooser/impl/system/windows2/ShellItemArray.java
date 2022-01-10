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

import consulo.fileChooser.impl.system.windows2.ShTypes.GETPROPERTYSTOREFLAGS;
import consulo.fileChooser.impl.system.windows2.ShTypes.PROPERTYKEY;

public class ShellItemArray extends Unknown implements IShellItemArray {

    public ShellItemArray() {
    }

    public ShellItemArray(Pointer pvInstance) {
        super(pvInstance);
    }

    // VTBL Id indexing starts at 3 after Unknown's 0, 1, 2

    public HRESULT BindToHandler(Pointer pbc, GUID.ByReference bhid, REFIID riid, PointerByReference ppvOut) {
        return (HRESULT) this._invokeNativeObject(3, new Object[] { this.getPointer(), pbc, bhid, riid, ppvOut },
                HRESULT.class);
    }

    public HRESULT GetPropertyStore(GETPROPERTYSTOREFLAGS flags, REFIID riid, PointerByReference ppv) {
        return (HRESULT) this._invokeNativeObject(4, new Object[] { this.getPointer(), flags, riid, ppv },
                HRESULT.class);
    }

    public HRESULT GetPropertyDescriptionList(PROPERTYKEY keyType, REFIID riid, PointerByReference ppv) {
        return (HRESULT) this._invokeNativeObject(5, new Object[] { this.getPointer(), keyType, riid, ppv },
                HRESULT.class);
    }

    public HRESULT GetAttributes(int AttribFlags, int sfgaoMask, IntByReference psfgaoAttribs) {
        return (HRESULT) this._invokeNativeObject(6,
                new Object[] { this.getPointer(), AttribFlags, sfgaoMask, psfgaoAttribs }, HRESULT.class);
    }

    public HRESULT GetCount(IntByReference pdwNumItems) {
        return (HRESULT) this._invokeNativeObject(7, new Object[] { this.getPointer(), pdwNumItems }, HRESULT.class);
    }

    public HRESULT GetItemAt(int dwIndex, PointerByReference ppsi) {
        return (HRESULT) this._invokeNativeObject(8, new Object[] { this.getPointer(), dwIndex, ppsi }, HRESULT.class);
    }

}

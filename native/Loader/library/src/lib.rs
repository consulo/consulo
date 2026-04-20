/*
 * Copyright 2013-2026 consulo.io
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
//! LoaderLibrary cdylib — C ABI exports.
//!
//! Signatures here are the binary-compatibility gate.
//! DO NOT change signatures or rename exports — old Loaders already in the field
//! call these by name with the exact types below.
//!
//! Every exported function wraps its body in `catch_unwind` — Rust panics
//! crossing a C boundary are UB.

use anyhow::{Context, Result};
use consulo_loader_core::{args, jre, jvm, logging, ui, vmoptions};
use log::debug;
use std::ffi::c_int;
use std::panic::{AssertUnwindSafe, catch_unwind};
use std::path::PathBuf;

#[cfg(target_os = "windows")]
use consulo_loader_core::path_ext::PathExt;

/// Shared post-marshalling flow — same on every OS.
fn launch_from_paths(
    build_dir: PathBuf,
    properties_file: PathBuf,
    vm_options_file: PathBuf,
    app_home: PathBuf,
    main_args: Vec<String>,
) -> Result<()> {
    debug!("build: {build_dir:?}");
    debug!("properties: {properties_file:?}");
    debug!("vmoptions: {vm_options_file:?}");
    debug!("app home: {app_home:?}");

    let jre_home = jre::locate(&build_dir).context("locate JRE")?;
    debug!("JRE: {jre_home:?}");

    let user_vmoptions = vmoptions::read(&vm_options_file)?;
    let app_vmoptions_path = build_dir.join("bin").join("app.vmoptions");
    let app_vmoptions = vmoptions::read(&app_vmoptions_path)?;

    let opts = args::assemble(args::LaunchInputs {
        build_dir: &build_dir,
        app_home: &app_home,
        properties_file: &properties_file,
        user_vmoptions,
        app_vmoptions,
    })?;

    jvm::launch(jvm::LaunchSpec {
        jre_home,
        vm_options: opts,
        main_args,
    })
}

fn guarded<F: FnOnce() -> Result<()>>(f: F) -> c_int {
    match catch_unwind(AssertUnwindSafe(f)) {
        Ok(Ok(())) => 0,
        Ok(Err(e)) => {
            log::error!("launchConsulo failed: {e:?}");
            ui::show_error(true, e);
            1
        }
        Err(_) => {
            log::error!("launchConsulo panicked");
            2
        }
    }
}

// ============================================================================
// Windows
// ============================================================================

#[cfg(target_os = "windows")]
mod win {
    use super::*;
    use std::ffi::{OsString, c_void};
    use std::os::windows::ffi::OsStringExt;

    /// Legacy export. Ancient Loaders (pre-launchConsulo2) call this.
    /// Show a modal "please update" dialog with a link to the wiki.
    #[unsafe(no_mangle)]
    pub unsafe extern "C" fn launchConsulo(
        _h_instance: *mut c_void,
        _h_prev_instance: *mut c_void,
        _lp_cmd_line: *mut u16,
        _n_cmd_show: c_int,
        _argc: c_int,
        _wargv: *mut *mut u16,
        _module_file: *mut u16,
        _working_directory: *mut u16,
        _properties_file: *mut u16,
        _vm_option_file: *mut u16,
    ) -> c_int {
        use windows::Win32::UI::Shell::ShellExecuteW;
        use windows::Win32::UI::WindowsAndMessaging;
        use windows::core::{PCWSTR, w};

        logging::init();
        log::warn!("legacy launchConsulo called — prompting user to reinstall");

        let text = w!(
            "Major boot change. Please reinstall Consulo.\n\nClick OK to open the migration notes."
        );
        let title = w!("Consulo");
        let choice = unsafe {
            WindowsAndMessaging::MessageBoxW(
                None,
                text,
                title,
                WindowsAndMessaging::MB_OKCANCEL | WindowsAndMessaging::MB_ICONWARNING,
            )
        };
        if choice == WindowsAndMessaging::IDOK {
            let url = w!("https://consulo.help/platform/boot");
            unsafe {
                let _ = ShellExecuteW(
                    None,
                    PCWSTR::null(),
                    url,
                    PCWSTR::null(),
                    PCWSTR::null(),
                    windows::Win32::UI::WindowsAndMessaging::SW_SHOWNORMAL,
                );
            }
        }
        1
    }

    #[unsafe(no_mangle)]
    pub unsafe extern "C" fn launchConsulo2(
        _h_instance: *mut c_void,
        _h_prev_instance: *mut c_void,
        _lp_cmd_line: *mut u16,
        _n_cmd_show: c_int,
        argc: c_int,
        wargv: *mut *mut u16,
        _module_file: *mut u16,
        working_directory: *mut u16,
        properties_file: *mut u16,
        vm_option_file: *mut u16,
        app_home: *mut u16,
    ) -> c_int {
        guarded(|| unsafe {
            logging::init();

            let build = wchar_to_path(working_directory)?.strip_ns_prefix()?;
            let props = wchar_to_path(properties_file)?.strip_ns_prefix()?;
            let vmopts = wchar_to_path(vm_option_file)?.strip_ns_prefix()?;
            let app = wchar_to_path(app_home)?.strip_ns_prefix()?;
            let main_args = wchar_argv_to_strings(argc, wargv);

            launch_from_paths(build, props, vmopts, app, main_args)
        })
    }

    unsafe fn wchar_to_path(ptr: *mut u16) -> Result<PathBuf> {
        if ptr.is_null() {
            anyhow::bail!("null wchar* path");
        }
        let mut len = 0usize;
        while unsafe { *ptr.add(len) } != 0 {
            len += 1;
        }
        let slice = unsafe { std::slice::from_raw_parts(ptr, len) };
        Ok(PathBuf::from(OsString::from_wide(slice)))
    }

    unsafe fn wchar_argv_to_strings(argc: c_int, argv: *mut *mut u16) -> Vec<String> {
        if argv.is_null() || argc <= 0 {
            return Vec::new();
        }
        let mut out = Vec::with_capacity(argc as usize);
        // Skip argv[0] — Java main doesn't want the program path.
        for i in 1..argc as isize {
            let p = unsafe { *argv.offset(i) };
            if p.is_null() {
                continue;
            }
            let mut len = 0usize;
            while unsafe { *p.add(len) } != 0 {
                len += 1;
            }
            let slice = unsafe { std::slice::from_raw_parts(p, len) };
            let s = OsString::from_wide(slice);
            if let Some(utf8) = s.to_str() {
                out.push(utf8.to_string());
            }
        }
        out
    }
}

// ============================================================================
// macOS
// ============================================================================

#[cfg(target_os = "macos")]
mod mac {
    use super::*;
    use core_foundation::base::TCFType;
    use core_foundation::string::{CFString, CFStringRef};
    use std::ffi::{CStr, c_char, c_void};

    /// macOS entry point. `working_directory`/`properties_file`/`vm_options_file`/
    /// `app_home` are `NSString*` — toll-free bridged to `CFStringRef` — passed
    /// by the caller (MacLoader). We don't retain/release; caller owns them.
    #[unsafe(no_mangle)]
    pub unsafe extern "C" fn launchConsulo(
        argc: c_int,
        argv: *mut *mut c_char,
        working_directory: *const c_void,
        properties_file: *const c_void,
        vm_options_file: *const c_void,
        app_home: *const c_void,
    ) -> c_int {
        guarded(|| unsafe {
            logging::init();

            let build = cfstring_to_path(working_directory)?;
            let props = cfstring_to_path(properties_file)?;
            let vmopts = cfstring_to_path(vm_options_file)?;
            let app = cfstring_to_path(app_home)?;

            // Restore cwd from $PWD if `open`/Dock launched us with cwd=/
            restore_cwd_from_pwd();

            let main_args = c_argv_to_strings(argc, argv);
            launch_from_paths(build, props, vmopts, app, main_args)
        })
    }

    unsafe fn cfstring_to_path(ptr: *const c_void) -> Result<PathBuf> {
        if ptr.is_null() {
            anyhow::bail!("null CFStringRef");
        }
        let cf: CFString = unsafe {
            CFString::wrap_under_get_rule(ptr as CFStringRef)
        };
        Ok(PathBuf::from(cf.to_string()))
    }

    fn restore_cwd_from_pwd() {
        if let Ok(cwd) = env::current_dir() {
            if cwd != Path::new("/") {
                return;
            }
        }
        if let Ok(pwd) = env::var("PWD") {
            let _ = env::set_current_dir(&pwd);
        }
    }

    unsafe fn c_argv_to_strings(argc: c_int, argv: *mut *mut c_char) -> Vec<String> {
        if argv.is_null() || argc <= 0 {
            return Vec::new();
        }
        let mut out = Vec::with_capacity(argc as usize);
        for i in 1..argc as isize {
            let p = unsafe { *argv.offset(i) };
            if p.is_null() {
                continue;
            }
            let c = unsafe { CStr::from_ptr(p) };
            // Skip macOS Finder-injected -psn_* argument.
            if c.to_bytes().starts_with(b"-psn_") {
                continue;
            }
            if let Ok(s) = c.to_str() {
                out.push(s.to_string());
            }
        }
        out
    }
}

// ============================================================================
// Linux
// ============================================================================

#[cfg(target_os = "linux")]
mod linux {
    use super::*;
    use std::ffi::{CStr, c_char};

    #[unsafe(no_mangle)]
    pub unsafe extern "C" fn launchConsulo(
        argc: c_int,
        argv: *mut *mut c_char,
        working_directory: *const c_char,
        properties_file: *const c_char,
        vm_options_file: *const c_char,
        app_home: *const c_char,
    ) -> c_int {
        guarded(|| unsafe {
            logging::init();

            let build = cstr_to_path(working_directory)?;
            let props = cstr_to_path(properties_file)?;
            let vmopts = cstr_to_path(vm_options_file)?;
            let app = cstr_to_path(app_home)?;
            let main_args = c_argv_to_strings(argc, argv);

            launch_from_paths(build, props, vmopts, app, main_args)
        })
    }

    unsafe fn cstr_to_path(ptr: *const c_char) -> Result<PathBuf> {
        if ptr.is_null() {
            anyhow::bail!("null c_char* path");
        }
        let c = unsafe { CStr::from_ptr(ptr) };
        let s = c
            .to_str()
            .map_err(|e| anyhow::anyhow!("non-UTF-8 path: {e}"))?;
        Ok(PathBuf::from(s))
    }

    unsafe fn c_argv_to_strings(argc: c_int, argv: *mut *mut c_char) -> Vec<String> {
        if argv.is_null() || argc <= 0 {
            return Vec::new();
        }
        let mut out = Vec::with_capacity(argc as usize);
        for i in 1..argc as isize {
            let p = unsafe { *argv.offset(i) };
            if p.is_null() {
                continue;
            }
            let c = unsafe { CStr::from_ptr(p) };
            if let Ok(s) = c.to_str() {
                out.push(s.to_string());
            }
        }
        out
    }
}

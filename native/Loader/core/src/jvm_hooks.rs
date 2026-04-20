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
//! JVM native-side error capture via `vfprintf` and `abort` option hooks.
//!
//! Registered via `JavaVMOption { optionString: "vfprintf"/"abort", extraInfo: fn_ptr }`.
//! Ported from [XPlatLauncher java.rs:38-69].
//!
//! When `HOOK_MESSAGES` is `Some`, `vfprintf_hook` captures the formatted
//! string into the vector instead of writing to stderr. On `JNI_CreateJavaVM`
//! failure, the captured text becomes the error-dialog message.
//!
//! Windows-only because the real wins come from native-GUI mode where stderr
//! is nowhere visible. Unix `Console.app` / terminal capture stderr anyway.

#![cfg(target_os = "windows")]

use log::{debug, error};
use std::ffi::{CStr, CString, c_char, c_int, c_void};
use std::sync::Mutex;

/// When `Some(_)`, `vfprintf_hook` captures into the vector. When `None`,
/// it passes through to the real libc `vfprintf`.
pub static HOOK_MESSAGES: Mutex<Option<Vec<String>>> = Mutex::new(None);

pub fn start_capture() {
    if let Ok(mut g) = HOOK_MESSAGES.lock() {
        *g = Some(Vec::new());
    }
}

pub fn stop_capture() -> Vec<String> {
    if let Ok(mut g) = HOOK_MESSAGES.lock() {
        g.take().unwrap_or_default()
    } else {
        Vec::new()
    }
}

/// JVM calls this for internal printf-style output. When capture is active,
/// format into a buffer and stash the string; else pass through to libc.
pub extern "C" fn vfprintf_hook(
    fp: *const c_void,
    format: *const c_char,
    args: va_list::VaList<'_>,
) -> c_int {
    // MSVC's UCRT exposes `vfprintf` / `vsnprintf` as inline functions;
    // the extern symbols only exist in `legacy_stdio_definitions.lib`.
    // `legacy_stdio_definitions` is a tiny static lib that forwards to
    // the UCRT inlines — safe to link unconditionally on MSVC.
    #[cfg_attr(target_env = "msvc", link(name = "legacy_stdio_definitions"))]
    unsafe extern "C" {
        fn vfprintf(fp: *const c_void, format: *const c_char, args: va_list::VaList<'_>) -> c_int;
        fn vsnprintf(
            s: *mut c_char,
            n: usize,
            format: *const c_char,
            args: va_list::VaList<'_>,
        ) -> c_int;
    }

    let mut guard = match HOOK_MESSAGES.lock() {
        Ok(g) => g,
        Err(_) => return unsafe { vfprintf(fp, format, args) },
    };

    match guard.as_mut() {
        None => unsafe { vfprintf(fp, format, args) },
        Some(messages) => {
            let mut buf = [0u8; 4096];
            let len = unsafe {
                vsnprintf(
                    buf.as_mut_ptr() as *mut c_char,
                    buf.len(),
                    format,
                    args,
                )
            };
            let text = unsafe { CStr::from_ptr(buf.as_ptr() as *const c_char) }
                .to_string_lossy()
                .into_owned();
            debug!("[JVM vfprintf] {text:?}");
            messages.push(text);
            len
        }
    }
}

/// JVM calls `abort()` on internal fatal errors. We log and surface any
/// captured `vfprintf_hook` text so the user sees something actionable
/// instead of an invisible crash.
pub extern "C" fn abort_hook() {
    error!("[JVM] abort_hook fired");

    let text: String = match HOOK_MESSAGES.lock() {
        Ok(g) => g.as_ref().map(|v| v.join("")).unwrap_or_default(),
        Err(e) => {
            error!("[JVM] abort_hook: HOOK_MESSAGES.lock() failed: {e}");
            return;
        }
    };

    if !text.is_empty() {
        crate::ui::show_error(true, anyhow::format_err!("{text}"));
    }
}

/// Produce the two extra `JavaVMOption` entries (`abort` + `vfprintf`).
/// The returned `CString`s must outlive the JNI_CreateJavaVM call.
pub fn hook_options() -> (Vec<CString>, Vec<jni::sys::JavaVMOption>) {
    let opt_abort = CString::new("abort").expect("no NUL in 'abort'");
    let opt_vfprintf = CString::new("vfprintf").expect("no NUL in 'vfprintf'");

    let options = vec![
        jni::sys::JavaVMOption {
            optionString: opt_abort.as_ptr() as *mut c_char,
            extraInfo: abort_hook as *mut c_void,
        },
        jni::sys::JavaVMOption {
            optionString: opt_vfprintf.as_ptr() as *mut c_char,
            extraInfo: vfprintf_hook as *mut c_void,
        },
    ];

    (vec![opt_abort, opt_vfprintf], options)
}

cmake_minimum_required(VERSION 3.1)
project(MacFsNotifier C)

if (NOT APPLE)
    message(FATAL_ERROR "macOS only.")
endif ()

if(TARGET_X64)
    set(FILE_NAME "fsnotifier64")
    set(CMAKE_OSX_ARCHITECTURES "x86_64")
elseif(TARGET_ARM64)
    set(FILE_NAME "fsnotifier-aarch64")
    set(CMAKE_OSX_ARCHITECTURES "arm64")
else()
    message(FATAL_ERROR "Unsupported platform")
endif()

set(CMAKE_C_STANDARD 11)
set(CMAKE_OSX_DEPLOYMENT_TARGET 10.9)

add_compile_options("-fmodules" "-flto=full" "-Wall" "-Wextra" "-Wpedantic" "-Wno-newline-eof")

add_executable(${FILE_NAME} fsnotifier.c)
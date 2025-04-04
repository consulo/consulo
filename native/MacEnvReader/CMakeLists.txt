cmake_minimum_required(VERSION 3.5)
project(MacEnvReader C)

if (NOT APPLE)
    message(FATAL_ERROR "macOS only.")
endif ()

if(TARGET_X64)
  set(FILE_NAME "printenv64")
set(CMAKE_OSX_ARCHITECTURES "x86_64")
elseif(TARGET_ARM64)
  set(FILE_NAME "printenv-aarch64")
  set(CMAKE_OSX_ARCHITECTURES "arm64")
else()
    message(FATAL_ERROR "Unsupported platform")
endif()

set(CMAKE_C_STANDARD 11)
set(CMAKE_OSX_DEPLOYMENT_TARGET 10.9)

add_compile_options("-fno-objc-arc" "-flto=full" "-Wall" "-Wextra" "-Wpedantic")

add_executable(${FILE_NAME} printenv.c)
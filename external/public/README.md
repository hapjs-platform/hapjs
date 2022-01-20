ndk-build.py
===========================
Check the NDK configuration of all project.
**Enable this function by adding the parameter -PuseSpecifiedNdk=true on
gradle.**

## 1. Use v8 ndk toolchain
Use unified toolchain by adding `local.properties` in necessary
```
  # add by ndk_build.py
  ndk.dir=external/v8/third_party/android_tools/ndk/
```

## 2. Check APP_STL in Application.mk
Quickapp use `APP_STL=c++_shared` as default config.
Add `# SPECIAL` comment as a special case to exclude.

eg1. Default config
```
APP_STL=c++_shared
```

eg2. Special case
```
APP_STL=gnustl_static #SPECIAL
```

include
==============================
Add public include files

######################################################################################################
INSPECTOR_BASE_PATH := $(call my-dir)

LOCAL_PATH := $(INSPECTOR_BASE_PATH)

INSPECTOR_PATH:=./inspector
PUBLIC_INCLUDE_PATH = $(LOCAL_PATH)/../../../../../../../external/public/include

######################################################################################################

include $(CLEAR_VARS)

LOCAL_CXX_STL := libc++
LOCAL_MULTILIB:="both"

LOCAL_CPP_FEATURES:=rtti

LOCAL_CPP_EXTENSION := .cpp

LOCAL_SRC_FILES := $(INSPECTOR_PATH)/java_v8_inspector.cpp \
  $(INSPECTOR_PATH)/java_native_reflect.cpp \

LOCAL_C_INCLUDES := $(INSPECTOR_PATH)
LOCAL_C_INCLUDES += $(PUBLIC_INCLUDE_PATH)


LOCAL_CFLAGS := -std=c++14 -Wall -Wno-unused-function -Wno-unused-variable -O3 -funroll-loops -ftree-vectorize -ffast-math -fpermissive -fpic -U__STDINT_LIMITS -fpermissive

LOCAL_MODULE:=libinspector

LOCAL_LDLIBS = -llog -latomic


include $(BUILD_SHARED_LIBRARY)

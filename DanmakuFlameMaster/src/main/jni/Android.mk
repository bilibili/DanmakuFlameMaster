LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LIB_FOLDER:= ../obj/local/$(TARGET_ARCH_ABI)
LOCAL_MODULE    := libndkbitmap
LOCAL_SRC_FILES := $(LOCAL_LIB_FOLDER)/libndkbitmap.so

include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_SKIA_FOLDER     := $(LOCAL_PATH)/../../../../../platform_external_skia
LOCAL_SYS_CORE_FOLDER := $(LOCAL_PATH)/../../../../../platform_system_core

LOCAL_C_INCLUDES      := $(LOCAL_SYS_CORE_FOLDER)/include

LOCAL_C_INCLUDES      += $(LOCAL_SKIA_FOLDER)/include \
                         $(LOCAL_SKIA_FOLDER)/include/c \
                         $(LOCAL_SKIA_FOLDER)/include/config \
                         $(LOCAL_SKIA_FOLDER)/include/core \
                         $(LOCAL_SKIA_FOLDER)/include/device \
                         $(LOCAL_SKIA_FOLDER)/include/gpu \
                         $(LOCAL_SKIA_FOLDER)/include/gpu/gl \
                         $(LOCAL_SKIA_FOLDER)/include/xml \
                         $(LOCAL_SKIA_FOLDER)/include/utils \
                         $(LOCAL_SKIA_FOLDER)/include/views \
                         $(LOCAL_SKIA_FOLDER)/src/gpu/gl

LOCAL_SHARED_LIBRARYS := libndkbitmap

LOCAL_MODULE    := DFMACC
LOCAL_SRC_FILES := jni_entry.cpp skia_redirector_jni.cpp sk_stupid_renderer.cpp
LOCAL_CPPFLAGS  := -std=c++11 -march=armv7-a -DHAVE_LITTLE_ENDIAN
LOCAL_LDLIBS    := -L$(LOCAL_PATH)/../obj/local/$(TARGET_ARCH_ABI)/ -llog -lcutils.21 -lskia.21

include $(BUILD_SHARED_LIBRARY)
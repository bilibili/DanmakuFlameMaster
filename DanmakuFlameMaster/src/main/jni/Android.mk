LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LIB_FOLDER:= ../obj/local/$(TARGET_ARCH_ABI)
LOCAL_MODULE    := libndkbitmap
LOCAL_SRC_FILES := $(LOCAL_LIB_FOLDER)/libndkbitmap.so

include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARYS := libndkbitmap

LOCAL_MODULE    := DFMACC
LOCAL_SRC_FILES := jni_entry.cpp \
                   ndk_bitmap.cpp \
                   skia_redirector_jni.cpp \
                   version_utils.cpp \
                   sk_stupid_renderer_16.cpp \
                   sk_stupid_renderer_18.cpp \
                   sk_stupid_injector.cpp

LOCAL_CPPFLAGS  := -std=c++11 -march=armv7-a
LOCAL_LDLIBS    := -L$(LOCAL_PATH)/../obj/local/$(TARGET_ARCH_ABI)/ -llog -lGLESv2 #-lcutils.19 -lskia.19

include $(BUILD_SHARED_LIBRARY)
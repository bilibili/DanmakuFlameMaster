#ifndef _JNI_ENTRY_HPP
#define _JNI_ENTRY_HPP

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved);
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* jvm, void* reserved);

#ifdef __cplusplus
}
#endif

#endif // _JNI_ENTRY_HPP

#ifndef _SKIA_REDIRECTOR_JNI_HPP
#define _SKIA_REDIRECTOR_JNI_HPP

#include <jni.h>

int initSkiaRedirectorJni(JNIEnv* env);
int termSkiaRedirectorJni();

int registerSkiaRedirectorMethods(JNIEnv* env, const char* className);

#endif // _SKIA_REDIRECTOR_JNI_HPP

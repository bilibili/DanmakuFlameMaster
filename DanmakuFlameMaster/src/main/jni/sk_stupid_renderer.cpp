#include <pthread.h>
#include <android/log.h>
#include <SkSurface.h>
#include <GrContext.h>
#include <SkGpuDevice.h>
#include <GrGLUtil.h>
#include <GrGLInterface.h>
#include "sk_stupid_renderer.hpp"

SkStupidRenderer::SkStupidRenderer(void* nativeHandle) {
	__android_log_print(ANDROID_LOG_DEBUG, "SkStupidRenderer", "ctor");
	pthread_mutex_init(&this->mCanvasMutex, nullptr);
}

SkStupidRenderer::~SkStupidRenderer() {
	__android_log_print(ANDROID_LOG_DEBUG, "SkStupidRenderer", "dtor");
	if (mBackendType != kNone_BackEndType) {
		teardownBackend();
	}

	SkSafeUnref(mCurrentContext);
	SkSafeUnref(mCurrentInterface);
	SkSafeUnref(mCurrentRenderTarget);

	pthread_mutex_destroy(&this->mCanvasMutex);
}

bool SkStupidRenderer::isHardwareAccelerated() {
	return (mBackendType == kNativeGL_BackEndType);
}

bool SkStupidRenderer::setupBackend(SkBackEndTypes backendType, int width, int height, int msaaSampleCount) {
	__android_log_print(ANDROID_LOG_DEBUG, "SkStupidRenderer", "setupBackend");
	SkASSERT(mBackendType == kNone_BackEndType);

	mBackendType = kNone_BackEndType;

	switch (backendType) {
		case kNone_BackEndType: // fallthrough
		case kNativeGL_BackEndType:
			mBackendType = kNativeGL_BackEndType;
			break;
		default:
			SkASSERT(false);
			break;
	}

	mMSAASampleCount = msaaSampleCount;

	SkASSERT(mCurrentInterface == nullptr);
	SkAutoTUnref<const GrGLInterface> glInterface;
	switch (mBackendType) {
		case kNone_BackEndType:	// fallthrough
		case kNativeGL_BackEndType:
			glInterface.reset(GrGLCreateNativeInterface());
			break;
		default:
			SkASSERT(false);
			break;
	}

	mCurrentInterface = glInterface;

	SkASSERT(mCurrentContext == nullptr);
	mCurrentContext = GrContext::Create(kOpenGL_GrBackend, (GrBackendContext)mCurrentInterface);

	if (mCurrentContext == nullptr || mCurrentInterface == nullptr) {
		SkSafeUnref(mCurrentContext);
		SkSafeUnref(mCurrentInterface);
		mCurrentContext = nullptr;
		mCurrentInterface = nullptr;
		SkDebugf("SkStupidRenderer::setupBackend(): Fail to setup GrGLInterface/GrContext");
	}

	this->mWidth = width;
	this->mHeight = height;
	this->windowSizeChanged();

	return true;
}

bool SkStupidRenderer::teardownBackend() {
	if (mSurface) {
		mSurface->unref();
		mSurface = nullptr;
		mCanvas = nullptr;
	}

	if (mCurrentContext) {
		mCurrentContext->contextDestroyed();
		mCurrentContext->unref();
		mCurrentContext = nullptr;
	}

	SkSafeUnref(mCurrentInterface);
	mCurrentInterface = nullptr;

	SkSafeUnref(mCurrentRenderTarget);
	mCurrentRenderTarget = nullptr;

	mBackendType = kNone_BackEndType;
	return true;
}

void SkStupidRenderer::windowSizeChanged() {
	if (mCurrentContext) {
		GrBackendRenderTargetDesc desc;
		desc.fWidth = mWidth;
		desc.fHeight = mHeight;
		desc.fConfig = kSkia8888_GrPixelConfig;
		desc.fOrigin = kBottomLeft_GrSurfaceOrigin;
		desc.fSampleCnt = mMSAASampleCount;
		desc.fStencilBits = 8;

		GrGLint buffer;
		GR_GL_GetIntegerv(mCurrentInterface, GR_GL_FRAMEBUFFER_BINDING, &buffer);
		desc.fRenderTargetHandle = buffer;

		SkSafeUnref(mCurrentRenderTarget);
		mCurrentRenderTarget = mCurrentContext->wrapBackendRenderTarget(desc);

		if (mSurface) {
			mSurface->unref();
			//SkAutoTUnref<SkBaseDevice> gpuDevice(new SkGpuDevice(mCurrentContext, mCurrentRenderTarget));   // 5.0
			//mCanvas = new SkCanvas(gpuDevice);
			mSurface = SkSurface::NewRenderTargetDirect(mCurrentContext, mCurrentRenderTarget);  // 4.4.x
			mCanvas = mSurface->getCanvas();
		}
	}
	__android_log_print(ANDROID_LOG_DEBUG, "SkStupidRenderer", "windowSizeChanged successed!");
}

void SkStupidRenderer::updateSize(int width, int height) {
	mWidth = width;
	mHeight = height;
	this->onSizeChange();
}

void SkStupidRenderer::onSizeChange() {
	this->windowSizeChanged();
}

SkCanvas* SkStupidRenderer::lockCanvas() {
	//pthread_mutex_lock(&this->mCanvasMutex);
	if (mSurface == nullptr) {
		if (mCurrentContext) {__android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "attempt to NewRenderTargetDirect");
			//SkAutoTUnref<SkBaseDevice> gpuDevice(new SkGpuDevice(mCurrentContext, mCurrentRenderTarget));   // 5.0
			//mCanvas = new SkCanvas(gpuDevice);
			mSurface = SkSurface::NewRenderTargetDirect(mCurrentContext, mCurrentRenderTarget);   // 4.4.x
			__android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "NewRenderTargetDirect succeed!");
		}
	}
	if (mCanvas == nullptr) {
		mCanvas = mSurface->getCanvas();
	}
	return mCanvas;
}

void SkStupidRenderer::unlockCanvasAndPost(SkCanvas* canvas) {
	if (mCurrentContext) {
		mCurrentContext->flush();

		if (!isHardwareAccelerated()) {
			SkDebugf("SkStupidRenderer::unlockCanvasAndPost(): surface is not a hardware-layer");
		}
	}
	//pthread_mutex_unlock(&this->mCanvasMutex);
}

#ifndef _SKIA_REDIRECTOR_STUPID_RENDERER_HPP
#define _SKIA_REDIRECTOR_STUPID_RENDERER_HPP

#include <jni.h>
#include <pthread.h>

class SkSurface; class SkCanvas; class GrContext; struct GrGLInterface; class GrRenderTarget;

enum SkBackEndTypes {
	kNone_BackEndType,
	kNativeGL_BackEndType
};

class SkStupidRenderer {
public:
	explicit SkStupidRenderer(void* nativeHandle);

	explicit SkStupidRenderer(SkStupidRenderer&) = delete;

	~SkStupidRenderer();

	bool isHardwareAccelerated();

	bool setupBackend(SkBackEndTypes backendType, int width, int height, int msaaSampleCount);

	bool teardownBackend();

	void updateSize(int width, int height);

	SkCanvas* lockCanvas();

	void unlockCanvasAndPost(SkCanvas* canvas);
public:
	jobject javaCanvas = nullptr; // temp workaround
protected:
	void onSizeChange();
private:
	void windowSizeChanged();
private:
	int mWidth = 0, mHeight = 0;
	int mMSAASampleCount = 0;
	SkCanvas* mCanvas = nullptr;
	SkSurface* mSurface = nullptr;
	pthread_mutex_t mCanvasMutex;
	SkBackEndTypes mBackendType = kNone_BackEndType;
	GrContext* mCurrentContext = nullptr;
	const GrGLInterface* mCurrentInterface = nullptr;
	GrRenderTarget* mCurrentRenderTarget = nullptr;
};

#endif // _SKIA_REDIRECTOR_STUPID_RENDERER_HPP

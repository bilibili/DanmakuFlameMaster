DanmakuFlameMaster(Optimize)
==================

[原始仓库和文档](https://github.com/bilibili/DanmakuFlameMaster)

### 仓库背景
&emsp;&emsp;之前在熊猫直播(已破产😢)工作，了解过一些弹幕库的内容(熊猫直播用的就是B站的开源弹幕库)，当时有点兴趣就看源码，想着能不能优化一下(B站好像不维护了),所以有了这个库。（这个仓库代码没有在线上验证过，仅当学习参考）
### 修改点
  - **去除CacheManagingDrawTask清除弹幕缓存等待30毫秒**<br/>
      原因:在弹幕量比较大时，弹幕移除屏幕外需要释放cache(Bitmap),清除每一cache会等待30毫秒，导致Cache线程不能快速构建没有显示的cache，从而导致UI线程直接绘制,造成卡顿.
  	```groovy
		synchronized (mDrawingNotify) {        			
            try {
                mDrawingNotify.wait(30);
         	} catch (InterruptedException e) {
                e.printStackTrace();
                return ACTION_BREAK;
            }
		}
	```
  - **强制在子线程中构建cache**<br/>
    &emsp;&emsp;弹幕库默认的实现是:当cache线程在UI线程需要绘制某一个弹幕时还没有准备好对应的cache，则会在UI线程构建cache并绘制到弹幕View上直接绘制是一个比较耗时的操作(可能需要1-10ms),这样的弹幕越多，造成主线程的卡顿越明显.<br/>
    &emsp;&emsp;此修改可以选择强制在弹幕计算需要绘制的弹幕(子线程)时判断cache状态，如果没有则直接在改线程触发构建cache,构建完成后才会将该弹幕交给UI线程渲染。<br/>
    &emsp;&emsp;此方案有一个弊端，就是当弹幕密度非常大时会造成弹幕拥堵，方案会自动将弹幕延期绘制，保证连续性.<br/>
    &emsp;&emsp;延期绘制是可选择性开启，默认关闭.
    
  - **替换canvas绘制到OpenGL绘制**<br/>
    &emsp;&emsp;弹幕库使用cache线程计算cache(Bitmap)，UI线程使用canvas绘制bitmap实现，虽然绘制bitmap非常快，但有两点依然存在的弊端.<br/>
     - 依然需要耗费UI线程的计算力，密度大时即使全部命中cache，也可能造成卡顿.
     - cache将在整个弹幕可见期间完全处于内存中，造成JVM内存压力大(粗略计算50条/s时内存占用在150MB+)。

    &emsp;&emsp;交给OpenGl渲染后，虽然依然需要构建cache,但当cache构建后就可直接映射到纹理，后面该cache块就可以复用了，java内存降低非常大.<br/>
    &emsp;&emsp;同时，渲染直接在opengl线程渲染，使用VSYNC触发一次绘制，弹幕在交给弹幕库后就完全不用UI线程执行任何操作了.(该渲染方式会新增两条线程,一条是gl线程,一条是纹理线程(与gl线程共享glcontext))
  
### 说明
&emsp;&emsp;启用OpenGL渲染，需要将DanmakuView替换成DanmakuGLSurfaceView，同时需要设置Context的mDisplayer
```groovy
    if (mDanmakuType == TYPE_DANMAKU_GL_VIEW) {
            mNormalDanmakuContext.mDisplayer = new AndroidGLDisplayer(mNormalDanmakuContext);
    }
 ```
&emsp;&emsp;如果使用OpenGL渲染，先前使用View.setAlpha设置弹幕的透明度，但SurfaceView不支持改操作，OpenGL渲染支持在渲染时设置透明度，复写了SurfaceView.setAlpha方法实现，所以依然可以通过这个方法设置透明度.<br/>
&emsp;&emsp;如果需要开启强制子线程构建cache时延期绘制,可以通过
```groovy
    mContext.cachingPolicy.mAllowDelayInCacheModel=true;
 ```
### benchmark
 &emsp;&emsp;仅仅经过简单自测，在同样的配置(小米8)和弹幕速率下:<br/>
 - 原始官方版本开到50条/s时大约每隔20s会出现几秒严重的卡顿。
 - 原始官方版本开到100条/s时大约每隔20s会出现几秒严重的卡顿,但卡顿时间和卡顿效果优于官方。
 - 使用OpenGL绘制,100条/s能一直保持流畅运行弹幕。
 - jvm 内存暂用更低(具体没测)
DanmakuFlameMaster
==================

android上开源弹幕解析绘制引擎项目。[![Build Status](https://travis-ci.org/Bilibili/DanmakuFlameMaster.png?branch=master)](https://travis-ci.org/Bilibili/DanmakuFlameMaster)

### DFM Inside: 
[![bili](https://raw.github.com/ctiao/ctiao.github.io/master/images/apps/bili.png?raw=true)](https://play.google.com/store/apps/details?id=tv.danmaku.bili)

- libndkbitmap.so(ndk)源码：https://github.com/Bilibili/NativeBitmapFactory
- 开发交流群：314468823 (加入请注明DFM开发交流)

### Features

- 使用多种方式(View/SurfaceView/TextureView)实现高效绘制

- B站xml弹幕格式解析

- 基础弹幕精确还原绘制

- 支持mode7特殊弹幕

- 多核机型优化，高效的预缓存机制

- 支持多种显示效果选项实时切换

- 实时弹幕显示支持

- 换行弹幕支持/运动弹幕支持

- 支持自定义字体

- 支持多种弹幕参数设置

- 支持多种方式的弹幕屏蔽

### TODO:

- 继续精确/稳定绘帧周期

- 增加OpenGL ES绘制方式

- 改进缓存策略和效率


### Download
Download the [latest version][1] or grab via Maven:

```xml
<dependency>
  <groupId>com.github.ctiao</groupId>
  <artifactId>dfm</artifactId>
  <version>0.4.8</version>
</dependency>
```

or Gradle:
```groovy
repositories {
    jcenter()
}

dependencies {
    compile 'com.github.ctiao:DanmakuFlameMaster:0.4.8'
}
```
Snapshots of the development version are available in [Sonatype's snapshots repository][2].


### License
    Copyright (C) 2013-2015 Chen Hui <calmer91@gmail.com>
    Licensed under the Apache License, Version 2.0 (the "License");


[1]:https://oss.sonatype.org/#nexus-search;gav~com.github.ctiao~dfm~~~
[2]:https://oss.sonatype.org/content/repositories/snapshots/

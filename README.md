DanmakuFlameMaster
==================

android上开源弹幕解析绘制引擎项目。[![Build Status](https://travis-ci.org/ctiao/DanmakuFlameMaster.png?branch=master)](https://travis-ci.org/ctiao/DanmakuFlameMaster)

### DFM Inside: 
[![hacfun](https://raw.github.com/ctiao/ctiao.github.io/master/images/apps/hacfun.png?raw=true)](https://play.google.com/store/apps/details?id=tv.ac.fun)
[![acfun](https://raw.github.com/ctiao/ctiao.github.io/master/images/apps/acfun.png?raw=true)](https://play.google.com/store/apps/details?id=tv.acfundanmaku.video)
[![bili](https://raw.github.com/ctiao/ctiao.github.io/master/images/apps/bili.png?raw=true)](https://play.google.com/store/apps/details?id=tv.danmaku.bili)

- AcFun视频民间版 https://play.google.com/store/apps/details?id=tv.ac.fun
- AcFun视频官方版 https://play.google.com/store/apps/details?id=tv.acfundanmaku.video
- 哔哩哔哩动画 https://play.google.com/store/apps/details?id=tv.danmaku.bili
- 斗鱼直播客户端 http://www.douyu.tv/client
- 猎豹浏览器 https://play.google.com/store/apps/details?id=com.ijinshan.browser
- 小米电视/盒子视频弹幕
- Ac动画 http://www.acfun.tv/app/
- 网易新闻客户端 http://www.163.com/newsapp/

以上客户端均使用DFM弹幕引擎,实际效果可安装以上app参看。

### Features

- 使用SurfaceView高效绘制

- A站json弹幕格式解析

- B站xml弹幕格式解析

- 基础弹幕精确还原绘制

- 支持特殊弹幕

- 多核机型优化，可选独立线程缓存进行绘制

- 支持多种绘制选项动态切换

- 支持实时弹幕显示

- 换行弹幕支持/运动弹幕支持

- 支持自定义字体

- 支持多种参数设置

- 支持多种方式的弹幕屏蔽

### TODO:

- 计算和绘制线程分离

- 减少cpu占用

- jni代码优化

#### Gradle
  ```groovy
    dependencies {
      compile 'me.neavo:danmakuflamemaster:x.x.x'
    }
  ```  

### Version
  * x.x.x = version, or set '+' for lastest

### License
    Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
    Licensed under the Apache License, Version 2.0 (the "License");

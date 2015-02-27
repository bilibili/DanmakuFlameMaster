DanmakuFlameMaster
==================

android上开源弹幕解析绘制引擎项目。[![Build Status](https://travis-ci.org/ctiao/DanmakuFlameMaster.png?branch=master)](https://travis-ci.org/ctiao/DanmakuFlameMaster)

### DFM Inside: 
[![hacfun](https://raw.github.com/ctiao/ctiao.github.io/master/images/apps/hacfun.png?raw=true)](http://www.coolapk.com/apk/tv.ac.fun)
[![acfun](https://raw.github.com/ctiao/ctiao.github.io/master/images/apps/acfun.png?raw=true)](http://www.coolapk.com/apk/tv.acfundanmaku.video)
[![bili](https://raw.github.com/ctiao/ctiao.github.io/master/images/apps/bili.png?raw=true)](https://play.google.com/store/apps/details?id=tv.danmaku.bili)

- 土豆客户端 http://mobile.tudou.com
- 优酷客户端 http://mobile.youku.com/index/wireless
- 网易新闻客户端 http://www.163.com/newsapp
- 猎豹浏览器 https://play.google.com/store/apps/details?id=com.ijinshan.browser
- 小米电视/盒子视频弹幕
- 被窝 http://mobile.beiwo.ac
- MissEvan http://www.missevan.cn/app
- 斗鱼直播客户端 http://www.douyutv.com/client
- AcFun视频民间版 https://github.com/yrom/acfunm
- 爱稀饭精选（AcFun主站客户端） http://www.acfun.tv/app
- 爱稀饭动画（AcFun动画客户端） http://www.acfun.tv/app
- 哔哩哔哩动画(B站) https://play.google.com/store/apps/details?id=tv.danmaku.bili
- 吐槽弹幕网(C站) http://www.tucao.cc/app/

以上客户端均使用DFM弹幕引擎,实际效果可安装以上app参看。

### Features

- 使用多种方式(View/SurfaceView/TextureView)实现高效绘制

- A站json弹幕格式解析

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


### Gradle
  ```groovy
    dependencies {
      compile 'me.neavo:danmakuflamemaster:x.x.x'
    }
  ```  

### Version
  * x.x.x = version, or set '+' for lastest（0.2.3）

### License
    Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
    Licensed under the Apache License, Version 2.0 (the "License");

DanmakuFlameMaster
==================

android上的弹幕转换、渲染输出源码


### Features

- 使用SurfaceView高效绘制

- A站json弹幕格式解析

- B站xml弹幕格式解析

- 基础弹幕精确还原绘制

- 多核机型优化，可选独立线程缓存进行绘制

- 支持多种绘制选项动态切换


### TODO:

- 从容器读取弹幕子集的效率优化 (完成)

- 自定义字体/字体描边/抗锯齿 (完成，等待分支合并)

- 换行弹幕支持/运动弹幕支持 (完成度：50%)

- 减少浮点运算

- 提供设置参数

- 支持进度跳转/多线程进度同步/支持线程绘制退出重启

- 支持实时弹幕显示


### License
    Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
    Licensed under the Apache License, Version 2.0 (the "License");
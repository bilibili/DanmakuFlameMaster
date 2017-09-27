/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.danmaku.model;

public class SpecialDanmaku extends BaseDanmaku {

    public static class ScaleFactor {
        int flag = 0;
        float scaleX;
        float scaleY;
        int width;
        int height;

        public ScaleFactor(int width, int height, float scaleX, float scaleY) {
            update(width, height, scaleX, scaleY);
        }

        public void update(int width, int height, float scaleX, float scaleY) {
            if (Float.compare(this.scaleX, scaleX) != 0 || Float.compare(this.scaleY, scaleY) != 0) {
                flag++;
            }
            this.width = width;
            this.height = height;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }

        public boolean isUpdated(int flag, int width, int height) {
            return this.flag != flag && (this.width != width || this.height != height);
        }
    }

    private class Point {
        float x, y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getDistance(Point p) {
            float _x = Math.abs(this.x - p.x);
            float _y = Math.abs(this.y - p.y);
            return (float) Math.sqrt(_x * _x + _y * _y);
        }
    }

    public class LinePath {
        Point pBegin, pEnd;
        public long duration, beginTime, endTime;
        float delatX, deltaY;

        public void setPoints(Point pBegin, Point pEnd) {
            this.pBegin = pBegin;
            this.pEnd = pEnd;
            this.delatX = pEnd.x - pBegin.x;
            this.deltaY = pEnd.y - pBegin.y;
        }

        public float getDistance() {
            return pEnd.getDistance(pBegin);
        }

        public float[] getBeginPoint() {
            return new float[]{
                    pBegin.x, pBegin.y
            };
        }

        public float[] getEndPoint() {
            return new float[]{
                    pEnd.x, pEnd.y
            };
        }

    }

    public float beginX, beginY;

    public float endX, endY;

    public float deltaX, deltaY;

    public long translationDuration;

    public long translationStartDelay;

    private ScaleFactor mScaleFactor;

    private int mScaleFactorChangedFlag;

    private int mCurrentWidth = 0;

    private int mCurrentHeight = 0;

    /**
     * Linear.easeIn or Quadratic.easeOut
     */
    public boolean isQuadraticEaseOut = false;

    public int beginAlpha;

    public int endAlpha;

    public int deltaAlpha;

    public long alphaDuration;

    public float rotateX, rotateZ;

    public float pivotX, pivotY;

    private float[] currStateValues = new float[4];

    public LinePath[] linePaths;

    @Override
    public void measure(IDisplayer displayer, boolean fromWorkerThread) {
        super.measure(displayer, fromWorkerThread);
        if (mCurrentWidth == 0 || mCurrentHeight == 0) {
            mCurrentWidth = displayer.getWidth();
            mCurrentHeight = displayer.getHeight();
        }
    }

    @Override
    public void layout(IDisplayer displayer, float x, float y) {
        getRectAtTime(displayer, mTimer.currMillisecond);
    }

    @Override
    public float[] getRectAtTime(IDisplayer displayer, long currTime) {

        if (!isMeasured())
            return null;

        if (mScaleFactor.isUpdated(this.mScaleFactorChangedFlag, mCurrentWidth, mCurrentHeight)) {
            float scaleX = mScaleFactor.scaleX;
            float scaleY = mScaleFactor.scaleY;
            setTranslationData(beginX * scaleX, beginY * scaleY, endX * scaleX, endY * scaleY, translationDuration, translationStartDelay);
            if (linePaths != null && linePaths.length > 0) {
                int length = linePaths.length;
                float[][] points = new float[length + 1][2];
                for (int j = 0; j < length; j++) {
                    points[j] = linePaths[j].getBeginPoint();
                    points[j + 1] = linePaths[j].getEndPoint();
                }
                for (int i = 0; i < points.length; i++) {
                    points[i][0] *= scaleX;
                    points[i][1] *= scaleY;
                }
                setLinePathData(points);
            }
            this.mScaleFactorChangedFlag = mScaleFactor.flag;
            this.mCurrentWidth = mScaleFactor.width;
            this.mCurrentHeight = mScaleFactor.height;
        }

        long deltaTime = currTime - getActualTime();

        // caculate alpha
        if (alphaDuration > 0 && deltaAlpha != 0) {
            if (deltaTime >= alphaDuration) {
                alpha = endAlpha;
            } else {
                float alphaProgress = deltaTime / (float) alphaDuration;
                int vectorAlpha = (int) (deltaAlpha * alphaProgress);
                alpha = beginAlpha + vectorAlpha;
            }
        }

        // caculate x y
        float currX = beginX;
        float currY = beginY;
        long dtime = deltaTime - translationStartDelay;
        if (translationDuration > 0 && dtime >= 0 && dtime <= translationDuration) {
            float tranalationProgress = 0f;
            if (linePaths != null) {
                LinePath currentLinePath = null;
                for (LinePath line : linePaths) {
                    if (dtime >= line.beginTime && dtime < line.endTime) {
                        currentLinePath = line;
                        break;
                    } else {
                        currX = line.pEnd.x;
                        currY = line.pEnd.y;
                    }
                }
                if (currentLinePath != null) {
                    float deltaX = currentLinePath.delatX;
                    float deltaY = currentLinePath.deltaY;
                    tranalationProgress = (deltaTime - currentLinePath.beginTime) / (float) currentLinePath.duration;
                    float beginX = currentLinePath.pBegin.x;
                    float beginY = currentLinePath.pBegin.y;
                    if (deltaX != 0) {
                        float vectorX = deltaX * tranalationProgress;
                        currX = beginX + vectorX;
                    }
                    if (deltaY != 0) {
                        float vectorY = deltaY * tranalationProgress;
                        currY = beginY + vectorY;
                    }
                }
            } else {
                tranalationProgress = isQuadraticEaseOut ? getQuadEaseOutProgress(dtime, translationDuration) : dtime / (float) translationDuration;
                if (deltaX != 0) {
                    float vectorX = deltaX * tranalationProgress;
                    currX = beginX + vectorX;
                }
                if (deltaY != 0) {
                    float vectorY = deltaY * tranalationProgress;
                    currY = beginY + vectorY;
                }
            }
        } else if (dtime > translationDuration) {
            currX = endX;
            currY = endY;
        }

        currStateValues[0] = currX;
        currStateValues[1] = currY;
        currStateValues[2] = currX + paintWidth;
        currStateValues[3] = currY + paintHeight;

        this.setVisibility(!isOutside());

        return currStateValues;
    }

    private final static float getQuadEaseOutProgress(long ctime, long duration) {
//            Math.easeOutQuad = function (t, b, c, d) {
//                t /= d;
//                return -c * t*(t-2) + b;
//            };
        float t = ctime;
//        float b = 0f;
        float c = 1.0f;
        float d = duration;
        return -c * (t /= d) * (t - 2); // + b;
    }

    @Override
    public float getLeft() {
        return currStateValues[0];
    }

    @Override
    public float getTop() {
        return currStateValues[1];
    }

    @Override
    public float getRight() {
        return currStateValues[2];
    }

    @Override
    public float getBottom() {
        return currStateValues[3];
    }

    @Override
    public int getType() {
        return TYPE_SPECIAL;
    }

    public void setTranslationData(float beginX, float beginY, float endX, float endY,
                                   long translationDuration, long translationStartDelay) {
        this.beginX = beginX;
        this.beginY = beginY;
        this.endX = endX;
        this.endY = endY;
        this.deltaX = endX - beginX;
        this.deltaY = endY - beginY;
        this.translationDuration = translationDuration;
        this.translationStartDelay = translationStartDelay;
    }

    public void setAlphaData(int beginAlpha, int endAlpha, long alphaDuration) {
        this.beginAlpha = beginAlpha;
        this.endAlpha = endAlpha;
        this.deltaAlpha = endAlpha - beginAlpha;
        this.alphaDuration = alphaDuration;
        if (beginAlpha != AlphaValue.MAX) {
            alpha = beginAlpha;
        }
    }

    public void setLinePathData(float[][] points) {
        if (points != null) {
            int length = points.length;
            this.beginX = points[0][0];
            this.beginY = points[0][1];
            this.endX = points[length - 1][0];
            this.endY = points[length - 1][1];
            if (points.length > 1) {
                linePaths = new LinePath[points.length - 1];
                for (int i = 0; i < linePaths.length; i++) {
                    linePaths[i] = new LinePath();
                    linePaths[i].setPoints(new Point(points[i][0], points[i][1]), new Point(
                            points[i + 1][0], points[i + 1][1]));
                }
                float totalDistance = 0;
                for (LinePath line : linePaths) {
                    totalDistance += line.getDistance();
                }
                LinePath lastLine = null;
                for (LinePath line : linePaths) {
                    line.duration = (long) ((line.getDistance() / totalDistance) * translationDuration);
                    line.beginTime = (lastLine == null ? 0 : lastLine.endTime);
                    line.endTime = line.beginTime + line.duration;
                    lastLine = line;
                }

            }
        }
    }

    public void setScaleFactor(ScaleFactor scaleFactor) {
        this.mScaleFactor = scaleFactor;
        this.mScaleFactorChangedFlag = scaleFactor.flag;
    }

}

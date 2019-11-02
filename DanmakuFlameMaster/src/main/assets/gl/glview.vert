attribute vec4 vPosition;
attribute vec2 vCoordinate;
precision highp float;

uniform mat4 vProject;
uniform mat4 vView;
uniform mat4 vModel;

varying vec2 aCoordinate;

void main()
{
    gl_Position=vProject*vView*vModel*vPosition;
    aCoordinate=vCoordinate;
}
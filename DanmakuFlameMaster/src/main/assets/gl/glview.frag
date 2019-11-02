precision mediump float;
uniform sampler2D vTexture;
uniform float alpha;
varying vec2 aCoordinate;

void main()
{
    gl_FragColor = texture2D(vTexture,aCoordinate);
    gl_FragColor.a = gl_FragColor.a * alpha;
}
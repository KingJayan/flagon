package dev.flagon.lwjgl.internal;

/** the calls a display list can hold */
public final class Ops {

    public static final int CALL_LIST = 1;
    public static final int MATRIX_MODE = 2;
    public static final int LOAD_IDENTITY = 3;
    public static final int PUSH_MATRIX = 4;
    public static final int POP_MATRIX = 5;
    public static final int TRANSLATE = 6;
    public static final int ROTATE = 7;
    public static final int SCALE = 8;
    public static final int ORTHO = 9;
    public static final int FRUSTUM = 10;
    public static final int PUSH_ATTRIB = 11;
    public static final int POP_ATTRIB = 12;
    public static final int COLOR = 13;
    public static final int NORMAL = 14;
    public static final int ALPHA_FUNC = 15;
    public static final int TEXTURE_ENV = 16;
    public static final int MODEL_AMBIENT = 17;
    public static final int SHININESS = 18;
    public static final int FOG_COLOR = 19;
    public static final int DRAW_BLOCK = 20;
    public static final int LIGHT_SCALAR = 21;
    public static final int LIGHT_VECTOR = 22;
    public static final int MATERIAL = 23;
    public static final int FOG = 24;
    public static final int CLIENT_ACTIVE_TEXTURE = 25;
    public static final int ACTIVE_TEXTURE = 26;
    public static final int BLEND_FUNC = 27;
    public static final int BLEND_FUNC_SEPARATE = 28;
    public static final int BLEND_EQUATION = 29;
    public static final int BLEND_EQUATION_SEPARATE = 30;
    public static final int BLEND_COLOR = 31;
    public static final int CLEAR_COLOR = 32;
    public static final int CLEAR_DEPTH = 33;
    public static final int CLEAR = 34;
    public static final int VIEWPORT = 35;
    public static final int SCISSOR = 36;
    public static final int CULLED_FACES = 37;
    public static final int FRONT_FACE = 38;
    public static final int COLOR_MASK = 39;
    public static final int DEPTH_WRITE = 40;
    public static final int DEPTH_FUNC = 41;
    public static final int CAPABILITY = 42;
    public static final int STENCIL_FUNC = 43;
    public static final int STENCIL_OP = 44;
    public static final int STENCIL_MASK = 45;
    public static final int CLEAR_STENCIL = 46;
    public static final int POLYGON_OFFSET = 47;
    public static final int LINE_WIDTH = 48;
    public static final int POINT_SIZE = 49;
    public static final int LOAD_MATRIX = 50;
    public static final int MULT_MATRIX = 51;
    public static final int TEX_COORD = 52;
    public static final int BIND_TEXTURE = 53;

    private Ops() {
    }
}

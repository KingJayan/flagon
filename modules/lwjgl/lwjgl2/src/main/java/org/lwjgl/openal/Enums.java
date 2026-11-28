package org.lwjgl.openal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** the openal enum names, as alGetEnumValue and alcGetEnumValue look them up */
final class Enums {

    private Enums() {
    }

    /** zero for a name openal does not have, as the native call answers */
    static int valueOf(String name) {
        return switch (name) {
            case "AL_INVALID" -> -1;
            case "ALC_TRUE" -> 0x1;
            case "AL_TRUE" -> 0x1;
            case "AL_SOURCE_ABSOLUTE" -> 0x201;
            case "AL_SOURCE_RELATIVE" -> 0x202;
            case "ALC_CAPTURE_DEVICE_SPECIFIER" -> 0x310;
            case "ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER" -> 0x311;
            case "ALC_CAPTURE_SAMPLES" -> 0x312;
            case "ALC_MAJOR_VERSION" -> 0x1000;
            case "ALC_MINOR_VERSION" -> 0x1001;
            case "AL_CONE_INNER_ANGLE" -> 0x1001;
            case "ALC_ATTRIBUTES_SIZE" -> 0x1002;
            case "AL_CONE_OUTER_ANGLE" -> 0x1002;
            case "ALC_ALL_ATTRIBUTES" -> 0x1003;
            case "AL_PITCH" -> 0x1003;
            case "ALC_DEFAULT_DEVICE_SPECIFIER" -> 0x1004;
            case "AL_POSITION" -> 0x1004;
            case "ALC_DEVICE_SPECIFIER" -> 0x1005;
            case "AL_DIRECTION" -> 0x1005;
            case "ALC_EXTENSIONS" -> 0x1006;
            case "AL_VELOCITY" -> 0x1006;
            case "ALC_FREQUENCY" -> 0x1007;
            case "AL_LOOPING" -> 0x1007;
            case "ALC_REFRESH" -> 0x1008;
            case "ALC_SYNC" -> 0x1009;
            case "AL_BUFFER" -> 0x1009;
            case "AL_GAIN" -> 0x100a;
            case "AL_MIN_GAIN" -> 0x100d;
            case "AL_MAX_GAIN" -> 0x100e;
            case "AL_ORIENTATION" -> 0x100f;
            case "ALC_MONO_SOURCES" -> 0x1010;
            case "AL_SOURCE_STATE" -> 0x1010;
            case "ALC_STEREO_SOURCES" -> 0x1011;
            case "AL_INITIAL" -> 0x1011;
            case "ALC_DEFAULT_ALL_DEVICES_SPECIFIER" -> 0x1012;
            case "AL_PLAYING" -> 0x1012;
            case "ALC_ALL_DEVICES_SPECIFIER" -> 0x1013;
            case "AL_PAUSED" -> 0x1013;
            case "AL_STOPPED" -> 0x1014;
            case "AL_BUFFERS_QUEUED" -> 0x1015;
            case "AL_BUFFERS_PROCESSED" -> 0x1016;
            case "AL_REFERENCE_DISTANCE" -> 0x1020;
            case "AL_ROLLOFF_FACTOR" -> 0x1021;
            case "AL_CONE_OUTER_GAIN" -> 0x1022;
            case "AL_MAX_DISTANCE" -> 0x1023;
            case "AL_SEC_OFFSET" -> 0x1024;
            case "AL_SAMPLE_OFFSET" -> 0x1025;
            case "AL_BYTE_OFFSET" -> 0x1026;
            case "AL_SOURCE_TYPE" -> 0x1027;
            case "AL_STATIC" -> 0x1028;
            case "AL_STREAMING" -> 0x1029;
            case "AL_UNDETERMINED" -> 0x1030;
            case "AL_FORMAT_MONO8" -> 0x1100;
            case "AL_FORMAT_MONO16" -> 0x1101;
            case "AL_FORMAT_STEREO8" -> 0x1102;
            case "AL_FORMAT_STEREO16" -> 0x1103;
            case "AL_FREQUENCY" -> 0x2001;
            case "AL_BITS" -> 0x2002;
            case "AL_CHANNELS" -> 0x2003;
            case "AL_SIZE" -> 0x2004;
            case "AL_DATA" -> 0x2005;
            case "AL_UNUSED" -> 0x2010;
            case "AL_PENDING" -> 0x2011;
            case "AL_PROCESSED" -> 0x2012;
            case "AL_CHANNEL_MASK" -> 0x3000;
            case "ALC_INVALID_DEVICE" -> 0xa001;
            case "AL_INVALID_NAME" -> 0xa001;
            case "ALC_INVALID_CONTEXT" -> 0xa002;
            case "AL_INVALID_ENUM" -> 0xa002;
            case "ALC_INVALID_ENUM" -> 0xa003;
            case "AL_INVALID_VALUE" -> 0xa003;
            case "ALC_INVALID_VALUE" -> 0xa004;
            case "AL_ILLEGAL_COMMAND" -> 0xa004;
            case "AL_INVALID_OPERATION" -> 0xa004;
            case "ALC_OUT_OF_MEMORY" -> 0xa005;
            case "AL_OUT_OF_MEMORY" -> 0xa005;
            case "AL_VENDOR" -> 0xb001;
            case "AL_VERSION" -> 0xb002;
            case "AL_RENDERER" -> 0xb003;
            case "AL_EXTENSIONS" -> 0xb004;
            case "AL_DOPPLER_FACTOR" -> 0xc000;
            case "AL_DOPPLER_VELOCITY" -> 0xc001;
            case "AL_SPEED_OF_SOUND" -> 0xc003;
            case "AL_DISTANCE_MODEL" -> 0xd000;
            case "AL_INVERSE_DISTANCE" -> 0xd001;
            case "AL_INVERSE_DISTANCE_CLAMPED" -> 0xd002;
            case "AL_LINEAR_DISTANCE" -> 0xd003;
            case "AL_LINEAR_DISTANCE_CLAMPED" -> 0xd004;
            case "AL_EXPONENT_DISTANCE" -> 0xd005;
            case "AL_EXPONENT_DISTANCE_CLAMPED" -> 0xd006;
            case "AL_FORMAT_VORBIS_EXT" -> 0x10003;
            default -> 0;
        };
    }

    /** a nul-terminated name from the buffer's position */
    static String text(ByteBuffer name) {
        int end = name.position();
        while (end < name.limit() && name.get(end) != 0) {
            end++;
        }
        byte[] bytes = new byte[end - name.position()];
        for (int at = 0; at < bytes.length; at++) {
            bytes[at] = name.get(name.position() + at);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

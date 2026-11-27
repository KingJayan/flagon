package org.lwjgl.openal;

/** what a device reports, as {@link ALC#createCapabilities} makes it: openal 1.1 and no extension */
public final class ALCCapabilities {

    public final boolean OpenALC10;
    public final boolean OpenALC11;
    public final boolean OpenALC_SOFT_loopback_bformat;
    public final boolean ALC_ENUMERATE_ALL_EXT;
    public final boolean ALC_ENUMERATION_EXT;
    public final boolean ALC_EXT_CAPTURE;
    public final boolean ALC_EXT_DEDICATED;
    public final boolean ALC_EXT_DEFAULT_FILTER_ORDER;
    public final boolean ALC_EXT_disconnect;
    public final boolean ALC_EXT_EFX;
    public final boolean ALC_EXT_thread_local_context;
    public final boolean ALC_LOKI_audio_channel;
    public final boolean ALC_SOFT_device_clock;
    public final boolean ALC_SOFT_HRTF;
    public final boolean ALC_SOFT_loopback;
    public final boolean ALC_SOFT_output_limiter;
    public final boolean ALC_SOFT_output_mode;
    public final boolean ALC_SOFT_pause_device;
    public final boolean ALC_SOFT_reopen_device;

    ALCCapabilities() {
        this.OpenALC10 = true;
        this.OpenALC11 = true;
        this.OpenALC_SOFT_loopback_bformat = false;
        this.ALC_ENUMERATE_ALL_EXT = false;
        this.ALC_ENUMERATION_EXT = false;
        this.ALC_EXT_CAPTURE = false;
        this.ALC_EXT_DEDICATED = false;
        this.ALC_EXT_DEFAULT_FILTER_ORDER = false;
        this.ALC_EXT_disconnect = false;
        this.ALC_EXT_EFX = false;
        this.ALC_EXT_thread_local_context = false;
        this.ALC_LOKI_audio_channel = false;
        this.ALC_SOFT_device_clock = false;
        this.ALC_SOFT_HRTF = false;
        this.ALC_SOFT_loopback = false;
        this.ALC_SOFT_output_limiter = false;
        this.ALC_SOFT_output_mode = false;
        this.ALC_SOFT_pause_device = false;
        this.ALC_SOFT_reopen_device = false;
    }
}

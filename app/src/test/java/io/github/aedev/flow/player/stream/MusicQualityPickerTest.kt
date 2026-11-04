package io.github.aedev.flow.player.stream

import io.github.aedev.flow.data.local.MusicAudioQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicQualityPickerTest {
    private data class S(val itag: Int, val kbps: Int, val codec: String, val channels: Int = 2)

    private val opus160 = S(251, 160, "audio/webm; codecs=\"opus\"")
    private val opus70 = S(250, 70, "audio/webm; codecs=\"opus\"")
    private val opus50 = S(249, 50, "audio/webm; codecs=\"opus\"")
    private val aac128 = S(140, 128, "audio/mp4; codecs=\"mp4a.40.2\"")
    private val eac3 = S(328, 384, "audio/mp4; codecs=\"ec-3\"", channels = 6)

    private fun pick(items: List<S>, quality: MusicAudioQuality) =
        MusicQualityPicker.pick(items, quality, bitrate = { it.kbps * 1000 }, codec = { it.codec }, channels = { it.channels })?.itag

    @Test
    fun `opus tiers pick the nearest opus stream`() {
        val streams = listOf(opus160, opus70, opus50, aac128)
        assertEquals(251, pick(streams, MusicAudioQuality.OPUS_160))
        assertEquals(250, pick(streams, MusicAudioQuality.OPUS_96))
        assertEquals(249, pick(streams, MusicAudioQuality.OPUS_48))
    }

    @Test
    fun `opus tiers never pick a multichannel stream`() {
        assertEquals(251, pick(listOf(opus160, eac3), MusicAudioQuality.OPUS_160))
    }

    @Test
    fun `opus tiers fall back to the nearest stream without opus`() {
        assertEquals(140, pick(listOf(aac128), MusicAudioQuality.OPUS_48))
    }

    @Test
    fun `dolby atmos prefers a dolby stream and falls back to the best`() {
        assertEquals(328, pick(listOf(opus160, aac128, eac3), MusicAudioQuality.DOLBY_ATMOS))
        assertEquals(251, pick(listOf(opus160, aac128), MusicAudioQuality.DOLBY_ATMOS))
    }

    @Test
    fun `flac tiers prefer flac and fall back to the best`() {
        val flacHi = S(901, 2300, "audio/flac")
        val flacLo = S(902, 1400, "audio/flac")
        assertEquals(901, pick(listOf(opus160, flacHi, flacLo), MusicAudioQuality.FLAC_24))
        assertEquals(902, pick(listOf(opus160, flacHi, flacLo), MusicAudioQuality.FLAC_16))
        assertEquals(251, pick(listOf(opus160, aac128), MusicAudioQuality.FLAC_24))
        assertEquals(251, pick(listOf(opus160, aac128), MusicAudioQuality.FLAC_16))
    }

    @Test
    fun `existing tiers keep their behaviour`() {
        val streams = listOf(opus160, opus50, aac128)
        assertEquals(251, pick(streams, MusicAudioQuality.HIGH))
        assertEquals(140, pick(streams, MusicAudioQuality.MEDIUM))
        assertEquals(249, pick(streams, MusicAudioQuality.LOW))
    }
}

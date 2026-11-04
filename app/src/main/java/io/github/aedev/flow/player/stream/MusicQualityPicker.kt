package io.github.aedev.flow.player.stream

import io.github.aedev.flow.data.local.MusicAudioQuality
import kotlin.math.abs

/**
 * The one place a [MusicAudioQuality] turns into a stream choice, shared by every stream type the app
 * resolves music from (InnerTube formats, NewPipe streams, download formats).
 *
 * - Auto / High: the best stream ([highScore], bitrate by default).
 * - Medium: the stream nearest 128 kbps. Low: the smallest stream.
 * - Opus 160 / 96 / 48: the Opus stream nearest that bitrate (YouTube serves Opus at roughly
 *   160, 70 and 50 kbps); without Opus, the nearest stream of any codec.
 * - Dolby Atmos: the best multichannel Dolby (E-AC-3 / AC-3) stream when the track has one,
 *   otherwise the best stream, so the choice never fails a song that has no Dolby mix.
 * - Lossless FLAC 24 / 16 bit: a FLAC stream when the track has one (24-bit the largest, 16-bit the
 *   smallest), otherwise the best stream. YouTube does not normally serve FLAC, so this is usually
 *   the same as High.
 */
object MusicQualityPicker {
    const val MEDIUM_BITRATE_TARGET = 128_000

    fun targetBitrate(quality: MusicAudioQuality): Int? =
        when (quality) {
            MusicAudioQuality.OPUS_160 -> 160_000
            MusicAudioQuality.OPUS_96 -> 96_000
            MusicAudioQuality.OPUS_48 -> 48_000
            else -> null
        }

    fun isOpus(mimeOrCodec: String?): Boolean = mimeOrCodec?.contains("opus", ignoreCase = true) == true

    fun isFlac(mimeOrCodec: String?): Boolean = mimeOrCodec?.contains("flac", ignoreCase = true) == true

    fun isDolby(mimeOrCodec: String?): Boolean {
        val value = mimeOrCodec?.lowercase() ?: return false
        return "ec-3" in value || "ac-3" in value || "eac3" in value || "ac3" in value
    }

    fun <T> pick(
        items: List<T>,
        quality: MusicAudioQuality,
        bitrate: (T) -> Int,
        codec: (T) -> String?,
        channels: (T) -> Int = { 2 },
        highScore: (T) -> Int = bitrate,
    ): T? {
        if (items.isEmpty()) return null
        val known = items.filter { bitrate(it) > 0 }.ifEmpty { items }

        return when (quality) {
            MusicAudioQuality.AUTO,
            MusicAudioQuality.HIGH,
            -> known.maxByOrNull(highScore)

            MusicAudioQuality.MEDIUM -> known.minByOrNull { abs(bitrate(it) - MEDIUM_BITRATE_TARGET) }

            MusicAudioQuality.LOW -> known.minByOrNull(bitrate)

            MusicAudioQuality.FLAC_24 -> known.filter { isFlac(codec(it)) }.maxByOrNull(bitrate) ?: known.maxByOrNull(highScore)

            MusicAudioQuality.FLAC_16 -> known.filter { isFlac(codec(it)) }.minByOrNull(bitrate) ?: known.maxByOrNull(highScore)

            MusicAudioQuality.DOLBY_ATMOS -> {
                val dolby = known.filter { isDolby(codec(it)) || channels(it) > 2 }
                dolby.maxWithOrNull(compareBy<T>(channels).thenBy(bitrate)) ?: known.maxByOrNull(highScore)
            }

            MusicAudioQuality.OPUS_160,
            MusicAudioQuality.OPUS_96,
            MusicAudioQuality.OPUS_48,
            -> {
                val target = targetBitrate(quality)!!
                val stereo = known.filterNot { isDolby(codec(it)) || channels(it) > 2 }.ifEmpty { known }
                val pool = stereo.filter { isOpus(codec(it)) }.ifEmpty { stereo }
                pool.minWithOrNull(compareBy<T> { abs(bitrate(it) - target) }.thenByDescending(bitrate))
            }
        }
    }
}

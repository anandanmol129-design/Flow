package io.github.aedev.flow.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicAudioQualityTest {
    @Test
    fun `auto streams high quality on wifi and medium on mobile data`() {
        assertEquals(MusicAudioQuality.HIGH, MusicAudioQuality.AUTO.resolve(onWifi = true))
        assertEquals(MusicAudioQuality.MEDIUM, MusicAudioQuality.AUTO.resolve(onWifi = false))
    }

    @Test
    fun `a fixed choice ignores the network`() {
        MusicAudioQuality.entries.filter { it != MusicAudioQuality.AUTO }.forEach { quality ->
            assertEquals(quality, quality.resolve(onWifi = true))
            assertEquals(quality, quality.resolve(onWifi = false))
        }
    }
}

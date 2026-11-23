package io.github.ephym6.plantid.data.image

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Photo ids come from navigation arguments, so they must never resolve outside the photo directory. */
class PhotoIdValidationTest {

    @Test
    fun `ids created by the store are valid`() {
        assertTrue(CachePhotoStore.isValidPhotoId("picked_1234567890.img"))
        assertTrue(CachePhotoStore.isValidPhotoId("capture_-987654321.jpg"))
    }

    @Test
    fun `path traversal and separators are rejected`() {
        listOf(
            "../secret.txt",
            "..",
            "a/b.jpg",
            "a\\b.jpg",
            "/data/data/x.jpg",
            "capture_1.jpg/../../x",
            "",
            "noextension",
            ".hidden",
        ).forEach { id ->
            assertFalse("expected invalid: '$id'", CachePhotoStore.isValidPhotoId(id))
        }
    }
}

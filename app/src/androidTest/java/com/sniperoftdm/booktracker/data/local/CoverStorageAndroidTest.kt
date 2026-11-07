package com.sniperoftdm.booktracker.data.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class CoverStorageAndroidTest {

    private lateinit var coverStorage: CoverStorage
    private lateinit var testDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        testDir = File(context.filesDir, "test_covers_${System.currentTimeMillis()}").apply { mkdirs() }
        coverStorage = CoverStorage(testDir)
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    @Test
    fun save_downscalesImage_preservingAspectRatio_andMaxSide600() {
        // Create 1200x800 bitmap (aspect ratio 3:2)
        val origBitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        origBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val input = ByteArrayInputStream(stream.toByteArray())

        val relativePath = coverStorage.save(input, 100L)
        assertNotNull(relativePath)
        assertEquals("covers/100.jpg", relativePath)

        val savedFile = File(testDir, relativePath!!)
        assertTrue(savedFile.exists())

        // Read saved bitmap dimensions
        val savedBitmap = BitmapFactory.decodeFile(savedFile.absolutePath)
        assertNotNull(savedBitmap)
        assertEquals(600, savedBitmap.width)
        assertEquals(400, savedBitmap.height)
    }

    @Test
    fun save_tallImage_downscalesHeightTo600() {
        // Create 800x1600 bitmap (tall)
        val origBitmap = Bitmap.createBitmap(800, 1600, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        origBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val input = ByteArrayInputStream(stream.toByteArray())

        val relativePath = coverStorage.save(input, 101L)
        assertNotNull(relativePath)

        val savedFile = File(testDir, relativePath!!)
        val savedBitmap = BitmapFactory.decodeFile(savedFile.absolutePath)
        assertNotNull(savedBitmap)
        assertEquals(300, savedBitmap.width)
        assertEquals(600, savedBitmap.height)
    }

    @Test
    fun save_smallImage_doesNotUpscale() {
        // Create 300x200 bitmap
        val origBitmap = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        origBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val input = ByteArrayInputStream(stream.toByteArray())

        val relativePath = coverStorage.save(input, 102L)
        assertNotNull(relativePath)

        val savedFile = File(testDir, relativePath!!)
        val savedBitmap = BitmapFactory.decodeFile(savedFile.absolutePath)
        assertNotNull(savedBitmap)
        assertEquals(300, savedBitmap.width)
        assertEquals(200, savedBitmap.height)
    }

    @Test
    fun save_rejectsNonImageInputGracefully() {
        val nonImageBytes = "this is not an image file at all".toByteArray()
        val input = ByteArrayInputStream(nonImageBytes)

        val relativePath = coverStorage.save(input, 103L)
        assertNull(relativePath)

        val file = File(coverStorage.coversDir, "103.jpg")
        assertFalse(file.exists())
    }
}

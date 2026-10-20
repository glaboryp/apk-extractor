package dev.gloria.apkextractor.extract

import dev.gloria.apkextractor.apps.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ApkResolverTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun apk(name: String): File = tmp.newFile(name).apply { writeText("apk") }

    private fun app(base: String, splits: List<String> = emptyList()) =
        InstalledApp("com.example.app", "App", false, base, splits)

    @Test
    fun `no splits resolves to Single`() {
        val base = apk("base.apk")
        assertEquals(ResolvedApk.Single(base), resolveApk(app(base.path)))
    }

    @Test
    fun `with splits resolves to Split base-first`() {
        val base = apk("base.apk")
        val config = apk("split_config.en.apk")
        val feature = apk("split_feature.apk")
        val result = resolveApk(app(base.path, listOf(config.path, feature.path)))
        assertEquals(ResolvedApk.Split(listOf(base, config, feature)), result)
    }

    @Test
    fun `missing split file resolves to Unreadable naming the file`() {
        val base = apk("base.apk")
        val missing = File(tmp.root, "split_missing.apk")
        val result = resolveApk(app(base.path, listOf(missing.path)))
        assertTrue(result is ResolvedApk.Unreadable)
        assertTrue((result as ResolvedApk.Unreadable).reason.contains("split_missing.apk"))
    }

    @Test
    fun `unreadable base resolves to Unreadable`() {
        val missing = File(tmp.root, "base.apk")
        val result = resolveApk(app(missing.path))
        assertTrue(result is ResolvedApk.Unreadable)
    }
}

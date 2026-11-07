package com.sniperoftdm.booktracker.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

class PluralsResourceTest {

    private val forbiddenPattern = Pattern.compile(
        """%(?:\d+\$)?(?:d|s)\s*(?:pages?|days?|books?|notes?|sessions?|minutes?|hours?|sayfa|gün|kitap|not|oturum|dakika|saat)\b""",
        Pattern.CASE_INSENSITIVE
    )

    @Test
    fun noPluralQuantityInStringsXml() {
        val projectDirs = listOf(
            File("src/main/res"),
            File("app/src/main/res")
        )
        val resDir = projectDirs.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find res directory in ${File(".").absolutePath}")

        val stringFiles = listOf(
            File(resDir, "values/strings.xml"),
            File(resDir, "values-tr/strings.xml")
        )

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()

        val violations = mutableListOf<String>()

        for (file in stringFiles) {
            assertTrue("File ${file.path} must exist", file.exists())
            val doc = builder.parse(file)
            val stringNodes = doc.getElementsByTagName("string")

            for (i in 0 until stringNodes.length) {
                val node = stringNodes.item(i)
                val name = node.attributes?.getNamedItem("name")?.nodeValue ?: "unknown"
                val text = node.textContent ?: ""

                val matcher = forbiddenPattern.matcher(text)
                if (matcher.find()) {
                    violations.add("${file.name} -> @string/$name: \"$text\" contains forbidden plural pattern '${matcher.group()}'")
                }
            }
        }

        assertTrue(
            "Found plural format strings in strings.xml that should be in plurals.xml:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}

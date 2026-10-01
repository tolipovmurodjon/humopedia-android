package com.mcompany.humo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// If the launch URL, the link host and the asset statement disagree, the app still opens but shows a browser URL bar.
class WebShellConfigTest {

    private val strings: Map<String, String> by lazy {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent.replace("\\\"", "\"")
        }
    }

    @Test
    fun launchUrlIsOnTheVerifiedHost() {
        assertTrue(strings.getValue("launch_url").startsWith("https://${strings.getValue("web_host")}/"))
    }

    @Test
    fun assetStatementNamesTheSameSite() {
        val site = Regex("\"site\":\\s*\"([^\"]+)\"").find(strings.getValue("asset_statements"))?.groupValues?.get(1)
        assertEquals("https://${strings.getValue("web_host")}", site)
    }
}

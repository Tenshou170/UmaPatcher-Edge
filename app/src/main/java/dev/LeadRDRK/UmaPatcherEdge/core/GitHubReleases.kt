package dev.LeadRDRK.UmaPatcherEdge.core

import android.net.Uri
import java.net.URL
import dev.LeadRDRK.UmaPatcherEdge.utils.fetchJson as utilsFetchJson

class GitHubReleases(
    private val repoPath: String,
    private val baseUrl: String = "https://api.github.com/repos"
) {
    private val releasesApi get() = URL("$baseUrl/$repoPath/releases")
    private val latestApi   get() = URL("$baseUrl/$repoPath/releases/latest")

    private fun fetchJson(url: URL): HashMap<*, *> =
        utilsFetchJson(url, "application/vnd.github+json")

    fun fetchReleases() = fetchJson(releasesApi)
    fun fetchLatest() = fetchJson(latestApi)

    fun getReleaseUrl(tagName: String) =
        "https://github.com/$repoPath/releases/tag/${Uri.encode(tagName)}"
}
package tasks

import contributors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.awaitResponse

suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) = coroutineScope {
    val repos = service
        .getOrgReposCall(req.org)
        .awaitResponse()
        .also { logRepos(req, it) }
        .body() ?: listOf()

    repos.withIndex().fold(emptyList<User>()) { contribs, (index, repo) ->
        log("getting contribs for ${req.org}/${repo.name}")
        val repoContribs =
            service
                .getRepoContributorsCall(req.org, repo.name)
                .awaitResponse()
                .also { logUsers(repo, it) }
                .bodyList()
        val updatedContribs = (contribs + repoContribs).aggregate()
        val completed = index == repos.lastIndex
        updateResults(updatedContribs, completed)
        updatedContribs
    }
}

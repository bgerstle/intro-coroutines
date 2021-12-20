package tasks

import contributors.*
import kotlinx.coroutines.*
import retrofit2.awaitResponse
import kotlin.coroutines.coroutineContext

suspend fun loadContributorsNotCancellable(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgReposCall(req.org)
        .awaitResponse()
        .also { logRepos(req, it) }
        .body() ?: listOf()

    return repos.map { repo ->
        GlobalScope.async(Dispatchers.Default) {
            log("getting contribs for ${req.org}/${repo.name}")
            delay(3000)
            service
                .getRepoContributorsCall(req.org, repo.name)
                .awaitResponse()
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }.awaitAll().flatten().aggregate()
}

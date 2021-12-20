package tasks

import contributors.*
import kotlinx.coroutines.*
import retrofit2.awaitResponse

suspend fun loadContributorsConcurrent(service: GitHubService, req: RequestData): List<User> = coroutineScope {
    val repos = service
        .getOrgReposCall(req.org)
        .awaitResponse()
        .also { logRepos(req, it) }
        .body() ?: listOf()

     repos.map { repo ->
        async(Dispatchers.Default) {
            log("getting contribs for ${req.org}/${repo.name}")
            service
                .getRepoContributorsCall(req.org, repo.name)
                .awaitResponse()
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }.awaitAll().flatten().aggregate()
}

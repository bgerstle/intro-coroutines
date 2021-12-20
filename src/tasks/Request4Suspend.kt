package tasks

import contributors.*
import retrofit2.awaitResponse

suspend fun loadContributorsSuspend(service: GitHubService, req: RequestData) : List<User> {
    val repos = service
        .getOrgReposCall(req.org)
        .awaitResponse()
        .also { logRepos(req, it) }
        .body() ?: listOf()

    return repos.flatMap { repo ->
        service
            .getRepoContributorsCall(req.org, repo.name)
            .awaitResponse()
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
}

package tasks

import contributors.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .body() ?: listOf()

        val repoContributorsChannel = Channel<Pair<Repo, List<User>>>(repos.size)

        launch {
            val contribsByRepo = mutableMapOf<Repo, List<User>>()
            var completed = false

            while (!completed) {
                val (repo, contribs) = repoContributorsChannel.receive()
                contribsByRepo[repo] = contribs

                completed = contribsByRepo.keys.containsAll(repos)

                val aggregatedContribs = contribsByRepo.values.flatten().aggregate()
                log("updating results${if (completed) " for the last time" else ""}!")
                updateResults(aggregatedContribs, completed)
            }
        }

        repos.map { repo ->
            log("getting contribs for ${req.org}/${repo.name}")
            async {
                // TODO: error handling with Result<>?
                val contribs =
                    service
                        .getRepoContributors(req.org, repo.name)
                        .also { logUsers(repo, it) }
                        .bodyList()
                repoContributorsChannel.send(Pair(repo, contribs))
            }
        }.awaitAll()

        log("done fetchin repos!")
    }
}

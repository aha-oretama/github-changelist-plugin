package aha.oretama.jp;

import org.kohsuke.github.GHRepository;

import java.io.IOException;

/**
 * @author aha-oretama
 */
public class GitHubRepositoryWrapper {

    private GHRepository ghRepository;

    GitHubRepositoryWrapper(GHRepository ghRepository) {
        this.ghRepository = ghRepository;
    }

    public GitHubPullRequestWrapper getGitHubPullRequest(String headName) throws IOException {
        Integer number = Integer.valueOf(headName.replace("PR-",""));
        return new GitHubPullRequestWrapper(ghRepository.getPullRequest(number));
    }
}

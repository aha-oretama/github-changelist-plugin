package aha.oretama.jp;

import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author aha-oretama
 */
public class GitHubPullRequestWrapper {

    private GHPullRequest ghPullRequest;

    GitHubPullRequestWrapper(GHPullRequest ghPullRequest) {
        this.ghPullRequest = ghPullRequest;
    }

    public List<String> getChangelist() {
        return ghPullRequest.listFiles().asList().stream()
            .map(GHPullRequestFileDetail::getFilename)
            .collect(Collectors.toList());
    }
}

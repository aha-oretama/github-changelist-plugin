package aha.oretama.jp;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.model.Item;
import hudson.security.ACL;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author aha-oretama
 */
public class GitHubWrapper {

    private final GitHub gitHub;

    private static final Logger LOGGER = Logger.getLogger(GitHubWrapper.class.getName());

    private GitHubWrapper(GitHub gitHub) {
        this.gitHub = gitHub;
    }

    public static GitHubWrapper connect(String apiUrl, String credentialsId) throws IOException {
        GitHubBuilder builder =  getBuilder(apiUrl, credentialsId);
        return new GitHubWrapper(builder.build());
    }

    public GitHubRepositoryWrapper getGitHubRepository(String repoOwner, String repository) throws IOException {
        return new GitHubRepositoryWrapper(gitHub.getRepository(repoOwner + "/" + repository));
    }

    private static GitHubBuilder getBuilder(String apiUri, String credentialsId) {
        GitHubBuilder builder = new GitHubBuilder()
            .withEndpoint(apiUri)
            .withConnector(new HttpConnectorWithJenkinsProxy());

        if (StringUtils.isEmpty(credentialsId)) {
            LOGGER.log(Level.WARNING, "credentialsId not set, using anonymous connection");
            return builder;
        }

        StandardCredentials credentials = lookupCredentials( credentialsId, apiUri);
        if (credentials == null) {
            LOGGER.log(Level.SEVERE, "Failed to look up credentials for using id: {1}",
                new Object[] { credentialsId });
        } else if (credentials instanceof StandardUsernamePasswordCredentials) {
            LOGGER.log(Level.FINEST, "Using username/password ");
            StandardUsernamePasswordCredentials upCredentials = (StandardUsernamePasswordCredentials) credentials;
            return builder.withPassword(upCredentials.getUsername(), upCredentials.getPassword().getPlainText());
        } else if (credentials instanceof StringCredentials) {
            LOGGER.log(Level.FINEST, "Using OAuth token");
            StringCredentials tokenCredentials = (StringCredentials) credentials;
            return builder.withOAuthToken(tokenCredentials.getSecret().getPlainText());
        } else {
            LOGGER.log(Level.SEVERE, "Unknown credential type for using id: {0}: {1}",
                new Object[] { credentialsId, credentials.getClass().getName() });
            return null;
        }
        return builder;
    }

    private static StandardCredentials lookupCredentials(String credentialId, String uri) {
        LOGGER.log(Level.FINE, "Looking up credentials for {0} for url {1}", new Object[] { credentialId, uri });

        List<StandardCredentials> credentials;

        LOGGER.log(Level.FINE, "Using null context because of issues not getting all credentials");

        credentials = CredentialsProvider
            .lookupCredentials(StandardCredentials.class, (Item) null, ACL.SYSTEM,
                URIRequirementBuilder.fromUri(uri).build());

        LOGGER.log(Level.FINE, "Found {0} credentials", new Object[]{credentials.size()});

        return (credentialId == null) ? null : CredentialsMatchers.firstOrNull(credentials,
            CredentialsMatchers.withId(credentialId));
    }
}

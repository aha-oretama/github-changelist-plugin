package aha.oretama.jp;

import com.google.inject.Inject;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.branch.Branch;
import jenkins.branch.BranchSource;

import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author aha-oretama
 */
public class GitHubChangelistStep extends AbstractStepImpl {

    private final String regex;
    private final String testTargetRegex;
    private static final Logger LOGGER = Logger.getLogger(GitHubChangelistStep.class.getName());

    @DataBoundConstructor public GitHubChangelistStep(String regex, String testTargetRegex) {
        this.regex = regex;
        this.testTargetRegex = testTargetRegex;
    }

    @Extension public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override public String getFunctionName() {
            return "changelist";
        }

        @Override public String getDisplayName() {
            return "return GitHubWrapper changelist.";
        }
    }


    public final static class Execution extends AbstractSynchronousStepExecution<List<String>> {

        @Inject
        private transient GitHubChangelistStep step;

        @StepContextParameter
        private transient Run<?,?> build;

        @StepContextParameter
        private transient TaskListener listener;


        @Override
        protected List<String> run() throws Exception {
            return getChangelist(build, listener, step.regex, step.testTargetRegex);
        }

        private List<String> getChangelist(Run<?, ?> build, TaskListener listener, String regex, String testTargetRegex)
            throws IOException, IllegalAccessException, NoSuchFieldException {

            Job job = build.getParent();

            BranchJobProperty branchJobProperty =
                (BranchJobProperty) job.getProperty(BranchJobProperty.class);
            if(branchJobProperty == null) {
                throw new AbortException("To use changelist, job must be on multi branch pipeline.");
            }

            String branchName = branchJobProperty.getBranch().getHead().getName();
            List<String> changelist = new ArrayList<>();
            if(branchName.startsWith("PR-")) {
                changelist.addAll(getPullRequestChanges(branchJobProperty));
            }else{
            }

            return new ArrayList<>();
        }

        private List<String> getPullRequestChanges(BranchJobProperty branchJobProperty)
            throws IOException, IllegalAccessException, NoSuchFieldException {

            Field field = JobProperty.class.getDeclaredField("owner");
            field.setAccessible(true);
            Object owner = field.get(branchJobProperty);
            if(!(owner instanceof WorkflowJob)) {
                throw new AbortException("To use changelist, job must be on multi branch pipeline.");
            }
            WorkflowMultiBranchProject parent =
                (WorkflowMultiBranchProject) ((WorkflowJob) owner).getParent();
            List<BranchSource> sources = parent.getSources();
            if(sources.size() == 0) {
                throw new AbortException("Set GitHubWrapper on multi branch job.");
            }
            GitHubSCMSource source = (GitHubSCMSource) sources.stream()
                .filter(branchSource -> branchSource.getSource() instanceof GitHubSCMSource)
                .findFirst().orElseThrow(() ->  new AbortException("Set GitHubWrapper on multi branch job."))
                .getSource();

            Branch branch = branchJobProperty.getBranch();

            GitHubWrapper gitHubWrapper =
                GitHubWrapper.connect(source.getApiUri(), source.getCredentialsId());
            GitHubRepositoryWrapper gitHubRepositoryWrapper =
                gitHubWrapper.getGitHubRepository(source.getRepoOwner(), source.getRepository());
            GitHubPullRequestWrapper gitHubPullRequest =
                gitHubRepositoryWrapper.getGitHubPullRequest(branch.getHead().getName());

            return gitHubPullRequest.getChangelist();
        }
    }

}

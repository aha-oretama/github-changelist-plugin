package aha.oretama.jp;

import com.google.inject.Inject;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;
import jenkins.branch.Branch;
import jenkins.branch.BranchSource;

import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aha-oretama
 */
public class GitHubChangelistStep extends AbstractStepImpl {

    /**
     * This regular expression is used to get the matched group from the changed file.
     * Default is to get the file name excluding 'Test'.
     */
    private String regex = "([^/]*?)(Test)?(\\..*)?$";

    /**
     * This regular expression is used to transform the matched group into output list.
     * Default is to get the file name formated as "**&#47;XXTest*".
     */
    private String testTargetRegex = "**/$1Test*";

    private static final Logger LOGGER = Logger.getLogger(GitHubChangelistStep.class.getName());

    @DataBoundConstructor
    public GitHubChangelistStep() {}

    @DataBoundSetter
    public void setRegex(String regex) {
        this.regex = regex;
    }

    @DataBoundSetter
    public void setTestTargetRegex(String testTargetRegex) {
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
            return "Get changelist.";
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
            List<String> changelist =
                getChangelist(build, listener);
            return RegexUtils.createRegexps(changelist, step.regex, step.testTargetRegex);
        }

        private List<String> getChangelist(Run<?, ?> build, TaskListener listener)
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
                changelist.addAll(getPullRequestChangelist(branchJobProperty));
            }else{
                changelist.addAll(getChangelistFromLastBuild(build));
            }

            return changelist;
        }

        private List<String> getPullRequestChangelist(BranchJobProperty branchJobProperty)
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

        private List<String> getChangelistFromLastBuild(Run build) throws AbortException {

            if(!(build instanceof WorkflowRun)){
                throw new AbortException("To use changelist, job must be on multi branch pipeline.");
            }

            List<GitChangeSet> gitChangeSets = new ArrayList<>();

            for (ChangeLogSet changeLogSet :((WorkflowRun) build).getChangeSets()) {
                if(changeLogSet.getKind().equals("git")){
                    gitChangeSets.addAll(
                        Stream.of(changeLogSet.getItems()).map(item -> (GitChangeSet)item).collect(Collectors.toList()));
                }
            }

            List<String> changelist = new ArrayList<>();
            for (GitChangeSet gitChangeSet: gitChangeSets) {
                changelist.addAll(gitChangeSet.getAffectedPaths());
            }

            return changelist;
        }
    }

}

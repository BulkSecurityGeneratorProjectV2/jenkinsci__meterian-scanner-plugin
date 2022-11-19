package io.meterian.integration_tests;

import io.meterian.test_management.TestManagement;
import org.apache.commons.io.FileUtils;

import org.junit.Before;
import org.junit.Test;
import static junit.framework.TestCase.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.meterian.jenkins.core.Meterian;
import io.meterian.jenkins.glue.MeterianPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeterianClientTest {

    private static final Logger log = LoggerFactory.getLogger(MeterianClientTest.class);

    private static final String CURRENT_WORKING_DIR = System.getProperty("user.dir");

    private TestManagement testManagement;
    private File logFile;
    private PrintStream jenkinsLogger;

    private String githubProjectName = "autofix-sample-maven-upgrade";
    private String gitRepoRootFolder = Paths.get(CURRENT_WORKING_DIR, "target/github-repo/").toString();
    private String gitRepoWorkingFolder = Paths.get(gitRepoRootFolder, githubProjectName).toString();

    @Before
    public void setup() throws IOException {
        logFile = Files.createTempFile("jenkins-logger-", Long.toString(System.nanoTime())).toFile();
        jenkinsLogger = new PrintStream(logFile);
        log.info("Jenkins log file: " + logFile.toPath().toString());

        testManagement = new TestManagement(gitRepoWorkingFolder, log, jenkinsLogger);

        FileUtils.deleteDirectory(new File(gitRepoRootFolder));

        new File(gitRepoRootFolder).mkdir();

        testManagement.performCloneGitRepo(
                "MeterianHQ",
                "autofix-sample-maven-upgrade",
                gitRepoRootFolder,
                "master");
    }

    @Test
    public void givenConfiguration_whenMeterianClientIsRun_thenItShouldNotThrowException() throws IOException {
        // Given: we are setup to run the meterian client against a repo that has vulnerabilities
        File logFile = Files.createTempFile("jenkins-logger", Long.toString(System.nanoTime())).toFile();
        PrintStream jenkinsLogger = new PrintStream(logFile);
        MeterianPlugin.Configuration configuration = testManagement.getConfiguration();

        // When: the meterian client is run against the locally cloned git repo
        try {
            File clientJar = testManagement.getClientJar();
            Meterian client = testManagement.getMeterianClient(configuration, clientJar);
            client.prepare("--interactive=false");
            client.run();
            jenkinsLogger.close();
        } catch (Exception ex) {
            fail("Should not have failed with the exception: " + ex.getMessage());
        }

        // Then: we should be able to see the expecting output in the execution analysis output logs
        testManagement.verifyRunAnalysisLogs(logFile,
                new String[]{},
                new String[]{
                        "[meterian] Client successfully authorized",
                        "[meterian] Meterian Client v",
                        "[meterian] Project information:",
                        "[meterian] JAVA scan -",
                        "MeterianHQ/autofix-sample-maven-upgrade.git",
                        "[meterian] Full report available at: ",
                        "[meterian] Build unsuccessful!",
                        "[meterian] Failed checks: [security]"
                }
        );
    }
}

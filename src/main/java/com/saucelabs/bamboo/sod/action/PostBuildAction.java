package com.saucelabs.bamboo.sod.action;

import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.CustomBuildProcessor;
import com.atlassian.bamboo.build.LogEntry;
import com.atlassian.bamboo.build.logger.BuildLogUtils;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.resultsummary.tests.TestState;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.saucelabs.bamboo.sod.AbstractSauceBuildPlugin;
import com.saucelabs.bamboo.sod.config.SODMappedBuildConfiguration;
import com.saucelabs.ci.sauceconnect.SauceConnectTwoManager;
import com.saucelabs.ci.sauceconnect.SauceTunnelManager;
import com.saucelabs.saucerest.SauceREST;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Invoked after a build has finished to reset the environment variables for the builder back to what they were prior
 * to the invocation of Sauce.  The class will also invoke the Sauce REST API to store the Bamboo build number against
 * the Sauce Job.  This will be performed if the output from the Bamboo Build includes a line beginning with 'SauceOnDemandSessionID'
 * (the selenium-client-factory library will output this line).
 *
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public class PostBuildAction extends AbstractSauceBuildPlugin implements CustomBuildProcessor {

    private static final Logger logger = Logger.getLogger(PostBuildAction.class);
    public static final String SAUCE_ON_DEMAND_SESSION_ID = "SauceOnDemandSessionID";
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("SauceOnDemandSessionID=([0-9a-fA-F]+)(?:.job-name=(.*))?");
    private static final String JOB_NAME_PATTERN = "\\b({0})\\b";


    /**
     * Populated via dependency injection.
     */
    private PlanManager planManager;

    /**
     * Populated via dependency injection.
     */
    private BuildLoggerManager buildLoggerManager;


    /**
     * Populated via dependency injection.
     */
    private SauceTunnelManager sauceTunnelManager;

    @NotNull
    public BuildContext call() {

        final SODMappedBuildConfiguration config = new SODMappedBuildConfiguration(buildContext.getBuildDefinition().getCustomConfiguration());
        if (config.isEnabled()) {
            try {
                getSauceTunnelManager().closeTunnelsForPlan(config.getTempUsername(), null);
                recordSauceJobResult(config);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        return buildContext;
    }

    public void init(@NotNull BuildContext context) {
        this.buildContext = context;
    }

    public void setPlanManager(PlanManager planManager) {
        this.planManager = planManager;
    }

    public void setSauceTunnelManager(SauceTunnelManager sauceTunnelManager) {
        this.sauceTunnelManager = sauceTunnelManager;
    }

    public SauceTunnelManager getSauceTunnelManager() {
        if (sauceTunnelManager == null) {
            //this will occur when a remote agent runs, as it doesn't have Spring components available
            setSauceTunnelManager(new SauceConnectTwoManager());
        }
        return sauceTunnelManager;
    }

    /**
     * Iterates over the output lines from the build.  For each line that begins with 'SauceOnDemandSessionID',
     * store the session id from the line in the custom build data of the build, and invoke the Sauce REST API
     * to store the Bamboo build number
     *
     * @param config
     */
    private void recordSauceJobResult(SODMappedBuildConfiguration config) throws IOException {
        //iterate over the entries of the build logger to see if one starts with 'SauceOnDemandSessionID'
        boolean foundLogEntry = false;
        logger.debug("Checking log interceptor entries");

        CurrentBuildResult buildResult = buildContext.getBuildResult();
        for (Map.Entry<String, String> entry : buildResult.getCustomBuildData().entrySet()) {
            if (entry.getKey().contains("SAUCE_JOB_ID")) {
                if (processLine(config, entry.getValue())) {
                    foundLogEntry = true;
                }
                ;
            }
        }

        if (!foundLogEntry) {
            logger.warn("No Sauce Session ids found in build context, reading from log file");
            //try read from the log file directly
            File logDirectory = BuildLogUtils.getLogFileDirectory(buildContext.getPlanKey());
            String logFileName = BuildLogUtils.getLogFileName(buildContext.getPlanKey(), buildContext.getBuildNumber());
            List lines = FileUtils.readLines(new File(logDirectory, logFileName));
            for (Object object : lines) {
                String line = (String) object;
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing line: " + line);
                }
                if (processLine(config, line)) {
                    foundLogEntry = true;
                }
            }
        }

        //if we still don't have anything, try the build logger output.  This will only have the last 100 lines.
        if (!foundLogEntry) {
            logger.warn("No Sauce Session ids found in log file, reading from build logger output");
            BuildLogger buildLogger = buildLoggerManager.getBuildLogger(buildContext.getBuildResultKey());
            for (LogEntry logEntry : buildLogger.getBuildLog()) {
                if (processLine(config, logEntry.getLog())) {
                    foundLogEntry = true;
                }

            }
        }

        if (!foundLogEntry) {
            logger.warn("No Sauce Session ids found in build output");
        }

    }

    private boolean processLine(SODMappedBuildConfiguration config, String line) {


        //extract session id
        String sessionId = null;
        String jobName = null;
        Matcher m = SESSION_ID_PATTERN.matcher(line);
        while (m.find()) {
            sessionId = m.group(1);
            if (m.groupCount() == 2) {
                jobName = m.group(2);
            }
        }

        if (sessionId == null) {
            sessionId = StringUtils.substringBetween(line, SAUCE_ON_DEMAND_SESSION_ID + "=", " ");
        }
        if (sessionId == null) {
            //we might not have a space separating the session id and job-name, so retrieve the text up to the end of the string
            sessionId = StringUtils.substringAfter(line, SAUCE_ON_DEMAND_SESSION_ID + "=");
        }
        if (sessionId != null && !sessionId.equalsIgnoreCase("null")) {
            if (sessionId.trim().equals("")) {
                logger.error("Session id for line" + line + " was blank");
                return false;
            } else {
                //TODO extract Sauce Job name (included on log line as 'job-name=')?
                storeBambooBuildNumberInSauce(config, sessionId, jobName);
                return true;
            }
        }
        return false;
    }

    /**
     * Invokes the Sauce REST API to store the build number and pass/fail status against the Sauce Job.
     *
     * @param config
     * @param sessionId the Sauce Job Id
     * @param jobName
     */

    private void storeBambooBuildNumberInSauce(SODMappedBuildConfiguration config, String sessionId, String jobName) {
        SauceREST sauceREST = new SauceREST(config.getTempUsername(), config.getTempApikey());

        Map<String, Object> updates = new HashMap<String, Object>();
        try {
            logger.debug("Invoking Sauce REST API for " + sessionId);
            String json = sauceREST.getJobInfo(sessionId);
            logger.debug("Results: " + json);
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(json);
            updates.put("build", getBuildNumber());
            if (jsonObject.get("passed") == null || jsonObject.get("passed").equals("")) {
                if (jsonObject.containsKey("name")) {
                    //use the job name stored on the job if available
                    jobName = (String) jsonObject.get("name");

                }
                Boolean testPassed = hasTestPassed(jobName);
                updates.put("passed", testPassed);
            }

            logger.debug("About to update job " + sessionId + " with build number " + getBuildNumber());
            sauceREST.updateJobInfo(sessionId, updates);
        } catch (ParseException e) {
            logger.error("Unable to set build number for " + sessionId, e);
        } catch (Exception e) {
            logger.error("Unexpected error processing " + sessionId, e);
        }

    }

    private Boolean hasTestPassed(String name) {
        //do we have a test which matches the job name?
        TestResults testResults = findTestResult(name);
        if (testResults != null) {
            return testResults.getState().equals(TestState.SUCCESS);
        }

        return (buildContext.getBuildResult().getBuildState().equals(BuildState.SUCCESS));
    }

    private TestResults findTestResult(String name) {
        if (name == null) {
            return null;
        }
        TestResults testResult = findTestResult(name, buildContext.getBuildResult().getFailedTestResults());
        if (testResult == null) {
            testResult = findTestResult(name, buildContext.getBuildResult().getSuccessfulTestResults());
        }
        return testResult;
    }

    private TestResults findTestResult(String name, Collection<TestResults> testResults) {
        for (TestResults testResult : testResults) {
            Pattern jobNamePattern = Pattern.compile(MessageFormat.format(JOB_NAME_PATTERN, name));
            Matcher matcher = jobNamePattern.matcher(testResult.getActualMethodName());
            if (name.equals(testResult.getActualMethodName()) //if job name equals full name of test
                    || name.contains(testResult.getActualMethodName()) //or if job name contains the test name
                    || matcher.find()) { //or if the full name of the test contains the job name (matching whole words only)
                //then we have a match
                return testResult;
            }
        }
        return null;
    }

    private String getBuildNumber() {
        return getBuildContextToUse().getBuildResultKey();
    }

    /**
     * Use the parent build context if available, otherwise use the build context.
     *
     * @return
     */
    private BuildContext getBuildContextToUse() {
        return buildContext.getParentBuildContext() == null ? buildContext : buildContext.getParentBuildContext();
    }

    public void setBuildLoggerManager(BuildLoggerManager buildLoggerManager) {
        this.buildLoggerManager = buildLoggerManager;
    }

}
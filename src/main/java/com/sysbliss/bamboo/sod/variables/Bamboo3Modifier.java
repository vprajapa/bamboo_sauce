package com.sysbliss.bamboo.sod.variables;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.configuration.AdministrationConfigurationManager;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.sysbliss.bamboo.sod.BrowserFactory;
import com.sysbliss.bamboo.sod.config.SODKeys;
import com.sysbliss.bamboo.sod.config.SODMappedBuildConfiguration;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @author Ross Rowe
 */
public class Bamboo3Modifier extends DefaultVariableModifier  {

    public Bamboo3Modifier(SODMappedBuildConfiguration config, AdministrationConfigurationManager administrationConfigurationManager, BuildDefinition definition, BuildContext buildContext, BrowserFactory sauceBrowserFactory) {
        super(config, administrationConfigurationManager, definition, buildContext, sauceBrowserFactory);
    }

    public Bamboo3Modifier(SODMappedBuildConfiguration config, BuildDefinition definition, BuildContext buildContext) {
        super(config, definition, buildContext);
    }

    public void storeVariables() throws JSONException {
        String envBuffer = createSeleniumEnvironmentVariables();
        try {
            Class taskDefinitionClass = Class.forName("com.atlassian.bamboo.task.TaskDefinition");
            if (taskDefinitionClass != null) {
                Method taskDefinitionsMethod = BuildDefinition.class.getMethod("getTaskDefinitions", null);
                List/*<TaskDefinition>*/ taskDefinitions = (List/*<TaskDefinition>*/) taskDefinitionsMethod.invoke(definition, null);
                for (Object taskDefinition : taskDefinitions) {
                    Method method = taskDefinitionClass.getMethod("getConfiguration", null);
                    Map<String, String> configuration = (Map<String, String>) method.invoke(taskDefinition);
                    String originalEnv = configuration.get("environmentVariables");
                    if (StringUtils.isNotBlank(originalEnv)) {
                        envBuffer = " " + envBuffer;
                    }

                    config.getMap().put(SODKeys.TEMP_ENV_VARS, originalEnv);
                    configuration.put("environmentVariables", originalEnv + envBuffer);
                }
            }
        } catch (Exception e) {
            //ignore and attempt to continue
        }
    }

    @Override
    public void restoreVariables() {
        try {
            Class taskDefinitionClass = Class.forName("com.atlassian.bamboo.task.TaskDefinition");
            if (taskDefinitionClass != null) {
                Method taskDefinitionsMethod = BuildDefinition.class.getMethod("getTaskDefinitions", null);
                List/*<TaskDefinition>*/ taskDefinitions = (List/*<TaskDefinition>*/) taskDefinitionsMethod.invoke(definition, null);
                for (Object taskDefinition : taskDefinitions) {
                    Method method = taskDefinitionClass.getMethod("getConfiguration", null);
                    Map<String, String> configuration = (Map<String, String>) method.invoke(taskDefinition);
                    configuration.put("environmentVariables", config.getMap().get(SODKeys.TEMP_ENV_VARS));
                    config.getMap().put(SODKeys.TEMP_ENV_VARS, "");
                }
            }
        } catch (Exception e) {
            //ignore and attempt to continue
        }
    }
}

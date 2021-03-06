package com.saucelabs.bamboo.sod.admin.action;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationManager;
import com.atlassian.bamboo.configuration.ConfigurationAction;
import com.opensymphony.xwork2.ActionContext;
import com.saucelabs.bamboo.sod.config.SODKeys;
import org.apache.commons.lang.StringUtils;

/**
 * Handles the validation and storage of the Sauce administration variables.
 * 
 * The values entered here apply to all projects in the Bamboo environment.
 *
 * 
 * @author <a href="http://www.sysbliss.com">Jonathan Doklovic</a>
 * @author Ross Rowe
 */
public class ConfigureSODAction extends ConfigurationAction
{
    private String username;
    private String accessKey;

    private String proxyHost;
    private String proxyPort;
    private String proxyUsername;
    private String proxyPassword;

    /**
     * Populated via dependency injection.
     */
    private transient AdministrationConfigurationManager administrationConfigurationManager;

    /**
     * Invoked when the Sauce Administration screen is opened, populates the underlying variables
     * with some default values.
     * @return 'input'
     */
    @Override
    public String doDefault()
    {
        final AdministrationConfiguration adminConfig = administrationConfigurationManager.getAdministrationConfiguration();
        setUsername(adminConfig.getSystemProperty(SODKeys.SOD_USERNAME_KEY));
        setAccessKey(adminConfig.getSystemProperty(SODKeys.SOD_ACCESSKEY_KEY));
        setProxyHost(adminConfig.getSystemProperty(SODKeys.PROXY_HOST_KEY));
        setProxyPort(adminConfig.getSystemProperty(SODKeys.PROXY_PORT_KEY));
        setProxyUsername(adminConfig.getSystemProperty(SODKeys.PROXY_USERNAME_KEY));
        setProxyPassword(adminConfig.getSystemProperty(SODKeys.PROXY_PASSWORD_KEY));
        return INPUT;
    }

    /**
     * Invoked when a user clicks 'Save' on the Sauce Administration screen.
     * 
     * @return 'success'
     */
    public String doSave()
    {

        final AdministrationConfiguration adminConfig = administrationConfigurationManager.getAdministrationConfiguration();
        adminConfig.setSystemProperty(SODKeys.SOD_USERNAME_KEY, getUsername());
        adminConfig.setSystemProperty(SODKeys.SOD_ACCESSKEY_KEY, getAccessKey());
        adminConfig.setSystemProperty(SODKeys.PROXY_HOST_KEY, getProxyHost());
        adminConfig.setSystemProperty(SODKeys.PROXY_PORT_KEY, getProxyPort());
        adminConfig.setSystemProperty(SODKeys.PROXY_USERNAME_KEY, getProxyUsername());
        adminConfig.setSystemProperty(SODKeys.PROXY_PASSWORD_KEY, getProxyPassword());
        administrationConfigurationManager.saveAdministrationConfiguration(adminConfig);
        //this is a bit of a hack to support unit testing
        //getBamboo() won't be null at runtime, but we can't mock the method
        if (ActionContext.getContext() != null && ActionContext.getContext().getApplication() != null) {
            getBamboo().restartComponentsFollowingConfigurationChange();
        }

        addActionMessage(getText("config.updated"));
        return SUCCESS;
    }



    /**
     * Performs validation over the variables that are populated from the administration.
     * interface.  
     */
    @Override
    public void validate()
    {
        if (StringUtils.isBlank(username))
        {
            addFieldError("username", "User Name is required.");
        }

        if (StringUtils.isBlank(accessKey))
        {
            addFieldError("accessKey", "Access Key is required.");
        }

    }

    public AdministrationConfigurationManager getAdministrationConfigurationManager()
    {
        return administrationConfigurationManager;
    }

    public void setAdministrationConfigurationManager(AdministrationConfigurationManager administrationConfigurationManager)
    {
        this.administrationConfigurationManager = administrationConfigurationManager;
    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public void setAccessKey(String accesskey)
    {
        this.accessKey = accesskey;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }
}
/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Implementation of <code>QueryFieldsMgr</code>.
 * 
 * @author Andrey Markelov
 */
public class QueryFieldsMgrImpl
    implements QueryFieldsMgr
{
    /**
     * PlugIn key.
     */
    private static final String PLUGIN_KEY = "QUERY_LINKING_ISSUE_CFS";

    /**
     * Property separator.
     */
    private static final String VAL_SEPARATOR = "||";

    /**
     * Plug-In settings factory.
     */
    private final PluginSettingsFactory pluginSettingsFactory;

    /**
     * Constructor.
     */
    public QueryFieldsMgrImpl(
        PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    /**
     * Create property key.
     */
    private String createPropKey(long cfId, long projId)
    {
        return (cfId + VAL_SEPARATOR + projId);
    }

    @Override
    public String getQueryFieldData(
        long cfId,
        long projId)
    {
        return (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(createPropKey(cfId, projId));
    }

    @Override
    public void setQueryFieldData(
        long cfId,
        long projId,
        String data)
    {
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(createPropKey(cfId, projId), data);
    }
}

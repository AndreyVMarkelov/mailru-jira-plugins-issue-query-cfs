/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.ArrayList;
import java.util.List;
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
    public boolean getAddNull(long cfId, long projId)
    {
        String addNull = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(createPropKey(cfId, projId).concat(".addnull"));
        return Boolean.parseBoolean(addNull);
    }

    @Override
    public List<String> getLinkeFieldsOptions(
        long cfId,
        long projId)
    {
        String options = (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(createPropKey(cfId, projId).concat(".options"));
        if (options == null)
        {
            List<String> list = new ArrayList<String>();
            list.add("key");
            return list;
        }
        else
        {
            return Utils.stringToList(options);
        }
    }

    @Override
    public String getQueryFieldData(
        long cfId,
        long projId)
    {
        return (String)pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(createPropKey(cfId, projId));
    }

    @Override
    public void setAddNull(long cfId, long projId, boolean data)
    {
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(createPropKey(cfId, projId).concat(".addnull"), Boolean.toString(data));
    }

    @Override
    public void setLinkerFieldOptions(
        long cfId,
        long projId,
        List<String> options)
    {
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(createPropKey(cfId, projId).concat(".options"), Utils.listToString(options));
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

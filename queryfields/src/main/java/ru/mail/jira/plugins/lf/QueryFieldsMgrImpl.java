/*
 * Created by Andrey Markelov 29-08-2012. Copyright Mail.Ru Group 2012. All
 * rights reserved.
 */
package ru.mail.jira.plugins.lf;


import java.util.ArrayList;
import java.util.List;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;


/**
 * Implementation of <code>QueryFieldsMgr</code>.
 * 
 * @author Andrey Markelov
 */
public class QueryFieldsMgrImpl implements QueryFieldsMgr
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
     * Plug-In settings.
     */
    private final PluginSettings pluginSettings;

    /**
     * Constructor.
     */
    public QueryFieldsMgrImpl(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettings = pluginSettingsFactory
            .createSettingsForKey(PLUGIN_KEY);
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
        String addNull = (String) getPluginSettings().get(
            createPropKey(cfId, projId).concat(".addnull"));
        return Boolean.parseBoolean(addNull);
    }

    @Override
    public List<String> getLinkeFieldsOptions(long cfId, long projId)
    {
        String options = (String) getPluginSettings().get(
            createPropKey(cfId, projId).concat(".options"));
        if (options == null)
        {
            List<String> list = new ArrayList<String>();
            list.add("key");
            list.add("editKey");
            return list;
        }
        else
        {
            return Utils.stringToList(options);
        }
    }

    private synchronized PluginSettings getPluginSettings()
    {
        return pluginSettings;
    }

    @Override
    public String getQueryFieldData(long cfId, long projId)
    {
        return (String) getPluginSettings().get(createPropKey(cfId, projId));
    }

    @Override
    public void setAddNull(long cfId, long projId, boolean data)
    {
        getPluginSettings().put(createPropKey(cfId, projId).concat(".addnull"),
            Boolean.toString(data));
    }

    @Override
    public void setLinkerFieldOptions(long cfId, long projId,
        List<String> options)
    {
        getPluginSettings().put(createPropKey(cfId, projId).concat(".options"),
            Utils.listToString(options));
    }

    @Override
    public void setQueryFieldData(long cfId, long projId, String data)
    {
        getPluginSettings().put(createPropKey(cfId, projId), data);
    }

    @Override
    public boolean isAutocompleteView(long cfId, long projId)
    {
        String autocompleteView = (String) getPluginSettings().get(
            createPropKey(cfId, projId).concat(".autocompleteview"));
        return Boolean.parseBoolean(autocompleteView);
    }

    @Override
    public void setAutocompleteView(long cfId, long projId, boolean data)
    {
        getPluginSettings().put(
            createPropKey(cfId, projId).concat(".autocompleteview"),
            Boolean.toString(data));
    }

    @Override
    public boolean getQueryFlag(long cfId)
    {
        String queryFlag = (String) getPluginSettings().get(
            String.valueOf(cfId).concat(".queryflag"));
        return Boolean.parseBoolean(queryFlag);
    }

    @Override
    public void setQueryFlag(long cfId, boolean data)
    {
        getPluginSettings().put(
            String.valueOf(cfId).concat(".queryflag"),
            Boolean.toString(data));
    }

    @Override
    public String getQueryFieldSQLData(long cfId, long projId)
    {
        return (String) getPluginSettings().get(createPropKey(cfId, projId).concat(".sql"));
    }

    @Override
    public void setQueryFieldSQLData(long cfId, long projId, String data)
    {
        getPluginSettings().put(createPropKey(cfId, projId).concat(".sql"), data);
        
    }
}

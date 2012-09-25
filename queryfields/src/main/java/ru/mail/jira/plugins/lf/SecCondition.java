/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.Collection;
import java.util.Map;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

/**
 * Section condition.
 * 
 * @author Andrey Markelov
 */
public class SecCondition
    implements Condition
{
    /**
     * Plugin manager.
     */
    private final PluginAccessor plMgr;

    /**
     * Constructor.
     */
    public SecCondition(PluginAccessor plMgr)
    {
        this.plMgr = plMgr;
    }

    @Override
    public boolean shouldDisplay(
        Map<String, Object> context)
    {
        Collection<Plugin> pls = plMgr.getPlugins();
        if (pls != null && !pls.isEmpty())
        {
            for (Plugin pl : pls)
            {
                if (pl.getKey().startsWith("ru.mail") && !pl.getKey().equals("ru.mail.jira.plugins.lf.queryfields"))
                {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void init(Map<String, String> params)
    throws PluginParseException
    {
        
    }
}

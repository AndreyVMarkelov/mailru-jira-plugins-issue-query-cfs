/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

/**
 * PlugIn stored data manager.
 * 
 * @author Andrey Markelov
 */
public interface QueryFieldsMgr
{
    /**
     * Get stored data for the custom field.
     */
    String getQueryFieldData(long cfId, long projId);

    /**
     * Put data for the custom field.
     */
    void setQueryFieldData(long cfId, long projId, String data);
}

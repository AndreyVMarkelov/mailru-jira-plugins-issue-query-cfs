/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

/**
 * This structure keeps plugIn custom fields data for administration page rendering.
 * 
 * @author Andrey Markelov
 */
public class QueryFieldStruct
{
    /**
     * Custom field ID.
     */
    private long cfId;

    /**
     * Field data.
     */
    private String data;

    /**
     * Custom field description.
     */
    private String descr;

    /**
     * Custom field name.
     */
    private String name;

    /**
     * Project ID.
     */
    private long projectId;

    /**
     * Add null option.
     */
    private boolean isAddNull;

    /**
     * Project name.
     */
    private String projectName;

    /**
     * Constructor.
     */
    public QueryFieldStruct(
        long cfId,
        String name,
        String descr,
        long projectId,
        String projectName,
        String data,
        boolean isAddNull)
    {
        this.cfId = cfId;
        this.name = name;
        this.descr = descr;
        this.projectId = projectId;
        this.projectName = projectName;
        this.data = data;
        this.isAddNull = isAddNull;
    }

    public long getCfId()
    {
        return cfId;
    }

    public String getData()
    {
        return data;
    }

    public String getDescr()
    {
        return descr;
    }

    public String getName()
    {
        return name;
    }

    public long getProjectId()
    {
        return projectId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public boolean isAddNull()
    {
        return isAddNull;
    }

    @Override
    public String toString()
    {
        return "QueryFieldStruct[cfId=" + cfId + ", descr=" + descr
            + ", name=" + name + ", projectId=" + projectId
            + ", projectName=" + projectName + ", data=" + data + ", isAddNull=" + isAddNull + "]";
    }
}

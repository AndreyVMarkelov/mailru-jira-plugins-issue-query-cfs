/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.List;

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
     * Add null option.
     */
    private boolean isAddNull;

    /**
     * Custom field name.
     */
    private String name;

    /**
     * Options.
     */
    private List<String> options;

    /**
     * Project ID.
     */
    private long projectId;

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
        boolean isAddNull,
        List<String> options)
    {
        this.cfId = cfId;
        this.name = name;
        this.descr = descr;
        this.projectId = projectId;
        this.projectName = projectName;
        this.data = data;
        this.isAddNull = isAddNull;
        this.options = options;
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

    public List<String> getOptions()
    {
        return options;
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
    public String toString() {
        return "QueryFieldStruct[cfId=" + cfId + ", data=" + data + ", descr="
            + descr + ", name=" + name + ", projectId=" + projectId + ", isAddNull="
            + isAddNull + ", options=" + options + ", projectName=" + projectName + "]";
    }
}

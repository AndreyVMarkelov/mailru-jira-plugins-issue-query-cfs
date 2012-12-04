/*
 * Created by Andrey Markelov 04-12-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf.struct;

/**
 * Issue data for view.
 *
 * @author Andrey Markelov
 */
public class IssueData
{
    /**
     * Details.
     */
    private String details;

    /**
     * Issue summary.
     */
    private String summary;

    /**
     * Constructor.
     */
    public IssueData(String summary, String details)
    {
        this.summary = summary;
        this.details = details;
    }

    public String getDetails()
    {
        return details;
    }

    public String getFullData()
    {
        return summary + details;
    }

    public String getSummary()
    {
        return summary;
    }

    @Override
    public String toString()
    {
        return "IssueData[summary=" + summary + ", details=" + details + "]";
    }
}

/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

/**
 * PlugIn constants.
 * 
 * @author Andrey Markelov
 */
public interface Consts
{
    /**
     * Project ID for global custom field.
     */
    long PROJECT_ID_FOR_GLOBAL_CF = -1;

    /**
     * Project name for global custom field.
     */
    String PROJECT_NAME_FOR_GLOBAL_CF = "";

    /**
     * Reverse linking part.
     */
    String REVERSE_LINK_PART = "rlink|";

    /**
     * Test JQL clause for reverve linked field.
     */
    String TEST_QUERY_PATTERN = "project = %s AND %s is not EMPTY";

    /**
     * Reverse linking JQL clause for reverve linked field.
     */
    String RLINK_QUERY_PATTERN = "project = %s AND %s = %s";
}

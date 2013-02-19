/*
 * Created by Andrey Markelov 29-08-2012. Copyright Mail.Ru Group 2012. All
 * rights reserved.
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
     * Link to current issue
     */
    String ISSUE_RLINK = "RLINK";

    /**
     * Test JQL clause for reverve linked field.
     */
    String TEST_QUERY_PATTERN = "project = %s AND %s is not EMPTY";

    /**
     * Reverse linking JQL clause for reverve linked field.
     */
    String RLINK_QUERY_PATTERN = "project = %s AND %s = %s";

    /**
     * Empty list value
     */
    String EMPTY_VALUE = "";

    String CF_KEY_QUERY_LINKER_MULTI_FIELD = "ru.mail.jira.plugins.lf.queryfields:mailru-multi-linker-field";

    String CF_KEY_QUERY_LINKER_FIELD = "ru.mail.jira.plugins.lf.queryfields:mailru-linker-field";

    String CF_KEY_QUERY_LINKED_FIELD = "ru.mail.jira.plugins.lf.queryfields:mailru-linked-field";

    String LANG_TYPE_SQL = "SQL";

    String LANG_TYPE_JQL = "JQL";

    String SQL_RLINK = "#RLINK";
    
    String SQL_PATTERN = "#PATTERN";
    
    String SQL_ROWNUM = "#ROWNUM";
}
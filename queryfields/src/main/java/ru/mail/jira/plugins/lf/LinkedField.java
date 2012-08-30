/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.Map;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.impl.TextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;

/**
 * 
 * 
 * @author Andrey Markelov
 */
public class LinkedField
    extends TextCFType
    implements SortableCustomField<String>
{
    /**
     * Search request service.
     */
    private final SearchRequestService srMgr;

    /**
     * Constructor.
     */
    public LinkedField(
        CustomFieldValuePersister customFieldValuePersister,
        GenericConfigManager genericConfigManager,
        SearchRequestService srMgr)
    {
        super(customFieldValuePersister, genericConfigManager);
        this.srMgr = srMgr;
    }

    @Override
    public int compare(
        String customFieldObjectValue1,
        String customFieldObjectValue2,
        FieldConfig fieldConfig)
    {
        return super.compare(customFieldObjectValue1, customFieldObjectValue2, fieldConfig);
    }

    @Override
    public Map<String, Object> getVelocityParameters(
        Issue issue,
        CustomField field,
        FieldLayoutItem fieldLayoutItem)
    {
        return super.getVelocityParameters(issue, field, fieldLayoutItem);
    }
}

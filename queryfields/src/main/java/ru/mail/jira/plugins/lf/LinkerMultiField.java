/*
 * Created by Andrey Markelov 05-10-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import static java.util.Collections.emptySet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import ru.mail.jira.plugins.lf.struct.IssueData;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.imports.project.customfield.ProjectCustomFieldImporter;
import com.atlassian.jira.imports.project.customfield.ProjectImportableCustomField;
import com.atlassian.jira.imports.project.customfield.SelectCustomFieldImporter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.customfields.GroupSelectorField;
import com.atlassian.jira.issue.customfields.MultipleCustomFieldType;
import com.atlassian.jira.issue.customfields.MultipleSettableCustomFieldType;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.config.item.SettableOptionsConfigItem;
import com.atlassian.jira.issue.customfields.impl.AbstractMultiSettableCFType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.customfields.statistics.SelectStatisticsMapper;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigItemType;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Multi linker field.
 * 
 * @author Andrey V. Markelov
 */
public class LinkerMultiField
    extends AbstractMultiSettableCFType
    implements MultipleSettableCustomFieldType,
               MultipleCustomFieldType,
               SortableCustomField<List<String>>,
               GroupSelectorField,
               ProjectImportableCustomField
{
    /**
     * Issue manager.
     */
    private final IssueManager issueMgr;

    private final ProjectCustomFieldImporter projectCustomFieldImporter;

	/**
     * PlugIn data manager.
     */
    private final QueryFieldsMgr qfMgr;

    /**
     * Search service.
     */
    private final SearchService searchService;
    
    private final ApplicationProperties applicationProperties;

	/**
     * Constructor.
     */
    public LinkerMultiField(
        final OptionsManager optionsManager,
        final CustomFieldValuePersister valuePersister,
        final GenericConfigManager genericConfigManager,
        QueryFieldsMgr qfMgr,
        SearchService searchService,
        IssueManager issueMgr, ApplicationProperties applicationProperties)
    {
        super(optionsManager, valuePersister, genericConfigManager);
        projectCustomFieldImporter = new SelectCustomFieldImporter(optionsManager);
        this.qfMgr = qfMgr;
        this.searchService = searchService;
        this.issueMgr = issueMgr;
        this.applicationProperties = applicationProperties;
    }

    public int compare(
        final List<String> customFieldObjectValue1,
        final List<String> customFieldObjectValue2,
        final FieldConfig fieldConfig)
    {
        final Options options = getOptions(fieldConfig, null);

        if (options != null)
        {
            final Long i1 = getLowestIndex(customFieldObjectValue1, options);
            final Long i2 = getLowestIndex(customFieldObjectValue2, options);

            return i1.compareTo(i2);
        }

        return 0;
    }

    public void createValue(
        final CustomField customField,
        final Issue issue,
        final Object value)
    {
        customFieldValuePersister.createValues(customField, issue.getId(), PersistenceFieldType.TYPE_LIMITED_TEXT, (Collection<?>) value);
    }

    public String getChangelogValue(
        final CustomField field,
        final Object value)
    {
        if (value != null)
        {
            return value.toString();
        }
        else
        {
            return "";
        }
    }

    @Override
    public List<FieldConfigItemType> getConfigurationItemTypes()
    {
        final List<FieldConfigItemType> configurationItemTypes = super.getConfigurationItemTypes();
        configurationItemTypes.add(new SettableOptionsConfigItem(this, optionsManager));
        return configurationItemTypes;
    }

    public Object getDefaultValue(
        final FieldConfig fieldConfig)
    {
        return genericConfigManager.retrieve(CustomFieldType.DEFAULT_VALUE_TYPE, fieldConfig.getId().toString());
    }

    public Set<Long> getIssueIdsWithValue(
        final CustomField field,
        final Option option)
    {
        if (option != null)
        {
            return customFieldValuePersister.getIssueIdsWithValue(field, PersistenceFieldType.TYPE_LIMITED_TEXT, option.getValue());
        }
        else
        {
            return emptySet();
        }
    }

    private Long getLowestIndex(
        final List<String> l,
        final Options options)
    {
        Long lowest = new Long(Long.MAX_VALUE);

        for (final String name : l)
        {
            final Option o = options.getOptionForValue(name, null);
            if ((o != null) && (o.getSequence() != null) && (o.getSequence().compareTo(lowest) < 0))
            {
                lowest = o.getSequence();
            }
        }

        return lowest;
    }

    public ProjectCustomFieldImporter getProjectImporter()
    {
        return projectCustomFieldImporter;
    }

    public Query getQueryForGroup(
        final String fieldName,
        final String groupName)
    {
        return new TermQuery(new Term(fieldName + SelectStatisticsMapper.RAW_VALUE_SUFFIX, groupName));
    }

    public Object getSingularObjectFromString(
        final String string)
    throws FieldValidationException
    {
        return string;
    }

    public String getStringFromSingularObject(
        final Object singularObject)
    {
        assertObjectImplementsType(String.class, singularObject);
        return (String) singularObject;
    }

    public Object getStringValueFromCustomFieldParams(
        final CustomFieldParams parameters)
    {
        return parameters.getValuesForNullKey();
    }

    public Object getValueFromCustomFieldParams(
        final CustomFieldParams parameters)
    throws FieldValidationException
    {
        final Collection<?> values = parameters.getAllValues();
        if (CustomFieldUtils.isCollectionNotEmpty(values))
        {
            return values;
        }
        else
        {
            return null;
        }
    }

    public Object getValueFromIssue(
        final CustomField field,
        final Issue issue)
    {
        final List<?> values = customFieldValuePersister.getValues(field, issue.getId(), PersistenceFieldType.TYPE_LIMITED_TEXT);
        if ((values == null) || values.isEmpty())
        {
            return null;
        }
        else
        {
            return values;
        }
    }

    @Override
    @NotNull
    public Map<String, Object> getVelocityParameters(
        Issue issue,
        CustomField field,
        FieldLayoutItem fieldLayoutItem)
    {
        Map<String, Object> params = super.getVelocityParameters(issue, field, fieldLayoutItem);
        params.put("baseUrl", applicationProperties.getBaseUrl());

        String jqlData = null;
        boolean addNull = false;
        boolean isAutocompleteView = false;
        List<String> options = null;
        if (field.isAllProjects())
        {
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
            addNull = qfMgr.getAddNull(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
            isAutocompleteView = qfMgr.isAutocompleteView(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
            options = qfMgr.getLinkeFieldsOptions(field.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF);
        }
        else
        {
            if (issue == null)
            {
                return params;
            }
            jqlData = qfMgr.getQueryFieldData(field.getIdAsLong(), issue.getProjectObject().getId());
            addNull = qfMgr.getAddNull(field.getIdAsLong(), issue.getProjectObject().getId());
            isAutocompleteView = qfMgr.isAutocompleteView(field.getIdAsLong(), issue.getProjectObject().getId());
            options = qfMgr.getLinkeFieldsOptions(field.getIdAsLong(), issue.getProjectObject().getId());
        }
        params.put("isAutocompleteView", isAutocompleteView);

        Map<String, IssueData> setVals = new LinkedHashMap<String, IssueData>();
        List<String> selVals = (List<String>)issue.getCustomFieldValue(field);
        if (selVals != null)
        {
            for (String selVal : selVals)
            {
                MutableIssue mi = issueMgr.getIssueObject(selVal);
                if (mi != null && Utils.isValidStr(mi.getSummary()))
                {
                    StringBuilder sb = new StringBuilder();
                    if (options.contains("status"))
                    {
                        sb.append(getI18nBean().getText("queryfields.opt.status")).append(": ").append(mi.getStatusObject().getName());
                    }
                    if (options.contains("assignee") && mi.getAssigneeUser() != null)
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(", ");
                        }
                        User aUser = mi.getAssigneeUser();
                        String encodedUser;
                        try
                        {
                            encodedUser = URLEncoder.encode(aUser.getName(), "UTF-8");
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            //--> impossible
                            encodedUser = aUser.getName();
                        }

                        sb.append(getI18nBean().getText("queryfields.opt.assignee")).append(": ")
                            .append("<a class='user-hover' rel='").append(aUser.getName()).append("' id='issue_summary_assignee_'")
                            .append(aUser.getName()).append("' href='/secure/ViewProfile.jspa?name='").append(encodedUser)
                            .append("'>").append(aUser.getDisplayName()).append("</a>");
                    }
                    if (options.contains("priority") && mi.getPriorityObject() != null)
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(", ");
                        }
                        sb.append(getI18nBean().getText("queryfields.opt.priority")).append(": ").append(mi.getPriorityObject().getName());
                    }
                    if (options.contains("due") && mi.getDueDate() != null)
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(", ");
                        }
                        sb.append(getI18nBean().getText("queryfields.opt.due")).append(": ").append(ComponentAccessor.getJiraAuthenticationContext().getOutlookDate().format(mi.getDueDate()));
                    }

                    if (sb.length() > 0)
                    {
                        sb.insert(0, " (");
                        sb.append(")");
                    }
                    IssueData issueData;
                    if (options.contains("key"))
                    {
                        issueData = new IssueData(mi.getKey().concat(":").concat(mi.getSummary()), sb.toString());
                    }
                    else
                    {
                        issueData = new IssueData(mi.getSummary(), sb.toString());
                    }
                    setVals.put(selVal, issueData);
                }
            }
        }
        params.put("setVals", setVals);
        
        ArrayList<Issue> selectedIssues = new ArrayList<Issue>(setVals.size());
        for (String issueKey : setVals.keySet())
        {
            Issue anIssue = issueMgr.getIssueObject(issueKey);
            if (anIssue != null)
            {
                selectedIssues.add(anIssue);
            }
        }
        params.put("selIssues", selectedIssues);

        if (!Utils.isValidStr(jqlData))
        {
            params.put("jqlNotSet", Boolean.TRUE);
            return params;
        }
        params.put("jqlNotSet", Boolean.FALSE);
        params.put("options", options);

        if (options.contains("editKey"))
        {
            params.put("hasKey", Boolean.TRUE);
        }

        User user = ComponentManager.getInstance().getJiraAuthenticationContext().getLoggedInUser();
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlData);
        if (parseResult.isValid())
        {
            params.put("jqlNotValid", Boolean.FALSE);
            com.atlassian.query.Query query = parseResult.getQuery();
            try
            {
                Map<String, String> cfVals = new LinkedHashMap<String, String>();
                SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
                List<Issue> issues = results.getIssues();
                for (Issue i : issues)
                {
                    cfVals.put(i.getKey(), i.getSummary());
                }

                if (addNull)
                {
                    cfVals.put("Empty", Consts.EMPTY_VALUE);
                }


                params.put("isError", Boolean.FALSE);
                params.put("cfVals", cfVals);
            }
            catch (SearchException e)
            {
                params.put("isError", Boolean.TRUE);
            }
        }
        else
        {
            params.put("jqlNotValid", Boolean.TRUE);
            return params;
        }

        return params;
    }

    public void removeValue(
        final CustomField field,
        final Issue issue,
        final Option option)
    {
        if (option != null)
        {
            customFieldValuePersister.removeValue(field, issue.getId(), PersistenceFieldType.TYPE_LIMITED_TEXT, option.getValue());
        }
    }

    public void setDefaultValue(
        final FieldConfig fieldConfig,
        final Object value)
    {
        Collection<?> values = (Collection<?>) value;

        if ((values != null) && (values.size() == 1))
        {
            final Object oFirstItem = values.iterator().next();
            if ("-1".equals(oFirstItem))
            {
                values = Collections.EMPTY_LIST;
            }
        }

        genericConfigManager.update(CustomFieldType.DEFAULT_VALUE_TYPE, fieldConfig.getId().toString(), values);
    }

    public void updateValue(
        final CustomField customField,
        final Issue issue,
        final Object value)
    {
        customFieldValuePersister.updateValues(customField, issue.getId(), PersistenceFieldType.TYPE_LIMITED_TEXT, (Collection<?>) value);
    }

    public void validateFromParams(
        final CustomFieldParams relevantParams,
        final ErrorCollection errorCollectionToAddTo,
        final FieldConfig config)
    {
        @SuppressWarnings("unchecked")
        final Collection<String> params = relevantParams.getAllValues();
        if ((params == null) || params.isEmpty())
        {
            return;
        }
    }

    @Override
    public boolean valuesEqual(
        final Object v1,
        final Object v2)
    {
        if (v1 == v2)
        {
            return true;
        }

        if ((v1 == null) || (v2 == null))
        {
            return false;
        }

        if ((v1 instanceof Collection) && (v2 instanceof Collection))
        {
            return CollectionUtils.isEqualCollection((Collection<?>) v1, (Collection<?>) v2);
        }
        {
            return v1.equals(v2);
        }
    }
}

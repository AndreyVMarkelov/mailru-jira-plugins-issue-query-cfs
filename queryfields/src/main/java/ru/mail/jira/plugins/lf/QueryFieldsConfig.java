/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.List;
import org.ofbiz.core.entity.GenericValue;
import ru.mail.jira.plugins.lf.struct.CfData;
import ru.mail.jira.plugins.lf.struct.QueryFieldStruct;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Administration page of query linking custom fields.
 *
 * @author Andrey Markelov
 */
public class QueryFieldsConfig
    extends JiraWebActionSupport
{
    /**
     * Unique ID.
     */
    private static final long serialVersionUID = 5304304911404478635L;

    /**
     * Application properties.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * Date.
     */
    private final CfData cfData;

    /**
     * Constructor.
     */
    public QueryFieldsConfig(
        ApplicationProperties applicationProperties,
        CustomFieldManager cfMgr,
        QueryFieldsMgr qfMgr)
    {
        this.applicationProperties = applicationProperties;
        this.cfData = new CfData();

        List<CustomField> cgList = cfMgr.getCustomFieldObjects();
        for (CustomField cf : cgList)
        {
            if (cf.getCustomFieldType().getKey().equals("ru.mail.jira.plugins.lf.queryfields:mailru-linker-field"))
            {
                if (cf.isAllProjects())
                {
                    QueryFieldStruct qfs = new QueryFieldStruct(
                        cf.getIdAsLong(),
                        cf.getName(),
                        cf.getDescription(),
                        Consts.PROJECT_ID_FOR_GLOBAL_CF,
                        Consts.PROJECT_NAME_FOR_GLOBAL_CF,
                        qfMgr.getQueryFieldData(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF),
                        qfMgr.getAddNull(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF),
                        qfMgr.getLinkeFieldsOptions(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF));
                    cfData.addLinkerField(qfs);
                }
                else
                {
                    List<GenericValue> projs = cf.getAssociatedProjects();
                    for (GenericValue proj : projs)
                    {
                        Long projId = (Long) proj.get("id");
                        String projName = (String) proj.get("name");

                        QueryFieldStruct qfs = new QueryFieldStruct(
                            cf.getIdAsLong(),
                            cf.getName(),
                            cf.getDescription(),
                            projId,
                            projName,
                            qfMgr.getQueryFieldData(cf.getIdAsLong(), projId),
                            qfMgr.getAddNull(cf.getIdAsLong(), projId),
                            qfMgr.getLinkeFieldsOptions(cf.getIdAsLong(), projId));
                        cfData.addLinkerField(qfs);
                    }
                }
            }
            else if (cf.getCustomFieldType().getKey().equals("ru.mail.jira.plugins.lf.queryfields:mailru-linked-field"))
            {
                if (cf.isAllProjects())
                {
                    QueryFieldStruct qfs = new QueryFieldStruct(
                        cf.getIdAsLong(),
                        cf.getName(),
                        cf.getDescription(),
                        Consts.PROJECT_ID_FOR_GLOBAL_CF,
                        Consts.PROJECT_NAME_FOR_GLOBAL_CF,
                        qfMgr.getQueryFieldData(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF),
                        qfMgr.getAddNull(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF),
                        qfMgr.getLinkeFieldsOptions(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF));
                    cfData.addLinkedField(qfs);
                }
                else
                {
                    List<GenericValue> projs = cf.getAssociatedProjects();
                    for (GenericValue proj : projs)
                    {
                        Long projId = (Long) proj.get("id");
                        String projName = (String) proj.get("name");

                        QueryFieldStruct qfs = new QueryFieldStruct(
                            cf.getIdAsLong(),
                            cf.getName(),
                            cf.getDescription(),
                            projId,
                            projName,
                            qfMgr.getQueryFieldData(cf.getIdAsLong(), projId),
                            qfMgr.getAddNull(cf.getIdAsLong(), projId),
                            qfMgr.getLinkeFieldsOptions(cf.getIdAsLong(), projId));
                        cfData.addLinkedField(qfs);
                    }
                }
            }
            else if (cf.getCustomFieldType().getKey().equals("ru.mail.jira.plugins.lf.queryfields:mailru-multi-linker-field"))
            {
                if (cf.isAllProjects())
                {
                    QueryFieldStruct qfs = new QueryFieldStruct(
                        cf.getIdAsLong(),
                        cf.getName(),
                        cf.getDescription(),
                        Consts.PROJECT_ID_FOR_GLOBAL_CF,
                        Consts.PROJECT_NAME_FOR_GLOBAL_CF,
                        qfMgr.getQueryFieldData(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF),
                        qfMgr.getAddNull(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF),
                        qfMgr.getLinkeFieldsOptions(cf.getIdAsLong(), Consts.PROJECT_ID_FOR_GLOBAL_CF));
                    cfData.addMultiFields(qfs);
                }
                else
                {
                    List<GenericValue> projs = cf.getAssociatedProjects();
                    for (GenericValue proj : projs)
                    {
                        Long projId = (Long) proj.get("id");
                        String projName = (String) proj.get("name");

                        QueryFieldStruct qfs = new QueryFieldStruct(
                            cf.getIdAsLong(),
                            cf.getName(),
                            cf.getDescription(),
                            projId,
                            projName,
                            qfMgr.getQueryFieldData(cf.getIdAsLong(), projId),
                            qfMgr.getAddNull(cf.getIdAsLong(), projId),
                            qfMgr.getLinkeFieldsOptions(cf.getIdAsLong(), projId));
                        cfData.addMultiFields(qfs);
                    }
                }
            }
        }
    }

    /**
     * Get context path.
     */
    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }

    /**
     * Get plugIn data.
     */
    public CfData getCfData()
    {
        return cfData;
    }

    /**
     * Check administer permissions.
     */
    public boolean hasAdminPermission()
    {
        User user = getLoggedInUser();
        if (user == null)
        {
            return false;
        }

        if (getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser()))
        {
            return true;
        }

        return false;
    }
}

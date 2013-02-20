/*
 * Created by Dmitry Miroshnichenko 28-11-2012. Copyright Mail.Ru Group 2012.
 * All rights reserved.
 */
package ru.mail.jira.plugins.lf;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;

import ru.mail.jira.plugins.lf.struct.AutocompleteUniversalData;
import ru.mail.jira.plugins.lf.struct.ISQLDataBean;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.util.I18nHelper;


@Path("/queryautocompsrv")
public class QueryAutocompleteService
{
    private final Logger log = Logger.getLogger(QueryAutocompleteService.class);

    private final QueryFieldsMgr qfMgr;

    public QueryAutocompleteService(QueryFieldsMgr qfMgr)
    {
        this.qfMgr = qfMgr;
    }

    @POST
    @Path("/getcfvals")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCfVals(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance()
            .getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryAutocompleteService::getCfVals - User is not logged");
            return Response
                .ok(i18n.getText("queryfields.service.user.notlogged"))
                .status(401).build();
        }

        String cfId = req.getParameter("cf_id");
        String issueKey = req.getParameter("issue_id");
        String pattern = req.getParameter("pattern");
        String rowCount = req.getParameter("rowcount");

        AutocompleteUniversalData data;
        List<ISQLDataBean> values = null;

        if (Utils.isValidStr(cfId) && issueKey != null
            && Utils.isValidLongParam(rowCount))
        {
            CustomField cf = ComponentManager.getInstance()
                .getCustomFieldManager().getCustomFieldObject(cfId);
            if (cf == null)
            {
                log.error("QueryAutocompleteService::getCfVals - Custom field is null. Incorrect data in plugin settings");
                return Response
                    .ok(i18n.getText("queryfields.service.error.cfid.invalid"))
                    .status(400).build();
            }
            UserProjectHistoryManager userProjectHistoryManager = ComponentManager
                .getComponentInstanceOfType(UserProjectHistoryManager.class);
            Project currentProject = userProjectHistoryManager
                .getCurrentProject(Permissions.BROWSE,
                    authCtx.getLoggedInUser());
            if (currentProject == null)
            {
                log.error("QueryAutocompleteService::getCfVals - Current project is null");
                return Response
                    .ok(i18n.getText("queryfields.service.error.curproject"))
                    .status(400).build();
            }

            boolean queryFlag = qfMgr.getQueryFlag(cf.getIdAsLong());
            if (Consts.LANG_TYPE_SQL.equals(Utils.getKeyByQueryFlag(queryFlag)))
            {
                long projectId;
                if (cf.isAllProjects())
                {
                    projectId = Consts.PROJECT_ID_FOR_GLOBAL_CF;
                }
                else
                {
                    projectId = currentProject.getId();
                }
                
                String preparedQuery = qfMgr.getQueryFieldSQLData(
                    cf.getIdAsLong(), projectId);
                if (Utils.isValidStr(preparedQuery))
                {
                    preparedQuery = preparedQuery.replaceAll(Consts.SQL_RLINK,
                        issueKey);
                    preparedQuery = preparedQuery.replaceAll(
                        Consts.SQL_PATTERN, pattern);
                    preparedQuery = preparedQuery.replaceAll(Consts.SQL_ROWNUM,
                        rowCount);

                    values = Utils.executeSQLQuery(preparedQuery,
                        AutocompleteUniversalData.class);
                }
            }
            else
            {
                String jqlData;
                if (cf.isAllProjects())
                {
                    jqlData = qfMgr.getQueryFieldData(cf.getIdAsLong(),
                        Consts.PROJECT_ID_FOR_GLOBAL_CF);
                }
                else
                {
                    jqlData = qfMgr.getQueryFieldData(cf.getIdAsLong(),
                        currentProject.getId());
                }

                List<Issue> issues = Utils.executeJQLQuery(jqlData);
                values = new ArrayList<ISQLDataBean>(issues.size());
                for (Issue issue : issues)
                {
                    String icon;
                    data = new AutocompleteUniversalData();
                    data.setName(issue.getKey());
                    data.setDescription(issue.getSummary());
                    if (issue.getIssueTypeObject() != null)
                    {
                        data.setType(issue.getIssueTypeObject().getName());
                        icon = issue.getIssueTypeObject().getIconUrl();
                        if (Utils.isValidStr(icon))
                        {
                            data.setTypeimage(icon);
                        }
                    }

                    if (issue.getStatusObject() != null)
                    {
                        data.setState(issue.getStatusObject().getName());
                    }

                    values.add(data);
                }
            }
        }
        else
        {
            log.error("QueryAutocompleteService::getCfVals - Incorrect parameters");
            return Response
                .ok(i18n
                    .getText("queryfields.service.error.parameters.invalid"))
                .status(400).build();
        }

        Response resp;
        if (values != null)
        {
            GenericEntity<List<ISQLDataBean>> retVal = new GenericEntity<List<ISQLDataBean>>(
                values)
            {
            };

            resp = Response.ok().entity(retVal).status(200).build();
        }
        else
        {
            resp = Response.ok().status(200).build();
        }

        return resp;
    }

    @POST
    @Path("/getissuetemplate")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getIssueTemplate(@Context HttpServletRequest req)
    {
        JiraAuthenticationContext authCtx = ComponentManager.getInstance()
            .getJiraAuthenticationContext();
        I18nHelper i18n = authCtx.getI18nHelper();
        User user = authCtx.getLoggedInUser();
        if (user == null)
        {
            log.error("QueryAutocompleteService::validateInput - User is not logged");
            return Response
                .ok(i18n.getText("queryfields.service.user.notlogged"))
                .status(401).build();
        }

        String cfId = req.getParameter("cf_id");
        String cfValue = req.getParameter("cf_value");

        if (!Utils.isValidStr(cfId) || !Utils.isValidStr(cfValue))
        {
            log.error("QueryAutocompleteService::validateInput - Invalid parameters");
            return Response
                .ok(i18n
                    .getText("queryfields.service.error.parameters.invalid"))
                .status(400).build();
        }

        CustomField cf = ComponentManager.getInstance().getCustomFieldManager()
            .getCustomFieldObject(cfId);
        if (cf == null)
        {
            log.error("QueryAutocompleteService::validateInput - Custom field is null. Incorrect data in plugin settings");
            return Response
                .ok(i18n.getText("queryfields.service.error.cfid.invalid"))
                .status(400).build();
        }

        AutocompleteUniversalData entity = new AutocompleteUniversalData();
        if (Utils.isOfQueryMultiSelectType(cf.getCustomFieldType().getKey()))
        {
            Issue issue = ComponentManager.getInstance().getIssueManager()
                .getIssueObject(cfValue);
            if (issue == null)
            {
                // nothing to do. Sending object with empty key
            }
            else
            {
                String icon;
                entity.setName(issue.getKey());
                if (issue.getIssueTypeObject() != null)
                {
                    entity.setType(issue.getIssueTypeObject().getName());
                    icon = issue.getIssueTypeObject().getIconUrl();
                    if (Utils.isValidStr(icon))
                    {
                        entity.setTypeimage(icon);
                    }
                }
                entity.setDescription(issue.getSummary());

                if (issue.getStatusObject() != null)
                {
                    entity.setState(issue.getStatusObject().getName());
                    icon = issue.getStatusObject().getIconUrl();
                    if (Utils.isValidStr(icon))
                    {
                        entity.setStateimage(icon);
                    }
                }

                if (issue.getPriorityObject() != null)
                {
                    entity.setPreference(issue.getPriorityObject().getName());
                    icon = issue.getPriorityObject().getIconUrl();
                    if (Utils.isValidStr(icon))
                    {
                        entity.setPreferenceimage(icon);
                    }
                }

                Map<String, Object> params = new HashMap<String, Object>();
                params.put("cfid", cfId);
                params.put("baseUrl", Utils.getBaseUrl(req));
                params.put("data", entity);
                params.put("i18n", i18n);

                try
                {
                    String body = ComponentAccessor.getVelocityManager()
                        .getBody("templates/", "edit-issue-representation.vm",
                            params);
                    return Response.ok(new HtmlEntity(body)).build();
                }
                catch (VelocityException vex)
                {
                    log.error(
                        "QueryAutocompleteService::manageInput - Velocity parsing error",
                        vex);
                    return Response
                        .ok(i18n
                            .getText("queryfields.service.velocity.parseerror"))
                        .status(500).build();
                }
            }
        }

        return Response.ok().status(200).build();
    }
}
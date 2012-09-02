// Created by Andrey Markelov 29-08-2012.
// Copyright Mail.Ru Group 2012. All rights reserved.

jQuery(document).ready(function() {
    jQuery(window).bind('beforeunload', function() {
        return null;
    });
});

//--> initialize dialog
function initJqlDlg(baseUrl, cfId, prId)
{
    var res = "";
    jQuery.ajax({
        url: baseUrl + "/rest/queryfieldsws/1.0/queryfieldssrv/initjqldlg",
        type: "POST",
        dataType: "json",
        data: {"cfId": cfId, "prId": prId},
        async: false,
        error: function(xhr, ajaxOptions, thrownError) {
            try {
                var respObj = eval("(" + xhr.responseText + ")");
                initErrorDlg(respObj.message).show();
            } catch(e) {
                initErrorDlg(xhr.responseText).show();
            }
        },
        success: function(result) {
            res = result.html;
        }
    });

    return res;
}

//--> initialize error dialog
function initErrorDlg(bodyText) {
    var errorDialog = new AJS.Dialog({
        width:420,
        height:250,
        id:"error-dialog",
        closeOnOutsideClick: true
    });

    errorDialog.addHeader(AJS.I18n.getText("queryfields.admin.title.error"));
    errorDialog.addPanel("ErrorMainPanel", '' +
        '<html><body><div class="error-message errdlg">' +
        bodyText +
        '</div></body></html>',
        "error-panel-body");
    errorDialog.addCancel(AJS.I18n.getText("queryfields.closebtn"), function() {
        errorDialog.hide();
    });

    return errorDialog;
}

//--> configure Jql for plugIn custom field
function configureJql(event, baseUrl, cfId, prId) {
    event.preventDefault();

    var dialogBody = initJqlDlg(baseUrl, cfId, prId);
    if (!dialogBody)
    {
        return;
    }

    jQuery("#configure_jql_dialog").remove();
    var md = new AJS.Dialog({
        width:550,
        height:350,
        id:"configure_jql_dialog",
        closeOnOutsideClick: true
    });
    md.addHeader(AJS.I18n.getText("queryfields.configjqltitle"));
    md.addPanel("load_panel", dialogBody);
    md.addButton(AJS.I18n.getText("queryfields.applyjqlbtn"), function() {
        jQuery.ajax({
            url: baseUrl + "/rest/queryfieldsws/1.0/queryfieldssrv/setjql",
            type: "POST",
            dataType: "json",
            data: AJS.$("#setjqlform").serialize(),
            async: false,
            error: function(xhr, ajaxOptions, thrownError) {
                var errText;
                try {
                    var respObj = eval("(" + xhr.responseText + ")");
                    if (respObj.message) {
                        errText = respObj.message;
                    } else if (respObj.html) {
                        errText = respObj.html;
                    } else {
                        errText = xhr.responseText;
                    }
                } catch(e) {
                    errText = xhr.responseText;
                }
                jQuery("#errorpart").empty();
                jQuery("#errorpart").append("<div class='errdiv'>" + errText + "</div>");
            },
            success: function(result) {
                document.location.reload(true);
            }
        });
    });
    md.addCancel(AJS.I18n.getText("queryfields.closebtn"), function() {
        md.hide();
    });
    md.show();
}

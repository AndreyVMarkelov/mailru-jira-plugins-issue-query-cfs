// Created by Andrey Markelov 29-08-2012.
// Copyright Mail.Ru Group 2012. All rights reserved.

//--> initialize dialog
function initJclDlg(baseUrl, cfId, prId)
{
    var res = "";
    jQuery.ajax({
        url: baseUrl + "/rest/queryfieldsws/1.0/queryfieldssrv/initjcldlg",
        type: "POST",
        dataType: "json",
        data: {"cfId": cfId, "prId": prId},
        async: false,
        error: function(xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
        },
        success: function(result) {
            res = result.html;
        }
    });

    return res;
}

//--> configure Jcl for plugIn custom field
function configureJcl(event, baseUrl, cfId, prId) {
    event.preventDefault();

    var dialogBody = initJclDlg(baseUrl, cfId, prId);
    if (!dialogBody)
    {
        return;
    }

    var md = new AJS.Dialog({
        width:680,
        height:520,
        id:"add_calendar_dialog",
        closeOnOutsideClick: true
    });
    md.addHeader(AJS.I18n.getText("mailrucal.createcaltitle"));
    md.addPanel("load_panel", dialogBody);
    md.addButton(AJS.I18n.getText("mailrucal.addcalbtn"), function() {
        AJS.$("#setjclform").submit();
    });
    md.addCancel(AJS.I18n.getText("mailrucal.closebtn"), function() {
        AJS.$("#setjclform").remove();
        md.hide();
    });
    md.show();
}

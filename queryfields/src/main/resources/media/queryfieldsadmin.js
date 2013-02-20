// Created by Andrey Markelov 29-08-2012.
// Copyright Mail.Ru Group 2012. All rights reserved.

jQuery(document).ready(function() {
    jQuery(window).bind('beforeunload', function() {
        
    });
    jQuery('#query-inline-sql-help-dialog').bind('click', function() {
		var md = new AJS.Dialog({
			width : 800,
			height : 600,
			id : "query-sql-help-popup-dialog",
			closeOnOutsideClick : true
		});
		md.addHeader(AJS.I18n.getText("queryfields.sql.help.title"));
		md.addPanel("main_panel", queryCfsGetSqlHelp(jQuery('#query-inline-sql-help-dialog').attr('base-url')));
		md.addCancel(AJS.I18n.getText("queryfields.sql.help.close"), function() {
			md.hide();
		});
		md.show();
	});
});

function queryCfsGetSqlHelp(baseUrl) {
	var res = "";

	jQuery.ajax({
		url : baseUrl + "/rest/queryfieldsws/1.0/queryfieldssrv/sqlhelp",
		type : "POST",
		dataType : "json",
		async : false,
		error : function(xhr, ajaxOptions, thrownError) {
			try {
				var respObj = eval("(" + xhr.responseText + ")");
				initErrorDlg(respObj.message).show();
			} catch (e) {
				initErrorDlg(xhr.responseText).show();
			}
		},
		success : function(result) {
			res = result.html;
		}
	});

	return res;
};

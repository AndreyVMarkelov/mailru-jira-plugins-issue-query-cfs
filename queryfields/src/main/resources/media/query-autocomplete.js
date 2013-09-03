/* Created by Andrey Markelov 11-01-2012. 
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
var queryAutocompleteRestUrl = "/rest/queryautocompsrv/1.0/queryautocompsrv/";
var queryCfValsMethod = 'getcfvals';
var queryIssueHtmlMethod = 'getissuetemplate';
var queryRepresentationTagPostfix = '-issuepicker-representation';
var QUERY_MAX_DISPLAY_ROWS = 20;
var QUERY_AUTOCOMPLETE_STARTS_AFTER = 2;
var QUERY_PREVENT_DEFAULT_FLAG = false; // firefox has problems with
// preventDefault

// hints
var cfQueryHistorySearchHint = AJS.I18n.getText("mailru.cf.query.js.historysearch");
var cfQueryMoreElemsHint = AJS.I18n.getText("mailru.cf.query.js.moreelems");

function getCFQueryMatchingElementsHint(nElems) {
    return AJS.I18n.getText("mailru.cf.query.js.matchingelems", nElems)
}

jQuery(document).ready(function() {
    JIRA.bind(
        JIRA.Events.NEW_CONTENT_ADDED,
        function(e, context) {
            manageQueryCfsAutocompleteFields('.cfs-query-linker-autocomplete');
            manageQueryCfsAutocompleteFields('.cfs-query-multi-linker-autocomplete');
            initSelectedIssuesList('.cfs-query-multi-linker-selected-list');
        }
    );
    manageQueryCfsAutocompleteFields('.cfs-query-linker-autocomplete');
    manageQueryCfsAutocompleteFields('.cfs-query-multi-linker-autocomplete');
    initSelectedIssuesList('.cfs-query-multi-linker-selected-list');
});

function manageQueryCfsAutocompleteFields(classname) {
    var elems = jQuery(classname);

    if (elems.length > 0) {
        for ( var i = 0; i < elems.length; i++) {
            var element = AJS.$("#" + elems[i].getAttribute("input-id"));
            if (!element.hasClass('gr-autocomplete-inited')) {
                element.addClass("gr-autocomplete-inited");
                element.cfsqueryautocomplete(elems[i].getAttribute("cf-id"), elems[i].getAttribute("cf-base-url"), queryAutocompleteRestUrl, 2);
            }
        }
    }
};

function initSelectedIssuesList(classname) {
    var elems = jQuery(classname);

    if (elems.length > 0) {
        for ( var i = 0; i < elems.length; i++) {
            var cfId = elems[i].getAttribute("cf-id");
            var restUrl = elems[i].getAttribute("base-url") + queryAutocompleteRestUrl;
            var issueOptions = jQuery('#' + cfId).children('.mail-issue-element');
            if (issueOptions.length > 0) {
                for ( var j = 0; j < issueOptions.length; j++) {
                    setSelectedIssue(restUrl, cfId, issueOptions[j].getAttribute("value"));
                }
            }
        }
    }
};

(function($) {
    $.fn.cfsqueryautocomplete = function(cfId, baseUrl, restUrlPart, minlength) {
        callback = typeof minlength == "function" ? minlength
            : (typeof callback == "function" ? callback : function() {
        });
        minlength = !isNaN(Number(minlength)) ? minlength : QUERY_AUTOCOMPLETE_STARTS_AFTER;
        var getValsUrl = baseUrl + restUrlPart + queryCfValsMethod;
        var getIssueHtmlUrl = baseUrl + restUrlPart + queryIssueHtmlMethod;
        var input = this;
        input[0].lastSelectedValue = input.val();

        var listDiv = $(document.createElement("div"));

        listDiv.css({
            top : "24px"
        });
        listDiv.addClass("suggestions");

        this.after(listDiv);

        listDiv.hide();
        function hideDropDown() {
            listDiv.hide();
            $(document).unbind("click", hideDropDown);
        }
        function suggest() {
            var currentTextfieldValue = input.val();

            var lastQuery = input[0].lastQuery ? input[0].lastQuery.toLowerCase() : "";
            var lastSelectedValue = input[0].lastQuery ? input[0].lastSelectedValue.toLowerCase() : "";
            if (currentTextfieldValue.length >= minlength
                    && $.trim(currentTextfieldValue).toLowerCase() != lastQuery
                    && $.trim(currentTextfieldValue).toLowerCase() != lastSelectedValue) {

                jQuery.ajax({
                    url : getValsUrl,
                    type : "POST",
                    dataType : "json",
                    data : {
                        "cf_id" : cfId
                    },
                    async : false,
                    error : function(xhr, ajaxOptions, thrownError) {
                        handleError(xhr, ajaxOptions, thrownError)
                    },
                    success : function(data) {
                        var html = "<div class=\"aui-list\">";
                        currentTextfieldValue = $.trim(currentTextfieldValue).toLowerCase();
                        if (data != null) {
                            var listBody = "<ul class=\"aui-list-section suggestions_ul_" + cfId + "\">";
                            var displayRowsCount = 0;
                            var i = 0;
                            while (i < data.length && displayRowsCount < QUERY_MAX_DISPLAY_ROWS) {
                                var obj_name = data[i].name;
                                var obj_type = data[i].type;
                                var obj_typeimage = data[i].typeimage;
                                var obj_descr = data[i].description;
                                var obj_state = data[i].state;
                                var obj_stateimage = data[i].stateimage;
                                var obj_preference = data[i].preference;
                                var obj_preferenceimage = data[i].preferenceimage;

                                var searchableData = obj_name;
                                var tag_title = obj_name;
                                if (obj_descr) {
                                    searchableData += " - " + obj_descr;
                                    tag_title += " - " + obj_descr;
                                }

                                var startIndex = searchableData.toLowerCase().indexOf(currentTextfieldValue);
                                if (startIndex != -1) {
                                    displayRowsCount++;
                                    var dataToList = "";

                                    var lastIndex = startIndex;
                                    var dataPos = 0;
                                    while (lastIndex != -1) {
                                        dataToList += searchableData.substring(dataPos,lastIndex)
                                            + "<b>" + searchableData.substring(lastIndex, lastIndex + currentTextfieldValue.length) + "</b>";
                                        dataPos = lastIndex + currentTextfieldValue.length;
                                        lastIndex = searchableData.indexOf(currentTextfieldValue, dataPos);
                                    }
                                    if (dataPos < searchableData.length) {
                                        dataToList += searchableData.substring(dataPos, searchableData.length);
                                    }
                                    var stylePart = '';
                                    if (obj_typeimage) {
                                        stylePart = "style=\"background-image: url(" + baseUrl + obj_typeimage + "); text-overflow: ellipsis; overflow: hidden;\"";
                                    }
                                    var statePart = '';
                                    if (obj_state) {
                                        statePart = " - <i>" + obj_state + "</i>";
                                    }

                                    listBody += "<li class=\"aui-list-item\"" + " li-cf-value-" + cfId + "=\"" + obj_name + "\">"
                                        + "<a class=\"aui-list-item-link aui-iconised-link\" href=\"#\"" + stylePart + "title=\"" + tag_title + "\">"
                                        + dataToList + statePart + "</a>" + "</li>";
                                }
                                i++;
                            }
                            var listHeader = "";
                            if (displayRowsCount > 0) {
                                listHeader = "<h5>" + cfQueryHistorySearchHint + "<span class=\"aui-section-description\"> ("
                                    + getCFQueryMatchingElementsHint(displayRowsCount);
                                if (i < data.length) {
                                    listHeader += cfQueryMoreElemsHint + ")</span></h5>";
                                } else {
                                    listHeader += ")</span></h5>";
                                }
                            }

                            listBody += "</ul>";
                        }

                        html += listHeader + listBody + "</div>";
                        listDiv.html(html);
                        $("li", listDiv).click(function(e) {
                            e.stopPropagation();
                            var value = this.getAttribute("li-cf-value-" + cfId);
                            select(value);
                        }).hover(function() {
                            $(".active").removeClass("active");
                            $(this).addClass("active");
                        }, function() {
                        });

                        $(document).click(hideDropDown);
                        listDiv.show();
                    }
                });

                input[0].lastQuery = currentTextfieldValue;
            } else if (currentTextfieldValue.length < minlength) {
                hideDropDown();
            }
        }
        ;
        input.keydown(function(e) {
            var that = this;
            if (this.timer) {
                clearTimeout(this.timer);
            }
            var actions = {
                "40" : function() { // down key
                    var li = $(".suggestions_ul_" + cfId + " .active").removeClass("active").next();
                    if (li.length) {
                        li.addClass("active");
                    } else {
                        $(".suggestions_ul_" + cfId + " li:first").addClass("active");
                    }
                    return !QUERY_PREVENT_DEFAULT_FLAG;
                },
                "38" : function() { // up key
                    var li = $(".suggestions_ul_" + cfId + " .active").removeClass("active").prev();
                    if (li.length) {
                        li.addClass("active");
                    } else {
                        $("li:last", listDiv).addClass("active");
                    }
                    return !QUERY_PREVENT_DEFAULT_FLAG;
                },
                "27" : function() { // escape key
                    hideDropDown();
                    return QUERY_PREVENT_DEFAULT_FLAG;
                },
                "13" : function() { // enter key
                    var obj = $(".suggestions_ul_" + cfId + " .active")[0];
                    if (obj) {
                        select(obj.getAttribute("li-cf-value-" + cfId));
                    }
                    return QUERY_PREVENT_DEFAULT_FLAG;
                },
                "9" : function() { // tab key
                    this[13]();
                    setTimeout(function() {
                        that.focus();
                    }, 0);
                    return !QUERY_PREVENT_DEFAULT_FLAG;
                }
            };
            var actionRes = true;
            if (listDiv.css("display") != "none" && e.keyCode in actions) {
                e.preventDefault(); // doesn't works in firefox
                actionRes = actions[e.keyCode]();
            }
            this.timer = setTimeout(suggest, 100);
            return actionRes;
        });

        function select(value) {
            if (value) {
                jQuery.ajax({
                    url : getIssueHtmlUrl,
                    type : "POST",
                    dataType : "json",
                    data : {
                        "cf_id" : cfId,
                        "cf_value" : value
                    },
                    async : true,
                    error : function(xhr, ajaxOptions, thrownError) {
                        handleError(xhr, ajaxOptions, thrownError)
                    },
                    success : function(data) {
                        // customization for different fields
                        if ($('.' + cfId + queryRepresentationTagPostfix).length > 0) {
                            if (data) {
                                var representation = $('.' + cfId + queryRepresentationTagPostfix);
                                if (representation.length == 1) {
                                    if (representation.children('#' + 'internal-' + cfId + '_' + value).length <= 0) {
                                        representation.html(representation.html() + data.html);
                                        var multiSelect = $('#' + cfId);
                                        multiSelect.html(multiSelect.html()
                                            + "<option id=\"" + cfId + "_" + value + "\" class=\"mail-issue-element\" value=\"" + value + "\" selected=\"selected\">" + value + "</option>");
                                    }
                                    input.val('');
                                    input[0].lastSelectedValue = '';
                                    input[0].defaultValue = '';
                                    input.trigger('input');
                                }
                            } else {
                                // do nothing
                            }
                        } else {
                            input.val(value);
                            input[0].lastSelectedValue = value;
                            input[0].defaultValue = value;
                            input.trigger('input');
                        }
                    }
                });

                hideDropDown();
            }
        }
        ;

        function handleError(xhr, ajaxOptions, thrownError) {
            try {
                var respObj = eval("(" + xhr.responseText + ")");
                initErrorDlg(respObj.message).show();
            } catch (e) {
                initErrorDlg(xhr.responseText).show();
            }
        }
        ;

    };
})(jQuery);

function removeSelectedIssue(cfId, issueId) {
    var element = jQuery('#internal-' + cfId + '_' + issueId);

    if (element.length == 1) {
        element.remove();
    }

    var associatedOption = jQuery('#' + cfId + '_' + issueId);

    if (associatedOption.length == 1) {
        associatedOption.remove();
    }
};

function setSelectedIssue(restUrl, cfId, issueId) {
    jQuery.ajax({
        url : restUrl + queryIssueHtmlMethod,
        type : "POST",
        dataType : "json",
        data : {
            "cf_id" : cfId,
            "cf_value" : issueId
        },
        async : false,
        error : function(xhr, ajaxOptions, thrownError) {
            handleError(xhr, ajaxOptions, thrownError)
        },
        success : function(data) {
            if (jQuery('.' + cfId + queryRepresentationTagPostfix).length > 0) {
                if (data) {
                    var representation = jQuery('.' + cfId + queryRepresentationTagPostfix);
                    if (representation.length == 1) {
                        representation.html(representation.html() + data.html);
                    }
                }
            }
        }
    });
};

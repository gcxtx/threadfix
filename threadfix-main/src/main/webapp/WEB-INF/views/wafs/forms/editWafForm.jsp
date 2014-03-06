<script type="text/ng-template" id="editWafModal.html">
    <div class="modal-header">
        <h4 id="myModalLabel">
            Edit WAF {{ object.name }}
            <span class="delete-span">
                <a ng-show="waf.applications"
                   id="deleteWafbutton"
                   class="btn btn-danger header-button"
                   ng-click="alert('WAFs with applications cannot be deleted.')">
                    Delete
                </a>
                <a ng-hide="waf.applications"
                   id="deleteWafbutton"
                   class="btn btn-danger header-button"
                   ng-click="showDeleteDialog('WAF')">
                    Delete
                </a>
            </span>
        </h4>
    </div>
    <div ng-form="form" class="modal-body">
        <table class="modal-form-table">
            <tbody>
            <tr>
                <td class="">Name</td>
                <td class="inputValue no-color">
                    <input ng-model="object.name" type="text" focus-on="focusInput" id="wafCreateNameInput" name="name" required ng-maxlength="50"/>
                    <span class="errors" ng-show="form.name.$dirty && form.name.$error.required">Name is required.</span>
                </td>
            </tr>
            <tr>
                <td>Type</td>
                <td class="inputValue no-color">
                    <select ng-model="object.wafType.id" id="typeSelect" name="wafType.id">
                        <option ng-repeat="type in config.wafTypeList"
                                ng-selected="object.wafType.id === type.id"
                                value="{{ type.id }}">
                            {{ type.name }}
                        </option>
                    </select>
                </td>
            </tr>
            </tbody>
        </table>
    </div>
	<%@ include file="/WEB-INF/views/modal/footer.jspf" %>
</script>

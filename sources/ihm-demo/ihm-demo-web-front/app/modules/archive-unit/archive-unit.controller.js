/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

angular.module('archive.unit')
  .constant('ARCHIVE_UNIT_MODULE_CONST', {
    'CONFIG_FILE_NOT_FOUND_MSG' : 'Fichier de configuration des détails à afficher est introuvable ou invalide.',
    'ARCHIVE_UNIT_FORM_PREFIX' : 'Archive Unit : ',
    'ARCHIVE_UNIT_FORM_TITLE_SEPARATOR' : '/',
    'SIMPLE_FIELD_TYPE': 'S',
    'COMPLEX_FIELD_TYPE': 'P',
    'TITLE_FIELD': 'Title',
    'START_DATE_FIELD':'StartDate',
    'END_DATE_FIELD':'EndDate',
    'DESCRIPTION_LEVEL_FIELD':'DescriptionLevel',
    'SUBMISSION_AGENCY_FIELD': 'SubmissionAgency',
    'ORIGINATING_AGENCY_FIELD':'OriginatingAgency',
    'ORIGINATING_AGENCY_IDENTIFIER_FIELD':'Identifier',
    'ORIGINATING_AGENCY_DESCRIPTION_FIELD':'OrganizationDescriptiveMetadata',
     'ORIGINATING_AGENCY_MGT':"#originating_agency",
     'ORIGINATING_AGENCIES_MGT':"#originating_agencies",

    'DESCIPTION_FIELD':'Description',
    'ID_KEY': '_id',
    'MGT_KEY': '_mgt',
    'STORAGE_KEY': '#storage',
    'NBC_KEY': '_nbc',
    'TECH_KEY': '_',
    'ID_LABEL': 'ID',
    'MGT_LABEL': 'Management',
    'INHERITED_RULE_LABEL': 'inheritedRule',
    'LIST_ITEM_LABEL': 'Valeur',
    'UNIT_PRENT_LIST': '_up',
    'MGT_WITH_CSHARP_KEY': '#mgt',
    'ORIGINATING_AGENCIES_WITH_CSHARP_KEY': '#originating_agencies',
    'ORIGINATING_AGENCY_WITH_CSHARP_KEY': '#originating_agency',
    'RULES_CATEGORY_KEYS': ['AccessRule','AppraisalRule','ClassificationRule','DisseminationRule','ReuseRule','StorageRule']
  })
  .filter('filterSize', function() {
    return function(bytes, precision) {
      if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
      if (typeof precision === 'undefined') precision = 1;
      var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
        number = Math.floor(Math.log(bytes) / Math.log(1024));
      return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) +  ' ' + units[number];
    }})
  .controller('ArchiveUnitController', function($scope, $routeParams, $filter, ihmDemoFactory, $window,
                                                ARCHIVE_UNIT_MODULE_CONST, ARCHIVE_UNIT_MODULE_FIELD_LABEL,
                                                ARCHIVE_UNIT_MODULE_OG_FIELD_LABEL, archiveDetailsService, $mdToast,
                                                $mdDialog, transferToIhmResult, RuleUtils, authVitamService, accessContractResource,
                                                uneceMappingService, $q){

    var self = this;

    self.displayLabel = function(key, parent, constants) {
      if (!constants) {
        constants = ARCHIVE_UNIT_MODULE_FIELD_LABEL;
      }
      var buildedKey = key;
      if (parent !== buildedKey) {
        // split(' ')[0] in order to skip ' <nb>' for arrays and get only the 'master' camelCase key (exemple from 'ParentKey 1')
        buildedKey = parent.split(' ')[0] + '.' + buildedKey;
      }

      // Return label value if find, field name else
      var labelValue = constants[buildedKey];
      if (!!labelValue) {
        return labelValue;
      }
      // Return label value for multivalued fields if found
      labelValue = constants[parent + ".default"];
      if (!!labelValue) {
        return labelValue + " " + key;
      }
      return $scope.fieldLabel = key;
    };

    //******************************* Alert diplayed ******************************* //
    self.showAlert = function($event, dialogTitle, message) {
      $mdDialog.show($mdDialog.alert().parent(angular.element(document.querySelector('#popupContainer')))
          .clickOutsideToClose(true)
          .title(dialogTitle)
          .textContent(message)
          .ariaLabel('Alert Dialog Demo')
          .ok('OK')
          .targetEvent($event)
      );
    };

    if (authVitamService.cookieValue("X-Access-Contract-Id")) {
      ihmDemoFactory.getAccessContracts({ContractName : decodeURIComponent(authVitamService.cookieValue("X-Access-Contract-Id")) || 'all'}).then(function (repsonse) {
        if (repsonse.status == 200 && repsonse.data['$results'] && repsonse.data['$results'].length > 0) {
          repsonse.data['$results'].forEach(function(contract) {
            if (contract.Name == authVitamService.cookieValue("X-Access-Contract-Id")) {
              $scope.userContract = contract;
            }
          });
          console.log($scope.userContract);
        }
      }, function (error) {
        console.log('Error while get tenant. Set default list : ', error);
      });
    }

    // **************************************************************************** //

    // *************** Set Edit mode ********************** //
    self.isEditMode = false;
    self.switchToEditMode = function switchToEditMode(){
      self.isEditMode = !self.isEditMode;
    };
    // **************************************************** //

    // *************** Cancel changes ********************** //
    self.cancelChanges = function cancelChanges(){
      self.isEditMode = false;
    };
    // **************************************************** //

    // 2- Details diplaying process
    self.archiveId = $routeParams.archiveId;
    self.archiveTitle = '';
    self.archiveArray=[];
    self.archiveTree = [];

    // Get required data
    self.archiveFields = $window.data;
    self.archiveDetailsConfig = $window.dataConfig;

    // Function buildSingleField: build single field structure
    var buildSingleField = function buildSingleField (value, key, parent, parents, constants, modifAllowed) {
      var fieldSet = {};
      var isMgtChild = false;

      fieldSet.fieldId = key;
      //Turns date into correct format for display
      if (key.indexOf('Date') != -1 || key.indexOf('LastModified') != -1){
        fieldSet.fieldValue = $filter('vitamFormatDate')(value);
      } else {
        fieldSet.fieldValue = value;
      }
      fieldSet.currentFieldValue = value;
      fieldSet.isChild = false;

      // parents list
      fieldSet.parents = [];
      if(parent !== key){

        for(var i=0; i<parents.length;i++){
          fieldSet.parents.push(parents[i]);
          if(parents[i] == ARCHIVE_UNIT_MODULE_CONST.MGT_KEY || parents[i] == ARCHIVE_UNIT_MODULE_CONST.MGT_WITH_CSHARP_KEY){
            isMgtChild = true;
          }
        }

        fieldSet.parents.push(parent);
        fieldSet.isChild = true;
      }

      fieldSet.fieldName = self.displayLabel(key, parent, constants);

      if(!angular.isObject(value)) {
        fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.SIMPLE_FIELD_TYPE;
        if(!isMgtChild && modifAllowed){
          fieldSet.isModificationAllowed = true;
        }
      } else {
        // Composite value
        fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.COMPLEX_FIELD_TYPE;
        var contentField = value;
        fieldSet.content = [];
        // fieldSet.isChild = true;

        var keyArrayIndex = 1;
        angular.forEach(contentField, function(value, key) {
          if (angular.isArray(contentField)) {
            key = fieldSet.fieldId;
          }
          if((key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY &&
              key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
              key.toString().charAt(0)!==ARCHIVE_UNIT_MODULE_CONST.TECH_KEY) ||
              key === ARCHIVE_UNIT_MODULE_CONST.NBC_KEY ){
            var fieldSetSecond = buildSingleField(value, key, fieldSet.fieldId, fieldSet.parents, constants, modifAllowed);
            fieldSetSecond.isChild = true;

            if(angular.isArray(contentField)){
              fieldSetSecond.fieldName = (fieldSet.fieldName ? fieldSet.fieldName : ARCHIVE_UNIT_MODULE_CONST.LIST_ITEM_LABEL) + ' ' + keyArrayIndex;
              keyArrayIndex = keyArrayIndex + 1;
            }
            fieldSet.content.push(fieldSetSecond);
          }
        });
      }
      return fieldSet;
    };

    //************* Intercept user changes *********** //
    self.modifiedFields = [];
    self.interceptUserChanges = function interceptUserChanges(fieldSet){
      var fieldStr = !fieldSet.fieldLabel ? fieldSet.fieldName : fieldSet.fieldLabel;
      var isDateField = fieldStr.toUpperCase().indexOf('DATE') > -1;
      if(isDateField && (fieldSet.currentFieldValue === '')){
        fieldSet.currentFieldValue = fieldSet.fieldValue;
        self.showAlert(null, "Erreur", "Veuillez saisir une date valide.");
      }

      fieldSet.isFieldModified = fieldSet.fieldValue !== fieldSet.currentFieldValue;
    };

    //************* Build modified fields *********** //
    var getModifiedFields = function getModifiedFields(fieldSet){
      if (fieldSet.isFieldModified == true) {
        // Add modified field to updateQueryFields
        var modifiedFieldSet = {};
        modifiedFieldSet.fieldId = fieldSet.fieldId;
        modifiedFieldSet.newFieldValue = fieldSet.currentFieldValue;

        // build field reference
        var fieldPath = "";
        for(var i=0; i < fieldSet.parents.length; i++){
          // TODO : correct this test
          if(angular.isNumber(fieldSet.parents[i])){
            fieldPath = fieldPath.substring(0, fieldPath.length - 1) + "[" + fieldSet.parents[i] + "].";
          }else{
            fieldPath = fieldPath + fieldSet.parents[i] + ".";
          }
        }

        // TODO : correct this test
        if(angular.isNumber(fieldSet.fieldId)){
          modifiedFieldSet.fieldId = fieldPath.substring(0, fieldPath.length - 1) + "[" + fieldSet.fieldId + "]";
        }else{
          modifiedFieldSet.fieldId = fieldPath + modifiedFieldSet.fieldId;
        }
        self.modifiedFields.push(modifiedFieldSet);
      } else if (fieldSet.content!== null && fieldSet.content !== undefined && fieldSet.content.length !== 0) {
        angular.forEach(fieldSet.content, function(nestedField) {
          getModifiedFields(nestedField);
        });
      }
    };


    //************* Save modifications *********** //
    self.saveModifications = function saveModifications($event) {
      // Prepare modified fields map
      self.modifiedFields = [];
      angular.forEach(self.archiveArray, function(value) {
        getModifiedFields(value);
      });
      angular.forEach(self.mainFields, function(value) {
        getModifiedFields(value);
      });

      // Call REST service
      ihmDemoFactory.saveArchiveUnit(self.archiveId, self.modifiedFields)
        .then(function (response) {
          // SUCCESS
          // Archive unit updated: send new select query to back office
          // Find archive unit details        	
          var displayUpdatedArchiveCallBack = function (data) {
            if(data.$results == null || data.$results == undefined ||
              data.$hits == null || data.$hits == undefined || data.httpCode > 200) {
              console.log("Erreur survenue lors de la mise à jour de l'unité archivistique");
              self.showAlert($event, "Erreur", "Erreur survenue lors de la mise à jour de l'unité archivistique");
            } else {
              // Archive unit found
              self.archiveFields = transferToIhmResult.transferUnit(data.$results)[0];
              //get archive object groups informations to be displayed in the table
              if (!!self.archiveFields._og) {
                ihmDemoFactory.getArchiveObjectGroup(self.archiveFields._id)
                  .then(function (response) {
                    var dataOG = response.data;
                    if (dataOG.nbObjects == null || dataOG.nbObjects == undefined ||
                      dataOG.versions == null || dataOG.versions == undefined){
                      // ObjectGroups Not Found
                      console.log("errorMsg");
                    } else {
                      $scope.archiveObjectGroups = dataOG;
                      $scope.archiveObjectGroupsOgId = self.archiveFields._og;
                      self.displayTechnicalMetadata();
                    }
                  },function () {
                    console.log("errorMsg");
                  });
              }

              self.archiveArray=[];
              self.displayArchiveDetails();

              // Refresh archive Details
              // Cancel EditMode
              self.isEditMode = false;
              self.showAlert($event, "Info", "Mise à jour réussie de l'unité archivistique");
            }
          };

          var failureUpdateDisplayCallback = function(errorMsg){
            // Display error message
            console.log(errorMsg);
            self.showAlert($event, "Erreur", "Erreur survenue lors de la mise à jour de l'unité archivistique");
          };
          if(response.data.httpCode >= 400) {
        	  console.log("Erreur survenue lors de la mise à jour de l'unité archivistique");
        	  self.refreshArchiveDetails();
              self.showAlert($event, "Erreur", "Erreur survenue lors de la mise à jour de l'unité archivistique");
          }
          else {
        	  archiveDetailsService.findArchiveUnitDetails(self.archiveId, displayUpdatedArchiveCallBack, failureUpdateDisplayCallback);
          }

        }, function (error) {
          console.log('Update Archive unit failed : ' + error.message);
          self.showAlert($event, "Erreur", "Erreur survenue lors de la mise à jour de l'unité archivistique");
        });
    };

// ************ Diplay Archive Unit Form dynamically ************* /
    self.refreshArchiveDetails = function () {
      var displayUpdatedArchiveCallBack = function (data) {
        if(data.$results == null || data.$results == undefined ||
          data.$hits == null || data.$hits == undefined) {
          console.log("errorMsg");
        } else {
          // Archive unit found
          var results = transferToIhmResult.transferUnit(data.$results);
          self.archiveFields = results[0];
          if (!!self.archiveFields._og) {
            //get archive object groups informations to be displayed in the table
            ihmDemoFactory.getArchiveObjectGroup(self.archiveFields._id)
              .then(function (response) {
                var dataOG = response.data;
                if (dataOG.nbObjects == null || dataOG.nbObjects == undefined ||
                  dataOG.versions == null || dataOG.versions == undefined) {
                  // ObjectGroups Not Found
                  console.log("errorMsg");
                } else {
                  $scope.archiveObjectGroups = dataOG;
                  $scope.archiveObjectGroupsOgId = self.archiveFields._og;
                  self.displayTechnicalMetadata();
                }
              }, function () {
                console.log("errorMsg");
              });
          }
          // Get Archive Tree
          ihmDemoFactory.getArchiveTree(self.archiveFields._id, self.archiveFields._us)
            .then(function (response) {
              self.archiveTree = response.data;

              // Add displayed unit details
              var displayedUnitDetails = {};
              displayedUnitDetails.Title = self.archiveFields.Title;
              displayedUnitDetails["#id"] = self.archiveId;

              self.fullArchiveTree = [];
              angular.forEach(self.archiveTree, function(value) {
                var currentPath = [];
                currentPath.push(displayedUnitDetails);
                angular.forEach(value, function(currentParent) {
                  currentPath.push(currentParent);
                });

                self.fullArchiveTree.push(currentPath);
              });

              console.log("Archive tree: " + self.archiveTree);
            },function (error) {
              console.log("Archive tree search failed");
            });

          self.archiveArray = [];
          self.managementItems = {};
          self.isEditMode = false;
          self.displayArchiveDetails();
        }
      };

      var failureUpdateDisplayCallback = function(errorMsg){
        // Display error message
        console.log(errorMsg);
      };
      archiveDetailsService.findArchiveUnitDetails(self.archiveId, displayUpdatedArchiveCallBack, failureUpdateDisplayCallback);
    };
    $scope.checkHerited = function(originId) {
      return (originId == self.archiveId) ? 'non' : 'oui';
    };
    $scope.checkSource = function(originId) {
      return (originId == self.archiveId) ? false : true;
    };
    $scope.defineStartDate = function(s){
      if (!s){
        return "Inconnue";
      } else return s.slice(0,10);
    };
    $scope.defineEndDate = function(s,e){
      if (!s && !e){
        return "Inconnue";
      } else if (s && !e) {
        return "Durée illimitée"
      } return e.slice(0,10);
    };
    $scope.exitRule = function(array){
      for (var i = 0; i < array.length; i++){
        if (array[i]["ruleId"])
          return true;
      }
      return false;
    };
    $scope.title = {};
    $scope.displayRule = {};
    $scope.toggleDetail = function($index, rule) {
      $scope.activePosition = $scope.activePosition == $index ? -1 : $index;
      $scope.displayRule[rule.ruleId] = !$scope.displayRule[rule.ruleId] ;

      if (!$scope.title[rule.ruleId]) {
        ihmDemoFactory.getArchiveUnitDetails(rule.originId)
          .then(function (response) {
            $scope.title[rule.ruleId] = response.data.$results[0].Title;
          });
      }

    };
    $scope.getSourcePath = function(rule) {
      if (rule.path) {
        return "#!/archiveunit/" + rule.path[0][0];
      }
      return '';
    };
    $scope.checkUpOrDown = function(rule){
      if ($scope.displayRule[rule.ruleId]) {
        return true;
      } else {
        return false;
      }
    };
    $scope.hasSortFinal = function(c){
      if (c == 'Durée d\'utilité Administrative' || c == 'Durée d\'utilité courante'){
        return true;
      } else return false;
    };
    self.displayArchiveDetails = function(){
      self.mainFields={};
      if(self.archiveFields == null || self.archiveFields == undefined){
        // Refresh screen
        self.refreshArchiveDetails();
      } else {
        // ID Field
        var fieldSet = {};
        var idField = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.ID_KEY];

        var inheritedRule = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.INHERITED_RULE_LABEL];
        var management = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.MGT_KEY];
        delete self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.INHERITED_RULE_LABEL];

        $scope.preventInheritance = {};

        $scope.refNonId = {};

        for (var key in management) {
          if(ARCHIVE_UNIT_MODULE_CONST.RULES_CATEGORY_KEYS.indexOf(key) === -1) {
            continue;
          }
          var translateKey = RuleUtils.translate(key);
          var currentRef = [];
          var tf = false;
          for (var n in management[key]) {
            var refArray = management[key][n].RefNonRuleId;
            for (var ref in refArray) {
              currentRef.push(refArray[ref]);
            }
            if (!tf) {
              var tf = management[key][n].PreventInheritance;
            }
          }
          if (typeof currentRef[0] !== 'undefined' && currentRef[0] !== null){
            $scope.refNonId[translateKey] = currentRef;
          }
          $scope.preventInheritance[translateKey] = tf;
        }

        self.ruleDisplay = {};
        var selfManagement = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.MGT_KEY];
        if (Array.isArray(selfManagement)) {
          selfManagement.forEach(function (element) {
            for (var key in element) {
              if(ARCHIVE_UNIT_MODULE_CONST.RULES_CATEGORY_KEYS.indexOf(key) === -1) {
                continue;
              }
              var translateKey = RuleUtils.translate(key);
              var rule = selfManagement[key];
              var displayArray = [];
              var displayObject = {};
              displayObject.ruleId = rule.Rule;
              delete rule.Rule;
              for (var detail in rule) {
                displayObject[detail] = rule[detail];
              }
              displayObject.originId = idField;
              displayArray.push(displayObject);
              displayObject = {};
              self.ruleDisplay[translateKey] = {};
              self.ruleDisplay[translateKey]['displayArray'] = displayArray;
            }
          })
        } else {
          for (var key in selfManagement) {
            if(ARCHIVE_UNIT_MODULE_CONST.RULES_CATEGORY_KEYS.indexOf(key) === -1) {
              continue;
            }
            var translateKey = RuleUtils.translate(key);
            var rule = selfManagement[key];
            if(angular.isArray(rule)) {
              // in case we have an array of rules
              var displayArray = [];
              var displayObject = {};
              for (var ruleKey in rule) {
                var oneRule = selfManagement[key][ruleKey];
                displayObject.ruleId = oneRule.Rule;
                delete oneRule.Rule;
                for (var detail in oneRule) {
                  displayObject[detail] = oneRule[detail];
                }
                displayObject.originId = idField;
                displayArray.push(displayObject);
                displayObject = {};
                self.ruleDisplay[translateKey] = {};
                self.ruleDisplay[translateKey]['displayArray'] = displayArray;
              }
            } else {
              // in case we just have one rule (should not happen)
              var displayArray = [];
              var displayObject = {};
              displayObject.ruleId = rule.Rule;
              delete rule.Rule;
              for (var detail in rule) {
                displayObject[detail] = rule[detail];
              }
              displayObject.originId = idField;
              displayArray.push(displayObject);
              displayObject = {};
              self.ruleDisplay[translateKey] = {};
              self.ruleDisplay[translateKey]['displayArray'] = displayArray;
            }
          }
        }
        for (var key in inheritedRule) {
          var translateKey = RuleUtils.translate(key);
          var rule = inheritedRule[key];
          var displayArray = [];
          var displayObject = {};
          for (var ruleId in rule) {
            if (ruleId != 'displayArray') {
              var origin = rule[ruleId];
              for (var originId in origin) {
                displayObject.ruleId = ruleId;
                displayObject.originId = originId;
                var originDetail = origin[originId];
                for (var detail in originDetail) {
                  displayObject[detail] = originDetail[detail];
                }

                displayArray.push(displayObject);
                displayObject = {};
              }
            }
          }
          self.ruleDisplay[translateKey] = self.ruleDisplay[translateKey] ? self.ruleDisplay[translateKey] : {};
          self.ruleDisplay[translateKey]['displayArray'] = displayArray;
        }
        delete self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.MGT_KEY];
        delete self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.STORAGE_KEY];

        if(idField !== self.archiveId){
          self.refreshArchiveDetails();
        } else {
          if(!angular.isObject(idField)){
            fieldSet.fieldName = ARCHIVE_UNIT_MODULE_CONST.ID_LABEL;
            fieldSet.fieldValue= idField;
            fieldSet.isChild = false;
            fieldSet.typeF= ARCHIVE_UNIT_MODULE_CONST.SIMPLE_FIELD_TYPE;
            fieldSet.fieldId = ARCHIVE_UNIT_MODULE_CONST.ID_KEY;
            fieldSet.parents = [];
            fieldSet.isModificationAllowed = false;
            self.mainFields[fieldSet.fieldName] =  fieldSet;
          }
          if (!!self.archiveFields._og) {
            //get archive object groups informations to be displayed in the table
            ihmDemoFactory.getArchiveObjectGroup(self.archiveFields._id)
              .then(function (response) {
                var dataOG = response.data;
                if (dataOG.nbObjects == null || dataOG.nbObjects == undefined ||
                  dataOG.versions == null || dataOG.versions == undefined) {
                  // ObjectGroups Not Found
                  console.log("errorMsg");
                } else {
                  $scope.archiveObjectGroups = dataOG;
                  $scope.archiveObjectGroupsOgId = self.archiveFields._og;
                  self.displayTechnicalMetadata();
                }
              }, function () {
                console.log("errorMsg");
              });
          }

          // Get Archive Tree
          ihmDemoFactory.getArchiveTree(self.archiveFields._id, self.archiveFields._us)
            .then(function (response) {
              self.archiveTree = response.data;

              // Add displayed unit details
              var displayedUnitDetails = {};
              displayedUnitDetails.Title = self.archiveFields.Title;
              displayedUnitDetails["#id"] = self.archiveId;

              self.fullArchiveTree = [];
              angular.forEach(self.archiveTree, function(value) {
                var currentPath = [];
                currentPath.push(displayedUnitDetails);
                angular.forEach(value, function(currentParent) {
                  currentPath.push(currentParent);
                });

                self.fullArchiveTree.push(currentPath);
              });
            },function (error) {
              console.log("Archive tree search failed");
            });

          // Other fields
          var mainFields = [ARCHIVE_UNIT_MODULE_CONST.TITLE_FIELD,
            ARCHIVE_UNIT_MODULE_CONST.DESCIPTION_FIELD,
            ARCHIVE_UNIT_MODULE_CONST.DESCRIPTION_LEVEL_FIELD,
            ARCHIVE_UNIT_MODULE_CONST.SUBMISSION_AGENCY_FIELD,
            ARCHIVE_UNIT_MODULE_CONST.ORIGINATING_AGENCY_FIELD,
            ARCHIVE_UNIT_MODULE_CONST.START_DATE_FIELD,
            ARCHIVE_UNIT_MODULE_CONST.END_DATE_FIELD];
          angular.forEach(self.archiveFields, function(value, key) {
            if(key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY &&
              key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
              key.toString().charAt(0)!==ARCHIVE_UNIT_MODULE_CONST.TECH_KEY) {
              var addedField = false;
              if (angular.isArray(value)) {
                var tmpValue = value;
                self.archiveFields[key] = tmpValue[0];
                value = tmpValue[0];
                  if (key == ARCHIVE_UNIT_MODULE_CONST.ORIGINATING_AGENCY_MGT || key == ARCHIVE_UNIT_MODULE_CONST.ORIGINATING_AGENCIES_MGT){
                    fieldSet = buildSingleField(value, key, key, [], null, false);
                  }
                  else {
                    fieldSet = buildSingleField(value, key, key, [], null, true);
                  }
                if (mainFields.indexOf(key) >= 0) {
                  self.mainFields[key] = fieldSet;
                } else {
                  self.archiveArray.push(fieldSet);
                }

                tmpValue.forEach(function(objectValue, index) {
                  if (index > 0) {
                    var newKey = self.displayLabel(key, key) + ' ' + index;
                    self.archiveFields[newKey] = objectValue;
                    if (key == ARCHIVE_UNIT_MODULE_CONST.ORIGINATING_AGENCY_MGT || key == ARCHIVE_UNIT_MODULE_CONST.ORIGINATING_AGENCIES_MGT){
                      fieldSet = buildSingleField(objectValue, newKey, newKey, [], null, false);
                    } else {
                      fieldSet = buildSingleField(objectValue, newKey, newKey, [], null, true);
                    }
                    self.archiveArray.push(fieldSet);
                  }
                });
                addedField = true;
              }
              // Get Title archive
              if(key == ARCHIVE_UNIT_MODULE_CONST.TITLE_FIELD){
                self.archiveTitle = value;
              }

              if (key == ARCHIVE_UNIT_MODULE_CONST.ORIGINATING_AGENCY_MGT || key == ARCHIVE_UNIT_MODULE_CONST.ORIGINATING_AGENCIES_MGT){
                self.fieldSet = buildSingleField(value, key, key, [], null, false);
              }
              else {
                self.fieldSet = buildSingleField(value, key, key, [], null, true);
              }
              if (!addedField) {
                if (mainFields.indexOf(key) >= 0 ) {
                  self.mainFields[key] = self.fieldSet;
                } else {
                	self.archiveArray.push(self.fieldSet);
                }
              }
            }
          });

          // Handle missing main fields
          angular.forEach(mainFields, function(key) {
            if (!self.mainFields[key]) {

              if (key === ARCHIVE_UNIT_MODULE_CONST.SUBMISSION_AGENCY_FIELD || key === ARCHIVE_UNIT_MODULE_CONST.ORIGINATING_AGENCY_FIELD) {
                self.fieldSet = buildSingleField({'Identifier': ''}, key, key, [], null, true);
              } else {
                self.fieldSet = buildSingleField('', key, key, [], null, true);
              }
              self.mainFields[key] = self.fieldSet;
            }
          });

          // _mgt field
          var mgtField = self.archiveFields[ARCHIVE_UNIT_MODULE_CONST.MGT_KEY];
          if(angular.isObject(mgtField)) {
            fieldSet = {};
            fieldSet.fieldName = ARCHIVE_UNIT_MODULE_CONST.MGT_LABEL;
            fieldSet.fieldValue = mgtField;
            fieldSet.isChild = false;
            fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.COMPLEX_FIELD_TYPE;
            fieldSet.fieldId = ARCHIVE_UNIT_MODULE_CONST.MGT_WITH_CSHARP_KEY;
            fieldSet.parents = [];

            var contentField = mgtField;
            self.managementItems = [];

            angular.forEach(contentField, function (value, key) {
              if (key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY && key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
                key.toString().charAt(0) !== ARCHIVE_UNIT_MODULE_CONST.TECH_KEY) {
                var fieldSetSecond = buildSingleField(value, key, ARCHIVE_UNIT_MODULE_CONST.MGT_WITH_CSHARP_KEY, fieldSet.parents, null, false);
                fieldSetSecond.isChild = false;
                self.managementItems.push(fieldSetSecond);
              }
              if (key !== ARCHIVE_UNIT_MODULE_CONST.MGT_KEY && key !== ARCHIVE_UNIT_MODULE_CONST.ID_KEY &&
                key.toString().charAt(0) !== ARCHIVE_UNIT_MODULE_CONST.TECH_KEY) {
                var fieldSetSecond = buildSingleField(value, key, ARCHIVE_UNIT_MODULE_CONST.MGT_WITH_CSHARP_KEY, fieldSet.parents, null, false);
                fieldSetSecond.isChild = false;
                self.managementItems.push(fieldSetSecond);
              }
            });
          }
        }
      }
    };

    self.displayTechnicalMetadata = function() {
      // technical metadatas
      self.technicalItems = {};
      angular.forEach($scope.archiveObjectGroups.versions, function(version) {
        var fieldSet = [];
        angular.forEach(version.metadatas, function(value, key) {

          if (key === 'PhysicalDimensions') {
            var allPromises = [];
            angular.forEach(value, function(value, kind) {
              allPromises.push(uneceMappingService.getDimensionWithDisplayableUnit(value, kind))
            });
            $q.all(allPromises).then(
              function onSuccess(allResolvePromise) {
                var physicalDimensions = {};
                angular.forEach(allResolvePromise, function(item) {
                  physicalDimensions[item.key] = item.value;
                });
                var canPush = true;
                angular.forEach(self.technicalItems[version['DataObjectVersion']], function(value) {
                  if (value.fieldName === "PhysicalDimensions") {
                    canPush = false;
                  }
                });
                if (canPush) {
                  var physicalMetadata = self.technicalItems[version['DataObjectVersion']];
                  var fieldSetSecond = buildSingleField(physicalDimensions, key, key, [],
                    ARCHIVE_UNIT_MODULE_OG_FIELD_LABEL, false);
                  fieldSetSecond.isChild = false;
                  physicalMetadata.push(fieldSetSecond);
                }
              }, function onError(error) {

              }
            );

          } else {
            var fieldSetSecond = buildSingleField(value, key, key, [], ARCHIVE_UNIT_MODULE_OG_FIELD_LABEL, false);
            fieldSetSecond.isChild = false;
            fieldSet.push(fieldSetSecond);
          }
        });
        self.technicalItems[version['DataObjectVersion']] = fieldSet;
      });
    };

    // Display Details
    self.displayArchiveDetails();

    //******** Toast diplayed only if the archive unit is already opened ********* //
    var last = {
      bottom: false,
      top: true,
      left: false,
      right: true
    };
    self.toastPosition = angular.extend({},last);
    self.getToastPosition = function() {
      sanitizePosition();
      return Object.keys(self.toastPosition)
        .filter(function(pos) { return self.toastPosition[pos]; })
        .join(' ');
    };
    function sanitizePosition() {
      var current = self.toastPosition;
      if ( current.bottom && last.top ) current.top = false;
      if ( current.top && last.bottom ) current.bottom = false;
      if ( current.right && last.left ) current.left = false;
      if ( current.left && last.right ) current.right = false;
      last = angular.extend({},current);
    }
    self.showMessageToast = function(message) {
      var pinTo = self.getToastPosition();
      $mdToast.show(
        $mdToast.simple()
          .textContent(message)
          .position(pinTo )
          .hideDelay(3000)
      );
    };
    $scope.download = function($event, objGId, usage, fileName) {
      var options = {};
      options.usage = usage;
      options.filename = fileName;
      window.open(ihmDemoFactory.getObjectAsInputStreamUrl(objGId, options), '_blank');
    };
    // **************************************************************************** //

    // ******************** Calculate Intent to render unit tree *********************** //
    self.getIntent = function(index) {
      var treeBranchStyle = {
        'margin-left': (index * 20)+'px'
      };

      return treeBranchStyle;
    };
    // ********************************************************************************* //

    self.hasPermission = authVitamService.hasPermission;
    $scope.isPhysicalArchive = function(version){
        return version.metadatas.PhysicalId != undefined;
    }
    $scope.getClassVersion = function(version) {
      if ($scope.userContract.EveryDataObjectVersion == true) {
        return '';
      }
      if ($scope.userContract.DataObjectVersion.indexOf(version.split('_')[0]) < 0) {
        return 'grayColor';
      }
      return '';
    }

  });

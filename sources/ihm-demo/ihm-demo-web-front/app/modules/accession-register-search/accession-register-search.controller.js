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

angular.module('accession.register.search')
  .constant('ACCESSIONREGISTER_CONSTANTS', {
    'GET_ALL_REGISTERS': 'ACCESSIONREGISTER',
    'ORIGINATING_AGENCY_FIELD': 'OriginatingAgency'
  })
  .controller('accessionRegisterSearchController', function($scope, $window, ACCESSIONREGISTER_CONSTANTS, ihmDemoFactory, responseValidator,ITEM_PER_PAGE, processSearchService, resultStartService) {

    $scope.startFormat = resultStartService.startFormat;

    $scope.search = {
      form: {
        serviceProducerCriteria: '',
        orderby: {
          field: ACCESSIONREGISTER_CONSTANTS.ORIGINATING_AGENCY_FIELD,
          sortType: 'ASC'
        }
      }, pagination: {
        currentPage: 0,
        resultPages: 0,
        itemsPerPage: ITEM_PER_PAGE
      }, error: {
        message: '',
        displayMessage: false
      }, response: {
        data: [],
        hints: {},
        totalResult: 0
      }
    };

    $scope.goToDetails = function(id) {
      $window.open('#!/accessionRegister/detail/' + id)
    };

    var preSearch = function() {
      var requestOptions = {};
      requestOptions.orderby = $scope.search.form.orderby;

      if(!$scope.search.form.serviceProducerCriteria){
        requestOptions[ACCESSIONREGISTER_CONSTANTS.GET_ALL_REGISTERS] = ACCESSIONREGISTER_CONSTANTS.GET_ALL_REGISTERS;
      } else {
        requestOptions[ACCESSIONREGISTER_CONSTANTS.ORIGINATING_AGENCY_FIELD] = $scope.search.form.serviceProducerCriteria;
      }
      return requestOptions;
    };

    var successCallback = function(response) {
      // FIXME #3023 Ugly fix that replace total by size in validation. If total well corrected in backend, no need to keep that solution
      // (This function should only return true or shouldn't be declared)
      if (!responseValidator.validateReceivedResponse(response) || response.data.$hits.size === 0) {
        return false;
      }
      $scope.search.response.data = response.data.$results;
      $scope.search.response.totalResult = response.data.$hits.total;
      $scope.search.pagination.resultPages = Math.ceil($scope.search.response.totalResult / $scope.search.pagination.itemsPerPage);
      $scope.search.pagination.currentPage = Math.floor($scope.search.pagination.startOffset / $scope.search.pagination.itemsPerPage) + 1;
      return true;
    };

    var computeErrorMessage = function() {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    // FIXME #3023 Ugly fix that override successCallback validation. If total well corrected in backend, no need to keep that solution.
    // (all 4 null/true at the end in param should be removed)
    var searchService = processSearchService.initAndServe(ihmDemoFactory.getAccessionRegisters, preSearch, successCallback, computeErrorMessage, $scope.search, true, null, null, null, true);
    $scope.searchRegistersByCriteria = searchService.processSearch;
    $scope.reinitForm = searchService.processReinit;
    $scope.onInputChange = searchService.onInputChange;

  });

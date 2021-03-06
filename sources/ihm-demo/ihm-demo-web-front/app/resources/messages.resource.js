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

// Define resources in order to call WebApp http endpoints for accession-register
angular.module('core')
  .factory('MessagesResource', function($q, $http, IHM_URLS, authVitamService) {

    var MESSAGES_ROOT = '/messages/';
    var MESSAGES_LOGBOOK = 'logbook';

    return function(options) {
      var deferred = $q.defer();

      /* If multiple languages, options should have a key param for language (exmpl: 'fr')
       * TODO : Check this key and call diferent API point for .properties file
       * In the future, we will have multiple .properties files (logbook, lifcycle, ...)
       * TODO : Add a new combinedPromise and a new angular.merge
       * FIXME: If translation key could be the same in diferent .properties file, MUST add an unique prefix per file
       */

      var combinedPromise = [];


      var status = authVitamService.isConnect('userCredentials');

      if(status === 'logged' && authVitamService.hasPermission('messages:logbook:read')) {
        combinedPromise.push($http.get(IHM_URLS.IHM_BASE_URL + MESSAGES_ROOT + MESSAGES_LOGBOOK));
      }

      // Specific to disabled translation and force show keys
      combinedPromise.push($http.get('static/languages_' + options.key + '.json'));

      $q.all(combinedPromise).then(
        function onSuccess(combinedResponses) {
          var allData = {};
          for (var i = 0, len = combinedResponses.length; i<len; i++) {
            angular.merge(allData, combinedResponses[i].data);
          }
          deferred.resolve(allData);
        }, function onError(error) {
          console.log('Error when resolving MessagesResource: ', error);
        }
      );

      return deferred.promise;
    };
  });

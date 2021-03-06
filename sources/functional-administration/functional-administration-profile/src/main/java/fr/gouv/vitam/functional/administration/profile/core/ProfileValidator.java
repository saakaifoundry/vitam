/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */
package fr.gouv.vitam.functional.administration.profile.core;

import fr.gouv.vitam.functional.administration.client.model.ProfileModel;

import java.io.InputStream;
import java.util.Optional;

/**
 * Used to validate profiles and to apply acceptance rules.
 *
 * Bellow the example of usage :
 *<pre>
 * {@code
 * private static ProfileValidator checkDuplicateInDatabaseValidator() {
 *    return (profile, profileIdentifier) -> {
 *        GenericRejectionCause rejection = null;
 *        boolean exist = ... exists in database?;
 *        if (exist) {
 *           rejection = GenericRejectionCause.rejectDuplicatedInDatabase(profileIdentifier);
 *        }
 *        return (rejection == null) ? Optional.empty() : Optional.of(rejection);
 *    };
 * }
 * }
 * </pre>
 *
 * The call the method like this to validate the profile p:
 *
 * GenericRejectionCause rejection = checkDuplicateInDatabaseValidator().validate(p, p.getName());
 *
 * Check if rejection is present then do the resolution
 *
 */
@FunctionalInterface
public interface ProfileValidator {



    /**
     * Validate a profile object
     *
     * @param profile to validate
     * @return empty optional if OK, Else return the rejection cause
     */
    Optional<RejectionCause> validate(ProfileModel profile);


    public class RejectionCause {

        private static final String ERR_INVALID_PROFILE= "The input profile json is is invalid";
        public static String ERR_DUPLICATE_PROFILE_ENTRY = "One or many profiles in the imported list have the same name : %s";
        public static String ERR_ID_NOT_ALLOWED_IN_CREATE = "Id must be null when creating profile (%s)";
        public static String ERR_DUPLICATE_PROFILE = "The profile %s already exists in database";
        public static String ERR_INVALID_FIELD = "The field %s has an invalid format";
        public static String ERR_MANDATORY_FIELD = "The field %s is mandatory";

        private String reason;

        public RejectionCause(String error) {
            setReason(error);
        }

        /**
         * Reject if id exisit and the action is creation.
         * If id exists, it should be an update instead of create
         *
         * @param profileIdentifier
         * @return RejectionCause
         */
        public static RejectionCause rejectIdNotAllowedInCreate(String profileIdentifier){
            return new RejectionCause(String.format(ERR_ID_NOT_ALLOWED_IN_CREATE , profileIdentifier));
        }

        /**
         * Reject if multiple profile have the same name in the same request before persist into database.
         * The profile identifier  must be unique
         *
         * @param profileIdentifier
         * @return RejectionCause
         */
        public static RejectionCause rejectDuplicatedEntry(String profileIdentifier){
            return new RejectionCause(String.format(ERR_DUPLICATE_PROFILE_ENTRY , profileIdentifier));
        }

        /**
         * Verify for each profile if already exists one in database that have the same identifier and/or name.
         * The database my manage this kind of constraint (by creating an unique index on the field or column)
         * @param identifier
         * @return RejectionCause
         */
        public static RejectionCause rejectDuplicatedInDatabase(String identifier){
            return new RejectionCause(String.format(ERR_DUPLICATE_PROFILE , identifier));
        }

        /**
         * Reject if one of multiple mandatory parameter are null
         * @param fieldName
         * @return RejectionCause
         */
        public static RejectionCause rejectMandatoryMissing(String fieldName){
            return new RejectionCause(String.format(ERR_MANDATORY_FIELD , fieldName));
        }

        public String getReason() {
            return reason;
        }

        private void setReason(String reason) {
            this.reason = reason;
        }
    }
}

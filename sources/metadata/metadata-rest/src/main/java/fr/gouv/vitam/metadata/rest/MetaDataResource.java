/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.rest;

import static fr.gouv.vitam.common.json.JsonHandler.toArrayList;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.json.JsonHandler;
import org.bson.Document;
import org.elasticsearch.ElasticsearchParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.metadata.api.MetaData;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.MongoDbAccessMetadataFactory;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;

/**
 * Units resource REST API
 */
@Path("/metadata/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class MetaDataResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataResource.class);


    private static final String INGEST = "ingest";
    private static final String ACCESS = "ACCESS";
    private static final String CODE_VITAM = "code_vitam";

    private final MetaData metaDataImpl;

    /**
     * MetaDataResource constructor
     *
     * @param configuration {@link MetaDataConfiguration}
     */
    public MetaDataResource(MetaDataConfiguration configuration) {
        metaDataImpl = MetaDataImpl.newMetadata(configuration, new MongoDbAccessMetadataFactory());
        LOGGER.info("init MetaData Resource server");
    }

    MongoDbAccessMetadataImpl getMongoDbAccess() {
        return ((MetaDataImpl) metaDataImpl).getMongoDbAccess();
    }

    /**
     * Insert unit with json request
     *
     * @param insertRequest the insert request in JsonNode format
     * @return Response
     */
    @Path("units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertUnit(JsonNode insertRequest) {
        Status status;

        try {
            metaDataImpl.insertUnit(insertRequest);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.CREATED)
            .entity(new RequestResponseOK(insertRequest)
                .setHits(1, 0, 1))
            .build();
    }

    /**
     * Select unit with json request
     *
     * @param request the request in JsonNode format
     * @return Response
     */
    @Path("units")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectUnit(JsonNode request) {
        return selectUnitsByQuery(request);
    }

    /**
     * select units list by query
     *
     * @param selectRequest
     * @return
     */
    private Response selectUnitsByQuery(JsonNode selectRequest) {
        Status status;
        final JsonNode copy = selectRequest.deepCopy();
        ArrayNode arrayNodeResults;
        try {
            arrayNodeResults = metaDataImpl.selectUnitsByQuery(selectRequest);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();

        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();

        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }

        return Response.status(Status.FOUND).entity(new RequestResponseOK(selectRequest)
            .addAllResults(JsonHandler.toArrayList(arrayNodeResults)).setQuery(copy)).build();
    }

    /**
     * @param selectRequest the select request in JsonNode format
     * @param unitId        the unit id to get
     * @return {@link Response} will be contains an json filled by unit result
     * @see entity(java.lang.Object, java.lang.annotation.Annotation[])
     * @see #type(javax.ws.rs.core.MediaType)
     */
    @Path("units/{id_unit}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(JsonNode selectRequest, @PathParam("id_unit") String unitId) {
        return selectUnitById(selectRequest, unitId);
    }

    /**
     * Update unit by query and path parameter unit_id
     *
     * @param updateRequest the update request
     * @param unitId        the id of unit to be update
     * @return {@link Response} will be contains an json filled by unit result
     * @see entity(java.lang.Object, java.lang.annotation.Annotation[])
     * @see type(javax.ws.rs.core.MediaType)
     */
    @Path("units/{id_unit}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitbyId(JsonNode updateRequest, @PathParam("id_unit") String unitId) {
        Status status;
        ArrayNode arrayNodeResults;
        try {
            arrayNodeResults = metaDataImpl.updateUnitbyId(updateRequest, unitId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.FOUND).entity(new RequestResponseOK(updateRequest)
            .addAllResults(JsonHandler.toArrayList(arrayNodeResults))).build();
    }

    /**
     * Selects unit by request and unit id
     */
    private Response selectUnitById(JsonNode selectRequest, String unitId) {
        Status status;
        ArrayNode arrayNodeResults;
        try {
            arrayNodeResults = metaDataImpl.selectUnitsById(selectRequest, unitId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ACCESS)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.FOUND).entity(new RequestResponseOK(selectRequest)
            .addAllResults(JsonHandler.toArrayList(arrayNodeResults))).build();
    }

    private Response metadataExecutionExceptionTrace(final MetaDataExecutionException e) {
        Status status;
        LOGGER.error(e);
        status = Status.INTERNAL_SERVER_ERROR;
        Throwable e2 = e.getCause();
        if (e2 !=null && e2 instanceof IllegalArgumentException || e2 instanceof ElasticsearchParseException) {
            status = Status.PRECONDITION_FAILED;
        } else {
            e2 = e;
        }
        return Response.status(status)
            .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ACCESS)
                .setState(CODE_VITAM)
                .setMessage(status.getReasonPhrase())
                .setDescription(e2.getMessage()))
            .build();
    }

    // OBJECT GROUP RESOURCE. TODO P1 see to externalize it (one resource for units, one resource for object group) to
    // avoid so much lines and complex maintenance

    /**
     * Create unit with json request
     *
     * @param insertRequest the insert query
     * @return the Response
     * @throws InvalidParseOperationException when json data exception occurred
     */
    @Path("objectgroups")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertObjectGroup(JsonNode insertRequest) throws InvalidParseOperationException {
        Status status;
        try {
            metaDataImpl.insertObjectGroup(insertRequest);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataAlreadyExistException e) {
            LOGGER.error(e);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(INGEST)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }

        return Response.status(Status.CREATED)
            .entity(new RequestResponseOK(insertRequest)
                .setHits(1, 0, 1))
            .build();
    }

    /**
     * Get ObjectGroup
     *
     * @param selectRequest the request
     * @param objectGroupId the objectGroup ID to get
     * @return a response with the select query and the required object group (can be empty)
     */
    @Path("objectgroups/{id_og}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupById(JsonNode selectRequest, @PathParam("id_og") String objectGroupId) {
        Status status;
        try {
            ParametersChecker.checkParameter("Request select required", selectRequest);
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(Status.PRECONDITION_FAILED).entity(
                new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("METADATA")
                    .setState(CODE_VITAM)
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(Status.PRECONDITION_FAILED.getReasonPhrase()))
                .build();
        }
        return selectObjectGroupById(selectRequest, objectGroupId);
    }

    /**
     * Get ObjectGroup
     *
     * @param updateRequest the query to update the objectgroup
     * @param objectGroupId the objectGroup ID to get
     * @return a response with the select query and the required object group (can be empty)
     */
    @Path("objectgroups/{id_og}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateObjectGroupById(JsonNode updateRequest, @PathParam("id_og") String objectGroupId) {
        Status status;
        try {
            ParametersChecker.checkParameter("UpdateQuery required", updateRequest);
            ParametersChecker.checkParameter("ObjectGroupId required", objectGroupId);
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(Status.PRECONDITION_FAILED).entity(
                new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("METADATA")
                    .setState(CODE_VITAM)
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(Status.PRECONDITION_FAILED.getReasonPhrase()))
                .build();
        }
        try {
            metaDataImpl.updateObjectGroupId(updateRequest, objectGroupId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("METADATA")
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (MetaDataExecutionException e) {
            LOGGER.error(e);
            status = Status.EXPECTATION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("METADATA")
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }
        return Response.status(Status.CREATED).entity(new RequestResponseOK().addResult(objectGroupId)).build();

    }

    /**
     * Selects unit by request and unit id
     */
    private Response selectObjectGroupById(JsonNode selectRequest, String objectGroupId) {
        Status status;
        final JsonNode copy = selectRequest.deepCopy();
        ArrayNode arrayNodeResults;
        try {
            arrayNodeResults = metaDataImpl.selectObjectGroupById(selectRequest, objectGroupId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("METADATA")
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (final MetaDataExecutionException e) {
            return metadataExecutionExceptionTrace(e);
        } catch (final MetaDataDocumentSizeException e) {
            LOGGER.error(e);
            status = Status.REQUEST_ENTITY_TOO_LARGE;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("METADATA")
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        } catch (MetaDataNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("METADATA")
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(status.getReasonPhrase()))
                .build();
        }

        return Response.status(Status.OK).entity(new RequestResponseOK(selectRequest)
            .addAllResults(toArrayList(arrayNodeResults)).setQuery(copy)).build();
    }

    @Path("accession-registers/units/{operationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response selectAccessionRegisterOnUnitByOperationId(@PathParam("operationId") String operationId) {
        List<Document> documents = metaDataImpl.selectAccessionRegisterOnUnitByOperationId(operationId);

        RequestResponseOK<UnitPerOriginatingAgency> responseOK = new RequestResponseOK<>();
        responseOK.setHttpCode(200);
        for (Document doc : documents) {
            UnitPerOriginatingAgency upoa = new UnitPerOriginatingAgency();
            upoa.setId(doc.getString("_id"));
            upoa.setCount(doc.getInteger("count"));
            responseOK.addResult(upoa);
        }

        return responseOK.toResponse();
    }

    @Path("accession-registers/objects/{operationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response selectAccessionRegisterOnObjectGroupByOperationId(@PathParam("operationId") String operationId) {
        List<Document> documents = metaDataImpl.selectAccessionRegisterOnObjectGroupByOperationId(operationId);

        RequestResponseOK<ObjectGroupPerOriginatingAgency> responseOK = new RequestResponseOK<>();
        responseOK.setHttpCode(200);
        for (Document doc : documents) {
            ObjectGroupPerOriginatingAgency ogpoa = new ObjectGroupPerOriginatingAgency();
            ogpoa.setOriginatingAgency(doc.getString("_id"));
            ogpoa.setNumberOfGOT(doc.getInteger("totalGOT"));
            ogpoa.setNumberOfObject(doc.getInteger("totalObject"));
            ogpoa.setSize(doc.getInteger("totalSize"));
            responseOK.addResult(ogpoa);
        }

        return responseOK.toResponse();
    }

}

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
package fr.gouv.vitam.access.external.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.response.RequestResponseError;

/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AccessExternalResourceImpl extends ApplicationStatusResource {

    private static final String PREDICATES_FAILED_EXCEPTION = "Predicates Failed Exception ";
    private static final String ACCESS_EXTERNAL_MODULE = "ACCESS_EXTERNAL";
    private static final String CODE_VITAM = "code_vitam";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalResourceImpl.class);

    /**
     * Constructor
     */
    public AccessExternalResourceImpl() {
        LOGGER.debug("AccessExternalResource initialized");
    }

    /**
     * get units list by query
     *
     * @param queryJson the query to get units
     * @return Response
     */
    @GET
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnits(JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        RequestResponse<JsonNode> result = null;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            result = client.selectUnits(queryJson);
            return Response.status(result.getHttpCode()).entity(result.toJsonNode()).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Predicate Failed Exception ", e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Request unauthorized ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits ", e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (BadRequestException e) {
            LOGGER.error("No search query specified, this is mandatory", e);
            return buildErrorResponse(VitamCode.GLOBAL_EMPTY_QUERY, e.getLocalizedMessage());
        }
    }

    /**
     * get units list by query with POST method
     *
     * @param queryJson the query to get units
     * @param xhttpOverride the use of override POST method
     * @return Response
     */
    @POST
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectUnits(JsonNode queryJson,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        LOGGER.debug("Execution of DSL Vitam from Access ongoing...");
        Status status;
        if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
            return getUnits(queryJson);
        } else {
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, MISSING_XHTTPOVERRIDE)).build();
        }
    }

    /**
     * update units list by query
     *
     * @param queryDsl the query to update
     * @return Response
     */
    @PUT
    @Path("/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnits(JsonNode queryDsl) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, NOT_YET_SUPPORTED)).build();
    }

    /**
     * get units list by query based on identifier
     *
     * @param queryJson query as String
     * @param idUnit the id of archive unit to get
     * @return Archive Unit
     */
    @GET
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(JsonNode queryJson, @PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        JsonNode result = null;
        ParametersChecker.checkParameter("unit id is required", idUnit);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            result = client.selectUnitbyId(queryJson, idUnit).toJsonNode().get("$results").get(0);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }

    /**
     * get units list by query based on identifier
     *
     * @param queryJson the query to get archive unit
     * @param xhttpOverride the use of override POST method
     * @param idUnit the archive unit id
     * @return Response
     */
    @POST
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectUnitById(JsonNode queryJson,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        @PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        ParametersChecker.checkParameter("unit id is required", idUnit);
        Status status;
        if (xhttpOverride != null && "GET".equalsIgnoreCase(xhttpOverride)) {
            return getUnitById(queryJson, idUnit);
        } else {
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, MISSING_XHTTPOVERRIDE)).build();
        }
    }

    /**
     * update archive units by Id with Json query
     *
     * @param queryJson the update query (null not allowed)
     * @param idUnit units identifier
     * @return a archive unit result list
     */
    @PUT
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitById(JsonNode queryJson, @PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        JsonNode result = null;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            result = client.updateUnitbyId(queryJson, idUnit).toJsonNode().get("$results").get(0);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (NoWritingPermissionException e) {
            LOGGER.error(e.getMessage(), e);
            status = Status.METHOD_NOT_ALLOWED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }

    /**
     * check existence of an unit
     *
     * @param idUnit the archive unit id
     * @return check result response
     */
    @HEAD
    @Path("/units/{idu}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkExitsUnitById(@PathParam("idu") String idUnit) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, NOT_YET_SUPPORTED)).build();
    }

    /**
     * get object group list by query and id
     *
     * @param idObjectGroup the object group id
     * @param queryJson the query to get object
     * @return Response
     * @Deprecated use /units/idu/object
     */
    @GET
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response getObjectGroup(@PathParam("ido") String idObjectGroup, JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        JsonNode result;
        Status status;
        try {
            try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
                result = client.selectObjectbyId(queryJson, idObjectGroup).toJsonNode().get("$results").get(0);
                return Response.status(Status.OK).entity(RequestResponseOK.getFromJsonNode(result)).build();
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }

    /**
     * @param headers the http header defined parameters of request
     * @param idObjectGroup the id object group
     * @param query the query to get object
     * @param asyncResponse the synchronized response
     * @Deprecated use /units/idu/object
     */
    @GET
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Deprecated
    public void getObjectIdoGet(@Context HttpHeaders headers, @PathParam("ido") String idObjectGroup,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        getObject(headers, idObjectGroup, query, asyncResponse, false);
    }

    /**
     * @param headers the http header defined parameters of request
     * @param idObjectGroup the id object group
     * @param queryJson the query to get object
     * @return Response
     * @Deprecated use /units/idu/object
     */
    @POST
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response getObjectGroupPost(@Context HttpHeaders headers,
        @PathParam("ido") String idObjectGroup, JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        final String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
        if (xHttpOverride == null || !"GET".equalsIgnoreCase(xHttpOverride)) {
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, MISSING_XHTTPOVERRIDE)).build();
        } else {
            return getObjectGroup(idObjectGroup, queryJson);
        }
    }

    /**
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     * 
     * @param headers the http header defined parameters of request
     * @param idObjectGroup the id object group
     * @param query the query to get object
     * @param asyncResponse the synchronized response
     * @Deprecated use /units/idu/object
     */
    @POST
    @Path("/objects/{ido}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Deprecated
    public void getObjectIdoPost(@Context HttpHeaders headers, @PathParam("ido") String idObjectGroup,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        getObject(headers, idObjectGroup, query, asyncResponse, true);
    }

    /**
     * @param headers the http header defined parameters of request
     * @param idu the id of archive unit
     * @param queryJson the query to get object
     * @return Response
     */
    @GET
    @Path("/units/{idu}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupMetadatas(@Context HttpHeaders headers, @PathParam("idu") String idu,
        JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        JsonNode result;
        Status status;
        try {
            try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
                String idObjectGroup = idObjectGroup(idu);
                result = client.selectObjectbyId(queryJson, idObjectGroup).toJsonNode().get("$results").get(0);
                return Response.status(Status.OK).entity(RequestResponseOK.getFromJsonNode(result)).build();
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }


    /**
     * @param headers the http header defined parameters of request
     * @param idu the id of archive unit
     * @param queryJson the query to get object
     * @return Response
     */
    @POST
    @Path("/units/{idu}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupMetadatasPost(@Context HttpHeaders headers,
        @PathParam("idu") String idu, JsonNode queryJson) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        Status status;
        final String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
        if (xHttpOverride == null || !"GET".equalsIgnoreCase(xHttpOverride)) {
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, MISSING_XHTTPOVERRIDE)).build();
        } else {
            return getObjectGroupMetadatas(headers, idu, queryJson);
        }
    }

    /**
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     * 
     * @param headers the http header defined parameters of request
     * @param idu the id of archive unit
     * @param query the query to get object
     * @param asyncResponse the synchronized response
     */
    @GET
    @Path("/units/{idu}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObject(@Context HttpHeaders headers, @PathParam("idu") String idu,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        Status status;
        try {
            String idObjectGroup = idObjectGroup(idu);
            getObject(headers, idObjectGroup, query, asyncResponse, false);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build());
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build());
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build());
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build());
        }
    }



    /**
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     * 
     * @param headers the http header defined parameters of request
     * @param idu the id of archive unit
     * @param query the query to get object
     * @param asyncResponse the synchronized response
     */
    @POST
    @Path("/units/{idu}/object")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getObjectPost(@Context HttpHeaders headers, @PathParam("idu") String idu,
        JsonNode query, @Suspended final AsyncResponse asyncResponse) {
        Status status;
        try {
            String idObjectGroup = idObjectGroup(idu);
            getObject(headers, idObjectGroup, query, asyncResponse, true);

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            status = Status.PRECONDITION_FAILED;
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build());
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error("Unauthorized request Exception ", e);
            status = Status.UNAUTHORIZED;
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build());
        } catch (final AccessInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            status = Status.NOT_FOUND;
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build());
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            status = Status.UNAUTHORIZED;
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build());
        }
    }

    /**
     * get object group list by query
     *
     * @param queryDsl the query to get list of object group
     * @return Response
     * @Deprecated use /units/idu/object
     */
    @GET
    @Path("/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response getObjectsList(JsonNode queryDsl) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, NOT_YET_SUPPORTED)).build();
    }

    /**
     * @param xhttpOverride the use of override POST method
     * @param query the query to get object
     * @return Response
     * @Deprecated use /units/idu/object
     */
    @POST
    @Path("/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response getObjectListPost(@HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride,
        JsonNode query) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, NOT_YET_SUPPORTED)).build();
    }

    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

    private String idObjectGroup(String idu)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException {
        // Select "Object from ArchiveUNit idu
        JsonNode result = null;
        ParametersChecker.checkParameter("unit id is required", idu);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            select.addUsedProjection("#object");
            result = client.selectUnitbyId(select.getFinalSelect(), idu).toJsonNode().get("$results").get(0);
            SanityChecker.checkJsonAll(result);
            return result.findValue("#object").textValue();
        }
    }

    private void getObject(HttpHeaders headers, String idObjectGroup,
        JsonNode query, final AsyncResponse asyncResponse, boolean post) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncObjectStream(asyncResponse, headers, idObjectGroup, query, post));
    }

    private void asyncObjectStream(AsyncResponse asyncResponse, HttpHeaders headers, String idObjectGroup,
        JsonNode query, boolean post) {

        try {
            if (post) {
                if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.METHOD_OVERRIDE)) {
                    AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                        Response.status(Status.PRECONDITION_FAILED)
                            .entity(getErrorEntity(Status.PRECONDITION_FAILED, MISSING_XHTTPOVERRIDE).toString())
                            .build());
                    return;
                }
                final String xHttpOverride = headers.getRequestHeader(GlobalDataRest.X_HTTP_METHOD_OVERRIDE).get(0);
                if (!HttpMethod.GET.equalsIgnoreCase(xHttpOverride)) {
                    AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                        Response.status(Status.METHOD_NOT_ALLOWED)
                            .entity(getErrorEntity(Status.METHOD_NOT_ALLOWED, MISSING_XHTTPOVERRIDE)
                                .toString())
                            .build());
                    return;
                }
            }
            if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.QUALIFIER) ||
                !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.VERSION)) {
                LOGGER.error("At least one required header is missing. Required headers: (" +
                    VitamHttpHeader.QUALIFIER.name() + ", " + VitamHttpHeader.VERSION.name() + ")");
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                    Response.status(Status.PRECONDITION_FAILED)
                        .entity(getErrorEntity(Status.PRECONDITION_FAILED, "QUALIFIER or VERSION missing").toString())
                        .build());
                return;
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage()).toString())
                .build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
            return;
        }

        final String xQualifier = headers.getRequestHeader(GlobalDataRest.X_QUALIFIER).get(0);
        final String xVersion = headers.getRequestHeader(GlobalDataRest.X_VERSION).get(0);
        AsyncInputStreamHelper helper;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            HttpHeaderHelper.checkVitamHeaders(headers);
            final Response response =
                client.getObject(query, idObjectGroup, xQualifier,
                    Integer.valueOf(xVersion));
            helper = new AsyncInputStreamHelper(asyncResponse, response);
            final ResponseBuilder responseBuilder =
                Response.status(Status.OK).header(GlobalDataRest.X_QUALIFIER, xQualifier)
                    .header(GlobalDataRest.X_VERSION, xVersion)
                    .header("Content-Disposition", response.getHeaderString("Content-Disposition"))
                    .type(response.getMediaType());
            helper.writeResponse(responseBuilder);
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            final Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, exc.getLocalizedMessage()).toString())
                .build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        } catch (final AccessInternalClientServerException exc) {
            LOGGER.error(exc.getMessage(), exc);
            final Response errorResponse =
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exc.getLocalizedMessage())
                        .toString())
                    .build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        } catch (final AccessInternalClientNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            final Response errorResponse =
                Response.status(Status.NOT_FOUND).entity(getErrorEntity(Status.NOT_FOUND, exc.getLocalizedMessage()).toString()).build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        } catch (AccessUnauthorizedException e) {
            final Response errorResponse =
                Response.status(Status.UNAUTHORIZED).entity(getErrorEntity(Status.UNAUTHORIZED, e.getLocalizedMessage())
                    .toString()).build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        }
    }


    /**
     * findDocuments
     *
     * @param select the query to find document of accession register
     * @param xhttpOverride the use of override POST method
     * @return Response
     */
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegister(JsonNode select) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final RequestResponse result = client.getAccessionRegister(select);
            if (result.isOk()) {
                return Response.status(Status.OK).entity(((RequestResponseOK) result).setQuery(select))
                    .build();
            } else {
                return Response.status(result.getHttpCode()).entity(result).build();
            }
        } catch (final ReferentialNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.OK).entity(new RequestResponseOK()).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error("Access contract does not allow ", e);
            final Status status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }



    /**
     * findDocumentByID
     *
     * @param documentId the document id to get
     * @return Response
     */
    @GET
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegisterById(@PathParam("id_document") String documentId) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.and().add(QueryHelper.eq("OriginatingAgency", documentId),
                QueryHelper.eq(VitamFieldsHelper.tenant(), tenantId)));
            final RequestResponse result = client.getAccessionRegister(select.getFinalSelect());
            if (result.isOk()) {
                return Response.status(Status.OK).entity(((RequestResponseOK) result).setQuery(select.getFinalSelect()))
                    .build();
            } else {
                return Response.status(result.getHttpCode()).entity(result).build();
            }
        } catch (final ReferentialNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.OK).entity(new RequestResponseOK()).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final AccessUnauthorizedException e) {
            LOGGER.error("Access contract does not allow ", e);
            final Status status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }


    /**
     * findAccessionRegisterDetail
     *
     * @param documentId the document id of accession register to get
     * @param select the query to get document
     * @param xhttpOverride the use of override POST method
     * @return Response
     */
    @GET
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/accession-register-detail")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegisterDetail(@PathParam("id_document") String documentId, JsonNode select) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        ParametersChecker.checkParameter("accession register id is a mandatory parameter", documentId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse accessionRegisterDetail =
                client.getAccessionRegisterDetail(documentId, select);
            return Response.status(Status.OK).entity(accessionRegisterDetail).build();
        } catch (final ReferentialNotFoundException e) {
            return Response.status(Status.OK).entity(new RequestResponseOK()).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        } catch (Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }

    /**
     * findDocuments
     *
     * @param select the query to find document of accession register
     * @param xhttpOverride the use of override POST method
     * @return Response
     */
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegister(JsonNode select,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride) {
        if (xhttpOverride == null || !"GET".equalsIgnoreCase(xhttpOverride)) {
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, MISSING_XHTTPOVERRIDE)).build();
        }
        return findAccessionRegister(select);
    }



    /**
     * findDocumentByID
     *
     * @param documentId the document id to get
     * @param xhttpOverride the use of override POST method
     * @return Response
     */
    @POST
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegisterById(@PathParam("id_document") String documentId,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride) {
        if (xhttpOverride == null || !"GET".equalsIgnoreCase(xhttpOverride)) {
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, MISSING_XHTTPOVERRIDE)).build();
        }
        return findAccessionRegisterById(documentId);
    }


    /**
     * findAccessionRegisterDetail
     *
     * @param documentId the document id of accession register to get
     * @param select the query to get document
     * @param xhttpOverride the use of override POST method
     * @return Response
     */
    @POST
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/accession-register-detail")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessionRegisterDetail(@PathParam("id_document") String documentId, JsonNode select,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride) {
        if (xhttpOverride == null || !"GET".equalsIgnoreCase(xhttpOverride)) {
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, MISSING_XHTTPOVERRIDE)).build();
        }
        return findAccessionRegisterDetail(documentId, select);
    }

    private Response buildErrorResponse(VitamCode vitamCode, String message) {
        return Response.status(vitamCode.getStatus())
            .entity(new RequestResponseError().setError(new VitamError(VitamCodeHelper.getCode(vitamCode))
                .setContext(vitamCode.getService().getName()).setState(vitamCode.getDomain().getName())
                .setMessage(vitamCode.getMessage()).setDescription(message)).toString())
            .build();
    }

}

/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functionaltest.cucumber.step;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.functional.administration.client.model.ContextModel;

/**
 * Context Step
 */
public class ContextStep {
    
    private World world;
    private String fileName;
    private String query;
    
    private static final String OPERATION_ID = "Operation-Id";

    public ContextStep(World world) {
        this.world = world;
    }
    
    /**
     * define a context
     *
     * @param fileName name of a context
     */
    @Given("^un contexte nommé (.*)$")
    public void a_context_named(String fileName) {
        this.fileName = fileName;
    }

    @Then("^j'importe ce contexte en succès")
    public void success_upload_context() 
        throws IOException, AccessExternalClientServerException, InvalidParseOperationException {
        Path context = Paths.get(world.getBaseDirectory(), fileName);
        final RequestResponse response =
            world.getAdminClient()
                .importContexts(Files.newInputStream(context, StandardOpenOption.READ), world.getTenantId());
        assertThat(Response.Status.OK.getStatusCode() == response.getStatus());
    }
    
    @Then("^j'importe ce contexte en échec")
    public void fail_upload_context() 
        throws AccessExternalClientServerException, InvalidParseOperationException, IOException {
        Path context = Paths.get(world.getBaseDirectory(), fileName);
        final RequestResponse response =
            world.getAdminClient()
                .importContexts(Files.newInputStream(context, StandardOpenOption.READ), world.getTenantId());           
        assertThat(Response.Status.BAD_REQUEST.getStatusCode() == response.getStatus());
    }
    
    @When("^je modifie le contexte avec le fichier de requête suivant (.*)$")
    public void update_context_by_query(String queryFilename) 
        throws InvalidParseOperationException, AccessExternalClientException, IOException{
        Path queryFile = Paths.get(world.getBaseDirectory(), queryFilename);
        this.query = FileUtil.readFile(queryFile.toFile());
        if (world.getOperationId() != null) {
            this.query = this.query.replace(OPERATION_ID, world.getOperationId());
        }
        
        JsonNode queryDsl = JsonHandler.getFromString(query);
        RequestResponse<ContextModel> requestResponse =
            world.getAdminClient()
                .updateContext(find_a_context_id(), queryDsl, world.getTenantId());
    }
    
    private String find_a_context_id() 
        throws AccessExternalClientNotFoundException, AccessExternalClientException, InvalidParseOperationException{
        JsonNode queryDsl = JsonHandler.createObjectNode();
        RequestResponse<ContextModel> requestResponse = 
            world.getAdminClient().findDocuments(AdminCollections.CONTEXTS, queryDsl, world.getTenantId());
        return requestResponse.toJsonNode().findValue("_id").asText();
    }
    
    @Then("^le contexte contient un contrat (.-*)$")
    private void has_contrat(String identifier) throws AccessExternalClientNotFoundException, AccessExternalClientException, InvalidParseOperationException{
        RequestResponse<ContextModel> requestResponse = 
            world.getAdminClient().findDocumentById(AdminCollections.CONTEXTS, find_a_context_id(), world.getTenantId());
        assertThat(requestResponse.toString().contains(identifier));
    }
    
}

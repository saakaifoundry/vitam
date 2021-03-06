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
package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.processing.common.model.UriPrefix;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.ValidationXsdUtils;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({WorkspaceClientFactory.class, LogbookLifeCyclesClientFactory.class,
    LogbookOperationsClientFactory.class, ValidationXsdUtils.class})
public class TransferNotificationActionHandlerATROKFileTest {
    private static final String ARCHIVE_ID_TO_GUID_MAP =
        "ARCHIVE_ID_TO_GUID_MAP_obj.json";
    private static final String DATA_OBJECT_ID_TO_GUID_MAP =
        "DATA_OBJECT_ID_TO_GUID_MAP_obj.json";
    private static final String DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP =
        "DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP.json";
    private static final String DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP =
        "DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP_obj.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS = "globalSEDAParameters.json";
    private static final String OBJECT_GROUP_ID_TO_GUID_MAP =
        "OBJECT_GROUP_ID_TO_GUID_MAP_obj.json";

    private static final String HANDLER_ID = "ATR_NOTIFICATION";

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private HandlerIOImpl action;
    private List<IOParameter> in;
    private List<IOParameter> out;
    private GUID guid;
    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        guid = GUIDFactory.newGUID();
        params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8080")
                .setUrlMetadata("http://localhost:8080").setObjectName("objectName.json").setCurrentStep("currentStep")
                .setContainerName(guid.getId()).setProcessId("aeaaaaaaaaaaaaababz4aakxtykbybyaaaaq2203");

        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        action = new HandlerIOImpl(guid.getId(), "workerId");

        PowerMockito.mockStatic(ValidationXsdUtils.class);
        PowerMockito.when(ValidationXsdUtils.checkWithXSD(anyObject(), anyObject())).thenReturn(true);

        in = new ArrayList<>();
        for (int i = 0; i < TransferNotificationActionHandler.HANDLER_IO_PARAMETER_NUMBER; i++) {
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "file" + i)));
        }
        action.addOutIOParameters(in);
        action.addOuputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP));
        action.addOuputResult(1, PropertiesUtils.getResourceFile(DATA_OBJECT_ID_TO_GUID_MAP));
        action.addOuputResult(2, PropertiesUtils.getResourceFile(DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP));
        action.addOuputResult(3, PropertiesUtils.getResourceFile(DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP));
        action.addOuputResult(4, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS));
        action.addOuputResult(5, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP));
        action.reset();
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "ATR/responseReply.xml")));
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenXMLCreationWhenValidThenResponseOK_and_objectGroupGuid_OK()
        throws Exception {
        final TransferNotificationActionHandler handler = new TransferNotificationActionHandler();
        final InputStream xmlFile;
        assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
        action.reset();
        action.addInIOParameters(in);
        action.addOutIOParameters(out);
        WorkerParameters parameters =
            params.putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.OK.name())
                .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
        final ItemStatus response = handler
            .execute(parameters, action);

        try {
            xmlFile = action.getInputStreamFromWorkspace(out.get(0).getUri().getPath());
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            throw new ProcessingException(e);
        }
        assertEquals(StatusCode.OK, response.getGlobalStatus());

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = xmlInputFactory.createXMLEventReader(xmlFile);
        String objectGroupGuid = null;

        while (true) {
            final XMLEvent event = reader.nextEvent();
            if (event.isStartElement() &&
                event.asStartElement().getName().getLocalPart().equals("DataObjectGroupSystemId")) {
                objectGroupGuid = reader.getElementText();
                break;
            }
        }

        JsonNode guidOG = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP));
        assertTrue(guidOG.toString().contains(objectGroupGuid));
    }
}

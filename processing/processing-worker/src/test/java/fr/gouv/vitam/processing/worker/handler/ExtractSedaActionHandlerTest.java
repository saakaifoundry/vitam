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
package fr.gouv.vitam.processing.worker.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;

public class ExtractSedaActionHandlerTest {
    ExtractSedaActionHandler handler;
    private static final String HANDLER_ID = "ExtractSeda";
    private SedaUtilsFactory factory;
    private SedaUtils sedaUtils;

    @Before
    public void setUp() {
        factory = mock(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL()
        throws XMLStreamException, IOException, ProcessingException {
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils).extractSEDA(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new ExtractSedaActionHandler(factory);
        assertEquals(ExtractSedaActionHandler.getId(), HANDLER_ID);
        final WorkParams params =
            new WorkParams().setServerConfiguration(new ServerConfiguration().setUrlWorkspace("")).setGuuid("");
        final EngineResponse response = handler.execute(params);
        assertEquals(response.getStatus(), StatusCode.FATAL);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK()
        throws XMLStreamException, IOException, ProcessingException {
        Mockito.doNothing().when(sedaUtils).extractSEDA(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new ExtractSedaActionHandler(factory);
        assertEquals(ExtractSedaActionHandler.getId(), HANDLER_ID);
        final WorkParams params =
            new WorkParams().setServerConfiguration(new ServerConfiguration().setUrlWorkspace("")).setGuuid("");
        final EngineResponse response = handler.execute(params);
        assertEquals(response.getStatus(), StatusCode.OK);
    }

}
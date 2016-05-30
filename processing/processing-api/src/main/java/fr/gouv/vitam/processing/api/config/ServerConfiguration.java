/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.api.config;

/**
 * 
 * ServerConfiguration class contains the different parameter {hostName
 * ,ipAddress, port }to connect a remote server such as workspace or metaData
 *
 */
public class ServerConfiguration {
	// FIXME REVIEW missing package-info
	// FIXME REVIEW no test! but this API contains also implementation
	// TODO REVIEW Comment
	public ServerConfiguration() {

	}

	private String urlMetada;

	private String urlWorkspace;

	/**
	 * @return the urlMetada
	 */
	// FIXME REVIEW do not return null
	public String getUrlMetada() {
		return this.urlMetada;
	}

	/**
	 * @param urlMetada
	 *            the urlMetada to set
	 */
	// TODO REVIEW use Object setArg(arg) signature, returning this; See Rule V4
	public void setUrlMetada(String urlMetada) {
		this.urlMetada = urlMetada;
	}

	/**
	 * @return the urlWorkspace
	 */
	// FIXME REVIEW do not return null
	public String getUrlWorkspace() {
		return this.urlWorkspace;
	}

	/**
	 * @param urlWorkspace
	 *            the urlWorkspace to set
	 */
	// TODO REVIEW use Object setArg(arg) signature, returning this;
	public void setUrlWorkspace(String urlWorkspace) {
		this.urlWorkspace = urlWorkspace;
	}

}
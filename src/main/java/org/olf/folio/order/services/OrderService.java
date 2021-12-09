package org.olf.folio.order.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;
import org.olf.folio.order.OrderImport;
import org.olf.folio.order.imports.FileStorageHelper;
import org.olf.folio.order.imports.LogHistory;
import org.olf.folio.order.imports.Results;


@Path ("/upload")
public class OrderService {

	Logger logger = Logger.getLogger(OrderService.class);
	@Context
	private HttpServletRequest servletRequest;
	private HttpServletResponse servletResponse;


	@POST
	@Produces("application/json")
	public Response uploadFile(
			@FormDataParam("order-file") InputStream marcFile,
			@FormDataParam("order-file") FormDataContentDisposition details)  {

		// define type of request (analyze only or actual import)
		boolean analyzeOnly = "true".equalsIgnoreCase(servletRequest.getParameter("analyzeOnly"));

		// initialize file names for input and results files and save the MARC input to file
    if (details.getFileName() == null || marcFile == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity(
							"No MARC file provided for this import request.").build();
		}

		// Store the MARC file and initialize names for files generated in the process.
		FileStorageHelper storage = FileStorageHelper.storeMarcFile	(marcFile, details.getFileName(),	analyzeOnly);

		// Run synchronous if just analysis (usually quick)
		if (analyzeOnly) {
			try {
				Results results = new OrderImport().runAnalyzeJob(storage);
				storage.storeResults(results);
				return Response.status(Response.Status.OK).entity(results.toJsonString()).build();
			} catch (Exception e) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
			}
		} else { // actual import, initialize results file
			Results importResults = new Results(true, storage).markStarted();
			try {
				importResults.setMarcRecordCount(OrderImport.countMarcRecords(storage));
				storage.storeResults(importResults);
			} catch (FileNotFoundException fnf) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity("Application error, could not find the MARC file that was only just saved: " + fnf.getMessage())
								.build();
			} catch (Exception e) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity("Could not store the initial, empty import results file: " + e.getMessage())
								.build();
			}
			// Run asynchronous if actual import
			new Thread(() -> {
				try {
					storage.storeResults(new OrderImport().runImportJob(storage, importResults));
				}
				catch (Exception e) {
					logger.error("There was a problem in the job thread: " + e.getMessage());
				}
			}).start();
			return Response.status(Response.Status.OK).entity(importResults.toJsonString()).build();
		}
	}

	@GET
	@Path("/results")
	@Produces("application/json")
	public Response results(
					@QueryParam("name") String resultsFileName)  {
		JSONObject results;
		try {
			logger.debug("Looking for results file for " + resultsFileName);
			results = FileStorageHelper.getResults(resultsFileName);
		} catch (IOException ioe) {
			logger.error("Error reading results file: " + resultsFileName + ": " + ioe.getMessage() + " - " + ioe.getCause());
			JSONObject error = new JSONObject();
			error.put("Error reading results file", ioe.getMessage());
			return Response.status(Response.Status.BAD_REQUEST).entity(error.toString(2)).build();
		}
		logger.debug("Responding with import results");
		return Response.status(Response.Status.OK).entity(results.toString(2)).build();
	}

	@GET
	@Path("/history")
	@Produces("application/json")
	public Response history()  {
		JSONObject history;
		try {
			history = LogHistory.loadDirectory();
		} catch (IOException ioe) {
			logger.error("Error reading loading history: " + ioe.getLocalizedMessage());
			JSONObject error = new JSONObject();
			error.put("error", ioe.getLocalizedMessage());
			return Response.status(Response.Status.BAD_REQUEST).entity(error.toString(2)).build();
		}
		logger.debug("Responding with history object " + history.toString(2));
		return Response.status(Response.Status.OK).entity(history.toString(2)).build();
	}

}

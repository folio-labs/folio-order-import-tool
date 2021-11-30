package org.olf.folio.order.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.OrderImport;


@Path ("/upload")
public class OrderService {

	Logger logger = Logger.getLogger(OrderService.class);
	@Context
	private HttpServletRequest servletRequest;
	private HttpServletResponse servletResponse;


	@POST
	@Produces("application/json")
	public Response uploadFile(
			@FormDataParam("order-file") InputStream uploadedInputStream,
			@FormDataParam("order-file") FormDataContentDisposition fileDetails)  {

		System.out.println(fileDetails.getFileName());
		String filePath = (String) servletRequest.getServletContext().getAttribute("uploadFilePath");
		String analyzeOnly = servletRequest.getParameter("analyzeOnly");
		boolean analyze = "true".equalsIgnoreCase(analyzeOnly);
		UUID fileName = UUID.randomUUID();
		String uploadedFileLocation = filePath + fileName + ".mrc";
		// SAVE FILE TO DISK
		writeFile(uploadedInputStream, uploadedFileLocation);
		// PASS FILE INFO TO 'OrderImport' WHICH MAKES THE FOLIO API CALLS
		OrderImport orderImport = new OrderImport();
		orderImport.setMyContext(servletRequest.getServletContext());
		try {
			JSONObject message = orderImport.upload(fileName + ".mrc", analyze);
			logger.info("Sending response to client: " + message.toString());
			return Response.status(Response.Status.OK).entity(message.toString()).build();		
		}
		catch(Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build();		
		}
	}

	@GET
	public Response justACheck()  {
		//RESET REFERENCE VALUES?
		return Response.status(Response.Status.OK).entity("OK").build();
	}

	private void writeFile(InputStream uploadedInputStream,
			String uploadedFileLocation) {
		try {
			int read;
			byte[] bytes = new byte[1024];
			OutputStream out = new FileOutputStream(uploadedFileLocation);
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.olf.folio.order.Constants;
import org.olf.folio.order.JustMarc;
import org.olf.folio.order.JustMarcFile;
import org.olf.folio.order.TestImport;





@Path ("/upload")
public class OrderService {

	@Context
	private HttpServletRequest servletRequest;
	private HttpServletResponse servletResponse;


	@POST
	@Produces("application/json")
	public Response uploadFile(
			@FormDataParam("order-file") InputStream uploadedInputStream,
			@FormDataParam("order-file") FormDataContentDisposition fileDetails) throws IOException, InterruptedException, Exception {

		System.out.println(fileDetails.getFileName());
		//TODO - CHANGE FILE NAME TO BE UNIQUE
		String filePath = (String) servletRequest.getServletContext().getAttribute("uploadFilePath");
		//String uploadedFileLocation = filePath + fileDetails.getFileName();
		UUID fileName = UUID.randomUUID();
		String uploadedFileLocation = filePath + fileName.toString() + ".mrc";
		// SAVE FILE TO DISK
		writeFile(uploadedInputStream, uploadedFileLocation);
		// PASS FILE INFO TO 'TestImport' WHICH MAKES THE FOLIO API CALLS
		TestImportOrderFirst testImport = new TestImportOrderFirst();
		testImport.setMyContext(servletRequest.getServletContext());
		try {
			//org.json.JSONObject message = testImport.upload(fileDetails.getFileName());
			org.json.JSONObject message = testImport.upload(fileName.toString() + ".mrc");
			return Response.status(Response.Status.OK).entity(message.toString()).build();		
		}
		catch(Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build();		
		}
		
	}
	
	@GET
	public Response justACheck() throws IOException, InterruptedException, Exception {

		    //RESET REFERENCE VALUES - SNAPSHOT CHANGES EVERY DAY
		   servletRequest.getServletContext().setAttribute(Constants.LOOKUP_TABLE,null);
		   return Response.status(Response.Status.OK).entity("OK").build();		
			

		
	}



	// save uploaded file to new location
	private void writeFile(InputStream uploadedInputStream,
			String uploadedFileLocation) {
		try {
			OutputStream out = new FileOutputStream(new File(
					uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			out = new FileOutputStream(new File(uploadedFileLocation));
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

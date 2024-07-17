package org.olf.folio.order.controllers;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

@MultipartConfig
public class OrderController extends HttpServlet {
	
	private static final Logger logger = Logger.getLogger(OrderController.class);

	// Session attributes
	public static String SESSION_USERNAME = "username";

	protected void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
		String username = request.getHeader("X-Remote-User");
		logger.debug("Maybe got username from session: " + username);
		if (username != null) {
			request.getSession().setAttribute(SESSION_USERNAME, username);
		}

		RequestDispatcher rd = request.getRequestDispatcher("WEB-INF/upload-order.jsp");
		rd.forward(request, response);	
	}

}

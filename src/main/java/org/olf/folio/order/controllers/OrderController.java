package org.olf.folio.order.controllers;

 
import java.io.IOException; 
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException; 
import javax.servlet.annotation.MultipartConfig; 
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 



@MultipartConfig
public class OrderController extends HttpServlet {
	
	protected void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
		 RequestDispatcher rd = null;
		 rd = request.getRequestDispatcher("WEB-INF/upload-order.jsp");
		 rd.forward(request, response);	
	}
	


}

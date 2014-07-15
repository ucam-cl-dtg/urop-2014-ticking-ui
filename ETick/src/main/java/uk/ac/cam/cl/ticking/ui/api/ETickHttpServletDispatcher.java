package uk.ac.cam.cl.ticking.ui.api;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

@WebServlet(urlPatterns = { "/api/*"}, initParams = {
		@WebInitParam(name = "javax.ws.rs.Application", value = "uk.ac.cam.cl.ticking.ui.api.ETickApplicationRegister"),
		@WebInitParam(name = "resteasy.servlet.mapping.prefix", value = "/api") })
public class ETickHttpServletDispatcher extends HttpServletDispatcher {
	private static final long serialVerisonUID = 1L;
}

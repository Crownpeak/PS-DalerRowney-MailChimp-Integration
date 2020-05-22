package com.dalerrowney;

import java.io.IOException;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.dalerrowney.mailchimp.*;

public class SignupServlet extends HttpServlet {
    protected MailChimpAPIClient mailChimp;

    public void init(ServletConfig config) throws ServletException {
        this.mailChimp =
            new MailChimpAPIClient();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    	Logger logger = new Logger();
    	
        Map<String, String[]> params = req.getParameterMap();
        String emailAddress = params.containsKey("email_address") ? params.get("email_address")[0] : "";
        String fname = params.containsKey("fname") ? params.get("fname")[0] : "";
        String lname = params.containsKey("lname") ? params.get("lname")[0] : "";
        String rawinterests = params.containsKey("interests") ? params.get("interests")[0] : "";
        if(emailAddress.length() > 0 && rawinterests.length() > 0)
        {
	        List<String> interests = Arrays.asList(rawinterests.split(","));
	        int result = mailChimp.registerMember(emailAddress, fname, lname, interests, logger);
	        resp.setStatus(result);
        }
        else {
        	logger.log("Cannot call API without email address and interests");
        	resp.setStatus(400);
        }
        resp.setContentType("text/plain");
        if(req.getHeader("x-debug") != null) {
        	try {
	        resp.getWriter().write(logger.toString());
	        resp.getWriter().flush();
        	}
        	catch(IOException e) {
        		// todo?
        	}
        }
    }
}
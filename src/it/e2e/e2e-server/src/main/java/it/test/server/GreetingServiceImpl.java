package it.test.server;

import it.test.model.FieldVerifier;
import it.test.model.GreetingResponse;
import it.test.shared.GreetingService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements
		GreetingService {

	public GreetingResponse greetServer(String input) throws IllegalArgumentException {
		// Verify that the input is valid.
		if (!FieldVerifier.isValidName(input)) {
			// If the input is not valid, throw an IllegalArgumentException back to
			// the client.
			throw new IllegalArgumentException(
					"Name must be at least 4 characters long");
		}

		GreetingResponse.Builder response = GreetingResponse.builder();

		response.setServerInfo(getServletContext().getServerInfo());
		response.setUserAgent(getThreadLocalRequest().getHeader("User-Agent"));

		response.setGreeting("Hello, " + input + "!");

		return response.build();
	}
}

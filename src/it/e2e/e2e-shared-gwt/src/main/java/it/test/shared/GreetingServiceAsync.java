package it.test.shared;

import it.test.model.GreetingResponse;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface GreetingServiceAsync {
	void greetServer(String input, AsyncCallback<GreetingResponse> callback)
			throws IllegalArgumentException;
}

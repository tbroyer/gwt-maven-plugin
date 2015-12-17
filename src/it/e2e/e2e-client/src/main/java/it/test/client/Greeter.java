package it.test.client;

import it.test.model.GreetingResponse;
import it.test.shared.GreetingService;
import it.test.shared.GreetingServiceAsync;
import com.google.auto.value.AutoValue;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Callback;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * This is a contrived example to use an annotation processor (AutoValue),
 * to test generated sources.
 */
class Greeter {
	/**
	 * Create a remote service proxy to talk to the server-side Greeting service.
	 */
	private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);

	void greet(String name, final Callback<Result, Throwable> callback) {
		greetingService.greetServer(name,
				new AsyncCallback<GreetingResponse>() {
					public void onFailure(Throwable caught) {
						callback.onFailure(caught);
					}

					public void onSuccess(GreetingResponse result) {
						callback.onSuccess(Result.create(new SafeHtmlBuilder()
								.appendEscaped(result.getGreeting())
								.appendHtmlConstant("<br><br>I am running ")
								.appendEscaped(result.getServerInfo())
								.appendHtmlConstant(".<br><br>It looks like you are using:<br>")
								.appendEscaped(result.getUserAgent())
								.toSafeHtml()));
					}
					});
	}

	@AutoValue
	static abstract class Result {
		public static Result create(SafeHtml html) {
			return new AutoValue_Greeter_Result(html);
		}

		abstract SafeHtml getHtml();
	}
}

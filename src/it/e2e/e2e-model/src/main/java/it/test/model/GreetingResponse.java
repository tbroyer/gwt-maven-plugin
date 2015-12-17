package it.test.model;

import java.io.Serializable;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.GwtCompatible;

@SuppressWarnings("serial")
@AutoValue
@GwtCompatible(serializable = true)
public abstract class GreetingResponse {
	public static Builder builder() {
		return new AutoValue_GreetingResponse.Builder();
	}

	public abstract String getGreeting();

	public abstract String getServerInfo();

	public abstract String getUserAgent();

	@AutoValue.Builder
	public static abstract class Builder {
		public abstract Builder setGreeting(String greeting);

		public abstract Builder setServerInfo(String serverInfo);

		public abstract Builder setUserAgent(String userAgent);

		public abstract GreetingResponse build();
	}
}

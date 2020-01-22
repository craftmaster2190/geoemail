package com.craftmaster.austin.geoemail;

import LocationIq.ApiClient;
import LocationIq.auth.ApiKeyAuth;
import com.locationiq.client.api.SearchApi;
import com.locationiq.client.model.Location;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

public enum Exceptional {
  ;

  public static <T> T get(Callable<T> callable) {
    try {
      return callable.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

public enum Functional {
  ;

  public static <T> Predicate<T> not(Predicate<T> predicate) {
    return predicate.negate();
  }
}

@SpringBootApplication
public class GeoemailApplication {

  public static void main(String[] args) {
    SpringApplication.run(GeoemailApplication.class, args);
  }
}

@Data
@org.springframework.context.annotation.Configuration
@ConfigurationProperties("geomail")
public class AppConfiguration {

  private String locationIqApiKey;
}

@Service
@RequiredArgsConstructor
public class LocationIqService {

  private final AppConfiguration appConfiguration;

  private final ThreadLocal<ApiClient> apiClient = ThreadLocal.withInitial(
      () -> {
        var defaultApiClient = new ApiClient();
        var key = (ApiKeyAuth) defaultApiClient.getAuthentication("key");
        key.setApiKey(appConfiguration.getLocationIqApiKey());
        return defaultApiClient;
      });

  public ApiClient get() {
    return apiClient.get();
  }

}

@Service
@RequiredArgsConstructor
public class GeocodingService {

  private final LocationIqService locationIqService;

  public Optional<Location> geocode(String address) {
    var searchApi = new SearchApi(locationIqService.get());
    var locationList = Exceptional.get(() -> searchApi
        .search(address, "JSON", 1, 0,
            null, null, 1, null, null,
            null, null, null, null));
    return Optional.ofNullable(locationList)
        .filter(Functional.not(List::isEmpty))
				.map(list -> list.get(0));
  }
}

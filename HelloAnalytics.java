import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.analyticsreporting.v4.AnalyticsreportingScopes;
import com.google.analyticsreporting.v4.Analyticsreporting;
import com.google.analyticsreporting.v4.model.ColumnHeader;
import com.google.analyticsreporting.v4.model.DateRange;
import com.google.analyticsreporting.v4.model.DateRangeValues;
import com.google.analyticsreporting.v4.model.Dimension;
import com.google.analyticsreporting.v4.model.GetReportsRequest;
import com.google.analyticsreporting.v4.model.GetReportsResponse;
import com.google.analyticsreporting.v4.model.Metric;
import com.google.analyticsreporting.v4.model.MetricHeaderEntry;
import com.google.analyticsreporting.v4.model.Report;
import com.google.analyticsreporting.v4.model.ReportRequest;
import com.google.analyticsreporting.v4.model.ReportRow;


/**
 * A simple example of how to access the Google Analytics API.
 */
public class HelloAnalytics {
  // Path to client_secrets.json file downloaded from the Developer's Console.
  // The path is relative to HelloAnalytics.java.
  private static final String CLIENT_SECRET_JSON_RESOURCE = "client_secrets.json";

  // Replace with your view ID.
  private static final String VIEW_ID = "<REPLACE_WITH_VIEW_ID>";

  // The directory where the user's credentials will be stored.
  private static final File DATA_STORE_DIR = new File(
      System.getProperty("user.home"), ".store/hello_analytics");

  private static final String APPLICATION_NAME = "Hello Analytics Reporting";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static NetHttpTransport httpTransport;
  private static FileDataStoreFactory dataStoreFactory;

  public static void main(String[] args) {
    try {
      Analyticsreporting service = initializeAnalyticsReporting();

      GetReportsResponse response = getReport(service);
      printResponse(response);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Initializes an authorized Analytics Reporting service object.
   *
   * @return The analytics reporting service object.
   * @throws IOException
   * @throws GeneralSecurityException
   */
  private static Analyticsreporting initializeAnalyticsReporting() throws GeneralSecurityException, IOException {

    httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

    // Load client secrets.
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
        new InputStreamReader(HelloAnalytics.class
            .getResourceAsStream(CLIENT_SECRET_JSON_RESOURCE)));

    // Set up authorization code flow for all authorization scopes.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
        .Builder(httpTransport, JSON_FACTORY, clientSecrets,
            AnalyticsreportingScopes.all()).setDataStoreFactory(dataStoreFactory)
        .build();

    // Authorize.
    Credential credential = new AuthorizationCodeInstalledApp(flow,
        new LocalServerReceiver()).authorize("user");
    // Construct the Analytics Reporting service object.
    return new Analyticsreporting.Builder(httpTransport, JSON_FACTORY, credential)
        .setApplicationName(APPLICATION_NAME).build();
  }

  /**
   * Query the Analytics Reporting API V4.
   * Constructs a request for the sessions for the past seven days.
   * Returns the API response.
   *
   * @param service
   * @return GetReportResponse
   * @throws IOException
   */
  private static GetReportsResponse getReport(Analyticsreporting service) throws IOException {
    // Create the DateRange object.
    DateRange dateRange = new DateRange();
    dateRange.setStartDate("7DaysAgo");
    dateRange.setEndDate("today");

    // Create the Metrics object.
    Metric sessions = new Metric()
        .setExpression("ga:sessions")
        .setAlias("sessions");

    //Create the Dimensions object.
    Dimension browser = new Dimension()
        .setName("ga:browser");

    // Create the ReportRequest object.
    ReportRequest request = new ReportRequest()
        .setViewId(VIEW_ID)
        .setDateRanges(Arrays.asList(dateRange))
        .setDimensions(Arrays.asList(browser))
        .setMetrics(Arrays.asList(sessions));

    ArrayList<ReportRequest> requests = new ArrayList<ReportRequest>();
    requests.add(request);

    // Create the GetReportsRequest object.
    GetReportsRequest getReport = new GetReportsRequest()
        .setReportRequests(requests);

    // Call the batchGet method.
    GetReportsResponse response = service.reports().batchGet(getReport).execute();

    // Return the response.
    return response;
  }

  /**
   * Parses and prints the Analytics Reporting API V4 response.
   *
   * @param response the Analytics Reporting API V4 response.
   */
  private static void printResponse(GetReportsResponse response) {

    for (Report report: response.getReports()) {
      ColumnHeader header = report.getColumnHeader();
      List<String> dimensionHeaders = header.getDimensions();
      List<MetricHeaderEntry> metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
      List<ReportRow> rows = report.getData().getRows();

      if (rows == null) {
         System.out.println("No data found for " + VIEW_ID);
         return;
      }

      for (ReportRow row: rows) {
        List<String> dimensions = row.getDimensions();
        List<DateRangeValues> metrics = row.getMetrics();
        for (int i = 0; i < dimensionHeaders.size() && i < dimensions.size(); i++) {
          System.out.println(dimensionHeaders.get(i) + ": " + dimensions.get(i));
        }

        for (int j = 0; j < metrics.size(); j++) {
          System.out.print("Date Range (" + j + "): ");
          DateRangeValues values = metrics.get(j);
          for (int k = 0; k < values.getValues().size() && k < metricHeaders.size(); k++) {
            System.out.println(metricHeaders.get(k).getName() + ": " + values.getValues().get(k));
          }
        }
      }
    }
  }
}

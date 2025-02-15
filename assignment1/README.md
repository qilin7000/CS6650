How to Run the Client

The setBasePath() method in PhaseThread.java also needs to match the correct server URL:
this.apiInstance = new SkiersApi(new ApiClient().setBasePath("http://localhost:8080/my_lab2-1.0-SNAPSHOT"));

Modify "http://localhost:8080/my_lab2-1.0-SNAPSHOT" to match your actual server.

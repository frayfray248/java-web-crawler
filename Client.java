import java.net.*;
import java.net.http.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.regex.*;

public class Client {

    //String representation of the website to be crawled
    private String website;

    //contains all the links found
    private ArrayList<String> links; 
    //contains all the links that haven't been crawled
    private ArrayList<String> newLinks;

    //constructor
    public Client(String website) {
        this.website = website;
        this.newLinks = new ArrayList<String>();
        this.links = new ArrayList<String>();
        //adding the root 
        this.links.add("/");
        this.newLinks.add("/");

    }

    /* This method calls the crawl method an amount of times and displays 
    the links found seperated by the level they were found. Parameter "depth"
    is the amount of levels to crawl. If no new links were found at a level, 
    "found no new links" will be displayed alongside the level seperator.
    */
    public void crawlLevels(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("\n\t\t LEVEL" + (i + 1) + ":");
            if (newLinks.size() == 0) System.out.print(" (found no new links)\n");
            System.out.println(links);
            crawl();
        }

    }


    /* This method searches for href links, using regular expressions in a 
    provided HttpResponse body and adds those links to the "links" class array list.
    Duplicate links are ignored. The new links array list is cleared at the beginning
    and the links found in the HttpResponse are added to the new links array.
    */
    public void parseLinks(String response) {
        newLinks.clear();

        //regex pattern and matcher
        Pattern linkPattern = Pattern.compile(
            "<a href=\"(.*)\"", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = linkPattern.matcher(response);

        //matcher loop
        String match = "";
        findMatch:
        while(matcher.find()) {
            match = matcher.group(1);

            //loop for ignoring duplicate links
            for (String link : links) {
                if (link.equals(match)) continue findMatch;
            }

            links.add(match);
            newLinks.add(match);
        }
    }


    /* This is the main crawl method. It  uses an HttpClient and a list of CompletableFutures
    to make requests to the class's "website". The CompletableFuture calls the parseLinks
    method to add links to the class's array list of links.
    */
    public void crawl() {

        //building URI list
        ArrayList<URI> urlList = new ArrayList<URI>();
        for(String url : newLinks) {
            urlList.add(URI.create(website + url));
        }

        //building the HttpRequest list
        List<HttpRequest> requests = urlList
            .stream()
            .map(url -> HttpRequest.newBuilder(url))
            .map(reqBuilder -> reqBuilder.build())
            .collect(Collectors.toList());

        //HttpClient
        HttpClient client = HttpClient.newHttpClient();

        //Building the completable future list
        CompletableFuture<?>[] asyncs = requests
            .stream()
            .map(request -> client
                .sendAsync(request,
                HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> parseLinks(response))) //parse links method call
            .toArray(CompletableFuture<?>[]::new);

        //Make all the http requests
        CompletableFuture.allOf(asyncs).join();
    }

    //Main method. Two program arguments are required: a website and an integer of levels to crawl
    public static void main(String[] args) {

        //arguement handling. The program exits if 2 arguments were not provided
        if (args.length != 2) {
            System.err.println("1 Url and an integer of levels to crawl required: ");
            System.err.println("<url> <number of levels>");
            System.exit(-1);
        }

        //argument handling. The program exits if an integer wasn't provided as the second argument
        try {
            int levels = Integer.parseInt(args[1]); 
        } catch (NumberFormatException e) {
            System.err.println("An integer of levels is required as the second argument.");
            System.exit(-1);
        }

        //building client object and having it crawl through the provided website
        try {
            Client client = new Client(args[0]);
            client.crawlLevels(Integer.parseInt(args[1]));

        } catch (Exception e) {
            System.out.println(e);
        }
        
    }
}
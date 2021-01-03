# Scoogle

Scoogle is a Search Engine written in Scala and using Akka Actors developed at *Bartholme Labs* in Crissier,
Switzerland. Its components are a webcrawler, a LevelDB key-value store, a server serving a backend API and a frontend
written in React. Since LevelDB does not support two simultaneous processes, it is advised to run the webcrawler first,
followed by the server once the webcrawler is stopped.

![demo](demo.gif)

## Webcrawler based on *Akka Actors*

The webcrawler takes as input the seed in the shape of an XML file. 
Find below an example of such a file. The different fields designate the following:

- `link` The link to a source. The webcrawler will take all the links as a seed.
- `depth` The depth up to which each source will be crawled.
- `crawlPresent` If the webcrawler encounters a link which is already present in the database, It will stop at this link if the option is set to `false`, otherwise it will go on crawling from this link.

```
<sources>
    <source>
        <link>https://www.wikipedia.com</link>
        <depth>3</depth>
        <crawlPresent>true</crawlPresent>
    </source>
    <source>
        <link>https://www.facebook.com</link>
        <depth>1</depth>
        <crawlPresent>false</crawlPresent>
    </source>
</sources>
```
#

Run the `WebCrawler.scala` file with the following command line arguments.

- `--databaseDirectory` The directory in which to put the database files. Defaults to *./target*.
- `--maxConcurrentSockets` The maximum number of TCP sockets that the program will open simultaneously. Sometimes if this number is too large, the requests will time out. The default is 30.
- `--sourceFilepath` The filepath of the source file containing the source links.
- `-h, --help` Show help message.

## Frontend written in *React*

Inside the *frontend* directory, run `npm start` in order to serve the frontend in developer mode. Run `npm run build` to build the production files. When the build has completed, the static files will be located inside the *target* directory of the maven project and will be served by the `Server` class.

The frontend consists of a home page with a searchbar. Upon hitting a *Search* button, the frontend displays a list of websites matching the keywords put in the searchbar.

## Server based on *Akka HTTP*

An *Akka HTTP* server runs and serves the following routes:

- `/` The frontend consisting of the static files inside the */build* folder
- `/api` Given a `query` HTTP parameter representing a list of space-separated keywords, this route will respond with a list of links that match against those keywords.

#

Run the `Server.scala` file with the following command line arguments.

- `--port` The port number on which the server listens.
- `--databaseDirectory` The directory containing the database files. Defaults to *./target*.
- `-h, --help` Show help message.

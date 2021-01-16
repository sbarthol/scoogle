# Scoogle

Scoogle is a Search Engine written in Scala and using Akka Actors developed at *Bartholme Labs* in Crissier,
Switzerland. Its components are a webcrawler, a HBase data store, a server serving a backend API and a frontend written
in React. The HBase master, ZooKeeper and servers need to be started first, either locally or remotely, either on the
local filesystem or on HDFS. The address and port number of the ZooKeeper Quorum server are given to the Scoogle
webcrawler and the server.

![demo](https://drive.google.com/uc?id=1A-X3PufeiBJ8SwROvygW2IEwrOQnnHvm)

## Webcrawler based on *Akka Actors*

### Seed

The webcrawler takes as input the seed in the shape of an XML file. Find below an example of such a file. The different
fields designate the following:

- `link` The link to a source. The webcrawler will take all the links as a seed.
- `depth` The depth up to which each source will be crawled.
- `crawlPresent` If the webcrawler encounters a link which is already present in the database, It will stop at this link
  if the option is set to `false`, otherwise it will go on crawling from this link.

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

### Running

Run the `WebCrawler.scala` file with the following command line arguments.

- `--databaseDirectory` The directory in which to put the database files. Defaults to *./target*.
- `--maxConcurrentSockets` The maximum number of TCP sockets that the program will open simultaneously. Sometimes if
  this number is too large, the requests will time out. The default is 30.
- `--sourceFilepath` The filepath of the source file containing the source links.
- `-h, --help` Show help message.

### Monitoring

The webcrawler exposes [Kamon](https://kamon.io) metrics in a [Prometheus](https://prometheus.io) format on a scraping
endpoint on ` http://localhost:9095`. A Prometheus server can be started in order to view the various Akka metrics.

![prometheus](https://drive.google.com/uc?id=1mODQzk9y-si5v7h4qNdABI5E6_OChzGv)

## Frontend written in *React*

Inside the *frontend* directory, run `npm start` in order to serve the frontend in developer mode. Run `npm run build`
to build the production files. When the build has completed, the static files will be located inside the *target*
directory of the maven project and will be served by the `Server` class.

The frontend consists of a home page with a searchbar. Upon hitting a *Search* button, the frontend displays a list of
websites matching the keywords put in the searchbar. The pagination groups the links by 10.

## Server based on *Akka HTTP*

An *Akka HTTP* server runs and serves the following routes:

- `/` The frontend consisting of the static files inside the */build* folder
- `/api` Given a `query` HTTP parameter representing a list of space-separated keywords and a `pageNumber` parameter,
  this route will respond with a list of at most ten links that match against those keywords for the given `pageNumber`.

#

Run the `Server.scala` file with the following command line arguments.

- `--port` The port number on which the server listens.
- `--databaseDirectory` The directory containing the database files. Defaults to *./target*.
- `-h, --help` Show help message.

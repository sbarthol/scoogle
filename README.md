# Scoogle

Scoogle is a Search Engine written in Scala and using Akka Actors developed at *Bartholme Labs* in Crissier,
Switzerland. Its components are a web crawler, a HBase data store, a server serving a backend API and a frontend written
in React. The HBase master, ZooKeeper and servers need to be started first, either locally or remotely, either on the
local filesystem or on HDFS. The address and port number of the ZooKeeper Quorum server are given to the Scoogle web
crawler and server.

![demo](https://drive.google.com/uc?id=1A-X3PufeiBJ8SwROvygW2IEwrOQnnHvm)

## Web Crawler based on *Akka Actors*

### Seed

The web crawler takes as input the seed in the shape of one or more XML files. Find below an example of such a file. The
different fields designate the following:

- `link` The link to a source. This can be a file on the local filesystem or a http(s) website. The web crawler will
  take all the links as a seed.
- `depth` The depth up to which each source will be crawled.

```xml
<sources>
    <source>
        <link>https://www.wikipedia.com</link>
        <depth>3</depth>
    </source>
    <source>
        <link>file:///path/to/file.html</link>
        <depth>1</depth>
    </source>
</sources>
```

### Running

After running `mvn clean package`, run `java -jar ./target/web-crawler.jar [options] [source ...]`

The options are

- `--zooKeeperAddress` The address of the ZooKeeper Quorum server. Defaults to `localhost`.
- `--zooKeeperPort` The port of the ZooKeeper Quorum server. Defaults `2181`.
- `--maxConcurrentSockets` The maximum number of TCP sockets that the program will open simultaneously. Sometimes if
  this number is too large, the requests will time out. The default is 30.
- `-h, --help` Show help message.

followed by a list of source xml files in the format described above.

### Terminating

Ideally the program should be terminated with a `SIGTERM` signal rather than `SIGINT`, `SIGABRT` or `SIGKILL` so that
the HBase connection can close gracefully and flush buffered data.

### Monitoring

The web crawler exposes [Kamon](https://kamon.io) metrics in a [Prometheus](https://prometheus.io) format on a scraping
endpoint on ` http://localhost:9095`. A Prometheus server can be started in order to view the various Akka metrics.

![prometheus](https://drive.google.com/uc?id=1PFvHFVYTiBU629cWccnwKlS08UEW6Blr)

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

After running `mvn clean package`, run `java -jar ./target/server.jar [options]`

The options are

- `--port` The port number on which the server listens.
- `--zooKeeperAddress` The address of the ZooKeeper Quorum server. Defaults to `localhost`.
- `--zooKeeperPort` The port of the ZooKeeper Quorum server. Defaults `2181`.
- `-h, --help` Show help message.

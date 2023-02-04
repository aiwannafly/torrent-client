# Torrent Client
A torrent-client made with use of `BitTorrent` peer-to-peer network protocol and implemented in Java.
It also has a graphical interface separated from a business login (MVC), powered by the `JavaFX` framework.

For the torrent file parsing was used https://github.com/m1dnight/torrent-parser.

The peers may upload their files, download them from others, they discover information about each other
from a simple torrent tracker, which implementation you may found in the `TrackerServer` class.

While a process of uploading file parts may be simple, downloading a file, especially downloading many files,
may be a little bit more complicated. All *downloaders* share one thread pool to execute their tasks, so it's
the basic thing of this client's work.

Here is the process of choosing a file for the distribution.

[![torrent1.png](https://i.postimg.cc/8cWfvSkC/torrent1.png)](https://postimg.cc/rRVp67r6)

And the process of uploading and downloading.

[![torrent2.png](https://i.postimg.cc/HLcYntkY/torrent2.png)](https://postimg.cc/3yh5bmGV)

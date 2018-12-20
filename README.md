# tanks2d
For now a quick and dirty proof of concept for multiplayer 2d tanks game.

Build:
```
mvn clean package
```

Starting the server:
```
java -jar tanks2d.jar --server
```

Joining game (change IP if server is not running locally):
```
java -jar tanks2d.jar --join 127.0.0.1
```
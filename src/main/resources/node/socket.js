var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var redis = require("redis")
    , subscriber = redis.createClient()
    , publisher = redis.createClient();

http.listen(3000, function(){
    console.log('listening on *:3000');
});

io.on('connection', function(socket){
    console.log("User with Ip " + socket.handshake.headers.origin + " has Connected " + new Date())
    publisher.lpush("connected:users_log", JSON.stringify(socket.handshake))
    publisher.publish("connected", socket.handshake.headers.origin);

    socket.on('disconnect', function(){
        console.log('user disconnected');
        publisher.publish("disconnected", socket.handshake.headers.origin);
    });
});

subscriber.on("message", function(channel, message) {
    io.emit('locationEvent', message)
    console.log("Message '" + message + "' on channel '" + channel + "' arrived!")
});

subscriber.subscribe("location");

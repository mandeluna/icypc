# icypc
### *2011 ACM Queue ICPC Programming Challenge*

This is a programming challenge based on an ICPC challenge problem in about 2009.
The problem was later picked up by ACM Queue in about 2011. It's been a while so
it's a bit difficult to find the specific details, so I've copied the game rules
and mechanics to (http://wart.ca/icypc/doc/).

## Dependencies

There is a JAR file `icypc.jar` which runs the players, and there are several 
dependent libraries that have been published to (http://wart.ca/icypc/ICPC_Challenge.zip)

## Running the Code

Once you've unpacked everything and compiled the code, you can run the examples as
follows:

```
export CLASSPATH='.:lib/*:out/production/ICPC_Challenge'
java -ea -jar icypc.jar -player java icypc.Hunter -player java oocl.icypc.Seeker
```

## Debugging

Extensive output will be written to `stderr` if the `SEEKER_DEBUG` environment variable
is set.

```
export SEEKER_DEBUG=true
```

## Enhancements

The `Seeker` class is based on the examples provided in the archive referred to above.

The main changes are as follows:

1. Player repositioning algorithm is used instead of random movement
2. Path finding (basic breadth-first search) to avoid collisions with obstacles and other players
3. Somewhat improved targeting
4. Improved snowman building
5. Unit tests to support most of the above

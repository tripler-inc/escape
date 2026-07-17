# escape
3D Maze Escape Game created entirely using AI with VS Code.
This is my second attempt creating a 3D Maze game from scratch with AI assistance.  This time, I had it create a plan first, with lots of detail about how it should work.  The rest was much better visual rendering and smooth scrolling movement, rather than block movement like the previous attempt.
Another difference is that it built the code modularly as several classes, rather than one long source code file.
After it built the code, I tested it and found a few issues.  For example, there are coins to collect, but you couldn't see them.  Your coin counter would just increment, seemingly randomly.  So I had to tell the AI that we need to see the coins and that they should appear, when they are in front of the player not obstructed by a wall, floating in midair.  It did a reasonable job interpreting my request.
Then I had to tell it that the ladder to the next level needs to be visible as well, and it did a pretty good job with that too, except the ladder was rendered as an opaque rectangle.  With a quick correction, it realised its mistake and corrected the ladder.  I was then able to tweak its appearance to make it look better.
Again, however, I had to also tell it that the key needed to be visible.  So it rendered a key.
I haven't decided whether I like this version better or not.

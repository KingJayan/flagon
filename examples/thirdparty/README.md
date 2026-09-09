# A third-party LWJGL 3 application

`HelloWorld.java` is the sample program from [LWJGL's own getting-started
guide](https://www.lwjgl.org/guide), taken from the
[lwjgl3-www](https://github.com/LWJGL/lwjgl3-www) repository
(`client/routes/guide/sample.java`). LWJGL is BSD-3-Clause.

One line was added and nothing else: a `package` declaration. The guide publishes the sample as a
snippet with no package, which no Maven project can compile; adding it is the single edit any user
of that sample has to make. Every other line is as published.

It is here to answer one question: does code nobody on this project wrote, compiled against the
real LWJGL 3 artifacts, run in the browser against the compatibility layer? This module compiles
it against `org.lwjgl:lwjgl`, `lwjgl-glfw` and `lwjgl-opengl` exactly as a desktop project would.
`examples/bundle` then compiles the same class against the compatibility layer instead.

Do not edit the file. Its value is that it is unmodified.

# Contributing to Mantle

Thanks for your interest in Mantle. This is an early-stage project and things move fast, so this
guide is short and will grow as the project does.

## Before you start

- Mantle targets Minecraft 1.21.1 on NeoForge and builds with JDK 21.
- Build with `./gradlew build` and run the dev client with `./gradlew runClient`.
- Open an issue before starting anything large. The simulation pieces are tightly coupled, and a
  quick conversation up front saves a lot of rework.

## Good practices

- **Keep it deterministic.** The whole simulation is built around the same seed always producing
  the same terrain. Do not introduce threading races, unordered iteration, or anything that pulls
  from a shared RNG in a way that changes results between runs. If your change touches generation,
  confirm the output is still stable across two runs of the same seed.
- **Verify your terrain changes.** The `verify/` directory holds standalone tools that render and
  check the terrain without launching Minecraft. Use them. If you change how rivers, erosion, or
  drainage behave, run the relevant tool and make sure you did not introduce floating water, stone
  sills, disconnected rivers, or similar artifacts.
- **Match the surrounding style.** Read the nearby code and follow its naming, structure, and
  comment density. Small, focused commits with clear messages are easier to review than one giant
  change.
- **Test in-game when it matters.** Some artifacts only show up once the world is actually
  generated. If your change affects what the player sees, load a fresh world and look at it.

## On AI use

AI tools are allowed. A bit of this project was built with them, so it would be hypocritical to
ban them.

That said, the bar is the same as for any other contribution: the code has to actually work and it
has to be code you understand and stand behind. Do not open a pull request full of generated slop
you have not read, run, or verified. If you used an AI tool to help write a change, you are still
responsible for it. Review it, test it, make sure it does what it claims, and clean it up so it
fits the rest of the codebase. Pull requests that are clearly unreviewed machine output will be
closed.

In short: use whatever tools help you, but ship working code you can explain.

## Pull requests

- Describe what the change does and why.
- Note how you tested it, including any `verify/` tools you ran or in-game checks you made.
- Keep unrelated changes out of the same pull request.

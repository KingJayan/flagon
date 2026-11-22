export interface Example {
  name: string;
  code: string;
}

export const EXAMPLES: readonly Example[] = [
  {
    name: 'Hello',
    code: `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from Java, compiled in your browser.");
    }
}
`,
  },
  {
    name: 'Records and streams',
    code: `import java.util.Comparator;
import java.util.List;

public class Main {
    record Planet(String name, double au) {}

    public static void main(String[] args) {
        var planets = List.of(
                new Planet("Mercury", 0.39), new Planet("Venus", 0.72), new Planet("Earth", 1.0),
                new Planet("Mars", 1.52), new Planet("Jupiter", 5.2));
        planets.stream()
                .filter(planet -> planet.au() > 0.5)
                .sorted(Comparator.comparingDouble(Planet::au).reversed())
                .forEach(planet -> System.out.println(planet.name() + " " + planet.au() + " AU"));
    }
}
`,
  },
  {
    name: 'Sealed types and switch',
    code: `public class Main {
    sealed interface Shape permits Circle, Square {}

    record Circle(double radius) implements Shape {}

    record Square(double side) implements Shape {}

    static double area(Shape shape) {
        return switch (shape) {
            case Circle circle -> Math.PI * circle.radius() * circle.radius();
            case Square square -> square.side() * square.side();
        };
    }

    public static void main(String[] args) {
        for (Shape shape : new Shape[] {new Circle(1), new Square(2)}) {
            System.out.println(shape + " has area " + area(shape));
        }
    }
}
`,
  },
  {
    name: 'Threads',
    code: `public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            for (int tick = 1; tick <= 3; tick++) {
                System.out.println("worker tick " + tick);
                try {
                    Thread.sleep(250);
                } catch (InterruptedException stopped) {
                    return;
                }
            }
        });
        worker.start();
        System.out.println("main waits for the worker");
        worker.join();
        System.out.println("worker done");
    }
}
`,
  },
];

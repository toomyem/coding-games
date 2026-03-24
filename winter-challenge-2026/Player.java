import java.io.File;
import java.util.*;

enum Dir {
    UP, LEFT, RIGHT, DOWN
}

record Pos(int x, int y) {
    public Pos move(Dir dir) {
        return switch (dir) {
            case UP -> new Pos(x, y - 1);
            case DOWN -> new Pos(x, y + 1);
            case LEFT -> new Pos(x - 1, y);
            case RIGHT -> new Pos(x + 1, y);
        };
    }

    @Override
    public String toString() {
        return x + "," + y;
    }
}

record Snake(int id, Pos head, List<Pos> body) {
    public static Snake of(int id, String body) {
        List<Pos> snakeBody = new ArrayList<>();
        for (String c : body.split(":")) {
            int x = Integer.parseInt(c.split(",")[0]);
            int y = Integer.parseInt(c.split(",")[1]);
            snakeBody.add(new Pos(x, y));
        }
        Pos head = snakeBody.removeFirst();
        return new Snake(id, head, snakeBody);
    }

    public Snake move(Dir dir, Board board) {
        Pos newHead = head.move(dir);
        List<Pos> newBody = new ArrayList<>(body);
        newBody.addFirst(head);
        if (!board.isPowerUp(newHead)) {
            newBody.removeLast();
        }
        return new Snake(id, newHead, newBody);
    }

    public Snake fall(Board board) {
        Pos newHead = head;
        List<Pos> newBody = new ArrayList<>(body);
        while (board.isEmpty(newHead.move(Dir.DOWN)) && newBody.stream().allMatch(pos -> board.isEmpty(pos.move(Dir.DOWN)))) {
            newHead = newHead.move(Dir.DOWN);
            newBody.replaceAll(pos -> pos.move(Dir.DOWN));
            if (newHead.y() >= board.getHeight() && newBody.stream().allMatch(pos -> pos.y() >= board.getHeight())) {
                return null;
            }
        }
        return new Snake(id, newHead, newBody);
    }
}

record Weight(int weight, List<Dir> steps) {
}

class Board {
    private final int width;
    private final int height;
    private final Set<Pos> walls = new HashSet<>();
    private final Set<Pos> powers = new HashSet<>();
    private final Set<Pos> snakes = new HashSet<>();
    private final Set<Pos> heads = new HashSet<>();
    private final boolean shouldDraw;
    private final Map<Pos, Weight> weights = new HashMap<>();

    public Board(int width, int height, boolean shouldDraw) {
        this.width = width;
        this.height = height;
        this.shouldDraw = shouldDraw;
    }

    public int getHeight() {
        return height;
    }

    public void addWalls(Set<Pos> walls) {
        this.walls.addAll(walls);
    }

    public void addPowerUps(Set<Pos> powers) {
        this.powers.addAll(powers);
    }

    public boolean isPowerUp(Pos pos) {
        return powers.contains(pos);
    }

    public void addSnake(Snake snake) {
        heads.add(snake.head());
        snakes.addAll(snake.body());
    }

    public void removeSnake(Snake snake) {
        heads.remove(snake.head());
        snake.body().forEach(snakes::remove);
    }

    public boolean isEmpty(Pos pos) {
        return !walls.contains(pos) && !powers.contains(pos) && !snakes.contains(pos) && !heads.contains(pos);
    }

    public boolean isAllowed(Pos pos) {
        return !walls.contains(pos) && !snakes.contains(pos) && !heads.contains(pos);
    }

    public boolean isRisky(Pos pos) {
        return heads.stream()
                .flatMap(p -> Arrays.stream(Dir.values()).map(p::move))
                .filter(this::isAllowed).anyMatch(p -> p.equals(pos));
    }

    public void reset() {
        powers.clear();
        heads.clear();
        snakes.clear();
        weights.clear();
    }

    private String move(int id, Dir d, String msg) {
        return id + " " + d + " " + msg;
    }

    public String calcMove(Snake s) {
        weights.clear();
        Queue<Snake> queue = new ArrayDeque<>();
        queue.add(s);
        weights.put(s.head(), new Weight(0, new LinkedList<>()));

        while (!queue.isEmpty()) {
            Snake snake = queue.poll();
            Pos head = snake.head();
            Weight weight = getWeight(head);

            if (powers.contains(head)) {
                powers.remove(head);
                return "MARK " + head.x() + " " + head.y() + ";" + move(s.id(), weight.steps().getLast(), head.toString());
            }
            int w = weight.weight() + 1;

            for (Dir dir : Dir.values()) {
                draw(snake);
                Snake newSnake = snake.move(dir, this);
                head = newSnake.head();
                if (head.x() < -1 || head.x() >= width+1 || head.y() < -1) continue;
                if (!isAllowed(head) || newSnake.body().contains(head)) continue;
                if (isRisky(head)) continue;
                newSnake = newSnake.fall(this);
                if (newSnake == null) continue;
                head = newSnake.head();
                draw(newSnake);
                if (w < getWeight(head).weight()) {
                    List<Dir> steps = new LinkedList<>(weight.steps());
                    steps.addFirst(dir);
                    weights.put(head, new Weight(w, steps));
                    queue.add(newSnake);
                }
            }
        }

        draw(s);
        for (Dir dir : Dir.values()) {
            Snake snake = s.move(dir, this);
            Pos head = snake.head();
            if (head.x() < -1 || head.x() >= width+1) continue;
            if (isAllowed(head) && !snake.body().contains(head) && !isRisky(head)) {
                return move(s.id(), dir, "!");
            }
        }
        return move(s.id(), Dir.UP, "?");
    }

    public Weight getWeight(Pos pos) {
        return weights.getOrDefault(pos, new Weight(1000, new LinkedList<>()));
    }

    public void draw(Snake snake) {
        if (!shouldDraw) return;
        for (int y = 0; y < height; y++) {
            String line = "";
            for (int x = 0; x < width; x++) {
                char c = '.';
                Pos pos = new Pos(x, y);
                if (walls.contains(pos)) {
                    c = '#';
                } else if (snakes.contains(pos) || snake.body().contains(pos)) {
                    c = 'o';
                } else if (heads.contains(pos) || snake.head().equals(pos)) {
                    c = '*';
                } else if (powers.contains(pos)) {
                    c = 'x';
                }
                line += c;
            }
            System.out.println(line);
        }
        System.out.println("===================");
    }
}

class Player {
    private static Set<Pos> readWalls(int width, int height, Scanner in) {
        Set<Pos> walls = new HashSet<>();
        for (int y = 0; y < height; y++) {
            String row = in.nextLine();
            for (int x = 0; x < width; x++) {
                char cell = row.charAt(x);
                if (cell == '#') {
                    walls.add(new Pos(x, y));
                }
            }
        }
        return walls;
    }

    private static Set<Integer> readIds(int n, Scanner in) {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < n; i++) {
            int id = in.nextInt();
            ids.add(id);
        }
        return ids;
    }

    private static Set<Pos> readPowerUps(int n, Scanner in) {
        Set<Pos> powers = new HashSet<>();
        for (int i = 0; i < n; i++) {
            int x = in.nextInt();
            int y = in.nextInt();
            powers.add(new Pos(x, y));
        }
        return powers;
    }

    private static List<Snake> readSnakes(int n, Scanner in) {
        List<Snake> snakes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int id = in.nextInt();
            String body = in.next();
            snakes.add(Snake.of(id, body));
        }
        return snakes;
    }

    public static void main(String[] args) throws Exception {
        Scanner in;
        if (args.length == 0) {
            in = new Scanner(System.in);
        } else {
            in = new Scanner(new File(args[0]));
        }
        /*int myId =*/ in.nextInt();
        int width = in.nextInt();
        int height = in.nextInt();
        if (in.hasNextLine()) {
            in.nextLine();
        }
        Board board = new Board(width, height, args.length > 0);
        board.addWalls(readWalls(width, height, in));
        int snakebotsPerPlayer = in.nextInt();
        Set<Integer> mySnakebotIds = readIds(snakebotsPerPlayer, in);
        /*Set<Integer> oppSnakebotIds =*/ readIds(snakebotsPerPlayer, in);

        // game loop
        for (int step = 0; step < 200; step++) {
            board.reset();
            int powerSourceCount = in.nextInt();
            board.addPowerUps(readPowerUps(powerSourceCount, in));
            int snakebotCount = in.nextInt();
            List<Snake> snakes = readSnakes(snakebotCount, in);
            snakes.sort(Comparator.comparingInt(s -> -s.body().size()));
            snakes.forEach(board::addSnake);

            List<String> actions = new ArrayList<>();
            for (Snake snake : snakes) {
                int id = snake.id();
                if (!mySnakebotIds.contains(id)) continue;
                board.removeSnake(snake);
                String move = board.calcMove(snake);
                board.addSnake(snake);
                actions.add(move);
            }
            System.out.println(String.join(";", actions));
        }
    }
}

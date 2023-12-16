import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class MazeSolver extends JFrame {
private static final int CELL_SIZE = 30;

private int[][] maze;
private int N;
private SwingWorker<Void, Void> mazeSolverWorker;
private int currentThreadID=1;
private int[] otherThreadID={currentThreadID};
private Map<Integer,Color> threadsData = new ConcurrentHashMap<>(Map.of(1,Color.GREEN,
2,Color.RED,
3,Color.BLUE,
4,new Color(174, 255, 0),
5,Color.PINK,
6,Color.ORANGE,
7,Color.CYAN,
8,Color.GRAY,
9,Color.MAGENTA
));
private ExecutorService executorService;
private AtomicInteger  counter = new AtomicInteger(1);
MazeSolver(int N) {
    this.N = N;
    this.maze = generateMaze(N);
    int numThreads = Runtime.getRuntime().availableProcessors();
    ThreadFactory threadFactory = new ThreadFactory(){
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(() -> {
                r.run();
            });
            return thread;
        }
    };
    executorService=Executors.newFixedThreadPool(numThreads,threadFactory);

    setTitle("Rat in a Maze Solver");
    setSize((N+1) * CELL_SIZE-15, (N+2) * CELL_SIZE-20);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    setVisible(true);
    //solveMaze(0, 0,currentThreadID);
}
private void randomMaze(int[][] maze){
    Random random = new Random();
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            if (i == 0 && j == 0||i==N-1 && j==N-1) {
                continue;
            }
            maze[i][j] = random.nextDouble() < 0.3 ? 1 : 0;
        }
    }
}
private int[][] generateMaze(int N) {
    int[][] maze = new int[N][N];

    maze[0][0] = 0;
    maze[N - 1][N - 1] = 0;

    randomMaze(maze);

    while (!hasPath(maze, 0, 0, N - 1, N - 1)) {
        randomMaze(maze);
    }
    // maze[N-1][N-2]=1;
    // maze[N-2][N-1]=1;
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            System.out.print(maze[i][j]+" ");
        }
        System.out.print("\n");
    }
    return maze;
}


private boolean hasPath(int[][] maze, int startX, int startY, int endX, int endY) {
    return dfs(maze, startX, startY, endX, endY);
}

private boolean dfs(int[][] maze, int x, int y, int endX, int endY) {
    if (x == endX && y == endY) {
        return true;
    }

    if (isValidMove(x + 1, y, maze) && dfs(maze, x + 1, y, endX, endY)) {
        return true;
    }

    if (isValidMove(x, y + 1, maze) && dfs(maze, x, y + 1, endX, endY)) {
        return true;
    }
    return false;
}

private synchronized boolean isValidMove(int x, int y, int[][] maze) {
    return x < N && y < N && maze[x][y] == 0;
}
private synchronized void solveMaze(int x, int y,int currentThreadID) {
    mazeSolverWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (x == N - 1 && y == N - 1) {
                    System.out.println("Maze Solved!");
                    executorService.shutdownNow();
                    return null;
                }
                else if (isValidMove(x + 1, y, maze) && isValidMove(x, y + 1, maze)) {
                    counter.incrementAndGet();
                    // Continue in the main thread for one direction
                    moveRat(x, y + 1,currentThreadID);
                    solveMaze(x, y + 1,currentThreadID);
                    SwingWorker<Void, Void> threadX = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() {
                            synchronized (MazeSolver.this) {
                                otherThreadID[0]++;
                                if(otherThreadID[0]>9)
                                    threadsData.put(otherThreadID[0], threadColorForCell());
                                moveRat(x + 1, y,otherThreadID[0]);
                                solveMaze(x + 1, y,otherThreadID[0]);
                            }
                            return null;
                        }
                    };
                    threadX.execute();
                    try {
                        threadX.get(); // Wait for the thread exploring direction Y to finish
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else if (isValidMove(x + 1, y, maze)) {
                    moveRat(x + 1, y,currentThreadID);
                    solveMaze(x + 1, y,currentThreadID);
                } else if (isValidMove(x, y + 1, maze)) {
                    moveRat(x, y + 1,currentThreadID);
                    solveMaze(x, y + 1,currentThreadID);
                }
                else{
                    counter.decrementAndGet();
                }
                return null;
            }
        };
        mazeSolverWorker.execute();
}

private void moveRat(int x, int y,int currentThreadID) {
        if (maze[x][y] != 0) {
            return;
        }
        maze[x][y] = currentThreadID+1; // Mark the path
        repaint(); // Update GUI
        try {
                Thread.sleep(200); // Simulate some delay for visualization
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    return;
}

@Override
public void paint(Graphics g) {
    super.paint(g);
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            String str="";
            if(i==0&&j==0||i==N-1&&j==N-1){
                g.setColor(Color.YELLOW);
                str = i==0&&j==0? "S":"E";
            }
            else if (maze[i][j] == 1) {
                g.setColor(Color.BLACK); // Blocked cell
            } else if (maze[i][j] == 0) {
                g.setColor(Color.WHITE); // Open cell
            } else {
                g.setColor(threadsData.get(maze[i][j]-1));
            }
            g.fillRect(j * CELL_SIZE + getInsets().left, i * CELL_SIZE + getInsets().top, CELL_SIZE, CELL_SIZE);
            g.setColor(Color.BLACK);
            g.drawRect(j * CELL_SIZE + getInsets().left, i * CELL_SIZE + getInsets().top, CELL_SIZE, CELL_SIZE);
            if(str!=""){
                g.setFont(new Font("SansSerif", Font.BOLD, 16));
                g.setColor(Color.BLACK);
                g.drawString(str, j * CELL_SIZE + getInsets().left + (CELL_SIZE - g.getFontMetrics().stringWidth(str)) / 2, i * CELL_SIZE + getInsets().top + (CELL_SIZE + g.getFontMetrics().getHeight()) / 2);
            }
        }
    }
}

private Color threadColorForCell() {
    Random random = new Random();
    int red = random.nextInt(151) + 50;
    int green = random.nextInt(151) + 50;
    int blue = random.nextInt(151) + 50;
    return new Color(red, green, blue);
}

public static void main(String[] args) {
    int N=10;
    MazeSolver mazeSolver =new MazeSolver(N);

    SwingUtilities.invokeLater(() -> {
            mazeSolver.solveMaze(0, 0, mazeSolver.currentThreadID);
    });
    while(mazeSolver.counter.get()>0);
    if (!mazeSolver.hasPath(mazeSolver.maze, 0, 0, N - 1, N - 1))
        System.out.println("no solution");
    }
}
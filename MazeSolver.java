import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
private volatile boolean shouldTerminate = false;
private AtomicBoolean mazeSolved = new AtomicBoolean(false);
Set <Integer> threadsInProgress = new CopyOnWriteArraySet<>(Collections.singleton(currentThreadID));
MazeSolver(int N) {
    this.N = N;
    this.maze = generateMaze(N);
    // Limit the number of threads to the available processor cores
    int numThreads = Runtime.getRuntime().availableProcessors();
    ThreadFactory threadFactory = new ThreadFactory(){
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(() -> {
                r.run(); // Run the actual task
            });
            return thread;
        }
    };
    Executors.newFixedThreadPool(numThreads,threadFactory);
    //ExecutorService executor = Executors.newFixedThreadPool(numThreads,threadFactory);

    setTitle("Rat in a Maze Solver");
    setSize((N+1) * CELL_SIZE-15, (N+2) * CELL_SIZE-20);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    setVisible(true);

    solveMaze(0, 0,currentThreadID); // Start solving maze

        // Check if the maze was solved after solveMaze is complete
    // if (!mazeSolved.get()) {
    //     System.out.println("no solution");
    // }
}
private int[][] generateMaze(int N) {
    int[][] maze = new int[N][N];
    Random random = new Random();

    // Ensure start and end points are open
    maze[0][0] = 0;
    maze[N - 1][N - 1] = 0;

    // Create a random maze with 30% blocked paths
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            if (i == 0 && j == 0||i==N-1 && j==N-1) {
                continue;
            }
            maze[i][j] = random.nextDouble() < 0.3 ? 1 : 0;
        }
    }
    // Ensure there is a path from the start to the end
    while (!hasPath(maze, 0, 0, N - 1, N - 1)) {
        // If there is no path, regenerate the maze
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i == 0 && j == 0||i==N-1 && j==N-1) {
                    continue;
                 }
                maze[i][j] = random.nextDouble() < 0.4 ? 1 : 0;
            }
        }
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
    boolean[][] visited = new boolean[N][N];
    return dfs(maze, startX, startY, endX, endY, visited);
}

private boolean dfs(int[][] maze, int x, int y, int endX, int endY, boolean[][] visited) {
    if (x == endX && y == endY) {
        return true;
    }

    if (isValidMove(x + 1, y, maze, visited) && dfs(maze, x + 1, y, endX, endY, visited)) {
        return true;
    }

    if (isValidMove(x, y + 1, maze, visited) && dfs(maze, x, y + 1, endX, endY, visited)) {
        return true;
    }

    return false;
}

private synchronized boolean isValidMove(int x, int y, int[][] maze, boolean[][] visited) {
    return x >= 0 && x < N && y >= 0 && y < N && maze[x][y] == 0 && !visited[x][y];
}

private synchronized void solveMaze(int x, int y,int currentThreadID) {
    mazeSolverWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                threadsInProgress.add(currentThreadID);
                AtomicBoolean isValidX = new AtomicBoolean(false);
                AtomicBoolean isValidY = new AtomicBoolean(false);
                synchronized(MazeSolver.this){
                    isValidX.set(isValidMove(x + 1, y, maze, new boolean[N][N]));
                    isValidY.set(isValidMove(x, y + 1, maze, new boolean[N][N]));
                }
                //System.out.println("currentThreadID : "+currentThreadID);
                if (x == N - 1 && y == N - 1) {
                    mazeSolved.getAndSet(true);
                    threadsInProgress.clear();
                    System.out.println("Maze Solved!");
                    return null;
                }
                else if (isValidX.get() && isValidY.get()) {
                    // Continue in the main thread for one direction
                    moveRat(x, y + 1,currentThreadID);
                    solveMaze(x, y + 1,currentThreadID);
                    //System.out.println("otherThreadID : "+otherThreadID);
                    // Explore the second direction in a separate thread
                    SwingWorker<Void, Void> threadX = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() {
                            synchronized (MazeSolver.this) {
                                otherThreadID[0]++;
                                if(otherThreadID[0]>9)
                                    threadsData.put(otherThreadID[0], threadColorForCell());
                                Thread.currentThread().setPriority(Thread.MIN_PRIORITY+Math.round((int)(x*y)/10));
                                threadsInProgress.add(otherThreadID[0]);
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
                } else if (isValidX.get()) {
                    moveRat(x + 1, y,currentThreadID);
                    solveMaze(x + 1, y,currentThreadID);
                } else if (isValidY.get()) {
                    moveRat(x, y + 1,currentThreadID);
                    solveMaze(x, y + 1,currentThreadID);
                }
                else{
                    threadsInProgress.remove(currentThreadID);
                }
                if(threadsInProgress.isEmpty()&&!mazeSolved.get())
                    System.out.println("no solution");
                // else{
                //     System.out.println("no soulution");
                // }
                // if(x==0&&y==0){
                //     System.out.println("Done!");
                // }
                return null;
            }
        };
        mazeSolverWorker.execute();
}

private void moveRat(int x, int y,int currentThreadID) {
    shouldTerminate = false;
    if (!shouldTerminate) {
        if (maze[x][y] != 0) {
            // Encountered a green square (path cell), terminate only the current thread
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            shouldTerminate = true;
            //Thread.currentThread().interrupt();
            return;
        }
            maze[x][y] = currentThreadID+1; // Mark the path
        SwingUtilities.invokeLater(() -> repaint()); // Update GUI on EDT
        try {
                Thread.sleep(200); // Simulate some delay for visualization
        } catch (InterruptedException e) {
            // Handle interruption if needed
            Thread.currentThread().interrupt(); // Preserve the interrupted status
        }
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
    // Provide the thread color based on the cell position or any other criteria
    // For simplicity, we use a random color for each cell
    Random random = new Random();
    int red = random.nextInt(151) + 50;
    int green = random.nextInt(151) + 50;
    int blue = random.nextInt(151) + 50;
    return new Color(red, green, blue);
}

public static void main(String[] args) {
    int N=10;
    SwingUtilities.invokeLater(() -> new MazeSolver(N));
}
}
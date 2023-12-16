import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class MazePanel extends JPanel {
    private int CELL_SIZE = 30;
    private AtomicInteger[][] maze;
    private int N;
    private Map<Integer,Color> threadsData = new ConcurrentHashMap<>();
    MazePanel(int N,AtomicInteger[][] maze,int CELL_SIZE,Map<Integer, Color> threadsData){
        this.N = N;
        this.maze = maze;
        this.CELL_SIZE = CELL_SIZE;
        this.threadsData = threadsData;
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                String str="";
                if(i==0&&j==0||i==N-1&&j==N-1){
                    g.setColor(Color.YELLOW);
                    str = i==0&&j==0? "S":"E";
                }
                else if (maze[i][j].get() == 1) {
                    g.setColor(Color.BLACK); // Blocked cell
                } else if (maze[i][j].get() == 0) {
                    g.setColor(Color.WHITE); // Open cell
                } else {
                    g.setColor(threadsData.get(maze[i][j].get()-1));
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
}

class MazeSolver extends JFrame{
private static final int CELL_SIZE = 30;

private AtomicInteger[][] maze;
private int N;
private SwingWorker<Void, Void> mazeSolverWorker;
private JTextField dimensionsTextField;
private JTextArea mazeInputTextArea;
private JButton solveButton;
private MazePanel mazePanel;
private int currentThreadID=1;
private AtomicInteger otherThreadID = new AtomicInteger(currentThreadID);
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
private volatile boolean isCancelled = false;
MazeSolver() {
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
    initComponents();
}
private void initComponents() {
    JFrame inputFrame = new JFrame();
    inputFrame.setTitle("Input Maze");
    inputFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    inputFrame.setSize(800, 500);
    inputFrame.setLocationRelativeTo(null);

    JPanel mainPanel = new JPanel(new GridBagLayout());
    inputFrame.add(mainPanel);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.EAST;
    mainPanel.add(new JLabel("Enter N:"), gbc);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    dimensionsTextField = new JTextField(5);
    mainPanel.add(dimensionsTextField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    mainPanel.add(new JLabel("Enter the maze elements (1 for blocked cell, 0 for open cell):"), gbc);

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.anchor = GridBagConstraints.WEST;
    mazeInputTextArea = new JTextArea(20, 20);
    mainPanel.add(new JScrollPane(mazeInputTextArea), gbc);

    solveButton = new JButton("Solve Maze");
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.anchor = GridBagConstraints.SOUTHWEST; // Center the button
    mainPanel.add(solveButton, gbc);

    inputFrame.setVisible(true);

    solveButton.addActionListener(e -> onSolveButtonClick());
}
private void onSolveButtonClick() {
    try {
        if (dimensionsTextField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter matrix dimensions.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if(mazeInputTextArea.getText().trim().isEmpty()){
            JOptionPane.showMessageDialog(this, "Please enter matrix values.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        N = Integer.parseInt(dimensionsTextField.getText());
        maze = new AtomicInteger[N][N];

        String[] rows = mazeInputTextArea.getText().split("\n");
        if (rows.length != N || rows[0].split(" ").length!=N) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter valid matrix.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (int i = 0; i < N; i++) {
            String[] cells = rows[i].split(" ");
            for (int j = 0; j < N; j++) {
                maze[i][j] = new AtomicInteger(Integer.parseInt(cells[j]));
            }
        }
        initializeFrame(N);
        isCancelled = false;
        counter = new AtomicInteger(1);
        otherThreadID = new AtomicInteger(currentThreadID);
        solveMaze(0, 0, currentThreadID);
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(this, "Invalid input. Please enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
    }
}

private void initializeFrame(int N) {
    JFrame mazeSolverFrame = new JFrame();
    mazeSolverFrame.setTitle("Rat in a Maze Solver");
    mazeSolverFrame.setSize((N+1) * CELL_SIZE-15, (N+2) * CELL_SIZE-20);
    mazeSolverFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    mazeSolverFrame.setLocationRelativeTo(null);
    mazePanel=new MazePanel(N,maze,CELL_SIZE,threadsData);
    mazeSolverFrame.setContentPane(mazePanel);
    mazeSolverFrame.setVisible(true);
    mazeSolverFrame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            isCancelled = true;
        }
    });
}

private boolean hasPath(AtomicInteger[][] maze, int startX, int startY, int endX, int endY) {
    return dfs(maze, startX, startY, endX, endY);
}

private boolean dfs(AtomicInteger[][] maze, int x, int y, int endX, int endY) {
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

private synchronized boolean isValidMove(int x, int y, AtomicInteger[][] maze) {
    return x < N && y < N && maze[x][y].get() == 0;
}
private synchronized void solveMaze(int x, int y,int currentThreadID) {
    mazeSolverWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                boolean isValdX=isValidMove(x + 1, y, maze);
                boolean isValdY=isValidMove(x, y + 1, maze);
                if (isCancelled) {
                    executorService.shutdownNow();
                    return null;  // Return early if the worker is cancelled
                }
                else if (x == N - 1 && y == N - 1) {
                    executorService.shutdownNow();
                    JOptionPane.showMessageDialog(MazeSolver.this, "Maze Solved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
                else if (isValdX && isValdY) {
                    counter.incrementAndGet();
                    // Continue in the main thread for one direction
                    moveRat(x, y + 1,currentThreadID);
                    solveMaze(x, y + 1,currentThreadID);
                    SwingWorker<Void, Void> threadX = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() {
                            if (isCancelled())
                                return null;  // Check cancellation status periodically
                            
                            synchronized (MazeSolver.this) {
                                otherThreadID.incrementAndGet();
                                if(otherThreadID.get()>9)
                                    threadsData.put(otherThreadID.get(), threadColorForCell());
                                moveRat(x + 1, y,otherThreadID.get());
                                solveMaze(x + 1, y,otherThreadID.get());
                            }
                            return null;
                            }
                        };
                    threadX.execute();
                    try {
                        threadX.get(); // Wait for the thread exploring direction Y to finish
                    } catch (InterruptedException | ExecutionException e) {
                        isCancelled = true;
                        return null;
                    }
                } else if (isValdX) {
                    moveRat(x + 1, y,currentThreadID);
                    solveMaze(x + 1, y,currentThreadID);
                } else if (isValdY) {
                    moveRat(x, y + 1,currentThreadID);
                    solveMaze(x, y + 1,currentThreadID);
                }
                else{
                    counter.decrementAndGet();
                }
                return null;
            }
        };
        mazeSolverWorker.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("state") && evt.getNewValue() == SwingWorker.StateValue.DONE) {
                    if (!hasPath(maze, 0, 0, N - 1, N - 1) && counter.get()==0) {
                        JOptionPane.showMessageDialog(MazeSolver.this, "No solution!", "Failed", JOptionPane.INFORMATION_MESSAGE);
                        counter.incrementAndGet();
                    }
                }
            }
        });
        mazeSolverWorker.execute();
}

private void moveRat(int x, int y,int currentThreadID) {
    synchronized (MazeSolver.this) {
        if (maze[x][y].get() == 0)
            maze[x][y] = new AtomicInteger(currentThreadID+1); // Mark the path
    }
    mazePanel.repaint(); // Update GUI
        try {
                Thread.sleep(200); // Simulate some delay for visualization
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    
    return;
}
private Color threadColorForCell() {
    Random random = new Random();
    int red = random.nextInt(151) + 50;
    int green = random.nextInt(151) + 50;
    int blue = random.nextInt(151) + 50;
    return new Color(red, green, blue);
}

public static void main(String[] args) {
    new MazeSolver();
    }
}
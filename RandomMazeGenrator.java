import java.util.*;

class RandomMazeGenrator{
    private int[][] maze;
    private int N;
    RandomMazeGenrator(int N){
        this.N = N;
        this.maze = randomMaze();
    }
    private int[][] randomMaze(){
        int[][] maze = new int[N][N];
        Random random = new Random();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i == 0 && j == 0||i==N-1 && j==N-1) {
                    continue;
                }
                maze[i][j] = random.nextDouble() < 0.3 ? 1 : 0;
            }
        }
        return maze;
    }

    private void printMaze(){
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print(maze[i][j]+" ");
            }
            System.out.print("\n");
        }
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the number N: ");
        int N = scanner.nextInt();
        RandomMazeGenrator randomMazeGenrator =new RandomMazeGenrator(N);
        randomMazeGenrator.printMaze();
        scanner.close();
    }
}
import mpi.MPI;

public class Main {
  public static void main(String[] args) {
    MPI.Init(args);

    int myrank = MPI.COMM_WORLD.Rank();
    int size = MPI.COMM_WORLD.Size();
    Object[] wrapper = new Object[1];
    
    final int N = 1000; // 
    double[][] A = new double[N][N];
    double[][] B = new double[N][N];
    double[][] C = new double[N][N];
    double[][] Z = new double[N][N];
    // Khởi tạo ma trận A và B chỉ ở rank 0
    if (myrank == 0) {
      System.out.println("Số phần tử của ma trận A: " + N * N);
      System.out.println("Số phần tử của ma trận B: " + N * N);
      int value = 1;
      for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
        	  A[i][j] = value;
              B[i][j] = value;
              value= i+1;
        }
      }

 	 // Tính toán ma trận C từ A và B
 	 // Lưu ý: Mỗi quá trình sẽ tính toán toàn bộ ma trận C trong giả định này,
 	 // nhưng trong thực tế, bạn có thể muốn chia công việc này giữa các quá trình.
 	 for (int i = 0; i < N; i++) {
 	     for (int j = 0; j < N; j++) {
 	         C[i][j] = 0; // Khởi tạo giá trị của C[i][j]
 	         for (int k = 0; k < N; k++) {
 	             C[i][j] += A[i][k] * B[k][j];
 	         }
 	     }
 	 }
 	 wrapper = new Object[1];
     wrapper[0] = C;
    }

    // Ghi lại thời gian bắt đầu
    long startTime = System.currentTimeMillis();

 // master: broadcast X
    MPI.COMM_WORLD.Bcast(wrapper, 0, wrapper.length, MPI.OBJECT, 0);
    // master + workers: receive X
    Z = (double[][]) wrapper[0];
   
    System.out.printf("[R%d]: [%.0f - %.0f]\n", myrank, Z[0][0], Z[N-1][N-1]);

    // master + workers: calculate sum(from,to)
    int from = (myrank == 0) ? 0 : ((Z.length / size) * myrank);
    int to = (myrank == size - 1) ? (Z.length - 1) : ((Z.length / size) * (myrank + 1) - 1);
    double sum = 0;
   
    
    
    // Mỗi rank sẽ tính tổng một phần của C và sau đó gửi tổng phần về rank 0
   
    for (int i = from; i < to; i++) {
      for (int j = 0; j < N; j++) {
        sum += C[i][j];
      }
    }

    // Thu thập tổng từ mỗi rank
    double[] sumOnRank = { sum };
    double[] sumGather = new double[size];
    
    MPI.COMM_WORLD.Gather( sumOnRank, 0, 1, MPI.DOUBLE, sumGather, 0, 1, MPI.DOUBLE, 0);

    // Tính tổng cuối cùng ở rank 0
    if (myrank == 0) {
      double totalSum = 0;
      for (double partialSum : sumGather) {
        totalSum += partialSum;
      }
      System.out.println("Tổng giá trị của ma trận C: " + totalSum);
      
      // Ghi lại thời gian kết thúc và tính thời gian thực thi
      long endTime = System.currentTimeMillis();
      System.out.println("Thời gian bắt đầu: " + startTime + " ms");
      System.out.println("Thời gian kết thúc: " + endTime + " ms");
      System.out.println("Thời gian thực thi: " + (endTime - startTime) + " ms");
      System.out.println("Số phần tử của ma trận C: " + N * N);
    }

    MPI.Finalize();
  }
}

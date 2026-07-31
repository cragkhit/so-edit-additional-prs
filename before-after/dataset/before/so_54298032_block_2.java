    import java.io.FileInputStream;
    import java.io.FileOutputStream;
    import java.io.IOException;
    import java.io.InputStream;
    import java.io.OutputStream;
    import java.io.PipedInputStream;
    import java.io.PipedOutputStream;
    public class SOQ54295781 {
        private static final int BUFF_SIZE = 4096;
        public static void main(final String[] args) throws IOException {
            final PipedOutputStream output = new PipedOutputStream();
            final PipedInputStream input = new PipedInputStream(output);
            Thread thread1 = new Thread(() -> {
                try (FileInputStream fileIn = new FileInputStream("test.pdf")) { //args[0]
                    copy(fileIn, output);
                    fileIn.close();
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                } finally {
                    try {
                        output.close();
                    } catch (IOException ex) {
                        ex.printStackTrace(System.err);
                    }
                }
            });
            Thread thread2 = new Thread(() -> {
                try (FileOutputStream fileOut = new FileOutputStream("test.copy.pdf")) {//args[1]
                    copy(input, fileOut);
                    fileOut.close();
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                } finally {
                    try {
                        input.close();
                    } catch (IOException ex) {
                        ex.printStackTrace(System.err);
                    }
                }
            });
            thread1.start();
            thread2.start();
        }
        private static long copy(InputStream from, OutputStream to)
                throws IOException {
            byte[] buf = new byte[BUFF_SIZE];
            long total = 0;
            while (true) {
                int r = from.read(buf);
                if (r == -1) {
                    break;
                }
                to.write(buf, 0, r);
                total += r;
            }
            return total;
        }
    }
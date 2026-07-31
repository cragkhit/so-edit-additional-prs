    public class Game extends JFrame implements MouseListener, KeyListener {
        CoOrdCounter co;
        Counter cnt;
        int counter = 0;
        ArrayList lines = new ArrayList();
        Point2D.Double start;
        Slider thread;
        Rectangle cow = new Rectangle(0, 0, 10, 10);
    //    boolean drawGuy = false,
        boolean drawGuy = true,
                useFinal = false,
                checkedForWin = false,
                alive = true;
        int finalTime = 0;
        int printX = 0, printY = 0;
        public Game() {
            super("Some Game");
            setSize(700, 700);
            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            addMouseListener(this);
            addKeyListener(this);
            cnt = new Counter();
            cnt.go = false;
            cnt.start();
            co = new CoOrdCounter();
            co.go = false;
            co.start();
        }
        public void paint(Graphics g) {
            super.paint(g);
            if (drawGuy) {
                try {
                    if (useFinal == false) {
                        g.drawString("Current Time: " + counter, 50, 50);
                    } else {
                        g.drawString("Current Time: " + finalTime, 50, 50);
                    }
                    g.drawString("Co-ords: (" + printX + "," + printY + ")", 50, 100);
                } catch (Exception exp) {
                }
                if (!alive) {
                    if (checkedForWin == false) {
                        finalTime = counter;
                        useFinal = true;
                    }
                }
            }
        }
        public void checkWin() {
            if (finalTime >= 45) {
                JOptionPane.showMessageDialog(null, "You won!\nThe farmer got tired and ran back!", "Cowbender I - The Slope", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        }
        @Override
        public void mouseClicked(MouseEvent e) {
        }
        @Override
        public void mousePressed(MouseEvent e) {
        }
        @Override
        public void mouseReleased(MouseEvent e) {
        }
        @Override
        public void mouseEntered(MouseEvent e) {
        }
        @Override
        public void mouseExited(MouseEvent e) {
        }
        @Override
        public void keyTyped(KeyEvent e) {
        }
        @Override
        public void keyPressed(KeyEvent e) {
        }
        @Override
        public void keyReleased(KeyEvent e) {
        }
        class CoOrdCounter extends Thread {
            public boolean go = true;
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(500);
                        printX = cow.x;
                        printY = cow.y;
                        System.out.println(printX + "x" + printY);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                repaint();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        class Counter extends Thread {
            public boolean go = true;
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        counter++;
                    }
                } catch (Exception e) {
                }
            }
        }
        private class Slider extends Thread {
            double velocity, gravity;
            boolean go = false;
            public void run() {
                if (go) {
                    initGuy();
                    velocity = 0;
                    gravity = 1;
                }
                while (go) {
                    try {
                        Line2D.Double lineTaken = null;
                        boolean onLine = false;
                        int firstOnLine = -1;
                        for (int i = lines.size() - 1; i >= 0; i--) {
                            Line2D.Double temp = (Line2D.Double) lines.get(i);
                            if (temp.intersects(cow.x, cow.y, 50, 50)) {
                                lineTaken = temp;
                                onLine = true;
                                if (firstOnLine != i) {
                                    firstOnLine = i;
                                    gravity = 0;
                                }
                                break;
                            }
                        }
                        if (onLine) {
                            double grav = (lineTaken.y2 - lineTaken.y1) / 50;
                            double vlct = (lineTaken.x2 - lineTaken.x1) / 100;
                            if (velocity < 5) {
                                velocity += vlct;
                            }
                            if (gravity < 2.5) {
                                gravity += grav;
                            }
                        } else {
                            gravity += .2;
                        }
                        cow.x += velocity;
                        cow.y += gravity;
                        if (cow.x > 10000) {
                            alive = false;
                        }
                        Thread.sleep(75);
                        repaint();
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            public void action(boolean b) {
                go = b;
            }
            public void initGuy() {
                Line2D.Double firstLine = (Line2D.Double) lines.get(0);
                int x = Integer.parseInt("" + Math.round(firstLine.x1));
                int y = Integer.parseInt("" + Math.round(firstLine.y1));
                cow = new Rectangle(x + 90, y - 60, 50, 50);
                drawGuy = true;
            }
        }
        /**
         * @param args
         */
        public static void main(String[] args) {
            Game g = new Game();
        }
    }
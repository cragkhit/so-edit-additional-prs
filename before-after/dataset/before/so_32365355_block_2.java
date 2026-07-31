	import java.util.Random;
	import java.util.concurrent.LinkedBlockingQueue;
	import java.util.concurrent.CountDownLatch;
	import java.util.concurrent.Semaphore;
	import java.util.concurrent.atomic.AtomicInteger;
	// http://stackoverflow.com/q/32358084/3080094
	public class RandomWell {
		public static void main(String[] args) {
			
			try {
				
				final FilledPool<Integer> pool = new FilledPool<Integer>(100, 1000);
				final CountDownLatch syncStart = new CountDownLatch(3);
				
				Thread consumer = new Thread() {
					@Override public void run() {
						try {
							syncStart.countDown();
							syncStart.await();
							for(;;) {
								pool.take();
								Thread.yield();
							}
						} catch (InterruptedException e) {
							System.out.println("Consumer stopped.");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				consumer.start();
				
				Thread producer = new Thread() {
					@Override public void run() {
						try {
							Random r = new Random();
							syncStart.countDown();
							syncStart.await();
							for(;;) {
								pool.awaitNewFilling();
								int fillTotal = 0;
								while (!pool.isMinFilled()) {
									int fill = pool.getFillSize();
									for (int i = 0; i < fill; i++) {
										pool.offer(r.nextInt());
									}
									fillTotal += fill;
									// System.out.println("Pool size: " + pool.sizeFast());
								}
								System.out.println("Filled " + fillTotal);
							}
						} catch (InterruptedException e) {
							System.out.println("Producer stopped.");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				producer.start();
				syncStart.countDown();
				syncStart.await();
				Thread.sleep(100);
				
				producer.interrupt();
				consumer.interrupt();
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		static class FilledPool<E> {
			
			private final LinkedBlockingQueue<E> pool;
			private final int minSize;
			private final int maxSize;
			private final AtomicInteger fastSize = new AtomicInteger(0);
			private final Semaphore needFilling = new Semaphore(0);
			private volatile boolean filling;
			
			public FilledPool(int minSize, int maxSize) {
				super();
				this.minSize = minSize;
				this.maxSize = maxSize;
				pool = new LinkedBlockingQueue<E>();
			}
			
			public E take() throws InterruptedException {
				
				// ArrayBlockingQueue.size() is slow (uses general lock), use seperate atomic counter
				// filling-check must be done before blocking take-operation in case pool is empty
				if (fastSize.decrementAndGet() < minSize && !filling) {
					filling = true;
					needFilling.release();
				}
				E e = null;
				try {
					e = pool.take();
				} finally {
					if (e == null) {
						fastSize.incrementAndGet();
					}
				}
				return e;
			}
			
			public void offer(E e) {
				
				pool.offer(e);
				fastSize.incrementAndGet();
			}
			
			public void awaitNewFilling() throws InterruptedException {
				
				// must check on minimum in case consumers outpace producer
				if (isMinFilled()) {
					filling = false;
					needFilling.acquire();
				}
			}
			
			public int sizeFast() { return fastSize.get(); }
			public boolean isMinFilled() { return minSize < sizeFast(); }
			public int getFillSize() { return maxSize - sizeFast() - 1; } 
			public boolean isFilling() { return filling; }
		}
	}